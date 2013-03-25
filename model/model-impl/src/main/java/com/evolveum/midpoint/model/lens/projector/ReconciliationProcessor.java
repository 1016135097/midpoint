/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.lens.projector;

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Processor that reconciles the computed account and the real account. There will be some deltas already computed from
 * the other processors. This processor will compare the "projected" state of the account after application of the deltas
 * to the actual (real) account with the result of the mappings. The differences will be expressed as additional 
 * "reconciliation" deltas.
 * 
 * @author lazyman
 * @author Radovan Semancik
 */
@Component
public class ReconciliationProcessor {
	
	@Autowired(required = true)
    private ProvisioningService provisioningService;

	@Autowired(required=true)
	PrismContext prismContext;
	
    public static final String PROCESS_RECONCILIATION = ReconciliationProcessor.class.getName() + ".processReconciliation";
    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationProcessor.class);

    <F extends ObjectType, P extends ObjectType> void processReconciliation(LensContext<F,P> context, LensProjectionContext<P> projectionContext, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processReconciliationUser((LensContext<UserType,AccountShadowType>) context, (LensProjectionContext<AccountShadowType>)projectionContext, result);
    }
    
    void processReconciliationUser(LensContext<UserType,AccountShadowType> context, LensProjectionContext<AccountShadowType> accContext, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    
    OperationResult subResult = result.createSubresult(PROCESS_RECONCILIATION);    
        
        try {
            if (!accContext.isDoReconciliation()) {
            	return;
            }
            
            SynchronizationPolicyDecision policyDecision = accContext.getSynchronizationPolicyDecision();
            if (policyDecision != null && 
            		(policyDecision == SynchronizationPolicyDecision.DELETE || policyDecision == SynchronizationPolicyDecision.UNLINK)) {
            	return;
            }

            if (accContext.getObjectOld() == null) {
                LOGGER.warn("Can't do reconciliation. Account context doesn't contain old version of account.");
                return;
            }
            
            if (!accContext.isFullShadow()) {
            	// We need to load the object
            	PrismObject<AccountShadowType> objectOld = provisioningService.getObject(
            			AccountShadowType.class, accContext.getOid(), null, result);
            	accContext.setObjectOld(objectOld);
            	accContext.setFullShadow(true);
            	accContext.recompute();
            }
            
            LOGGER.trace("Attribute reconciliation processing ACCOUNT {}",accContext.getResourceShadowDiscriminator());

            Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> squeezedAttributes = accContext.getSqueezedAttributes();
            
//                Map<QName, PrismValueDeltaSetTriple<ValueConstruction<?>>> tripleMap = accContext.getAttributeValueDeltaSetTripleMap();
            
            if (squeezedAttributes.isEmpty()) {
            	return;
            }

            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(accContext.getResource(), LayerType.MODEL, 
            		prismContext);
            RefinedObjectClassDefinition accountDefinition = refinedSchema.getAccountDefinition(accContext.getResourceShadowDiscriminator().getIntent());
            
            reconcileAccount(accContext, squeezedAttributes, accountDefinition);
        } catch (RuntimeException e) {
        	subResult.recordFatalError(e);
        	throw e;
        } catch (SchemaException e) {
        	subResult.recordFatalError(e);
        	throw e;
		} finally {
            subResult.computeStatus();
        }
    }

    private void reconcileAccount(LensProjectionContext<AccountShadowType> accCtx,
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> squeezedAttributes, RefinedObjectClassDefinition accountDefinition) throws SchemaException {

    	PrismObject<AccountShadowType> account = accCtx.getObjectNew();

        PrismContainer attributesContainer = account.findContainer(AccountShadowType.F_ATTRIBUTES);
        Collection<QName> attributeNames = MiscUtil.union(squeezedAttributes.keySet(),attributesContainer.getValue().getPropertyNames());

        for (QName attrName: attributeNames) {
        	//LOGGER.trace("Attribute reconciliation processing attribute {}",attrName);
        	RefinedAttributeDefinition attributeDefinition = accountDefinition.getAttributeDefinition(attrName);
        	if (attributeDefinition == null) {
        		throw new SchemaException("No definition for attribute "+attrName+" in "+accCtx.getResourceShadowDiscriminator());
        	}
        	
        	DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> pvwoTriple = squeezedAttributes.get(attrName);
        	Collection<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> shouldBePValues = null;
        	if (pvwoTriple == null) {
        		shouldBePValues = new ArrayList<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>();
        	} else {
        		shouldBePValues = pvwoTriple.getNonNegativeValues();
        	}
        	
        	boolean hasNonInitialShouldBePValue = false;
        	for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePValue: shouldBePValues) {
        		if (shouldBePValue.getMapping() != null && shouldBePValue.getMapping().getStrength() == MappingStrengthType.STRONG) {
        			hasNonInitialShouldBePValue = true;
        			break;
        		}
        	}
        	
        	PrismProperty<?> attribute = attributesContainer.findProperty(attrName);
        	Collection<PrismPropertyValue<Object>> arePValues = null;
        	if (attribute != null) {
        		arePValues = attribute.getValues(Object.class);
        	} else {
        		arePValues = new HashSet<PrismPropertyValue<Object>>();
        	}
        	
        	// Too loud :-)
        	//LOGGER.trace("SHOULD BE:\n{}\nIS:\n{}",shouldBePValues,arePValues);
        	
        	boolean hasValue = false;
        	for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePvwo: shouldBePValues) {
        		Mapping<?> shouldBeMapping = shouldBePvwo.getMapping();
        		if (shouldBeMapping == null) {
        			continue;
        		}
        		if (shouldBeMapping.getStrength() == MappingStrengthType.WEAK && (!arePValues.isEmpty() || hasNonInitialShouldBePValue)) {
        			// "initial" value and the attribute already has a value. Skip it.
        			continue;
        		}
        		Object shouldBeRealValue = shouldBePvwo.getPropertyValue().getValue();
        		if (!isInValues(shouldBeRealValue, arePValues)) {
        			if (attributeDefinition.isSingleValue()) {
	        			if (hasValue) {
	        				throw new SchemaException("Attept to set more than one value for single-valued attribute "+attrName+" in "+accCtx.getResourceShadowDiscriminator());
	        			}
	        			recordDelta(accCtx, attributeDefinition, ModificationType.REPLACE, shouldBeRealValue, shouldBePvwo.getAccountConstruction().getSource());
        			} else {
        				recordDelta(accCtx, attributeDefinition, ModificationType.ADD, shouldBeRealValue, shouldBePvwo.getAccountConstruction().getSource());
        			}
        			hasValue = true;
        		}
        		
        	}
        	
        	if (!attributeDefinition.isTolerant()) {
        		for (PrismPropertyValue<Object> isPValue: arePValues) {
        			if (!isInPvwoValues(isPValue.getValue(), shouldBePValues)) {
        				recordDelta(accCtx, attributeDefinition, ModificationType.DELETE, isPValue.getValue(), null);
        			}
        		}
        	}
        }
    }

	private <T> void recordDelta(LensProjectionContext<AccountShadowType> accCtx, ResourceAttributeDefinition attrDef, 
			ModificationType changeType, T value, ObjectType originObject) throws SchemaException {
		LOGGER.trace("Reconciliation will {} value of attribute {}: {}", new Object[]{changeType, attrDef, value});
		
		PropertyDelta<T> attrDelta = new PropertyDelta<T>(SchemaConstants.PATH_ATTRIBUTES, attrDef.getName(), attrDef);
		PrismPropertyValue<T> pValue = new PrismPropertyValue<T>(value, OriginType.RECONCILIATION, originObject);
		if (changeType == ModificationType.ADD) {
			attrDelta.addValueToAdd(pValue);
		} else if (changeType == ModificationType.DELETE) {
			attrDelta.addValueToDelete(pValue);
		} else if (changeType == ModificationType.REPLACE) {
			attrDelta.setValueToReplace(pValue);
		} else {
			throw new IllegalArgumentException("Unknown change type "+changeType);
		}
		
		accCtx.addToSecondaryDelta(attrDelta);
	}

	private boolean isInValues(Object shouldBeValue, Collection<PrismPropertyValue<Object>> arePValues) {
		if (arePValues == null || arePValues.isEmpty()) {
			return false;
		}
		for (PrismPropertyValue<Object> isPValue: arePValues) {
			if (isPValue.getValue().equals(shouldBeValue)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isInPvwoValues(Object value, Collection<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> shouldBePvwos) {
		for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePvwo: shouldBePvwos) {
			PrismPropertyValue<?> shouldBePPValue = shouldBePvwo.getPropertyValue();
			Object shouldBeValue = shouldBePPValue.getValue();
			if (shouldBeValue.equals(value)) {
				return true;
			}
    	}
		return false;
	}

}
