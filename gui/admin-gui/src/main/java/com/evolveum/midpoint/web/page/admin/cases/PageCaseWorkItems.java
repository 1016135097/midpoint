/*
 * Copyright (c) 2010-2018 Evolveum et al.
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

package com.evolveum.midpoint.web.page.admin.cases;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.ContainerListDataProvider;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseWorkItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.cases.dto.SearchingUtils;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.reports.component.SingleValueChoosePanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.wf.util.QueryUtils;

/**
 * @author bpowers
 */
public abstract class PageCaseWorkItems extends PageAdminCaseWorkItems {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItems.class);

    private static final String DOT_CLASS = PageCaseWorkItems.class.getName() + ".";
    private static final String PARAMETER_CASE_ID = "caseId";
    private static final String PARAMETER_CASE_WORK_ITEM_ID = "caseWorkItemId";

    // Search Form
    private static final String ID_SEARCH_FILTER_FORM = "searchFilterForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_SEARCH_FILTER_RESOURCE = "filterResource";
    private static final String ID_SEARCH_FILTER_ASSIGNEE_CONTAINER = "filterAssigneeContainer";
    private static final String ID_SEARCH_FILTER_ASSIGNEE = "filterAssignee";
    private static final String ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES = "filterIncludeClosedCases";
    // Data Table
    private static final String ID_CASE_WORK_ITEMS_TABLE = "caseWorkItemsTable";
    private static final String ID_BUTTON_BAR = "buttonBar";
    // Buttons
    private static final String ID_CREATE_CASE_BUTTON = "createCaseButton";

    private LoadableModel<Search> searchModel = null;
    private boolean all;

    public PageCaseWorkItems(boolean all) {
        this.all = all;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private ObjectQuery createQuery() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectQuery query;
        boolean authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
        S_FilterEntryOrEmpty queryStart = getPrismContext().queryFor(CaseWorkItemType.class);
        if (all && authorizedToSeeAll) {
            query = queryStart.build();
        } else {
//             not authorized to see all => sees only allocated to him (not quite what is expected, but sufficient for the time being)
            query = QueryUtils.filterForAssignees(queryStart, SecurityUtils.getPrincipalUser(),
                    OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getRelationRegistry())
                    .and().item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull().build();
        }
//        IsolatedCheckBoxPanel includeClosedCases = (IsolatedCheckBoxPanel) getCaseWorkItemsSearchField(ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES);
//        if (includeClosedCases == null || !includeClosedCases.getValue()) {
            query.addFilter(
                getPrismContext().queryFor(CaseWorkItemType.class)
                        .not()
                        .item(PrismConstants.T_PARENT, CaseType.F_STATE)
                        .eq(SchemaConstants.CASE_STATE_CLOSED)
                        .build()
                        .getFilter()
            );
//        }

        // Resource Filter
//        SingleValueChoosePanel<ObjectReferenceType, ObjectType> resourceChoice = (SingleValueChoosePanel) getCaseWorkItemsSearchField(ID_SEARCH_FILTER_RESOURCE);
//        if (resourceChoice != null) {
//            List<ObjectType> resources = resourceChoice.getModelObject();
//            if (resources != null && resources.size() > 0) {
//                ObjectType resource = resources.get(0);
//                if (resource != null) {
//                    query.addFilter(
                            // TODO MID-3581
//                        getPrismContext().queryFor(CaseWorkItemType.class)
//                                .item(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF).ref(ObjectTypeUtil.createObjectRef(resource,
//		                        getPrismContext()).asReferenceValue()).buildFilter()
//                    );
//                }
//            }
//        }

        return query;
    }

    private void initLayout() {
        ContainerableListPanel workItemsPanel = new ContainerableListPanel(ID_CASE_WORK_ITEMS_TABLE,
                UserProfileStorage.TableId.PAGE_CASE_WORK_ITEMS_PANEL) {
            @Override
            protected Class getType() {
                return CaseWorkItemType.class;
            }

            @Override
            protected PageStorage getPageStorage() {
                return PageCaseWorkItems.this.getSessionStorage().getWorkItemStorage();
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> initColumns() {
                return PageCaseWorkItems.this.initColumns();
            }
        };
        workItemsPanel.setOutputMarkupId(true);
        add(workItemsPanel);
    }

    private List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> initColumns(){
        List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<PrismContainerValueWrapper<CaseWorkItemType>>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(CaseWorkItemType.COMPLEX_TYPE));
            }

        });
        columns.add(new LinkColumn<PrismContainerValueWrapper<CaseWorkItemType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(ColumnUtils.unwrapRowModel(rowModel).getName());
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                //TODO should we check any authorization?
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                PageParameters pageParameters = new PageParameters();
                CaseWorkItemType caseWorkItemType = rowModel.getObject().getRealValue();
                CaseType parentCase = CaseTypeUtil.getCase(caseWorkItemType);
                WorkItemId workItemId = WorkItemId.create(parentCase != null ? parentCase.getOid() : "", caseWorkItemType.getId());
                pageParameters.add(OnePageParameterEncoder.PARAMETER, workItemId.asString());
                navigateToNext(PageCaseWorkItem.class, pageParameters);
            }
        });

        columns.addAll(ColumnUtils.getDefaultWorkItemColumns(PageCaseWorkItems.this));
        return columns;
    }

    private Table getCaseWorkItemsTable() {
        return (Table) get(createComponentPath(ID_CASE_WORK_ITEMS_TABLE));
    }

    private Panel getCaseWorkItemsSearchField(String itemPath) {
        return (Panel) get(createComponentPath(ID_SEARCH_FILTER_FORM, itemPath));
    }

    private void initSearch() {
        final Form searchFilterForm = new Form(ID_SEARCH_FILTER_FORM);
        add(searchFilterForm);
        searchFilterForm.setOutputMarkupId(true);

        List<Class<? extends ObjectType>> allowedClasses = new ArrayList<>();
        allowedClasses.add(ResourceType.class);
        MultiValueChoosePanel<ObjectType> resource = new SingleValueChoosePanel<ObjectReferenceType, ObjectType>(
                ID_SEARCH_FILTER_RESOURCE, allowedClasses, objectReferenceTransformer,
                new PropertyModel<ObjectReferenceType>(Model.of(new ObjectViewDto()), ObjectViewDto.F_NAME)){

            @Override
            protected void choosePerformedHook(AjaxRequestTarget target, List<ObjectType> selected) {
                super.choosePerformedHook(target, selected);
                searchFilterPerformed(target);
            }

            @Override
            protected void removePerformedHook(AjaxRequestTarget target, ObjectType value) {
                super.removePerformedHook(target, value);
                searchFilterPerformed(target);
            }
        };
        searchFilterForm.add(resource);

        allowedClasses = new ArrayList<>();
        allowedClasses.add(UserType.class);
        WebMarkupContainer assigneeContainer = new WebMarkupContainer(ID_SEARCH_FILTER_ASSIGNEE_CONTAINER);
        MultiValueChoosePanel<ObjectType> assignee = new SingleValueChoosePanel<ObjectReferenceType, ObjectType>(
                ID_SEARCH_FILTER_ASSIGNEE, allowedClasses, objectReferenceTransformer,
                new PropertyModel<ObjectReferenceType>(Model.of(new ObjectViewDto()), ObjectViewDto.F_NAME)){

            @Override
            protected void choosePerformedHook(AjaxRequestTarget target, List<ObjectType> selected) {
                super.choosePerformedHook(target, selected);
                searchFilterPerformed(target);
            }

            @Override
            protected void removePerformedHook(AjaxRequestTarget target, ObjectType value) {
                super.removePerformedHook(target, value);
                searchFilterPerformed(target);
            }
        };
        assigneeContainer.add(assignee);
        assigneeContainer.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isAuthorizedToSeeAllCases();
            }
        });
        searchFilterForm.add(assigneeContainer);

        IsolatedCheckBoxPanel includeClosedCases = new IsolatedCheckBoxPanel(ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES, new Model<Boolean>(false)) {
            private static final long serialVersionUID = 1L;

            public void onUpdate(AjaxRequestTarget target) {
                searchFilterPerformed(target);
            }
        };
        searchFilterForm.add(includeClosedCases);
    }

    private boolean isAuthorizedToSeeAllCases() {
        boolean authorizedToSeeAll;
		try {
			authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
			return all && authorizedToSeeAll;
		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException
				| CommunicationException | ConfigurationException | SecurityViolationException e) {
			// TODO handle more cleanly
            throw new SystemException("Couldn't evaluate authoriztion: "+e.getMessage(), e);
		}
    }
    //endregion

    //region Actions
    private void searchFilterPerformed(AjaxRequestTarget target) {
        ObjectQuery query;
        try {
            query = createQuery();
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException
				| ConfigurationException | SecurityViolationException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't create case work item query", e);
        }

        Table panel = getCaseWorkItemsTable();
        DataTable table = panel.getDataTable();
        CaseWorkItemDtoProvider provider = (CaseWorkItemDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        table.setCurrentPage(0);

        target.add(getFeedbackPanel());
        target.add((Component) getCaseWorkItemsTable());
    }
    //endregion

    private Function<ObjectType, ObjectReferenceType> objectReferenceTransformer =
            (Function<ObjectType, ObjectReferenceType> & Serializable) (ObjectType o) ->
                    ObjectTypeUtil.createObjectRef(o, getPrismContext());

    private static class ButtonBar extends Fragment {

        private static final long serialVersionUID = 1L;

        public <O extends ObjectType> ButtonBar(String id, String markupId, PageCaseWorkItems page) {
            super(id, markupId, page);

            initLayout(page);
        }

        private <O extends ObjectType> void initLayout(final PageCaseWorkItems page) {

            AjaxButton createCase = new AjaxButton(ID_CREATE_CASE_BUTTON, page.createStringResource("PageCaseWorkItems.button.createCase")) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    page.navigateToNext(PageCase.class);
                }

            };
            createCase.add(new VisibleEnableBehaviour(){
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible(){

                    boolean isVisible = false;
                    try {
                        PrismObject<CaseType> objectToCreate = new CaseType().asPrismObject();
                        if (objectToCreate != null) {
                            page.getMidpointApplication().getPrismContext().adopt(objectToCreate);
                        }
                        isVisible = ((PageBase) getPage()).isAuthorized(ModelAuthorizationAction.ADD.getUrl(),
                                null, objectToCreate, null, null, null);
                    } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException
            				| ConfigurationException | SecurityViolationException ex) {
                        LOGGER.error("Failed to check authorization for ADD action on new object of " + CaseType.class.getSimpleName()
                                + " type, ", ex);
                    }
                    return isVisible;
                }
            });
            createCase.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return page.isAuthorizedToSeeAllCases();
                }
            });
            add(createCase);
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> getQueryOptions() {
        return getOperationOptionsBuilder()
                .item(AbstractWorkItemType.F_ASSIGNEE_REF).resolve()
                .item(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF).resolve()
                .item(PrismConstants.T_PARENT, CaseType.F_TARGET_REF).resolve()
                .build();
    }
}
