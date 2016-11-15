/**
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.model.api.context;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public interface EvaluatedAssignment<F extends FocusType> extends DebugDumpable {

	AssignmentType getAssignmentType();

	Collection<Authorization> getAuthorizations();
	
	Collection<AdminGuiConfigurationType> getAdminGuiConfigurations();
	
	DeltaSetTriple<? extends EvaluatedAssignmentTarget> getRoles();

	DeltaSetTriple<EvaluatedConstruction> getEvaluatedConstructions(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

	PrismObject<?> getTarget();
	
	QName getRelation();

	boolean isValid();

	boolean isPresentInCurrentObject();

	boolean isPresentInOldObject();
	
	/**
	 * Returns all policy rules that apply to this assignment - even those that were not triggered.
	 * The policy rules are compiled from all the applicable sources (target, meta-roles, etc.)
	 */
	Collection<EvaluatedPolicyRule> getPolicyRules();
	
	public Collection<String> getPolicySituations();
	
	void triggerConstraint(EvaluatedPolicyRule rule, EvaluatedPolicyRuleTrigger trigger) throws PolicyViolationException;
}