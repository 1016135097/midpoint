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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract from ApprovalSchemaExecutionInformationType that could be directly displayed via the GUI as "approval process preview"
 * (either for the whole process or only the future stages).
 *
 * @author mederly
 */
public class ApprovalProcessExecutionInformationDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private final boolean wholeProcess;                                   // do we represent whole process or only the future stages?
	private final int currentStageNumber;                                 // current stage number (0 if there's no current stage, i.e. process has not started yet)
	private final int numberOfStages;
	private final List<ApprovalStageExecutionInformationDto> stages = new ArrayList<>();

	public ApprovalProcessExecutionInformationDto(boolean wholeProcess, int currentStageNumber, int numberOfStages) {
		this.wholeProcess = wholeProcess;
		this.currentStageNumber = currentStageNumber;
		this.numberOfStages = numberOfStages;
	}

	public static ApprovalProcessExecutionInformationDto createFrom(ApprovalSchemaExecutionInformationType info,
			ObjectResolver resolver, boolean wholeProcess, Task opTask,
			OperationResult result) {
		int currentStageNumber = info.getCurrentStageNumber() != null ? info.getCurrentStageNumber() : 0;
		int numberOfStages = info.getStage().size();
		ObjectResolver.Session session = resolver.openResolutionSession(null);
		ApprovalProcessExecutionInformationDto rv = new ApprovalProcessExecutionInformationDto(wholeProcess, currentStageNumber, numberOfStages);
		int startingStageNumber = wholeProcess ? 1 : currentStageNumber+1;
		for (int i = startingStageNumber - 1; i < numberOfStages; i++) {
			rv.stages.add(ApprovalStageExecutionInformationDto.createFrom(info, i, resolver, session, opTask, result));
		}
		return rv;
	}

	public boolean isWholeProcess() {
		return wholeProcess;
	}

	public int getCurrentStageNumber() {
		return currentStageNumber;
	}

	public int getNumberOfStages() {
		return numberOfStages;
	}

	public List<ApprovalStageExecutionInformationDto> getStages() {
		return stages;
	}
}
