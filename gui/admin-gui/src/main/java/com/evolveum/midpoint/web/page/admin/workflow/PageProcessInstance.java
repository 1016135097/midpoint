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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.wf.ProcessInstance;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.util.string.StringValue;

/**
 * @author lazyman
 * @author mserbak
 * @author mederly
 */
public class PageProcessInstance extends PageAdminWorkItems {
	private static final long serialVersionUID = -5933030498922903813L;

	private static final Trace LOGGER = TraceManager.getTrace(PageProcessInstance.class);
	private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
    public static final String PARAM_PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String PARAM_PROCESS_INSTANCE_FINISHED = "processInstanceFinished";     // boolean value
    public static final String PARAM_PROCESS_INSTANCE_BACK = "processInstanceBack";
    public static final String PARAM_PROCESS_INSTANCE_BACK_REQUESTED_BY = "PageProcessInstancesRequestedBy";
    public static final String PARAM_PROCESS_INSTANCE_BACK_REQUESTED_FOR = "PageProcessInstancesRequestedFor";
    public static final String PARAM_PROCESS_INSTANCE_BACK_ALL = "PageProcessInstancesAll";
    private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadProcessInstance";

	private IModel<ProcessInstanceDto> model;

    public PageProcessInstance() {
		model = new LoadableModel<ProcessInstanceDto>(false) {

			@Override
			protected ProcessInstanceDto load() {
				return loadProcessInstance();
			}
		};

        initLayout();
	}

    private ProcessInstanceDto loadProcessInstance() {
		OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
        StringValue back = getPageParameters().get(PARAM_PROCESS_INSTANCE_BACK);
		try {
            StringValue pid = getPageParameters().get(PARAM_PROCESS_INSTANCE_ID);
            boolean finished = getPageParameters().get(PARAM_PROCESS_INSTANCE_FINISHED).toBoolean();
            ProcessInstance processInstance = getWorkflowDataAccessor().getProcessInstanceByInstanceId(pid.toString(), finished, result);
            return new ProcessInstanceDto(processInstance);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get process instance information.", ex);
            showResult(result);
            getSession().error(getString("pageProcessInstance.message.cantGetDetails"));

            if (!result.isSuccess()) {
                showResultInSession(result);
            }
            throw new RestartResponseException(backPage(back.toString()));
        }

	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		initMainInfo(mainForm);

		initButtons(mainForm);
	}

	private void initMainInfo(Form mainForm) {
        Label name = new Label("name", new PropertyModel(model, "name"));
        mainForm.add(name);

		Label pid = new Label("pid", new PropertyModel(model, "instanceId"));
		mainForm.add(pid);

        Label started = new Label("started", new PropertyModel(model, "started"));
        mainForm.add(started);

        Label finished = new Label("finished", new PropertyModel(model, "finished"));
        mainForm.add(finished);

        Label tasks = new Label("tasks", new PropertyModel(model, "tasks"));
        mainForm.add(tasks);

        Label details = new Label("details", new PropertyModel(model, "details"));
        mainForm.add(details);
	}

	private void initButtons(final Form mainForm) {
		AjaxLinkButton backButton = new AjaxLinkButton("backButton",
				createStringResource("pageProcessInstance.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(backPage(getPageParameters().get(PARAM_PROCESS_INSTANCE_BACK).toString()));
			}
		};
		mainForm.add(backButton);
	}

    private Class backPage(String backValue) {
        if (PARAM_PROCESS_INSTANCE_BACK_ALL.equals(backValue)) {
            return PageProcessInstancesAll.class;
        } else if (PARAM_PROCESS_INSTANCE_BACK_REQUESTED_BY.equals(backValue)) {
            return PageProcessInstancesRequestedBy.class;
        } else {
            return PageProcessInstancesRequestedFor.class;
        }
    }


}
