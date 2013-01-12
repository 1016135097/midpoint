package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

@Component
public class ConfigurationExceptionHandler extends ErrorHandler {

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	
	@Override
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex, boolean compensate,
			OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException {
		
		if (shadow.getOid() == null){
			throw new ConfigurationException("Configuration error: "+ex.getMessage(), ex);
		}
		
		Collection<ItemDelta> modification = createAttemptModification(shadow, null);
		try {
			cacheRepositoryService.modifyObject(shadow.asPrismObject().getCompileTimeClass(), shadow.getOid(),
					modification, parentResult);
		} catch (Exception e) {
			//this should not happen. But if it happens, we should return original exception
		}
		
		ObjectDelta delta = null;
		switch(op){
		case ADD :
			delta = ObjectDelta.createAddDelta(shadow.asPrismObject());
			break;
		case DELETE:
			delta = ObjectDelta.createDeleteDelta(shadow.getClass(), shadow.getOid(), prismContext);
			break;
		case MODIFY:
			Collection<? extends ItemDelta> modifications = null;
			if (shadow.getObjectChange() != null) {
				ObjectDeltaType deltaType = shadow.getObjectChange();

				modifications = DeltaConvertor.toModifications(deltaType.getModification(), shadow
						.asPrismObject().getDefinition());
			}
			delta = ObjectDelta.createModifyDelta(shadow.getOid(), modifications, shadow.getClass(), prismContext);
			break;
		}
		
		if (op != FailedOperation.GET){
		Task task = taskManager.createTaskInstance();
		ResourceOperationDescription operationDescription = createOperationDescription(shadow, shadow.getResource(),
				delta, task, parentResult);
		changeNotificationDispatcher.notifyFailure(operationDescription, task, parentResult);
		}

		throw new ConfigurationException("Configuration error: "+ex.getMessage(), ex);
	}

}
