/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.*;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.impl.util.SingleItemSerializationSafeContainerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil.getRequiredVariable;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getTaskManager;

public class PrepareApprover implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(PrepareApprover.class);

    public void execute(DelegateExecution execution) {

    	PrismContext prismContext = getPrismContext();
		OperationResult result = new OperationResult(PrepareApprover.class.getName() + ".execute");
		Task wfTask = ActivitiUtil.getTask(execution, result);
		Task opTask = getTaskManager().createTaskInstance();

		LightweightObjectRef approverRef = getRequiredVariable(execution, ProcessVariableNames.APPROVER_REF, LightweightObjectRef.class,
				null);
		ApprovalLevelImpl level = ActivitiUtil.getRequiredVariable(execution, ProcessVariableNames.LEVEL, ApprovalLevelImpl.class,
				null);

        String assignee = null;
        String candidateGroups = null;
        if (approverRef.getType() == null || UserType.COMPLEX_TYPE.equals(approverRef.getType())) {
            assignee = approverRef.getOid();
        } else if (RoleType.COMPLEX_TYPE.equals(approverRef.getType())) {
            candidateGroups = MiscDataUtil.ROLE_PREFIX + ":" + approverRef.getOid();
        } else if (OrgType.COMPLEX_TYPE.equals(approverRef.getType())) {
            candidateGroups = MiscDataUtil.ORG_PREFIX + ":" + approverRef.getOid();
        } else {
            throw new IllegalStateException("Unsupported type of the approver: " + approverRef.getType());
        }

		execution.setVariableLocal(ProcessVariableNames.ASSIGNEE, assignee);
		execution.setVariableLocal(ProcessVariableNames.CANDIDATE_GROUPS, candidateGroups);

		List<?> additionalInformation;
        if (level.getAdditionalInformation() != null) {
			try {
				WfExpressionEvaluationHelper evaluator = SpringApplicationContextHolder.getExpressionEvaluationHelper();
				ExpressionVariables variables = evaluator.getDefaultVariables(execution, wfTask, result);
				additionalInformation = evaluator.evaluateExpression(level.getAdditionalInformation(), variables, execution,
								"additional information expression", Object.class, DOMUtil.XSD_STRING, opTask, result);
			} catch (Throwable t) {
        		throw new SystemException("Couldn't evaluate additional information expression in " + execution, t);
			}
		} else {
        	additionalInformation = new AdditionalInformationGenerator().getDefaultAdditionalInformation(wfTask, level.getOrder());
		}
		if (additionalInformation != null && !additionalInformation.isEmpty()) {
			execution.setVariableLocal(CommonProcessVariableNames.ADDITIONAL_INFORMATION,
					new SingleItemSerializationSafeContainerImpl<>(
							wrapAdditionalInformationIfNeeded(additionalInformation), prismContext));
		}

        LOGGER.debug("Creating work item for assignee={}, candidateGroups={}, additionalInformation='{}'",
				assignee, candidateGroups, additionalInformation);
    }

	@SuppressWarnings("unchecked")
	private List<InformationType> wrapAdditionalInformationIfNeeded(List<?> data) {		// data is not empty
		if (data.stream().allMatch(o -> o instanceof String)) {
			InformationType info = new InformationType();
			for (Object o : data) {
				InformationPartType part = new InformationPartType();
				part.setText((String) o);
				info.getPart().add(part);
			}
			return Collections.singletonList(info);
		} else if (data.stream().allMatch(o -> o instanceof InformationType)) {
			return (List<InformationType>) data;
		} else {
			throw new SystemException("Couldn't create 'additional information' structure from list of "
				+ data.stream().map(o -> o != null ? o.getClass().getSimpleName() : null).collect(Collectors.joining(", ", "[", "]")));
		}
	}
}
