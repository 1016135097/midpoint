/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.importer;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.List;

/**
 * Extension of validator used to import objects to the repository.
 * <p/>
 * In addition to validating the objects the importer also tries to resolve the
 * references and may also do other repository-related stuff.
 *
 * @author Radovan Semancik
 */
@Component
public class ObjectImporter {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectImporter.class);
    private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName()
            + ".resolveReference";

    @Autowired(required = true)
    private Protector protector;
    @Autowired(required = true)
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired(required = true)
    private PrismContext prismContext;
    @Autowired(required = true)
    private TaskManager taskManager;

    public void importObjects(InputStream input, final ImportOptionsType options, final Task task, final OperationResult parentResult,
                              final RepositoryService repository) {

        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <T extends Objectable> EventResult postMarshall(PrismObject<T> prismObjectObjectable, Element objectElement, OperationResult objectResult) {
                LOGGER.debug("Importing object {}", prismObjectObjectable);
                
                T objectable = prismObjectObjectable.asObjectable();
                if (!(objectable instanceof ObjectType)) {
                	String message = "Cannot process type "+objectable.getClass()+" as it is not a subtype of "+ObjectType.class;
                	objectResult.recordFatalError(message);
                    LOGGER.error("Import of object {} failed: {}",
                            new Object[]{prismObjectObjectable, message});
                    return EventResult.skipObject();
                }
                ObjectType objectType = (ObjectType)objectable;
                PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) prismObjectObjectable;
                
                if (LOGGER.isTraceEnabled()) {
                	LOGGER.trace("IMPORTING object:\n{}", object.dump());
                }
                
                resolveReferences(object, repository, 
                		options.isReferentialIntegrity() == null ? false : options.isReferentialIntegrity(), objectResult);
                
                objectResult.computeStatus();
                if (!objectResult.isAcceptable()) {
                	return EventResult.skipObject(objectResult.getMessage());
                }
                
                generateIdentifiers(object, repository,  objectResult);
                
                objectResult.computeStatus();
                if (!objectResult.isAcceptable()) {
                	return EventResult.skipObject(objectResult.getMessage());
                }

                if (BooleanUtils.isTrue(options.isValidateDynamicSchema())) {
                    validateWithDynamicSchemas(object, objectElement, repository, objectResult);
                }

                objectResult.computeStatus();
                if (!objectResult.isAcceptable()) {
                	return EventResult.skipObject(objectResult.getMessage());
                }
                
                if (BooleanUtils.isTrue(options.isEncryptProtectedValues())) {
                    encryptValues(object, objectResult);
                }

                objectResult.computeStatus();
                if (!objectResult.isAcceptable()) {
                	return EventResult.skipObject(objectResult.getMessage());
                }
                
                try {

                    importObjectToRepository(object, options, repository, objectResult);

                    LOGGER.info("Imported object {}", object);

                } catch (SchemaException e) {
                    objectResult.recordFatalError("Schema violation: "+e.getMessage(), e);
                    LOGGER.error("Import of object {} failed: Schema violation: {}",
                            new Object[]{object, e.getMessage(), e});
                } catch (ObjectAlreadyExistsException e) {
                	objectResult.recordFatalError("Object already exists: "+e.getMessage(), e);
                    LOGGER.error("Import of object {} failed: Object already exists: {}",
                            new Object[]{object, e.getMessage(), e});
                    LOGGER.error("Object already exists", e);
                } catch (RuntimeException e) {
                    objectResult.recordFatalError("Unexpected problem: "+e.getMessage(), e);
                    LOGGER.error("Import of object {} failed: Unexpected problem: {}",
                            new Object[]{object, e.getMessage(), e});
                }

                objectResult.recordSuccessIfUnknown();
                if (objectResult.isAcceptable()) {
                    // Continue import
                    return EventResult.cont();
                } else {
                    return EventResult.skipObject(objectResult.getMessage());
                }
            }

			@Override
            public void handleGlobalError(OperationResult currentResult) {
                // No reaction
            }

        };

        Validator validator = new Validator(prismContext, handler);
        validator.setVerbose(true);
        validator.setValidateSchema(BooleanUtils.isTrue(options.isValidateStaticSchema()));
        if (options.getStopAfterErrors() != null) {
            validator.setStopAfterErrors(options.getStopAfterErrors().longValue());
        }
        if (options.isSummarizeErrors()) {
        	parentResult.setSummarizeErrors(true);
        }
        if (options.isSummarizeSucceses()) {
        	parentResult.setSummarizeSuccesses(true);
        }

        validator.validate(input, parentResult, OperationConstants.IMPORT_OBJECT);

    }

    private <T extends ObjectType> void importObjectToRepository(PrismObject<T> object, ImportOptionsType options, RepositoryService repository,
                                          OperationResult objectResult) throws SchemaException, ObjectAlreadyExistsException {

        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".importObjectToRepository");

        try {

            repository.addObject(object, result);
            if (object.canRepresent(TaskType.class))
            	taskManager.onTaskCreate(object.getOid(), result);
            result.recordSuccess();

        } catch (ObjectAlreadyExistsException e) {
            if (BooleanUtils.isTrue(options.isOverwrite())) {
                // Try to delete conflicting object
            	result.muteLastSubresultError();
                String deletedOid = deleteObject(object, repository, result);
                if (deletedOid != null) {
                    if (object.canRepresent(TaskType.class))
                    	taskManager.onTaskDelete(deletedOid, result);
                    if (BooleanUtils.isTrue(options.isKeepOid())) {
                        object.setOid(deletedOid);
                    }
                    repository.addObject(object, result);
                    if (object.canRepresent(TaskType.class))
                    	taskManager.onTaskCreate(object.getOid(), result);
                    result.recordSuccess();
                } else {
                    // cannot delete, throw original exception
                    result.recordFatalError("Object already exists, cannot overwrite", e);
                    throw e;
                }
            } else {
                result.recordFatalError("Object already exists", e);
                throw e;
            }
        }
    }


    /**
     * @return OID of the deleted object or null (if nothing was deleted)
     */
    private <T extends ObjectType> String deleteObject(PrismObject<T> object, RepositoryService repository, OperationResult objectResult) throws SchemaException {
        if (!StringUtils.isBlank(object.getOid())) {
            // The conflict is either UID or we should not proceed as we could delete wrong object
            try {
                repository.deleteObject(object.getCompileTimeClass(), object.getOid(), objectResult);
            } catch (ObjectNotFoundException e) {
                // Cannot delete. The conflicting thing was obviously not OID. Just throw the original exception
                return null;
            }
            // deleted
            return object.getOid();
        } else {
            // The conflict was obviously name. As we have no explicit OID in the object to import
            // it is pretty safe to try to delete the conflicting object
            // look for an object by name and type and delete it
//            QueryType query = QueryUtil.createNameQuery(object.asObjectable());
			ObjectQuery query = ObjectQuery.createObjectQuery(EqualsFilter.createEqual(object.getCompileTimeClass(),
					prismContext, ObjectType.F_NAME, object.asObjectable().getName()));
			
            List<PrismObject<? extends ObjectType>> objects = (List) repository.searchObjects(object.getCompileTimeClass(),
            		query, null, objectResult);
            if (objects.size() != 1) {
                // too few or too much results, not safe to delete
                return null;
            }
            String oidToDelete = objects.get(0).getOid();
            try {
                repository.deleteObject(object.getCompileTimeClass(), oidToDelete, objectResult);
            } catch (ObjectNotFoundException e) {
                // Cannot delete. Some strange conflict ...
                return null;
            }
            return oidToDelete;
        }
    }

    protected <T extends ObjectType> void validateWithDynamicSchemas(PrismObject<T> object, Element objectElement,
                                                           RepositoryService repository, OperationResult objectResult) {

        // TODO: check extension schema (later)

        if (object.canRepresent(ConnectorType.class)) {
            ConnectorType connector = (ConnectorType) object.asObjectable();
            checkSchema(connector.getSchema(), "connector", objectResult);
            objectResult.computeStatus("Connector schema error");

        } else if (object.canRepresent(ResourceType.class)) {


            // Only two object types have XML snippets that conform to the dynamic schema

        	PrismObject<ResourceType> resource = (PrismObject<ResourceType>)object;
            ResourceType resourceType = resource.asObjectable();
            PrismContainer<Containerable> configurationContainer = ResourceTypeUtil.getConfigurationContainer(resource);
            if (configurationContainer == null || configurationContainer.isEmpty()) {
                // Nothing to check
                objectResult.recordWarning("The resource has no configuration");
                return;
            }

            // Check the resource configuration. The schema is in connector, so fetch the connector first
            String connectorOid = resourceType.getConnectorRef().getOid();
            if (StringUtils.isBlank(connectorOid)) {
                objectResult.recordFatalError("The connector reference (connectorRef) is null or empty");
                return;
            }

            PrismObject<ConnectorType> connector = null;
            ConnectorType connectorType = null;
            try {
                connector = repository.getObject(ConnectorType.class, connectorOid, objectResult);
                connectorType = connector.asObjectable();
            } catch (ObjectNotFoundException e) {
                // No connector, no fun. We can't check the schema. But this is referential integrity problem.
                // Mark the error ... there is nothing more to do
                objectResult.recordFatalError("Connector (OID:" + connectorOid + ") referenced from the resource is not in the repository", e);
                return;
            } catch (SchemaException e) {
                // Probably a malformed connector. To be kind of robust, lets allow the import.
                // Mark the error ... there is nothing more to do
                objectResult.recordPartialError("Connector (OID:" + connectorOid + ") referenced from the resource has schema problems: " + e.getMessage(), e);
                LOGGER.error("Connector (OID:{}) referenced from the imported resource \"{}\" has schema problems: {}", new Object[]{connectorOid, resourceType.getName(), e.getMessage(), e});
                return;
            }
            
            Element connectorSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(connector);
            PrismSchema connectorSchema = null;
            if (connectorSchemaElement == null) {
            	// No schema to validate with
            	return;
            }
			try {
				connectorSchema = PrismSchema.parse(connectorSchemaElement, "schema for " + connector, prismContext);
			} catch (SchemaException e) {
				objectResult.recordFatalError("Error parsing connector schema for " + connector + ": "+e.getMessage(), e);
				return;
			}
            QName configContainerQName = new QName(connectorType.getNamespace(), ResourceType.F_CONFIGURATION.getLocalPart());
    		PrismContainerDefinition<?> configContainerDef = connectorSchema.findContainerDefinitionByElementName(configContainerQName);
    		if (configContainerDef == null) {
    			objectResult.recordFatalError("Definition of configuration container " + configContainerQName + " not found in the schema of of " + connector);
                return;
    		}
            
            try {
				configurationContainer.applyDefinition(configContainerDef);
			} catch (SchemaException e) {
				objectResult.recordFatalError("Configuration error in " + resource + ": "+e.getMessage(), e);
                return;
			}
            
            // Also check integrity of the resource schema
            checkSchema(resourceType.getSchema(), "resource", objectResult);

            objectResult.computeStatus("Dynamic schema error");

        } else if (object.canRepresent(ResourceObjectShadowType.class)) {
            // TODO

            //objectResult.computeStatus("Dynamic schema error");
        }

        return;
    }

    /**
     * Try to parse the schema using schema processor. Report errors.
     *
     * @param dynamicSchema
     * @param schemaName
     * @param objectResult
     */
    private void checkSchema(XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".check" + StringUtils.capitalize(schemaName) + "Schema");

        Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);

        if (dynamicSchema == null || xsdElement == null) {
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Missing dynamic " + schemaName + " schema");
            return;
        }

        try {
            PrismSchema.parse(xsdElement, schemaName, prismContext);
        } catch (SchemaException e) {
            result.recordFatalError("Error during " + schemaName + " schema integrity check: " + e.getMessage(), e);
            return;
        }
        result.recordSuccess();
    }

    /**
     * Validate the provided XML snippet with schema definition fetched in runtime.
     *
     * @param contentElements DOM tree to validate
     * @param elementRef      the "correct" name of the root element
     * @param dynamicSchema   dynamic schema
     * @param schemaName
     * @param objectResult
     */
    private PrismContainer validateDynamicSchema(List<Object> contentElements, QName elementRef,
                                                    XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".validate" + StringUtils.capitalize(schemaName) + "Schema");

        Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);
        if (xsdElement == null) {
        	result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No "+schemaName+" schema present");
        	return null;
        }

        com.evolveum.midpoint.prism.schema.PrismSchema schema = null;
        try {
            schema = com.evolveum.midpoint.prism.schema.PrismSchema.parse(xsdElement, schemaName, prismContext);
        } catch (SchemaException e) {
            result.recordFatalError("Error during " + schemaName + " schema parsing: " + e.getMessage(), e);
            LOGGER.trace("Validation error: {}" + e.getMessage());
            return null;
        }

        PrismContainerDefinition containerDefinition = schema.findItemDefinition(elementRef, PrismContainerDefinition.class);

        PrismContainer propertyContainer = null;

        result.recordSuccess();
        return propertyContainer;

    }

    protected <T extends ObjectType> void resolveReferences(PrismObject<T> object, final RepositoryService repository,
    		final boolean enforceReferentialIntegrity, final OperationResult result) {
    	
    	Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (!(visitable instanceof PrismReferenceValue)) {
					return;
				}
				resolveRef((PrismReferenceValue)visitable, repository, enforceReferentialIntegrity, result);
			}
		};
		object.accept(visitor);
    }
    
    
    private void resolveRef(PrismReferenceValue refVal, RepositoryService repository, 
    				boolean enforceReferentialIntegrity, OperationResult parentResult) {
    	PrismReference reference = (PrismReference) refVal.getParent();
    	QName refName = reference.getName();
        OperationResult result = parentResult.createSubresult(OPERATION_RESOLVE_REFERENCE);
        result.addContext(OperationResult.CONTEXT_ITEM, refName);

        QName typeQName = null;
        if (refVal.getTargetType() != null) {
        	typeQName = refVal.getTargetType();
        }
        if (typeQName == null) {
        	PrismReferenceDefinition definition = (PrismReferenceDefinition) refVal.getParent().getDefinition();
        	if (definition != null) {
        		typeQName = definition.getTargetTypeName();
        	}
        }
        Class<? extends ObjectType> type = ObjectType.class;
        if (typeQName != null) {
        	type = (Class<? extends ObjectType>) prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
            if (type == null) {
                result.recordWarning("Unknown type specified in reference or definition of reference " + refName + ": "
                        + typeQName);
                type = ObjectType.class;
            }
        }
        Element filter = refVal.getFilter();
        
        if (!StringUtils.isBlank(refVal.getOid())) {
            // We have OID
            if (filter != null) {
                // We have both filter and OID. We will choose OID, but let's at
                // least log a warning
                result.appendDetail("Both OID and filter for property " + refName);
                result.recordPartialError("Both OID and filter for property " + refName);
                refVal.setFilter(null);
            }
            // Nothing to resolve, but let's check if the OID exists
            PrismObject<? extends ObjectType> object = null;
            try {
                object = repository.getObject(type, refVal.getOid(), result);
            } catch (ObjectNotFoundException e) {
            	String message = "Reference " + refName + " refers to a non-existing object " + refVal.getOid();
            	if (enforceReferentialIntegrity) {
            		LOGGER.error(message);
            		result.recordFatalError(message);
            	} else {
            		LOGGER.warn(message);
            		result.recordWarning(message);
            	}
            } catch (SchemaException e) {
            	
                result.recordPartialError("Schema error while trying to retrieve object " + refVal.getOid()
                        + " : " + e.getMessage(), e);
                LOGGER.error(
                        "Schema error while trying to retrieve object " + refVal.getOid() + " : "
                                + e.getMessage(), e);
                // But continue otherwise
            }
            if (object != null && refVal.getType() != null) {
                // Check if declared and actual type matches
                if (!object.getClass().equals(type)) {
                    result.recordWarning("Type mismatch on property " + refName + ": declared:"
                            + refVal.getType() + ", actual: " + object.getClass());
                }
            }
            result.recordSuccessIfUnknown();
            parentResult.computeStatus();
            return;
        }
        if (filter == null) {
            // No OID and no filter. We are lost.
            result.recordFatalError("Neither OID nor filter for property " + refName
                    + ": cannot resolve reference");
            return;
        }
        // No OID and we have filter. Let's check the filter a bit
        
        ObjectFilter objFilter =  null;
        try{
        	PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        	objFilter = QueryConvertor.parseFilter(objDef, filter);
        } catch (SchemaException ex){
        	LOGGER.error("Failed to convert object filter from filter: "+ DOMUtil.printDom(filter) + " Reason: "+ ex.getMessage(), ex);
        	throw new SystemException("Failed to convert object filter from filter. Reason: " + ex.getMessage(), ex);
        }
        
        LOGGER.trace("Resolving using filter {}", objFilter.dump());
//        NodeList childNodes = filter.getChildNodes();
//        if (childNodes.getLength() == 0) {
        if (objFilter == null){
            result.recordFatalError("OID not specified and filter is empty for property " + refName);
            return;
        }
//        // Let's do resolving
//        QueryType queryType = new QueryType();
//        queryType.setFilter(filter);
        List<PrismObject<? extends ObjectType>> objects = null;
        QName objectType = refVal.getTargetType();
        if (objectType == null) {
            result.recordFatalError("Missing definition of type of reference " + refName);
            return;
        }
        try {
        	ObjectQuery query = ObjectQuery.createObjectQuery(objFilter);
            objects = (List)repository.searchObjects(type, query, null, result);

        } catch (SchemaException e) {
            // This is unexpected, but may happen. Record fatal error
            result.recordFatalError("Repository schema error during resolution of reference " + refName, e);
            return;
        } catch (SystemException e) {
            // We don't want this to tear down entire import.
            result.recordFatalError("Repository system error during resolution of reference " + refName, e);
            return;
        }
        if (objects.isEmpty()) {
            result.recordFatalError("Repository reference " + refName
                    + " cannot be resolved: filter matches no object");
            return;
        }
        if (objects.size() > 1) {
            result.recordFatalError("Repository reference " + refName
                    + " cannot be resolved: filter matches " + objects.size() + " objects");
            return;
        }
        // Bingo. We have exactly one object.
        String oid = objects.get(0).getOid();
        refVal.setOid(oid);
        result.recordSuccessIfUnknown();
    }

    private <T extends ObjectType> void generateIdentifiers(PrismObject<T> object, RepositoryService repository,
			OperationResult objectResult) {
		if (object.canRepresent(TaskType.class)) {
			TaskType task = (TaskType)object.asObjectable();
			if (task.getTaskIdentifier() == null || task.getTaskIdentifier().isEmpty()) {
				task.setTaskIdentifier(lightweightIdentifierGenerator.generate().toString());
			}
		}
	}
    
    private <T extends ObjectType> void encryptValues(final PrismObject<T> object, OperationResult objectResult) {
        final OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".encryptValues");
        Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue pval = (PrismPropertyValue)visitable;
				encryptValue(object, pval, result);
			}
		};
		object.accept(visitor);
        result.recordSuccessIfUnknown();
    }
    
    private <T extends ObjectType> void encryptValue(PrismObject<T> object, PrismPropertyValue pval, OperationResult result) {
    	Itemable item = pval.getParent();
    	if (item == null) {
    		return;
    	}
    	ItemDefinition itemDef = item.getDefinition();
    	if (itemDef == null || itemDef.getTypeName() == null) {
    		return;
    	}
    	if (!itemDef.getTypeName().equals(ProtectedStringType.COMPLEX_TYPE)) {
    		return;
    	}
    	QName propName = item.getName();
    	PrismPropertyValue<ProtectedStringType> psPval = (PrismPropertyValue<ProtectedStringType>)pval;
    	ProtectedStringType ps = psPval.getValue();
    	if (ps.getClearValue() != null) {
            try {
                LOGGER.info("Encrypting cleartext value for field " + propName + " while importing " + object);
                protector.encrypt(ps);
            } catch (EncryptionException e) {
                LOGGER.info("Faild to encrypt cleartext value for field " + propName + " while importing " + object);
                result.recordFatalError("Faild to encrypt value for field " + propName + ": " + e.getMessage(), e);
                return;
            }
        }
    }
}
 