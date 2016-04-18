/*
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
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.FocusBrowserPanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider2;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentTabPanel.Operation;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


/**
 * Implementation classes : ResourceContentResourcePanel, ResourceContentRepositoryPanel
 * @author katka
 *
 */
public abstract class ResourceContentPanel extends Panel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ResourceContentPanel.class);

	private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";
	private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
	private static final String OPERATION_LOAD_SHADOW_OWNER = DOT_CLASS + "loadOwner";
	private static final String OPERATION_UPDATE_STATUS = DOT_CLASS + "updateStatus";
	private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
	private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

	private static final String ID_TABLE = "table";

	private PageBase pageBase;
	private ShadowKindType kind;
	private String intent;
	private QName objectClass;
	
	private LoadableModel<Search> searchModel;
	
	IModel<PrismObject<ResourceType>> resourceModel;

	public PageBase getPageBase() {
		return pageBase;
	}

	public ShadowKindType getKind() {
		return kind;
	}

	public String getIntent() {
		return intent;
	}

	public IModel<PrismObject<ResourceType>> getResourceModel() {
		return resourceModel;
	}

	public QName getObjectClass() {
		return objectClass;
	}
	
	public RefinedObjectClassDefinition getDefinitionByKind() throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema
				.getRefinedSchema(resourceModel.getObject(), getPageBase().getPrismContext());
		return refinedSchema.getRefinedDefinition(getKind(), getIntent());

	}
	
	public RefinedObjectClassDefinition getDefinitionByObjectClass() throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema
				.getRefinedSchema(resourceModel.getObject(), getPageBase().getPrismContext());
		return refinedSchema.getRefinedDefinition(getObjectClass());

	}

	public ResourceContentPanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			QName objectClass, ShadowKindType kind, String intent, PageBase pageBase) {
		super(id);
		this.pageBase = pageBase;
		this.kind = kind;
		this.resourceModel = resourceModel;
		this.intent = intent;
		this.objectClass = objectClass;
		initLayout();
	}

	private void initLayout() {
		
		searchModel = new LoadableModel<Search>(false) {

			@Override
			public Search load() {

				return ResourceContentPanel.this.createSearch();
			}
		};

		
		ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType> provider = new ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType>(
				this, ShadowType.class);

		try {

			ObjectQuery query = createQuery();
			provider.setQuery(query);
			
			if (query == null) {
				Label label = new Label(ID_TABLE, "Nothing to show. Select intent to search");
				add(label);
				initCustomLayout();
				return;

			}

		} catch (SchemaException e) {
			Label label = new Label(ID_TABLE, "Nothing to show. Select intent to search");
			add(label);
			initCustomLayout();
			return;
		}

		provider.setEmptyListOnNullQuery(true);
		provider.setSort(null);
		createSearchOptions(provider);
		List<IColumn> columns = initColumns();
//		ObjectListPanel<ShadowType> table = new ObjectListPanel<ShadowType>(ID_TABLE, ShadowType.class, getPageBase()){
//			@Override
//			protected List<IColumn<SelectableBean<ShadowType>, String>> initColumns() {
//				return (List) ResourceContentPanel.this.initColumns();
//			}
//		};
		final BoxedTablePanel<SelectableBean<ShadowType>> table = new BoxedTablePanel(ID_TABLE, provider, columns,
				UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL, 10) {
			
			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return new SearchFormPanel(headerId, searchModel) {

					@Override
					protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
						ResourceContentPanel.this.searchPerformed(query, target);
					}
				};
			}
		}; // parentPage.getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL)
		table.setOutputMarkupId(true);
		add(table);

		initCustomLayout();
	}

	private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
		BoxedTablePanel<SelectableBean<ShadowType>> table = getTable();
		ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType> provider = (ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType>) table
				.getDataTable().getDataProvider();
//		BaseSortableDataProvider<SelectableBean<T>> provider = getDataProvider();
		ObjectQuery baseQuery;
		try {
			baseQuery = createQuery();
			if (baseQuery == null){
				warn("Could not search objects if either kind/intet or object class is set.");
				return;
			}
		} catch (SchemaException e) {
			warn("Could not create query.");
			return;
		}
		
		
		ObjectQuery customQuery = ObjectQuery.createObjectQuery(AndFilter.createAnd(baseQuery.getFilter(), query.getFilter()));
		provider.setQuery(customQuery);

		// RolesStorage storage = getSessionStorage().getRoles();
		// storage.setRolesSearch(searchModel.getObject());
		// storage.setRolesPaging(null);

		table = getTable();
		table.setCurrentPage(null);
		target.add((Component) table);
		target.add(getPageBase().getFeedbackPanel());

	}
	
	protected abstract void initCustomLayout();

	protected ObjectQuery createQuery() throws SchemaException {
		ObjectQuery baseQuery = null;

		if (kind == null) { 
			if (objectClass == null){
				return null;
			}
			return ObjectQueryUtil.createResourceAndObjectClassQuery(
					resourceModel.getObject().getOid(), objectClass,
					getPageBase().getPrismContext());
		}
		
		RefinedObjectClassDefinition rOcDef = getDefinitionByKind();
		if (rOcDef != null) {
			if (rOcDef.getKind() != null) {
				baseQuery = ObjectQueryUtil.createResourceAndKindIntent(resourceModel.getObject().getOid(),
						rOcDef.getKind(), rOcDef.getIntent(), getPageBase().getPrismContext());
			} else {
				baseQuery = ObjectQueryUtil.createResourceAndObjectClassQuery(
						resourceModel.getObject().getOid(), rOcDef.getTypeName(),
						getPageBase().getPrismContext());
			}
		}
		return baseQuery;
	}
	
	protected abstract Search createSearch();
	

	private void createSearchOptions(ObjectDataProvider2 provider) {

		Collection<SelectorOptions<GetOperationOptions>> opts = SelectorOptions.createCollection(
				ShadowType.F_ASSOCIATION, GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));

		if (addAdditionalOptions() != null) {
			opts.add(addAdditionalOptions()); // new
												// SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch()));
		}
		provider.setUseObjectCounting(isUseObjectCounting());
		provider.setOptions(opts);

	}

	private StringResourceModel createStringResource(String key) {
		return pageBase.createStringResource(key);
	}

	private List<IColumn> initColumns() {

		List<ColumnTypeDto> columnDefs = Arrays.asList(
				new ColumnTypeDto("ShadowType.synchronizationSituation",
						SelectableBean.F_VALUE + ".synchronizationSituation",
						ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.intent", SelectableBean.F_VALUE + ".intent",
						ShadowType.F_INTENT.getLocalPart()));

		List<IColumn> columns = new ArrayList<>();
		IColumn column = new CheckBoxColumn(new Model<String>(), SelectableBean.F_SELECTED);
		columns.add(column);

		columns.add(ColumnUtils.getShadowIconColumn());
		
		column = new LinkColumn<SelectableBean<ShadowType>>(createStringResource("pageContentAccounts.name"),
				SelectableBean.F_VALUE + ".name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel) {
				SelectableBean<ShadowType> shadow = rowModel.getObject();
				ShadowType shadowType = shadow.getValue();
				shadowDetailsPerformed(target, WebComponentUtil.getName(shadowType), shadowType.getOid());
			}
		};
		columns.add(column);

		column = new AbstractColumn<SelectableBean<ShadowType>, String>(
				createStringResource("pageContentAccounts.identifiers")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
					String componentId, IModel<SelectableBean<ShadowType>> rowModel) {

				SelectableBean<ShadowType> dto = rowModel.getObject();
				RepeatingView repeater = new RepeatingView(componentId);

				for (ResourceAttribute<?> attr : ShadowUtil.getAllIdentifiers(dto.getValue())) {
					repeater.add(new Label(repeater.newChildId(),
							attr.getElementName().getLocalPart() + ": " + attr.getRealValue()));

				}
				cellItem.add(repeater);

			}
		};
		columns.add(column);

		columns.addAll(ColumnUtils.createColumns(columnDefs));
		column = new LinkColumn<SelectableBean<ShadowType>>(createStringResource("pageContentAccounts.owner"),
				true) {

			@Override
			protected IModel createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel) {

				return new AbstractReadOnlyModel<FocusType>() {

					@Override
					public FocusType getObject() {
						FocusType owner = loadShadowOwner(rowModel);
						if (owner == null) {
							return null;
						}
						return owner;

					}

				};
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel) {
				SelectableBean<ShadowType> shadow = rowModel.getObject();
				ShadowType shadowType = shadow.getValue();
				ownerDetailsPerformed(target, this.getModelObjectIdentifier());
			}
		};
		columns.add(column);
		
		columns.add(new LinkColumn<SelectableBean<ShadowType>>(createStringResource("PageAccounts.accounts.result")){

            @Override
            protected IModel<String> createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel){
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                    	return getResultLabel(rowModel);
                    }
                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel) {
            	OperationResultType resultType = getResult(rowModel);
            	OperationResult result = OperationResult.createOperationResult(resultType);
            	
            	OperationResultPanel body = new OperationResultPanel(ResourceContentPanel.this.getPageBase().getMainPopupBodyId(), new Model<OpResult>(OpResult.getOpResult(pageBase, result)));
            	body.setOutputMarkupId(true);
            	ResourceContentPanel.this.getPageBase().showMainPopup(body, new Model<String>("Result"), target, 900, 500);
                
            }
        });

		column = new InlineMenuHeaderColumn(createHeaderMenuItems());
		columns.add(column);

		return columns;
	}
	
	private OperationResultType getResult(IModel<SelectableBean<ShadowType>> model) {
		 ShadowType shadow = getShadow(model);
	        if (shadow == null){
	        	return null;
	        }
	        OperationResultType result = shadow.getResult();
	        if (result == null) {
	        	return null;
	        }
	        return result;
	}
	
	private String getResultLabel(IModel<SelectableBean<ShadowType>> model){
   	 
		OperationResultType result = getResult(model);
        if (result == null){
        	return "";
        }
        
        StringBuilder b = new StringBuilder(createStringResource("FailedOperationTypeType." + getShadow(model).getFailedOperationType()).getObject());
        b.append(":");
        b.append(createStringResource("OperationResultStatusType." + result.getStatus()).getObject());
        
        return b.toString();
   }
   
   private ShadowType getShadow(IModel<SelectableBean<ShadowType>> model){
   	 if(model == null || model.getObject() == null || model.getObject().getValue() == null){
            return null;
        }
   	 
   	 return (ShadowType) model.getObject().getValue();
   }

	private void ownerDetailsPerformed(AjaxRequestTarget target, String ownerOid) {
		if (StringUtils.isEmpty(ownerOid)) {

			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, ownerOid);
		setResponsePage(PageUser.class, parameters);
	}

	private <F extends FocusType> F loadShadowOwner(IModel<SelectableBean<ShadowType>> model) {
		F owner = null;

		ShadowType shadow = model.getObject().getValue();
		String shadowOid;
		if (shadow != null) {
			shadowOid = shadow.getOid();
		} else {
			return null;
		}

		Task task = pageBase.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

		try {
			PrismObject prismOwner = pageBase.getModelService().searchShadowOwner(shadowOid, null, task,
					result);

			if (prismOwner != null) {
				owner = (F) prismOwner.asObjectable();
			}
		} catch (ObjectNotFoundException exception) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		} catch (Exception ex) {
			result.recordFatalError(pageBase.getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
			LoggingUtils.logException(LOGGER, "Could not load owner of account with oid: " + shadowOid, ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebComponentUtil.showResultInPage(result)) {
			pageBase.showResult(result, false);
		}

		return owner;
	}

	private void shadowDetailsPerformed(AjaxRequestTarget target, String accountName, String accountOid) {
		if (StringUtils.isEmpty(accountOid)) {
			error(pageBase.getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
					accountOid));
			target.add(pageBase.getFeedbackPanel());
			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, accountOid);
		setResponsePage(PageAccount.class, parameters);
	}

	private <F extends FocusType> F loadShadowOwner(String shadowOid) {

		Task task = pageBase.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

		try {
			PrismObject prismOwner = pageBase.getModelService().searchShadowOwner(shadowOid, null, task,
					result);

			if (prismOwner != null) {
				return (F) prismOwner.asObjectable();
			}
		} catch (ObjectNotFoundException exception) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		} catch (Exception ex) {
			result.recordFatalError(pageBase.getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
			LoggingUtils.logException(LOGGER, "Could not load owner of account with oid: " + shadowOid, ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebComponentUtil.showResultInPage(result)) {
			pageBase.showResult(result, false);
		}

		return null;
	}

	private List<InlineMenuItem> createHeaderMenuItems() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						 updateResourceObjectStatusPerformed(target, true);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						 updateResourceObjectStatusPerformed(target, false);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						 deleteResourceObjectPerformed(target); 
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						 importResourceObject(target); 
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						changeOwner(target, null, Operation.REMOVE);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.changeOwner"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {

						FocusBrowserPanel<UserType> browser = new FocusBrowserPanel<UserType>(
								pageBase.getMainPopupBodyId(), UserType.class, WebComponentUtil.createFocusTypeList(), false, pageBase) {
							protected void onClick(AjaxRequestTarget target, UserType focus) {
								changeOwner(target, focus, Operation.MODIFY);
							}
						};

						pageBase.showMainPopup(browser, new Model<String>("ChangeOwner"), target, 900, 500);

					}
				}));

		return items;
	}
	
	protected void importResourceObject(AjaxRequestTarget target){
		List<SelectableBean<ShadowType>> selectedShadow = WebComponentUtil.getSelectedData(getTable());
		
		OperationResult result = new OperationResult(OPERATION_IMPORT_OBJECT);
		Task task = pageBase.createSimpleTask(OPERATION_IMPORT_OBJECT);
		
		if (selectedShadow == null && selectedShadow.isEmpty()){
			result.recordWarning("Nothing select to import");
			getPageBase().showResult(result);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		
		for (SelectableBean<ShadowType> shadow : selectedShadow) {
			try {
				getPageBase().getModelService().importFromResource(shadow.getValue().getOid(), task, result);
			} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
					| CommunicationException | ConfigurationException e) {
				result.recordPartialError("Could not import account " + shadow.getValue(), e);
				LOGGER.error("Could not import account {} ", shadow.getValue(), e);
				continue;
			}
		}
		
		result.computeStatusIfUnknown();
		getPageBase().showResult(result);
		target.add(getPageBase().getFeedbackPanel());
	}
	
	//TODO: as a task?
	protected void deleteResourceObjectPerformed(AjaxRequestTarget target){
		List<SelectableBean<ShadowType>> selectedShadow = WebComponentUtil.getSelectedData(getTable());
		
		
		OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
		Task task = pageBase.createSimpleTask(OPERATION_DELETE_OBJECT);
		
		if (selectedShadow == null && selectedShadow.isEmpty()){
			result.recordWarning("Nothing selected to delete");
			getPageBase().showResult(result);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		
		ModelExecuteOptions opts = createModelOptions();
		
		for (SelectableBean<ShadowType> shadow : selectedShadow){
			try {
				ObjectDelta<ShadowType> deleteDelta = ObjectDelta.createDeleteDelta(ShadowType.class, shadow.getValue().getOid(), getPageBase().getPrismContext());
				getPageBase().getModelService().executeChanges(WebComponentUtil.createDeltaCollection(deleteDelta), opts, task, result);
			} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
					| ExpressionEvaluationException | CommunicationException | ConfigurationException
					| PolicyViolationException | SecurityViolationException e) {
				// TODO Auto-generated catch block
				result.recordPartialError("Could not delete object " + shadow.getValue(), e);
				LOGGER.error("Could not delete {}, using option {}", shadow.getValue(), opts, e);
				continue;
			}
		}
		
		result.computeStatusIfUnknown();
		getPageBase().showResult(result);
		target.add(getPageBase().getFeedbackPanel());
		
	}
	
	protected abstract ModelExecuteOptions createModelOptions();
	
	protected void updateResourceObjectStatusPerformed(AjaxRequestTarget target, boolean enabled){
List<SelectableBean<ShadowType>> selectedShadow = WebComponentUtil.getSelectedData(getTable());
		
		
		OperationResult result = new OperationResult(OPERATION_UPDATE_STATUS);
		Task task = pageBase.createSimpleTask(OPERATION_UPDATE_STATUS);
		
		if (selectedShadow == null && selectedShadow.isEmpty()){
			result.recordWarning("Nothing selected to update status");
			getPageBase().showResult(result);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		
		
		
		ModelExecuteOptions opts = createModelOptions();
		
		for (SelectableBean<ShadowType> shadow : selectedShadow){
			ActivationStatusType status = enabled ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
			try {	
				ObjectDelta<ShadowType> deleteDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, shadow.getValue().getOid(), SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, getPageBase().getPrismContext(), status);
				getPageBase().getModelService().executeChanges(WebComponentUtil.createDeltaCollection(deleteDelta), opts, task, result);
			} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
					| ExpressionEvaluationException | CommunicationException | ConfigurationException
					| PolicyViolationException | SecurityViolationException e) {
				// TODO Auto-generated catch block
				result.recordPartialError("Could not update status (to " + status+ ") for " + shadow.getValue(), e);
				LOGGER.error("Could not update status (to {}) for {}, using option {}", status, shadow.getValue(), opts, e);
				continue;
			}
		}
		
		result.computeStatusIfUnknown();
		getPageBase().showResult(result);
		target.add(getPageBase().getFeedbackPanel());
		
	
	}

	private PrismObjectDefinition getFocusDefinition() {
		return pageBase.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(FocusType.class);
	}

	private BoxedTablePanel getTable() {
		return (BoxedTablePanel) get(pageBase.createComponentPath(ID_TABLE));
	}

	private void changeOwner(AjaxRequestTarget target, FocusType ownerToChange, Operation operation) {
		List<SelectableBean<ShadowType>> selectedShadow = WebComponentUtil.getSelectedData(getTable());

		Collection<? extends ItemDelta> modifications = new ArrayList<>();

		ReferenceDelta delta = null;
		switch (operation) {

			case REMOVE:
				for (SelectableBean<ShadowType> selected : selectedShadow) {
					modifications = new ArrayList<>();
					FocusType owner = loadShadowOwner(selected.getValue().getOid());
					if (owner != null) {
						delta = ReferenceDelta.createModificationDelete(FocusType.F_LINK_REF,
								getFocusDefinition(),
								ObjectTypeUtil.createObjectRef(selected.getValue()).asReferenceValue());

						((Collection) modifications).add(delta);
						changeOwnerInternal(owner.getOid(), modifications, target);
					}
				}
				break;
			case MODIFY:
				if (!isSatisfyConstraints(selectedShadow)) {
					break;
				}

				ShadowType shadow = selectedShadow.iterator().next().getValue();
				FocusType owner = loadShadowOwner(shadow.getOid());
				if (owner != null) {
					delta = ReferenceDelta.createModificationDelete(FocusType.F_LINK_REF,
							getFocusDefinition(), ObjectTypeUtil.createObjectRef(shadow).asReferenceValue());

					((Collection) modifications).add(delta);
					changeOwnerInternal(owner.getOid(), modifications, target);
				}
				modifications = new ArrayList<>();

				delta = ReferenceDelta.createModificationAdd(FocusType.F_LINK_REF, getFocusDefinition(),
						ObjectTypeUtil.createObjectRef(shadow).asReferenceValue());
				((Collection) modifications).add(delta);
				changeOwnerInternal(ownerToChange.getOid(), modifications, target);

				break;
		}

	}

	private boolean isSatisfyConstraints(List selected) {
		if (selected.size() > 1) {
			error("Could not link to more than one owner");
			return false;
		}

		if (selected.isEmpty()) {
			warn("Could not link to more than one owner");
			return false;
		}

		return true;
	}

	private void changeOwnerInternal(String ownerOid, Collection<? extends ItemDelta> modifications,
			AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
		Task task = pageBase.createSimpleTask(OPERATION_CHANGE_OWNER);
		ObjectDelta objectDelta = ObjectDelta.createModifyDelta(ownerOid, modifications, FocusType.class,
				pageBase.getPrismContext());
		Collection deltas = new ArrayList<>();
		deltas.add(objectDelta);
		try {
			if (!deltas.isEmpty()) {
				pageBase.getModelService().executeChanges(deltas, null, task, result);

			}
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {

		}

		result.computeStatusIfUnknown();

		pageBase.showResult(result);
		target.add(pageBase.getFeedbackPanel());
		target.add(ResourceContentPanel.this);
	}

	protected abstract SelectorOptions<GetOperationOptions> addAdditionalOptions();

	protected abstract boolean isUseObjectCounting();

}
