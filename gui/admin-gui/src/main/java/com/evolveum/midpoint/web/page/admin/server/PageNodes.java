/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.server.dto.NodeDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/nodes", matchUrlForSecurity = "/admin/nodes")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                        label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                        description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_NODES_URL,
                        label = "PageNodes.auth.nodes.label",
                        description = "PageNodes.auth.nodes.description")})
public class PageNodes extends PageAdmin {

    public static final long WAIT_FOR_TASK_STOP = 2000L;
    private static final String ID_TABLE = "table";
    private static final String DOT_CLASS = PageNodes.class.getName() + ".";
    private static final String OPERATION_DELETE_NODES = DOT_CLASS + "deleteNodes";
    private static final String OPERATION_START_SCHEDULERS = DOT_CLASS + "startSchedulers";
    private static final String OPERATION_STOP_SCHEDULERS_AND_TASKS = DOT_CLASS + "stopSchedulersAndTasks";
    private static final String OPERATION_STOP_SCHEDULERS = DOT_CLASS + "stopSchedulers";
    private static final String OPERATION_DEACTIVATE_SERVICE_THREADS = DOT_CLASS + "deactivateServiceThreads";
    private static final String OPERATION_REACTIVATE_SERVICE_THREADS = DOT_CLASS + "reactivateServiceThreads";

    public PageNodes() {
        initLayout();
    }


    private void initLayout() {
        MainObjectListPanel<NodeType> table = new MainObjectListPanel<>(ID_TABLE, NodeType.class, UserProfileStorage.TableId.PAGE_TASKS_NODES_PANEL, null) {

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, NodeType object) {
                //nothing to do, details not enabled.
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<NodeType>> rowModel) {
                return false;
            }

            @Override
            protected boolean isCreateNewObjectEnabled() {
                return false;
            }

            @Override
            protected List<IColumn<SelectableBean<NodeType>, String>> createColumns() {
                return initNodeColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createNodesInlineMenu();
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId){
                List<Component> buttonsList = super.createToolbarButtonsList(buttonId);
                List<AjaxButton> diagnosticButtons = initDiagnosticButtons(buttonId);
                buttonsList.addAll(diagnosticButtons);
                return buttonsList;
            }
        };
        add(table);
    }

    private List<AjaxButton> initDiagnosticButtons(String buttonId) {
        List<AjaxButton> diagnosticButtons = new ArrayList<>(2);
        AjaxButton deactivate = new AjaxButton(buttonId,
                createStringResource("pageTasks.button.deactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deactivateServiceThreadsPerformed(target);
            }
        };
        deactivate.add(AttributeModifier.append("class", "btn btn-margin-left btn-danger"));
        diagnosticButtons.add(deactivate);

        AjaxButton reactivate = new AjaxButton(buttonId,
                createStringResource("pageTasks.button.reactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reactivateServiceThreadsPerformed(target);
            }
        };
        reactivate.add(AttributeModifier.append("class", "btn btn-margin-left btn-success"));
        diagnosticButtons.add(reactivate);
        return diagnosticButtons;
    }

    // region Diagnostics actions
    private void deactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        Task opTask = createSimpleTask(OPERATION_DEACTIVATE_SERVICE_THREADS);
        OperationResult result = opTask.getResult();

        try {
            boolean stopped = getTaskService().deactivateServiceThreads(WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (stopped) {
                    result.recordStatus(OperationResultStatus.SUCCESS,
                            createStringResource("pageTasks.message.deactivateServiceThreadsPerformed.success").getString());
                } else {
                    result.recordWarning(
                            createStringResource("pageTasks.message.deactivateServiceThreadsPerformed.warning").getString());
                }
            }
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.deactivateServiceThreadsPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        getTable().refreshTable(NodeType.class, target);
        target.add(getTable());
    }

    private void reactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        Task opTask = createSimpleTask(OPERATION_REACTIVATE_SERVICE_THREADS);
        OperationResult result = opTask.getResult();

        try {
            getTaskService().reactivateServiceThreads(opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.reactivateServiceThreadsPerformed.success").getString());
            }
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.reactivateServiceThreadsPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        getTable().refreshTable(NodeType.class, target);
        target.add(getTable());
    }


    private List<IColumn<SelectableBean<NodeType>, String>> initNodeColumns() {
        List<IColumn<SelectableBean<NodeType>, String>> columns = new ArrayList<>();

        columns.add(new EnumPropertyColumn<>(createStringResource("pageTasks.node.executionStatus"),
                SelectableBeanImpl.F_VALUE + "." + NodeType.F_EXECUTION_STATUS) {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("pageTasks.node.contact")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<NodeType>>> item, String componentId, IModel<SelectableBean<NodeType>> rowModel) {
                item.add(new Label(componentId, () -> getContactLabel(rowModel)));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("pageTasks.node.lastCheckInTime")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<NodeType>>> item, String componentId,
                                     final IModel<SelectableBean<NodeType>> rowModel) {
                item.add(new Label(componentId, (IModel<Object>) () -> getLastCheckInTime(rowModel)));
            }
        });
        CheckBoxColumn<SelectableBean<NodeType>> check = new CheckBoxColumn<>(createStringResource("pageTasks.node.clustered"), SelectableBeanImpl.F_VALUE + "." + NodeType.F_CLUSTERED);
        check.setEnabled(false);
        columns.add(check);
        columns.add(new AbstractColumn<>(createStringResource("pageTasks.node.statusMessage")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<NodeType>>> item, String componentId, IModel<SelectableBean<NodeType>> rowModel) {
                String statusMessage;
                if (rowModel == null || rowModel.getObject() == null) {
                    statusMessage = "";
                } else {
                    NodeType node = rowModel.getObject().getValue();
                    if (node.getConnectionResult() != null && node.getConnectionResult().getStatus() != OperationResultStatusType.SUCCESS &&
                            StringUtils.isNotEmpty(node.getConnectionResult().getMessage())) {
                        statusMessage = node.getConnectionResult().getMessage();
                    } else if (node.getErrorStatus() != null && node.getErrorStatus() != NodeErrorStatusType.OK) {
                        statusMessage = node.getErrorStatus().toString();         // TODO: explain and localize this
                    } else if (node.getExecutionStatus() == NodeExecutionStatusType.ERROR) {      // error status not specified
                        statusMessage = "Unspecified error (or the node is just starting or shutting down)";
                    } else {
                        statusMessage = "";
                    }
                }

                item.add(new Label(componentId, statusMessage));
            }
        });

        return columns;
    }

    private String getContactLabel(IModel<SelectableBean<NodeType>> model) {

        NodeType node = model.getObject().getValue();
        if (node == null) {
            return null;
        }
        String url = node.getUrl();
        if (url != null) {
            return url;
        }
        return node.getHostname();

    }

    private String getLastCheckInTime(IModel<SelectableBean<NodeType>> nodeModel) {
        SelectableBean<NodeType> bean = nodeModel.getObject();
        if (bean == null) {
            return "";
        }
        NodeType node = bean.getValue();
        XMLGregorianCalendar xmlGregTime = node.getLastCheckInTime();
        if (xmlGregTime == null) {
            return "";
        }
        Long time = MiscUtil.asDate(xmlGregTime).getTime();
        if (time == 0) {
            return "";
        }

        return createStringResource("pageTasks.message.getLastCheckInTime", DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - time, true, true)).getString();
    }

    private List<InlineMenuItem> createNodesInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.startScheduler")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<NodeType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        startSchedulersPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_START_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.startSchedulerAction").getString();
                return PageNodes.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.stopScheduler")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<NodeType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        stopSchedulersPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_STOP_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.stopSchedulerAction").getString();
                return PageNodes.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageTasks.button.stopSchedulerAndTasks")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<NodeType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        stopSchedulersAndTasksPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.stopSchedulerTasksAction").getString();
                return PageNodes.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteNode")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<NodeType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteNodesPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteAction").getString();
                return PageNodes.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        return items;
    }

    private void startSchedulersPerformed(AjaxRequestTarget target, IModel<SelectableBean<NodeType>> selectedNode) {
        Task opTask = createSimpleTask(OPERATION_START_SCHEDULERS);
        OperationResult result = opTask.getResult();

        List<NodeType> selectedNodes = getSelectedNodes(target, selectedNode);
        if (selectedNodes.isEmpty()) {
            return;
        }
        try {
            getTaskService().startSchedulers(getNodeIdentifiers(selectedNodes), opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.startSchedulersPerformed.success").getString());
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.startSchedulersPerformed.fatalError").getString(), e);
        }

        showResult(result);
        getTable().refreshTable(NodeType.class, target);
        target.add(getTable());
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target, IModel<SelectableBean<NodeType>> model) {
        List<NodeType> selectedNodes = getSelectedNodes(target, model);
        if (CollectionUtils.isEmpty(selectedNodes)) {
            return;
        }
        Task opTask = createSimpleTask(OPERATION_STOP_SCHEDULERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().stopSchedulers(getNodeIdentifiers(selectedNodes), opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.stopSchedulersPerformed.success").getString());
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.stopSchedulersPerformed.fatalError").getString(), e);
        }
        showResult(result);
        getTable().refreshTable(NodeType.class, target);
        target.add(getTable());
    }

    private void stopSchedulersAndTasksPerformed(AjaxRequestTarget target, IModel<SelectableBean<NodeType>> selectedNode) {
        List<NodeType> selectedNodes = getSelectedNodes(target, selectedNode);
        if (CollectionUtils.isEmpty(selectedNodes)) {
            return;
        }

        Task opTask = createSimpleTask(OPERATION_STOP_SCHEDULERS_AND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            boolean suspended = getTaskService().stopSchedulersAndTasks(getNodeIdentifiers(selectedNodes), WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS,
                            createStringResource("pageTasks.message.stopSchedulersAndTasksPerformed.success").getString());
                } else {
                    result.recordWarning(
                            createStringResource("pageTasks.message.stopSchedulersAndTasksPerformed.warning").getString());
                }
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.stopSchedulersAndTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        getTable().refreshTable(NodeType.class, target);
        target.add(getTable());

    }

    private void deleteNodesPerformed(AjaxRequestTarget target, IModel<SelectableBean<NodeType>> selectedNode) {
        List<NodeType> selectedNodes = getSelectedNodes();
        if (CollectionUtils.isEmpty(selectedNodes)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_NODES);
        Task task = createSimpleTask(OPERATION_DELETE_NODES);

        for (NodeType nodeDto : selectedNodes) {
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            deltas.add(getPrismContext().deltaFactory().object().createDeleteDelta(NodeType.class, nodeDto.getOid()));
            try {
                getModelService().executeChanges(deltas, null, task, result);
            } catch (Exception e) { // until java 7 we do it in this way
                result.recordFatalError(createStringResource("pageTasks.message.deleteNodesPerformed.fatalError").getString()
                        + nodeDto.getNodeIdentifier(), e);
            }
        }

        result.computeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS,
                    createStringResource("pageTasks.message.deleteNodesPerformed.success").getString());
        }
        showResult(result);

        getTable().refreshTable(NodeType.class, target);
        target.add(getTable());
    }

    private List<NodeType> getSelectedNodes(AjaxRequestTarget target, IModel<SelectableBean<NodeType>> selectedNode) {
        if (selectedNode != null) {
            return Arrays.asList(selectedNode.getObject().getValue());
        }

        List<NodeType> selectedNodes = getSelectedNodes();
        if (CollectionUtils.isEmpty(selectedNodes)) {
            warn("PageNodes.nothing.selected");
            target.add(getFeedbackPanel());
        }
        return selectedNodes;
    }

    private List<String> getNodeIdentifiers(List<NodeType> selectedNodes) {
        return selectedNodes.stream().map(n -> n.getNodeIdentifier()).collect(Collectors.toList());
    }

    private List<NodeType> getSelectedNodes() {
        return getTable().getSelectedObjects();
    }

    private MainObjectListPanel<NodeType> getTable() {
        return (MainObjectListPanel<NodeType>) get(ID_TABLE);
    }

    private IModel<String> getNodeConfirmationMessageModel(ColumnMenuAction action, String actionName) {
        if (action.getRowModel() == null) {
            return createStringResource("pageTasks.message.confirmationMessageForMultipleNodeObject", actionName,
                    getTable().getSelectedObjectsCount());
        } else {
            String objectName = ((NodeDto) (action.getRowModel().getObject())).getName();
            return createStringResource("pageTasks.message.confirmationMessageForSingleNodeObject", actionName, objectName);
        }

    }

}
