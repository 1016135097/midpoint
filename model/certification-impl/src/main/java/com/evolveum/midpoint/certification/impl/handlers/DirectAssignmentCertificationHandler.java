/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentReviewScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class DirectAssignmentCertificationHandler extends BaseCertificationHandler {

    public static final String URI = NS_HANDLERS_PREFIX + "#direct-assignment";

    private static final transient Trace LOGGER = TraceManager.getTrace(DirectAssignmentCertificationHandler.class);

    @PostConstruct
    public void init() {
        certificationManager.registerHandler(URI, this);
    }

    @Override
    public QName getDefaultObjectType() {
        return UserType.COMPLEX_TYPE;
    }

    // converts assignments to cases
    @Override
    public Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<ObjectType> objectPrism, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) {

        AccessCertificationAssignmentReviewScopeType scope = null;
        if (campaign.getScopeDefinition() instanceof AccessCertificationAssignmentReviewScopeType) {
            scope = (AccessCertificationAssignmentReviewScopeType) campaign.getScopeDefinition();
        }

        ObjectType object = objectPrism.asObjectable();
        if (!(object instanceof FocusType)) {
            throw new IllegalStateException(DirectAssignmentCertificationHandler.class.getSimpleName() + " cannot be run against non-focal object: " + ObjectTypeUtil.toShortString(object));
        }
        FocusType focus = (FocusType) object;

        List<AccessCertificationCaseType> caseList = new ArrayList<>();
        if (isIncludeAssignments(scope)) {
            for (AssignmentType assignment : focus.getAssignment()) {
                processAssignment(assignment, false, scope, object, caseList);
            }
        }
        if (object instanceof AbstractRoleType && isIncludeInducements(scope)) {
            for (AssignmentType assignment : ((AbstractRoleType) object).getInducement()) {
                processAssignment(assignment, true, scope, object, caseList);
            }
        }
        return caseList;
    }

    private void processAssignment(AssignmentType assignment, boolean isInducement, AccessCertificationAssignmentReviewScopeType scope,
                                   ObjectType object, List<AccessCertificationCaseType> caseList) {
        AccessCertificationAssignmentCaseType assignmentCase = new AccessCertificationAssignmentCaseType(prismContext);
        assignmentCase.asPrismContainerValue().setConcreteType(AccessCertificationAssignmentCaseType.COMPLEX_TYPE);
        assignmentCase.setAssignment(assignment.clone());
        assignmentCase.setIsInducement(isInducement);
        assignmentCase.setSubjectRef(ObjectTypeUtil.createObjectRef(object));
        boolean valid;
        if (assignment.getTargetRef() != null) {
            assignmentCase.setTargetRef(assignment.getTargetRef());
            if (RoleType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                valid = isIncludeRoles(scope);
            } else if (OrgType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                valid = isIncludeOrgs(scope);
            } else {
                throw new IllegalStateException("Unexpected targetRef type: " + assignment.getTargetRef().getType() + " in " + ObjectTypeUtil.toShortString(assignment));
            }
        } else if (assignment.getConstruction() != null) {
            assignmentCase.setTargetRef(assignment.getConstruction().getResourceRef());
            valid = isIncludeResources(scope);
        } else {
            valid = false;      // neither role/org nor resource assignment; ignored for now
        }
        if (valid) {
            caseList.add(assignmentCase);
        }
    }

    private boolean isIncludeAssignments(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeAssignments());
    }

    private boolean isIncludeInducements(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeInducements());
    }

    private boolean isIncludeResources(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeResources());
    }

    private boolean isIncludeRoles(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeRoles());
    }

    private boolean isIncludeOrgs(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeOrgs());
    }

    @Override
    public void doRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, Task task, OperationResult caseResult) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        if (!(aCase instanceof AccessCertificationAssignmentCaseType)) {
            throw new IllegalStateException("Expected " + AccessCertificationAssignmentCaseType.class + ", got " + aCase.getClass() + " instead");
        }
        AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) aCase;
        String subjectOid = assignmentCase.getSubjectRef().getOid();
        Long assignmentId = assignmentCase.getAssignment().getId();
        if (assignmentId == null) {
            throw new IllegalStateException("No ID for an assignment to remove: " + assignmentCase.getAssignment());
        }
        Class clazz = ObjectTypes.getObjectTypeFromTypeQName(assignmentCase.getSubjectRef().getType()).getClassDefinition();
        PrismContainerValue<AssignmentType> cval = new PrismContainerValue<>(prismContext);
        cval.setId(assignmentId);

        // quick "solution" - deleting without checking the assignment ID
        ContainerDelta assignmentDelta;
        if (Boolean.TRUE.equals(assignmentCase.isIsInducement())) {
            assignmentDelta = ContainerDelta.createModificationDelete(AbstractRoleType.F_INDUCEMENT, clazz, prismContext, cval);
        } else {
            assignmentDelta = ContainerDelta.createModificationDelete(FocusType.F_ASSIGNMENT, clazz, prismContext, cval);
        }
        ObjectDelta objectDelta = ObjectDelta.createModifyDelta(subjectOid, Arrays.asList(assignmentDelta), clazz, prismContext);
        LOGGER.info("Going to execute delta: {}", objectDelta.debugDump());
        modelService.executeChanges((Collection) Arrays.asList(objectDelta), null, task, caseResult);
        LOGGER.info("Case {} in {} ({} {} of {}) was successfully revoked",
                aCase.asPrismContainerValue().getId(), ObjectTypeUtil.toShortString(campaign),
                Boolean.TRUE.equals(assignmentCase.isIsInducement()) ? "inducement":"assignment",
                assignmentId, subjectOid);
    }
}
