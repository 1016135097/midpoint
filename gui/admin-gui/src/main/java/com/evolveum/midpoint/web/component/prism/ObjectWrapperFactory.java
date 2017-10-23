/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.prism;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.refinery.CompositeRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

/**
 * @author Viliam Repan (lazyman)
 */
public class ObjectWrapperFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectWrapperFactory.class);

    private static final String DOT_CLASS = ObjectWrapperFactory.class.getName() + ".";
    private static final String CREATE_CONTAINERS = DOT_CLASS + "createContainers";
    private static final String CREATE_OBJECT_WRAPPER = DOT_CLASS + "createObjectWrapper";

    private static final List<QName> CONTAINERS_TO_IGNORE = Arrays.asList(
    		 SubjectedObjectSelectorType.COMPLEX_TYPE,
    		    TriggerType.COMPLEX_TYPE,
    		    OperationExecutionType.COMPLEX_TYPE,
    		    ApprovalSchemaType.COMPLEX_TYPE,
    		    PasswordHistoryEntryType.COMPLEX_TYPE,
    		    SecurityQuestionsCredentialsType.COMPLEX_TYPE,
    		    NonceType.COMPLEX_TYPE);

    private ModelServiceLocator modelServiceLocator;

    private OperationResult result;

    public ObjectWrapperFactory(ModelServiceLocator modelServiceLocator) {
        Validate.notNull(modelServiceLocator, "Service locator must not be null");

        this.modelServiceLocator = modelServiceLocator;
    }

    public OperationResult getResult() {
        return result;
    }

    public <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName,
            String description, PrismObject<O> object, ContainerStatus status, Task task) {
        return createObjectWrapper(displayName, description, object, status, AuthorizationPhaseType.REQUEST, task);
    }

    public <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName, String description,
			PrismObject<O> object, ContainerStatus status, AuthorizationPhaseType authorizationPhase, Task task) {
        if (authorizationPhase == null) {
            authorizationPhase = AuthorizationPhaseType.REQUEST;
        }
        try {
            OperationResult result = task.getResult();

            PrismObjectDefinition<O> objectDefinitionForEditing = modelServiceLocator.getModelInteractionService()
                    .getEditObjectDefinition(object, authorizationPhase, task, result);
            if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Edit definition for {}:\n{}", object, objectDefinitionForEditing.debugDump(1));
            }
            
            if (objectDefinitionForEditing != null) {
            	object.setDefinition(objectDefinitionForEditing);
            }
            
            RefinedObjectClassDefinition objectClassDefinitionForEditing = null;
            if (isShadow(object)) {
                PrismReference resourceRef = object.findReference(ShadowType.F_RESOURCE_REF);
                PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
                Validate.notNull(resource, "No resource object in the resourceRef");
                objectClassDefinitionForEditing = modelServiceLocator.getModelInteractionService().getEditObjectClassDefinition(
                        (PrismObject<ShadowType>) object, resource, authorizationPhase, task, result);
                if (objectClassDefinitionForEditing != null) {
                	object.findOrCreateContainer(ShadowType.F_ATTRIBUTES).applyDefinition((PrismContainerDefinition) objectClassDefinitionForEditing.toResourceAttributeContainerDefinition());;
                }
                
            }
            return createObjectWrapper(displayName, description, object, objectDefinitionForEditing,
                    objectClassDefinitionForEditing, status, result);
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | SecurityViolationException ex) {
            throw new SystemException(ex);
        }
    }

    public <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName,
			String description, PrismObject<O> object, PrismObjectDefinition<O> objectDefinitionForEditing,
			RefinedObjectClassDefinition objectClassDefinitionForEditing, ContainerStatus status) {
        return createObjectWrapper(displayName, description, object, objectDefinitionForEditing,
                objectClassDefinitionForEditing, status, null);
    }

    private <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName,
			String description, PrismObject<O> object, PrismObjectDefinition<O> objectDefinitionForEditing,
			RefinedObjectClassDefinition objectClassDefinitionForEditing, ContainerStatus status,
			OperationResult result) {

        if (result == null) {
            this.result = new OperationResult(CREATE_OBJECT_WRAPPER);
        } else {
            this.result = result;
        }
        
		ObjectWrapper<O> objectWrapper = new ObjectWrapper<>(displayName, description, object,
				status);

		List<ContainerWrapper<? extends Containerable>> containerWrappers = createContainerWrappers(objectWrapper, object,
				objectDefinitionForEditing, status, this.result);
		objectWrapper.setContainers(containerWrappers);

        this.result.computeStatusIfUnknown();

        objectWrapper.setResult(this.result);

        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Created object wrapper:\n{}", objectWrapper.debugDump());
        }

        return objectWrapper;
    }

    private <O extends ObjectType> List<ContainerWrapper<? extends Containerable>> createContainerWrappers(ObjectWrapper<O> oWrapper,
			PrismObject<O> object, PrismObjectDefinition<O> objectDefinitionForEditing, ContainerStatus cStatus, OperationResult pResult) {
        OperationResult result = pResult.createSubresult(CREATE_CONTAINERS);

        List<ContainerWrapper<? extends Containerable>> containerWrappers = new ArrayList<>();

        ContainerWrapperFactory cwf = new ContainerWrapperFactory(modelServiceLocator);
        try {
//            Class<O> clazz = object.getCompileTimeClass();
//            if (ShadowType.class.isAssignableFrom(clazz)) {
//            	addShadowContainers(containerWrappers, oWrapper, object, objectDefinitionForEditing, cwf, cStatus, result);
//            } else if (ResourceType.class.isAssignableFrom(clazz)) { //TODO: is this even used???
//                addResourceContainers(containerWrappers, oWrapper, object, result);
//            } else if (ReportType.class.isAssignableFrom(clazz)) { //TODO is this even used???
//                addReportContainers(containerWrappers, oWrapper, object, result);
//            } else {
                ContainerWrapper<O> mainContainerWrapper = cwf.createContainerWrapper(object, cStatus, ItemPath.EMPTY_PATH);
                mainContainerWrapper.setDisplayName("prismContainer.mainPanelDisplayName");
                result.addSubresult(cwf.getResult());
                containerWrappers.add(mainContainerWrapper);

                addContainerWrappers(containerWrappers, oWrapper, object, null, result);
//            }
        } catch (SchemaException | RuntimeException e) {
            //TODO: shouldn't be this exception thrown????
            LoggingUtils.logUnexpectedException(LOGGER, "Error occurred during container wrapping", e);
            result.recordFatalError("Error occurred during container wrapping, reason: " + e.getMessage(),
                    e);
        }

        containerWrappers.sort(new ItemWrapperComparator());
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        return containerWrappers;
    }

    private <O extends ObjectType> PrismObjectDefinition<O> getDefinition(PrismObject<O> object,
    		PrismObjectDefinition<O> objectDefinitionForEditing) {
        if (objectDefinitionForEditing != null) {
            return objectDefinitionForEditing;
        }
        return object.getDefinition();
    }

    private <O extends ObjectType> List<ContainerWrapper<? extends Containerable>> createCustomContainerWrapper(
    															ObjectWrapper<O> oWrapper, PrismObject<O> object,
                                                                PrismObjectDefinition<O> objectDefinitionForEditing,
                                                                QName name, OperationResult result) throws SchemaException {
        PrismContainer container = object.findContainer(name);
        ContainerStatus status = container == null ? ContainerStatus.ADDING : ContainerStatus.MODIFYING;
        List<ContainerWrapper<? extends Containerable>> list = new ArrayList<>();
        if (container == null) {
            PrismContainerDefinition<?> definition = getDefinition(object, objectDefinitionForEditing).findContainerDefinition(name);
            container = definition.instantiate();
        }

        ContainerWrapperFactory cwf = new ContainerWrapperFactory(modelServiceLocator);
        ContainerWrapper wrapper = cwf.createContainerWrapper(container, status, new ItemPath(name));
        result.addSubresult(cwf.getResult());
        list.add(wrapper);
        if (!ShadowType.F_ASSOCIATION.equals(name)) {
            addContainerWrappers(list, oWrapper, container, new ItemPath(name), result);
        }

        return list;
    }

	private <O extends ObjectType, C extends Containerable> void addContainerWrappers(
			List<ContainerWrapper<? extends Containerable>> containerWrappers, ObjectWrapper<O> oWrapper,
			PrismContainer<C> parentContainer, ItemPath path, OperationResult result) throws SchemaException {

		PrismContainerDefinition<C> parentContainerDefinition = parentContainer.getDefinition();

		List<ItemPathSegment> segments = new ArrayList<>();
		if (path != null) {
			segments.addAll(path.getSegments());
		}
		ItemPath parentPath = new ItemPath(segments);
		for (ItemDefinition def : (Collection<ItemDefinition>) parentContainerDefinition.getDefinitions()) {
			if (!(def instanceof PrismContainerDefinition)) {
				continue;
			}
			if (isIgnoreContainer(def.getTypeName())) {
				continue;
			}

			LOGGER.trace("ObjectWrapper.createContainerWrapper processing definition: {}", def);

			PrismContainerDefinition<?> containerDef = (PrismContainerDefinition) def;

			ItemPath newPath = createPropertyPath(parentPath, containerDef.getName());

			ContainerWrapperFactory cwf = new ContainerWrapperFactory(modelServiceLocator);

			PrismContainer prismContainer = parentContainer.findContainer(def.getName());
			
			ContainerWrapper<C>  container = createContainerWrapper(oWrapper.getObject(), prismContainer, containerDef, cwf, newPath);
			result.addSubresult(cwf.getResult());
			if (container != null) {
				containerWrappers.add(container);
			}
				
		}
	}
	
	private <O extends ObjectType, C extends Containerable> ContainerWrapper<C> createContainerWrapper(PrismObject<O> object, PrismContainer<C> prismContainer, PrismContainerDefinition<C> containerDef, ContainerWrapperFactory cwf, ItemPath newPath) throws SchemaException{
		if (ShadowAssociationType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
			ObjectType objectType = object.asObjectable();
			ShadowType shadow = null;
			if (objectType instanceof ShadowType) {
				shadow = (ShadowType) objectType;
			} else {
				throw new SchemaException("Something very strange happenned. Association contianer in the " + objectType.getClass().getSimpleName() + "?");
			}
			Task task = modelServiceLocator.createSimpleTask("Load resource ref");
			//TODO: is it safe to case modelServiceLocator to pageBase?
			PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(shadow.getResourceRef(), (PageBase) modelServiceLocator, task, result);
			
			result.computeStatusIfUnknown();
			if (!result.isAcceptable()) {
				LOGGER.error("Cannot find resource referenced from shadow. {}", result.getMessage());
				result.recordPartialError("Could not find resource referenced from shadow.");
				return null;
			}
			
			ContainerWrapper<C> container;
			if (prismContainer != null) {
				return (ContainerWrapper<C>) cwf.createAssociationWrapper(resource, shadow.getKind(), shadow.getIntent(), (PrismContainer<ShadowAssociationType>) prismContainer, ContainerStatus.MODIFYING, newPath);
			}
			prismContainer = containerDef.instantiate();
			return (ContainerWrapper<C>) cwf.createAssociationWrapper(resource, shadow.getKind(), shadow.getIntent(), (PrismContainer<ShadowAssociationType>) prismContainer, ContainerStatus.ADDING, newPath);
			
		}

		ContainerWrapper<C> container;
		if (prismContainer != null) {
			return cwf.createContainerWrapper(prismContainer, ContainerStatus.MODIFYING, newPath);
		}
		
		prismContainer = containerDef.instantiate();
		return cwf.createContainerWrapper(prismContainer, ContainerStatus.ADDING, newPath);
		
	}
	
	 private ObjectFilter getAssociationsSearchFilter(PrismObject<ShadowType> shadow) {
		 
		 Map<QName, PrismContainer<ShadowAssociationType>> assocMap = new HashMap<>();
         PrismContainer<ShadowAssociationType> associationContainer = shadow.findContainer(ShadowType.F_ASSOCIATION);
     	if (associationContainer != null && associationContainer.getValues() != null) {
	            // Do NOT load shadows here. This will be huge overhead if there are many associations.
	        	// Load them on-demand (if necessary at all).
	            List<PrismContainerValue<ShadowAssociationType>> associations = associationContainer.getValues();
	            if (associations != null) {
	                for (PrismContainerValue<ShadowAssociationType> cval : associations) {
	                    ShadowAssociationType associationType = cval.asContainerable();
	                    QName assocName = associationType.getName();
	                    PrismContainer<ShadowAssociationType> fractionalContainer = assocMap.get(assocName);
	                    if (fractionalContainer == null) {
	                        fractionalContainer = new PrismContainer<>(ShadowType.F_ASSOCIATION, ShadowAssociationType.class, cval.getPrismContext());
	                        fractionalContainer.setDefinition(cval.getParent().getDefinition());
	                        // HACK: set the name of the association as the element name so wrapper.getName() will return correct data.
	                        fractionalContainer.setElementName(assocName);
	                        assocMap.put(assocName, fractionalContainer);
	                    }
	                    try {
	                        fractionalContainer.add(cval.clone());
	                    } catch (SchemaException e) {
	                        // Should not happen
	                        throw new SystemException("Unexpected error: " + e.getMessage(), e);
	                    }
	                }
	            }
         }

         PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
         PrismObject<ResourceType> resource = resourceRef.getValue().getObject();

         // HACK. The revive should not be here. Revive is no good. The next use of the resource will
         // cause parsing of resource schema. We need some centralized place to maintain live cached copies
         // of resources.
         try {
             resource.revive(modelServiceLocator.getPrismContext());
         } catch (SchemaException e) {
             throw new SystemException(e.getMessage(), e);
         }
         RefinedResourceSchema refinedSchema;
         CompositeRefinedObjectClassDefinition rOcDef;
         try {
             refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
             rOcDef = refinedSchema.determineCompositeObjectClassDefinition(shadow);
         } catch (SchemaException e) {
             throw new SystemException(e.getMessage(), e);
         }
         // Make sure even empty associations have their wrappers so they can be displayed and edited
//         for (RefinedAssociationDefinition assocDef : rOcDef.getAssociationDefinitions()) {
//             QName name = assocDef.getName();
//             if (!assocMap.containsKey(name)) {
//                 PrismContainer<ShadowAssociationType> fractionalContainer = new PrismContainer<>(ShadowType.F_ASSOCIATION, ShadowAssociationType.class, prismContext);
//                 fractionalContainer.setDefinition(cWrapper.getItemDefinition());
//                 // HACK: set the name of the association as the element name so wrapper.getName() will return correct data.
//                 fractionalContainer.setElementName(name);
//                 assocMap.put(name, fractionalContainer);
//             }
//         }

         List<ObjectFilter> associatonFilters = new ArrayList<>();
         
         ShadowType shadowType = shadow.asObjectable();
         rOcDef.getAssociationDefinitions().forEach(def -> {
        	 RefinedObjectClassDefinition assocDef = def.getAssociationTarget();
        	 associatonFilters.add(createAssociationFilter(shadowType.getResourceRef().getOid(), assocDef.getKind(), assocDef.getObjectClassDefinition().getTypeName()));
         });
         
         return OrFilter.createOr(associatonFilters);
	        
	    }
	  
	 private ObjectFilter createAssociationFilter(String resourceOid, ShadowKindType kind, QName objectClass) {
		 return QueryBuilder.queryFor(ShadowType.class, modelServiceLocator.getPrismContext())
	                .item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
	                .and().item(ShadowType.F_KIND).eq(kind)
	                .and().item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
	                .buildFilter();
	 }

    private boolean isIgnoreContainer(QName containerDefinitionName) {
    	 for (QName container : CONTAINERS_TO_IGNORE) {
    		 if (container.equals(containerDefinitionName)){
    			 return true;
    		 }
    	 }

    	 return false;
    }

    private boolean hasResourceCapability(ResourceType resource,
                                          Class<? extends CapabilityType> capabilityClass) {
        if (resource == null) {
            return false;
        }
        return ResourceTypeUtil.hasEffectiveCapability(resource, capabilityClass);
    }

    private <O extends ObjectType> void addResourceContainerWrapper(
    																List<ContainerWrapper<? extends Containerable>> containerWrappers,
    																ObjectWrapper<O> oWrapper, PrismObject<O> object,
                                                                  PrismObject<ConnectorType> connector,
                                                                  OperationResult result) throws SchemaException {

        PrismContainer<ConnectorConfigurationType> container = object.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);

        ConnectorType connectorType = connector.asObjectable();
        PrismSchema schema = ConnectorTypeUtil.parseConnectorSchema(connectorType,
                connector.getPrismContext());
        PrismContainerDefinition<ConnectorConfigurationType> definition = ConnectorTypeUtil.findConfigurationContainerDefinition(
                connectorType, schema);

		// brutal hack - the definition has (errorneously) set maxOccurs =
		// unbounded. But there can be only one configuration container.
		// See MID-2317 and related issues
		PrismContainerDefinition<ConnectorConfigurationType> definitionFixed = definition.clone();
        ((PrismContainerDefinitionImpl) definitionFixed).setMaxOccurs(1);

		if (container == null) {
            container = definitionFixed.instantiate();
        } else {
			container.setDefinition(definitionFixed);
		}

        addContainerWrappers(containerWrappers, oWrapper, container, new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION), result);
    }

//    private <O extends ObjectType> void addShadowContainers(
//    		List<ContainerWrapper<? extends Containerable>> containers,
//    		ObjectWrapper<O> oWrapper, PrismObject<O> object, PrismObjectDefinition<O> objectDefinitionForEditing,
//			ContainerWrapperFactory cwf, ContainerStatus cStatus,
//            OperationResult result) throws SchemaException {
//
//    	PrismContainer attributesContainer = object.findContainer(ShadowType.F_ATTRIBUTES);
//        ContainerStatus status = attributesContainer != null ? cStatus : ContainerStatus.ADDING;
//        if (attributesContainer == null) {
//            PrismContainerDefinition<?> definition = object.getDefinition().findContainerDefinition(
//                    ShadowType.F_ATTRIBUTES);
//            attributesContainer = definition.instantiate();
//        }
//        
//        
//
//        ContainerWrapper attributesContainerWrapper = cwf.createContainerWrapper(attributesContainer, status,
//                new ItemPath(ShadowType.F_ATTRIBUTES));
//        result.addSubresult(cwf.getResult());
//
////        attributesContainerWrapper.setMain(true);
//        attributesContainerWrapper.setDisplayName("prismContainer.shadow.mainPanelDisplayName");
//        containers.add(attributesContainerWrapper);
//
//        if (hasResourceCapability(((ShadowType) object.asObjectable()).getResource(),
//                ActivationCapabilityType.class)) {
//            containers
//                    .addAll(createCustomContainerWrapper(oWrapper, object, objectDefinitionForEditing, ShadowType.F_ACTIVATION, result));
//        }
//        if (hasResourceCapability(((ShadowType) object.asObjectable()).getResource(),
//                CredentialsCapabilityType.class)) {
//        	containers
//                    .addAll(createCustomContainerWrapper(oWrapper, object, objectDefinitionForEditing, ShadowType.F_CREDENTIALS, result));
//        }
//
//        PrismContainer<ShadowAssociationType> associationContainer = object.findOrCreateContainer(ShadowType.F_ASSOCIATION);
//        attributesContainerWrapper = cwf.createContainerWrapper(associationContainer, ContainerStatus.MODIFYING,
//                new ItemPath(ShadowType.F_ASSOCIATION));
//        result.addSubresult(cwf.getResult());
//        containers.add(attributesContainerWrapper);
//}

//    private <O extends ObjectType> void addResourceContainers(
//    														List<ContainerWrapper<? extends Containerable>> containers,
//    														ObjectWrapper<O> oWrapper, PrismObject<O> object,
//                                                            OperationResult result) throws SchemaException {
//        PrismObject<ConnectorType> connector = loadConnector(object);
//        if (connector != null) {
//            addResourceContainerWrapper(containers, oWrapper, object, connector, result);
//        }
//    }
//
//    private <O extends ObjectType> void addReportContainers(
//    														List<ContainerWrapper<? extends Containerable>> containers,
//    														ObjectWrapper<O> oWrapper, PrismObject<O> object,
//    														OperationResult result) throws SchemaException {
//        PrismContainer container = object.findContainer(ReportType.F_CONFIGURATION);
//        ContainerStatus status = container != null ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
//
//        if (container == null) {
//            PrismSchema schema = ReportTypeUtil.parseReportConfigurationSchema(
//                    (PrismObject<ReportType>) object, object.getPrismContext());
//            PrismContainerDefinition<?> definition = ReportTypeUtil.findReportConfigurationDefinition(schema);
//            if (definition == null) {
//                return;
//            }
//            container = definition.instantiate();
//        }
//        ContainerWrapperFactory cwf = new ContainerWrapperFactory(modelServiceLocator);
//        ContainerWrapper wrapper = cwf.createContainerWrapper(container, status, new ItemPath(ReportType.F_CONFIGURATION));
//        result.addSubresult(cwf.getResult());
//
//        containers.add(wrapper);
//    }
//
//    private PrismObject<ConnectorType> loadConnector(PrismObject object) {
//        PrismReference connectorRef = object.findReference(ResourceType.F_CONNECTOR_REF);
//        return connectorRef != null ? (connectorRef.getValue() != null ? connectorRef.getValue().getObject() : null) : null;
//        // todo reimplement
//    }

    private ItemPath createPropertyPath(ItemPath path, QName element) {
       return path.append(element);
    }

    private boolean isShadow(PrismObject object) {
        return (object.getCompileTimeClass() != null && ShadowType.class.isAssignableFrom(object
                .getCompileTimeClass()))
                || (object.getDefinition() != null && object.getDefinition().getName()
                .equals(ShadowType.COMPLEX_TYPE));
    }
}
