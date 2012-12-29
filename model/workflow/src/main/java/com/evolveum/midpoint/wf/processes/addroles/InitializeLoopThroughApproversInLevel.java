/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processes.addroles;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationParameters;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConstants;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 7.8.2012
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
public class InitializeLoopThroughApproversInLevel implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughApproversInLevel.class);

    private ExpressionFactory expressionFactory;

    public void execute(DelegateExecution execution) {

        ApprovalLevelType level = (ApprovalLevelType) execution.getVariable(AddRolesProcessWrapper.LEVEL);

        DecisionList decisionList = new DecisionList();
        if (level.getAutomaticallyApproved() != null) {
            boolean preApproved;
            try {
                preApproved = evaluateBooleanExpression(level.getAutomaticallyApproved(), execution);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Pre-approved = " + preApproved + " for level " + level);
                }
            } catch (Exception e) {     // todo
                throw new SystemException("Couldn't evaluate auto-approval expression", e);
            }
            decisionList.setPreApproved(preApproved);
        }
        execution.setVariableLocal(WfConstants.VARIABLE_DECISION_LIST, decisionList);

        Set<ObjectReferenceType> approverRefs = new HashSet<ObjectReferenceType>();

        if (!decisionList.isPreApproved()) {
            approverRefs.addAll(level.getApproverRef());
            approverRefs.addAll(evaluateExpressions(level.getApproverExpression(), execution));

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Approvers at the level " + level + " are: " + approverRefs);
            }
            if (approverRefs.isEmpty()) {
                LOGGER.warn("No approvers at the level '" + level.getName() + "' for process " + execution.getVariable(WfConstants.VARIABLE_PROCESS_NAME) + " (id " + execution.getProcessInstanceId() + ")");
            }
        }

        Boolean stop;
        if (approverRefs.isEmpty() || decisionList.isPreApproved()) {
            stop = Boolean.TRUE;
        } else {
            stop = Boolean.FALSE;
        }
        execution.setVariableLocal(AddRolesProcessWrapper.APPROVERS_IN_LEVEL, new ArrayList<ObjectReferenceType>(approverRefs));
        execution.setVariableLocal(AddRolesProcessWrapper.LOOP_APPROVERS_IN_LEVEL_STOP, stop);
    }

    private Collection<? extends ObjectReferenceType> evaluateExpressions(List<ExpressionType> approverExpressionList, DelegateExecution execution) {
        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        for (ExpressionType approverExpression : approverExpressionList) {
            try {
                retval.addAll(evaluateExpression(approverExpression, execution));
            } catch (Exception e) {     // todo fixme
                throw new SystemException("Couldn't evaluate approver expression", e);
            }
        }
        return retval;
    }

    private Collection<ObjectReferenceType> evaluateExpression(ExpressionType approverExpression, DelegateExecution execution) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (expressionFactory == null) {
            expressionFactory = getExpressionFactory();
        }

        OperationResult result = new OperationResult("dummy");

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName approverOidName = new QName(SchemaConstants.NS_C, "approverOid");
        PrismPropertyDefinition approverOidDef = new PrismPropertyDefinition(approverOidName, approverOidName, DOMUtil.XSD_STRING, prismContext);
        Expression<PrismValue> expression = expressionFactory.makeExpression(approverExpression, approverOidDef, "approverExpression", result);
        ExpressionEvaluationParameters params = new ExpressionEvaluationParameters(null, getDefaultVariables(execution), "approverExpression", result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult = expression.evaluate(params);

        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        for (PrismPropertyValue<String> item : exprResult.getZeroSet()) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(item.getValue());
            retval.add(ort);
        }
        return retval;

    }

    private boolean evaluateBooleanExpression(ExpressionType expressionType, DelegateExecution execution) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (expressionFactory == null) {
            expressionFactory = getExpressionFactory();
        }

        OperationResult result = new OperationResult("dummy");

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition resultDef = new PrismPropertyDefinition(resultName, resultName, DOMUtil.XSD_BOOLEAN, prismContext);
        Expression<PrismValue> expression = expressionFactory.makeExpression(expressionType, resultDef, "automatic approval expression", result);
        ExpressionEvaluationParameters params = new ExpressionEvaluationParameters(null, getDefaultVariables(execution), "automatic approval expression", result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple = expression.evaluate(params);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Auto-approval expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    private ExpressionFactory getExpressionFactory() {
        LOGGER.info("Getting expressionFactory");
        ExpressionFactory ef = SpringApplicationContextHolder.getApplicationContext().getBean("expressionFactory", ExpressionFactory.class);
        if (ef == null) {
            throw new IllegalStateException("expressionFactory bean cannot be found");
        }
        return ef;
    }


    private Map<QName, Object> getDefaultVariables(DelegateExecution execution) {

        Map<QName, Object> variables = new HashMap<QName, Object>();

        PrismObject<UserType> user = (PrismObject<UserType>) execution.getVariable(WfConstants.VARIABLE_MIDPOINT_OBJECT_NEW);
        if (user != null) {
            variables.put(SchemaConstants.I_USER, user);
        }

        PrismObject<UserType> requester = (PrismObject<UserType>) execution.getVariable(WfConstants.VARIABLE_MIDPOINT_REQUESTER);
        if (requester != null) {
            variables.put(SchemaConstants.I_REQUESTER, requester);
        }

        return variables;
    }


}