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

package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.validation.Schema;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.TypedAssignablePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import org.springframework.expression.spel.ast.Assign;

/**
 * Created by honchar.
 */
public class AbstractRoleAssignmentPanel extends AssignmentPanel {

	private static final long serialVersionUID = 1L;

    private static final String ID_RELATION = "relation";
    private static final String ID_SHOW_ALL_ASSIGNMENTS_BUTTON = "showAllAssignmentsButton";


    public AbstractRoleAssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
    	super(id, assignmentContainerWrapperModel);
    }

    protected void initCustomLayout(WebMarkupContainer assignmentsContainer){

    	DropDownChoicePanel<RelationTypes> relation = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), Model.of(), this, true);
        relation.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            	refreshTable(target);
            }
        });
        relation.setOutputMarkupId(true);
        relation.setOutputMarkupPlaceholderTag(true);
        relation.add(new VisibleEnableBehaviour() {

        	private static final long serialVersionUID = 1L;

			@Override
        	public boolean isVisible() {
        		return AbstractRoleAssignmentPanel.this.isRelationVisible();
        	}

        });
        assignmentsContainer.addOrReplace(relation);

        AjaxButton showAllAssignmentsButton = new AjaxButton(ID_SHOW_ALL_ASSIGNMENTS_BUTTON,
                createStringResource("AssignmentTablePanel.menu.showAllAssignments")) {

        	private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                showAllAssignments(ajaxRequestTarget);
            }
        };
        assignmentsContainer.addOrReplace(showAllAssignmentsButton);
        showAllAssignmentsButton.setOutputMarkupId(true);

    }

    private DropDownChoicePanel<RelationTypes> getRelationPanel() {
    	return (DropDownChoicePanel<RelationTypes>) getAssignmentContainer().get(ID_RELATION);
    }


       protected void showAllAssignments(AjaxRequestTarget target) {
       }

       @Override
    protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
    	   TypedAssignablePanel<RoleType> panel = new TypedAssignablePanel<RoleType>(
                   getPageBase().getMainPopupBodyId(), RoleType.class, true, getPageBase()) {

    		   private static final long serialVersionUID = 1L;

               @Override
               protected void addPerformed(AjaxRequestTarget target, List selected, QName relation) {
            	   super.addPerformed(target, selected, relation);
                   addSelectedAssignmentsPerformed(target, selected, relation);
               }

           };
           panel.setOutputMarkupId(true);
           getPageBase().showMainPopup(panel, target);
    }
       protected <T extends ObjectType> void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<T> assignmentsList, QName relation){
           if (assignmentsList == null || assignmentsList.isEmpty()){
                   warn(getParentPage().getString("AssignmentTablePanel.message.noAssignmentSelected"));
                   target.add(getPageBase().getFeedbackPanel());
                   return;
           }
           
           for (T object : assignmentsList){
        	   PrismContainerValue<AssignmentType> newAssignment = getModelObject().getItem().createNewValue();
        	   ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(object, relation);
        	   AssignmentType assignmentType = newAssignment.asContainerable();
        	   if (ResourceType.class.equals(object.getClass())) {
        		   ConstructionType constructionType = new ConstructionType();
        		   constructionType.setResourceRef(ref);
        		   assignmentType.setConstruction(constructionType);
        	   } else {
        		   assignmentType.setTargetRef(ref);
        	   }
        	   ContainerValueWrapper<AssignmentType> newAssignmentValueWrapper = createNewAssignmentContainerValueWrapper(newAssignment);
//        	   getModelObject().getValues().add(newAssignmentValueWrapper);
               
//               ContainerValueWrapper<AssignmentType> valueWrapper = factory.createContainerValueWrapper(getModelObject(), newAssignment.asPrismContainerValue(),
//                       ValueStatus.ADDED, new ItemPath(FocusType.F_ASSIGNMENT));
//               getModelObject().getValues().add(valueWrapper);
           }

           refreshTable(target);

       }

    protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("ObjectReferenceType.relation")) {
            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
                String relation = assignmentModel.getObject().getContainerValue().getValue().getTargetRef() != null ?
                        assignmentModel.getObject().getContainerValue().getValue().getTargetRef().getRelation().getLocalPart() : "";
                item.add(new Label(componentId, relation));
            }
        });

        return columns;
    }

    private VisibleEnableBehaviour visibleIfRoleBehavior(IModel<AssignmentEditorDto> assignmentModel){
        return new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return AssignmentEditorDtoType.ROLE.equals(assignmentModel.getObject().getType());
            }
        };
    }

    protected void initPaging(){
        getAssignmentsStorage().setPaging(ObjectPaging.createPaging(0, (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE)));
    }

	@Override
	protected TableId getTableId() {
		return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
	}

	@Override
	protected int getItemsPerPage() {
		return (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE);
	}

	protected ObjectQuery createObjectQuery() {
		return QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
				.block()
					.not()
						.item(new ItemPath(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF))
							.isNull()
				.endBlock()
					.or()
						.item(new ItemPath(AssignmentType.F_TARGET_REF))
							.ref(getRelation())
				.build();
	};

	private QName getRelation() {
		DropDownChoicePanel<RelationTypes> relationPanel = getRelationPanel();
		if (relationPanel == null) {
		 return PrismConstants.Q_ANY;
		}

		if (relationPanel.getModel() == null) {
			return PrismConstants.Q_ANY;
		}

		if (relationPanel.getModel().getObject() == null) {
			return PrismConstants.Q_ANY;
		}

		return relationPanel.getModel().getObject().getRelation();
	}

	@Override
	protected AbstractAssignmentDetailsPanel createDetailsPanel(String idAssignmentDetails, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> model) {
		return new AbstractRoleAssignmentDetailsPanel(ID_ASSIGNMENT_DETAILS, form, model);
	}

	protected boolean isRelationVisible() {
		return true;
	}

}
