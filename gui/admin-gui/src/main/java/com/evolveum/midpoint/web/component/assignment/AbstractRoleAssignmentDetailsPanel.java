/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;

/**
 * TODO: is this class abstract or not?
 */
public class AbstractRoleAssignmentDetailsPanel<R extends AbstractRoleType> extends AbstractAssignmentDetailsPanel<R> {

	private static final long serialVersionUID = 1L;

	private static List hiddenItems = new ArrayList<>();

	static  {
			hiddenItems.add(AssignmentType.F_POLICY_RULE);
	};

	public AbstractRoleAssignmentDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
		super(id, form, assignmentModel);
	}

	
	@Override
	protected List<ItemPath> collectContainersToShow() {
		List<ItemPath> pathsToShow = new ArrayList<>();
		if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(getModelObject().getContainerValue().getValue()))) {
			pathsToShow.add(getAssignmentPath().append(AssignmentType.F_CONSTRUCTION));
		}
		
		if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(getModelObject().getContainerValue().getValue()))) {
			pathsToShow.add(getAssignmentPath().append(AssignmentType.F_PERSONA_CONSTRUCTION));
		}
		return pathsToShow;
	}

}
