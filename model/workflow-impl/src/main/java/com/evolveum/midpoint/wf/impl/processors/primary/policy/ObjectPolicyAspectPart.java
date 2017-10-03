/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaHelper;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalSpecificContent;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.policy.ProcessSpecifications.ProcessSpecification;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectName;
import static java.util.Collections.singletonList;

/**
 * @author mederly
 */
@Component
public class ObjectPolicyAspectPart {

	private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyAspectPart.class);

	@Autowired private PolicyRuleBasedAspect main;
	@Autowired protected ApprovalSchemaHelper approvalSchemaHelper;
	@Autowired protected MiscDataUtil miscDataUtil;
	@Autowired protected PrismContext prismContext;
	@Autowired protected ItemApprovalProcessInterface itemApprovalProcessInterface;
	@Autowired protected BaseConfigurationHelper baseConfigurationHelper;

	<T extends ObjectType> void extractObjectBasedInstructions(@NotNull ObjectTreeDeltas<T> objectTreeDeltas,
			@Nullable PrismObject<UserType> requester, @NotNull List<PcpChildWfTaskCreationInstruction<?>> instructions,
			@NotNull ModelInvocationContext<T> ctx, @NotNull OperationResult result)
			throws SchemaException {

		ObjectDelta<T> focusDelta = objectTreeDeltas.getFocusChange();
		LensFocusContext<T> focusContext = (LensFocusContext<T>) ctx.modelContext.getFocusContext();
		PrismObject<T> object = focusContext.getObjectOld() != null ?
				focusContext.getObjectOld() : focusContext.getObjectNew();

		List<EvaluatedPolicyRule> triggeredApprovalActionRules = main.selectTriggeredApprovalActionRules(focusContext.getPolicyRules());
		LOGGER.trace("extractObjectBasedInstructions: triggeredApprovalActionRules:\n{}", debugDumpLazily(triggeredApprovalActionRules));

		// default rule
		if (!triggeredApprovalActionRules.isEmpty()) {
			addObjectOidIfNeeded(focusDelta, ctx.modelContext);
			ProcessSpecifications processSpecifications = ProcessSpecifications.createFromRules(triggeredApprovalActionRules);
			for (ProcessSpecification processSpecificationEntry : processSpecifications.getSpecifications()) {
				if (focusDelta.isEmpty()) {
					break;  // we're done
				}
				WfProcessSpecificationType processSpecification = processSpecificationEntry.basicSpec;
				List<ObjectDelta<T>> deltasToApprove = getDeltasToApprove(focusDelta, processSpecification);
				LOGGER.trace("Deltas to approve:\n{}", debugDumpLazily(deltasToApprove));
				if (deltasToApprove.isEmpty()) {
					continue;
				}
				LOGGER.trace("Remaining delta:\n{}", debugDumpLazily(focusDelta));
				ApprovalSchemaBuilder builder = new ApprovalSchemaBuilder(main, approvalSchemaHelper);
				for (Pair<ApprovalPolicyActionType, EvaluatedPolicyRule> actionWithRule : processSpecificationEntry.actionsWithRules) {
					ApprovalPolicyActionType approvalAction = actionWithRule.getLeft();
					builder.add(main.getSchemaFromAction(approvalAction), approvalAction.getCompositionStrategy(), object, actionWithRule.getRight());
				}
				buildSchemaForObject(requester, instructions, ctx, result, deltasToApprove, builder);
			}
		} else if (baseConfigurationHelper.getUseDefaultApprovalPolicyRules(ctx.wfConfiguration) != DefaultApprovalPolicyRulesUsageType.NEVER) {
			ApprovalSchemaBuilder builder = new ApprovalSchemaBuilder(main, approvalSchemaHelper);
			if (builder.addPredefined(object, SchemaConstants.ORG_OWNER, result)) {
				LOGGER.trace("Added default approval action, as no explicit one was found");
				addObjectOidIfNeeded(focusDelta, ctx.modelContext);
				List<ObjectDelta<T>> deltasToApprove = singletonList(focusDelta.clone());
				focusDelta.clear();
				buildSchemaForObject(requester, instructions, ctx, result, deltasToApprove, builder);
			}
		}
	}

	private <T extends ObjectType> void addObjectOidIfNeeded(ObjectDelta<T> focusDelta, ModelContext<T> modelContext) {
		if (focusDelta.isAdd()) {
			if (focusDelta.getObjectToAdd().getOid() == null) {
				String newOid = OidUtil.generateOid();
				focusDelta.getObjectToAdd().setOid(newOid);
				((LensFocusContext<?>) modelContext.getFocusContext()).setOid(newOid);
			}
		}
	}

	private <T extends ObjectType> void buildSchemaForObject(PrismObject<UserType> requester,
			List<PcpChildWfTaskCreationInstruction<?>> instructions, ModelInvocationContext<T> ctx,
			@NotNull OperationResult result, List<ObjectDelta<T>> deltasToApprove,
			ApprovalSchemaBuilder builder) throws SchemaException {
		ApprovalSchemaBuilder.Result builderResult = builder.buildSchema(ctx, result);
		if (!approvalSchemaHelper.shouldBeSkipped(builderResult.schemaType)) {
			prepareObjectRelatedTaskInstructions(instructions, builderResult, deltasToApprove, ctx.modelContext, requester, result);
		}
	}

	private <T extends ObjectType> List<ObjectDelta<T>> getDeltasToApprove(ObjectDelta<T> focusDelta, WfProcessSpecificationType processSpecification)
			throws SchemaException {
		List<ObjectDelta<T>> rv = new ArrayList<>();
		if (processSpecification == null || processSpecification.getDeltaFrom().isEmpty()) {
			return addWholeDelta(focusDelta, rv);
		}
		for (DeltaSourceSpecificationType sourceSpec : processSpecification.getDeltaFrom()) {
			if (sourceSpec == null || sourceSpec.getItem().isEmpty() && sourceSpec.getItemValue() == null) {
				return addWholeDelta(focusDelta, rv);
			} else if (!sourceSpec.getItem().isEmpty()) {
				ObjectDelta.FactorOutResult<T> out = focusDelta.factorOut(ItemPathType.toItemPathList(sourceSpec.getItem()), false);
				rv.addAll(out.offsprings);
			} else {
				assert sourceSpec.getItemValue() != null;
				ObjectDelta.FactorOutResult<T> out = focusDelta.factorOutValues(sourceSpec.getItemValue().getItemPath(), false);
				rv.addAll(out.offsprings);
			}
		}
		return rv;
	}

	@NotNull
	private <T extends ObjectType> List<ObjectDelta<T>> addWholeDelta(ObjectDelta<T> focusDelta, List<ObjectDelta<T>> rv) {
		rv.add(focusDelta.clone());
		focusDelta.clear();
		return rv;
	}

	private <T extends ObjectType> void prepareObjectRelatedTaskInstructions(
			List<PcpChildWfTaskCreationInstruction<?>> instructions, ApprovalSchemaBuilder.Result builderResult,
			List<ObjectDelta<T>> deltasToApprove, ModelContext<T> modelContext,
			PrismObject<UserType> requester, OperationResult result) throws SchemaException {

		for (ObjectDelta<T> deltaToApprove : deltasToApprove) {
			//String objectOid = getFocusObjectOid(modelContext);
			String objectName = getFocusObjectName(modelContext);

			String opName;
			if (deltaToApprove.isAdd()) {
				opName = "addition";
			} else if (deltaToApprove.isDelete()) {
				opName = "deletion";
			} else {
				opName = "modification";
			}

			String approvalTaskName = "Approve " + opName + " of " + objectName;

			PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> instruction =
					PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(main.getChangeProcessor(), approvalTaskName,
							builderResult.schemaType, builderResult.attachedRules);

			instruction.prepareCommonAttributes(main, modelContext, requester);

			instruction.setDeltasToProcess(deltaToApprove);

			instruction.setObjectRef(modelContext, result);

			String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
			instruction.setTaskName("Approval " + andExecuting + "of " + opName + " of " + objectName);
			instruction.setProcessInstanceName(StringUtils.capitalizeFirstLetter(opName) + " of " + objectName);

			itemApprovalProcessInterface.prepareStartInstruction(instruction);

			instructions.add(instruction);
		}
	}

//	private ObjectDelta<?> subtractModifications(@NotNull ObjectDelta<?> focusDelta, @NotNull Set<ItemPath> itemPaths) {
//		if (itemPaths.isEmpty()) {
//			ObjectDelta<?> originalDelta = focusDelta.clone();
//			focusDelta.clear();
//			return originalDelta;
//		}
//		if (!focusDelta.isModify()) {
//			throw new IllegalStateException("Not a MODIFY delta; delta = " + focusDelta);
//		}
//		return focusDelta.subtract(itemPaths);
//	}
}
