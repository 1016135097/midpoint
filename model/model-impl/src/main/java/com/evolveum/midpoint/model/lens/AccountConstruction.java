/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author semancik
 *
 */
public class AccountConstruction implements DebugDumpable, Dumpable {

	private AssignmentPath assignmentPath;
	private AccountConstructionType accountConstructionType;
	private ObjectType source;
	private OriginType originType;
	private String channel;
	private ObjectDeltaObject<UserType> userOdo;
	private ResourceType resource;
	private ObjectResolver objectResolver;
	private MappingFactory valueConstructionFactory;
	private Collection<Mapping<? extends PrismPropertyValue<?>>> attributeConstructions;
	private RefinedAccountDefinition refinedAccountDefinition;
	private PrismContext prismContext;
	
	private static final Trace LOGGER = TraceManager.getTrace(AccountConstruction.class);
	
	public AccountConstruction(AccountConstructionType accountConstructionType, ObjectType source) {
		this.accountConstructionType = accountConstructionType;
		this.source = source;
		this.assignmentPath = null;
		this.attributeConstructions = null;
	}

	public void setSource(ObjectType source) {
		this.source = source;
	}
		
	public ObjectType getSource() {
		return source;
	}

	public OriginType getOriginType() {
		return originType;
	}

	public void setOriginType(OriginType originType) {
		this.originType = originType;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public void setUserOdo(ObjectDeltaObject<UserType> userOdo) {
		this.userOdo = userOdo;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	PrismContext getPrismContext() {
		return prismContext;
	}

	void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public MappingFactory getValueConstructionFactory() {
		return valueConstructionFactory;
	}

	public void setValueConstructionFactory(MappingFactory valueConstructionFactory) {
		this.valueConstructionFactory = valueConstructionFactory;
	}

	public String getAccountType() {
		if (refinedAccountDefinition == null) {
			throw new IllegalStateException("Account type can only be fetched from evaluated AccountConstruction");
		}
		return refinedAccountDefinition.getAccountTypeName();
	}
	
	public Object getDescription() {
		return accountConstructionType.getDescription();
	}
	
	public Collection<Mapping<? extends PrismPropertyValue<?>>> getAttributeConstructions() {
		if (attributeConstructions == null) {
			attributeConstructions = new ArrayList<Mapping<? extends PrismPropertyValue<?>>>();
		}
		return attributeConstructions;
	}
	
	public Mapping<? extends PrismPropertyValue<?>> getAttributeConstruction(QName attrName) {
		for (Mapping<? extends PrismPropertyValue<?>> myVc : getAttributeConstructions()) {
			if (myVc.getItemName().equals(attrName)) {
				return myVc;
			}
		}
		return null;
	}
	
	public void addAttributeConstruction(Mapping<? extends PrismPropertyValue<?>> valueConstruction) {
		getAttributeConstructions().add(valueConstruction);
	}

	public boolean containsAttributeConstruction(QName attributeName) {
		for (Mapping<?> myVc: getAttributeConstructions()) {
			if (attributeName.equals(myVc.getItemName())) {
				return true;
			}
		}
		return false;
	}

	public AssignmentPath getAssignmentPath() {
		return assignmentPath;
	}

	public void setAssignmentPath(AssignmentPath assignmentPath) {
		this.assignmentPath = assignmentPath;
	}

	public ResourceType getResource(OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (resource == null) {
			if (accountConstructionType.getResource() != null) {
				resource = accountConstructionType.getResource();
			} else if (accountConstructionType.getResourceRef() != null) {
				try {
					resource = objectResolver.resolve(accountConstructionType.getResourceRef(), ResourceType.class,
							"account construction in "+ source , result);
				} catch (ObjectNotFoundException e) {
					throw new ObjectNotFoundException("Resource reference seems to be invalid in account construction in " + source + ": "+e.getMessage(), e);
				}
			}
			if (resource == null) {
				throw new SchemaException("No resource set in account construction in " + source);
			}
		}
		return resource;
	}
	
	public void evaluate(OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		evaluateAccountType(result);
		evaluateAttributes(result);
	}
	
	private void evaluateAccountType(OperationResult result) throws SchemaException, ObjectNotFoundException {
		String resourceOid = null;
		if (accountConstructionType.getResourceRef() != null) {
			resourceOid = accountConstructionType.getResourceRef().getOid();
		}
		if (accountConstructionType.getResource() != null) {
			resourceOid = accountConstructionType.getResource().getOid();
		}
		if (!getResource(result).getOid().equals(resourceOid)) {
			throw new IllegalStateException("The specified resource and the resource in construction does not match");
		}
		
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(getResource(result), prismContext);
		
		refinedAccountDefinition = refinedSchema.getAccountDefinition(accountConstructionType.getIntent());
		
		if (refinedAccountDefinition == null) {
			if (accountConstructionType.getIntent() != null) {
				throw new SchemaException("No account type '"+accountConstructionType.getIntent()+"' found in "+ObjectTypeUtil.toShortString(getResource(result))+" as specified in account construction in "+ObjectTypeUtil.toShortString(source));
			} else {
				throw new SchemaException("No default account type found in "+ObjectTypeUtil.toShortString(getResource(result))+" as specified in account construction in "+ObjectTypeUtil.toShortString(source));
			}
		}
	}

	private void evaluateAttributes(OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		attributeConstructions = new ArrayList<Mapping<? extends PrismPropertyValue<?>>>();
//		LOGGER.trace("Assignments used for account construction for {} ({}): {}", new Object[]{this.resource,
//				assignments.size(), assignments});
		for (ResourceAttributeDefinitionType attribudeDefinitionType : accountConstructionType.getAttribute()) {
			QName attrName = attribudeDefinitionType.getRef();
			if (attrName == null) {
				throw new SchemaException("No attribute name (ref) in attribute definition in account construction in "+source);
			}
			if (!attribudeDefinitionType.getInbound().isEmpty()) {
				throw new SchemaException("Cannot process inbound section in definition of attribute "+attrName+" in account construction in "+source);
			}
			MappingType outboundMappingType = attribudeDefinitionType.getOutbound();
			if (outboundMappingType == null) {
				throw new SchemaException("No oubound section in definition of attribute "+attrName+" in account construction in "+source);
			}
			Mapping<? extends PrismPropertyValue<?>> attributeConstruction = evaluateAttribute(attribudeDefinitionType, result);
			if (attributeConstruction != null) {
				attributeConstructions.add(attributeConstruction);
			}
		}
	}

	private Mapping<? extends PrismPropertyValue<?>> evaluateAttribute(ResourceAttributeDefinitionType attribudeDefinitionType,
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		QName attrName = attribudeDefinitionType.getRef();
		if (attrName == null) {
			throw new SchemaException("Missing 'ref' in attribute construction in account construction in "+ObjectTypeUtil.toShortString(source));
		}
		if (!attribudeDefinitionType.getInbound().isEmpty()) {
			throw new SchemaException("Cannot process inbound section in definition of attribute "+attrName+" in account construction in "+source);
		}
		MappingType outboundMappingType = attribudeDefinitionType.getOutbound();
		if (outboundMappingType == null) {
			throw new SchemaException("No onbound section in definition of attribute "+attrName+" in account construction in "+source);
		}
		PrismPropertyDefinition outputDefinition = findAttributeDefinition(attrName);
		if (outputDefinition == null) {
			throw new SchemaException("Attribute "+attrName+" not found in schema for account type "+getAccountType()+", "+ObjectTypeUtil.toShortString(getResource(result))+" as definied in "+ObjectTypeUtil.toShortString(source), attrName);
		}
		Mapping<? extends PrismPropertyValue<?>> mapping = valueConstructionFactory.createMapping(outboundMappingType,
				"for attribute " + PrettyPrinter.prettyPrint(attrName)  + " in "+source);
		
		if (!mapping.isApplicableToChannel(channel)) {
			return null;
		}
		
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
		mapping.setSourceContext(userOdo);
		mapping.setRootNode(userOdo);
		mapping.setDefaultTargetDefinition(outputDefinition);
		mapping.setOriginType(originType);
		mapping.setOriginObject(source);
		if (!assignmentPath.isEmpty()) {
			AssignmentType assignmentType = assignmentPath.getFirstAssignment();
			mapping.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, assignmentType.asPrismContainerValue());
		}
		// TODO: other variables ?
		
		// Set condition masks. There are used as a brakes to avoid evaluating to nonsense values in case user is not present
		// (e.g. in old values in ADD situations and new values in DELETE situations).
		if (userOdo.getOldObject() == null) {
			mapping.setConditionMaskOld(false);
		}
		if (userOdo.getNewObject() == null) {
			mapping.setConditionMaskNew(false);
		}

		mapping.evaluate(result);
		
		LOGGER.trace("Evaluated mapping for "+attrName+": "+mapping);
		return mapping;
	}

	private PrismObjectDefinition<?> getUserDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}

	private ResourceAttributeDefinition findAttributeDefinition(QName attributeName) {
		return refinedAccountDefinition.getObjectClassDefinition().findAttributeDefinition(attributeName);
	}
	
	public boolean hasValueForAttribute(QName attributeName) {
		for (Mapping<? extends PrismPropertyValue<?>> attributeConstruction: attributeConstructions) {
			if (attributeName.equals(attributeConstruction.getItemName())) {
				PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> outputTriple = attributeConstruction.getOutputTriple();
				if (outputTriple != null && !outputTriple.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assignmentPath == null) ? 0 : assignmentPath.hashCode());
		result = prime * result + ((attributeConstructions == null) ? 0 : attributeConstructions.hashCode());
		result = prime * result
				+ ((refinedAccountDefinition == null) ? 0 : refinedAccountDefinition.hashCode());
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccountConstruction other = (AccountConstruction) obj;
		if (assignmentPath == null) {
			if (other.assignmentPath != null)
				return false;
		} else if (!assignmentPath.equals(other.assignmentPath))
			return false;
		if (attributeConstructions == null) {
			if (other.attributeConstructions != null)
				return false;
		} else if (!attributeConstructions.equals(other.attributeConstructions))
			return false;
		if (refinedAccountDefinition == null) {
			if (other.refinedAccountDefinition != null)
				return false;
		} else if (!refinedAccountDefinition.equals(other.refinedAccountDefinition))
			return false;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	@Override
	public String dump() {
		return debugDump();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append("AccountConstruction(");
		if (refinedAccountDefinition == null) {
			sb.append("null");
		} else {
			sb.append(refinedAccountDefinition.getResourceAccountType());
		}
		sb.append(")");
		if (attributeConstructions != null) {
			for (Mapping attrConstr: attributeConstructions) {
				sb.append("\n");
				sb.append(attrConstr.debugDump(indent+1));
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "AccountConstruction(" + attributeConstructions + ")";
	}
	
}
