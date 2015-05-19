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

package com.evolveum.midpoint.certification.api;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public interface CertificationManager {

    /**
     * Creates a certification campaign. Basically, it prepares AccessCertificationCampaignType object, based on
     * general information in certification definition (mandatory), and specific configuration values for this campaign,
     * that may be provided in the form of campaign object.
     *
     * Mandatory information in the certification definition are:
     *  - definition name
     *  - definition description
     *  - handlerUri
     *  - scope definition
     *  - stage(s) definition
     *
     * Optional information in the certification definition:
     *  - owner reference
     *  - tenant reference
     *
     * Optional information in the certification campaign object:
     *  - campaign name
     *  - campaign description
     *  - owner reference
     *  - tenant reference
     *
     * Owner reference: if not specified neither in campaign nor in the certification definition,
     * current user will be used as the owner of the created campaign.
     *
     * The campaign will NOT be started upon creation. It should be started explicitly by calling nextStage method.
     *
     * @param certificationDefinition Certification definition for this campaign.
     * @param campaign Specific values for this campaign (optional).
     *                It must not be persistent, i.e. its OID must not be set.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return Object for the created campaign. It will be stored in the repository as well.
     */
    AccessCertificationCampaignType createCampaign(AccessCertificationDefinitionType certificationDefinition, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    /**
     * Starts a given stage in the certification campaign.
     *
     * In the first stage, certification cases will be generated for the campaign, depending on the certification
     * definition (scope and handler). In all stages, reviewers will be assigned to cases, based again on the
     * definition (reviewer specification in stage definition and handler).
     *
     * @param campaign Certification campaign. If its definition reference is already resolved, it will be used.
     *                 Otherwise, the implementation will resolve the definition by itself.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     */
    void nextStage(AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    /**
     * Returns a set of certification cases that match a given query.
     * The query can contain a filter and/or a paging instruction.
     * Filter can point to the following attributes:
     *  - reviewerRef (AccessCertificationCaseType.F_REVIEWER_REF), i.e. returning the cases who are assigned to given reviewer(s)
     *  - subjectRef (AccessCertificationCaseType.F_SUBJECT_REF), i.e. returning the cases belonging to given subject (e.g. user)
     *  - targetRef (AccessCertificationCaseType.F_TARGET_REF), i.e. returning the cases pointing to e.g. given role/org/resource (in case of assignment-related cases)
     * Paging instruction can be used to sort and page search results. Sorting can be based on
     *  - name of subject, by setting paging.orderBy = subjectRef
     *  - name of target, by setting paging.orderBy = targetRef
     * Note that in order to use names as a sorting criteria, it is necessary to include RESOLVE_NAMES option in the operation call.
     * Paging is specified by offset (counting from 0) and maxSize. Paging cooke is ignored.
     *
     * NOTE THAT THE SORTING INTERFACE WILL PROBABLY BE CHANGED IN NEAR FUTURE.
     *
     * @param campaignOid OID of the campaign to query.
     * @param query Specification of the cases to retrieve.
     * @param options Options to use (currently supported is RESOLVE_NAMES).
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return A list of relevant certification cases.
     */
    List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException;

    /**
     * Returns a set of certification decisions that match a given query.
     * Each decision is returned in context of its certification case.
     * So, in contrast to searchCases method that returns specified cases with all their decisions,
     * this one returns a list of cases where each case has at most one decision: the one that corresponds
     * to specified reviewer and current certification stage. Zero decisions means that the reviewer has not
     * provided any decision yet.
     *
     * Query argument for cases is the same as in the searchCases call.
     * Contrary to searchCases, this method allows to collect cases for more than one campaign
     * (e.g. to present a reviewer all of his/her cases).
     * So, instead of campaignOid there is a campaignQuery allowing to select one, more, and even all campaigns.
     *
     * Contrary to all the other methods, cases returned from this method have campaignRef set.
     *
     * @param campaignQuery Specification of campaigns to query.
     * @param caseQuery Specification of the cases to retrieve.
     * @param reviewerOid OID of the reviewer whose decisions we want to retrieve.
     * @param options Options to use (currently supported is RESOLVE_NAMES).
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     * @return A list of relevant certification cases.
     */

    List<AccessCertificationCaseType> searchDecisions(ObjectQuery campaignQuery, ObjectQuery caseQuery,
                                                      String reviewerOid,
                                                      Collection<SelectorOptions<GetOperationOptions>> options,
                                                      Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException;

    /**
     * Records a particular decision of a reviewer.
     *
     * @param campaignOid OID of the campaign to which the decision belongs.
     * @param caseId ID of the certification case to which the decision belongs.
     * @param decision The decision itself.
     * @param task Task in context of which all operations will take place.
     * @param parentResult Result for the operations.
     */
    void recordDecision(String campaignOid, long caseId, AccessCertificationDecisionType decision,
                        Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException, ObjectAlreadyExistsException;

}
