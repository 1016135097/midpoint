package com.evolveum.midpoint.web.component.prism;

import java.util.List;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;

public class PrismContainerValueHeaderPanel<C extends Containerable> extends PrismHeaderPanel<ContainerValueWrapper<C>> {

	private static final long serialVersionUID = 1L;

	private static final String ID_SORT_PROPERTIES = "sortProperties";
    private static final String ID_SHOW_METADATA = "showMetadata";
    private static final String ID_SHOW_EMPTY_FIELDS = "showEmptyFields";
    private static final String ID_ADD_CHILD_CONTAINER = "addChildContainer";
    private static final String ID_CHILD_CONTAINERS_SELECTOR_PANEL = "childContainersSelectorPanel";
    private static final String ID_CHILD_CONTAINERS_LIST = "childContainersList";
    private static final String ID_ADD_BUTTON = "addButton";

    private boolean isChildContainersSelectorPanelVisible = false;
	
	public PrismContainerValueHeaderPanel(String id, IModel<ContainerValueWrapper<C>> model) {
		super(id, model);
	}

	@Override
	protected void initButtons() {
		VisibleEnableBehaviour buttonsVisibleBehaviour = new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return PrismContainerValueHeaderPanel.this.isButtonsVisible();
			}
		};

		ToggleIconButton showMetadataButton = new ToggleIconButton(ID_SHOW_METADATA,
				GuiStyleConstants.CLASS_ICON_SHOW_METADATA, GuiStyleConstants.CLASS_ICON_SHOW_METADATA) {
			private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget target) {
				ContainerValueWrapper<C> wrapper = PrismContainerValueHeaderPanel.this.getModelObject();
				wrapper.setShowMetadata(!wrapper.isShowMetadata());
				onButtonClick(target);
            }

			@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isShowMetadata();
			}
        };
		showMetadataButton.add(new AttributeModifier("title", new AbstractReadOnlyModel() {

			@Override
			public Object getObject() {
				return PrismContainerValueHeaderPanel.this.getModelObject() == null ? "" : (PrismContainerValueHeaderPanel.this.getModelObject().isShowMetadata() ?
						createStringResource("PrismObjectPanel.hideMetadata").getString() :
						createStringResource("PrismObjectPanel.showMetadata").getString());
			}
		}));
		showMetadataButton.add(new VisibleEnableBehaviour() {
			
			@Override
			public boolean isVisible() {
				for (ItemWrapper wrapper : getModelObject().getItems()) {
					if (MetadataType.COMPLEX_TYPE.equals(wrapper.getItemDefinition().getTypeName())) {
						return true;
					}
				}
				return false;
			}
			
		});
		add(showMetadataButton);

		ToggleIconButton showEmptyFieldsButton = new ToggleIconButton(ID_SHOW_EMPTY_FIELDS,
				GuiStyleConstants.CLASS_ICON_SHOW_EMPTY_FIELDS, GuiStyleConstants.CLASS_ICON_NOT_SHOW_EMPTY_FIELDS) {
			private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget target) {
				onShowEmptyClick(target);
            }

			
			@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isShowEmpty();
			}
        };
		showEmptyFieldsButton.setOutputMarkupId(true);

		showEmptyFieldsButton.add(buttonsVisibleBehaviour);
        add(showEmptyFieldsButton);

        ToggleIconButton sortPropertiesButton = new ToggleIconButton(ID_SORT_PROPERTIES,
        		GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {
        	private static final long serialVersionUID = 1L;

        	@Override
            public void onClick(AjaxRequestTarget target) {
        		ContainerValueWrapper<C> containerValueWrapper = PrismContainerValueHeaderPanel.this.getModelObject();
        		containerValueWrapper.setSorted(!containerValueWrapper.isSorted());
        		containerValueWrapper.sort(getPageBase());

                onButtonClick(target);
            }

        	@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isSorted();
			}
        };
        sortPropertiesButton.add(buttonsVisibleBehaviour);
        add(sortPropertiesButton);
		
        ToggleIconButton addChildContainerButton = new ToggleIconButton(ID_ADD_CHILD_CONTAINER,
        		GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS, GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS) {
        	private static final long serialVersionUID = 1L;

        	@Override
            public void onClick(AjaxRequestTarget target) {
				isChildContainersSelectorPanelVisible = true;
				target.add(PrismContainerValueHeaderPanel.this);
            }

        	@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isSorted();
			}
        };
		addChildContainerButton.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return getModelObject().containsMultivalueContainer();
			}
		});
        add(addChildContainerButton);

		WebMarkupContainer childContainersSelectorPanel = new WebMarkupContainer(ID_CHILD_CONTAINERS_SELECTOR_PANEL);
		childContainersSelectorPanel.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return isChildContainersSelectorPanelVisible;
			}
		});
		childContainersSelectorPanel.setOutputMarkupId(true);
		add(childContainersSelectorPanel);

		List<QName> pathsList = getModelObject().getChildMultivalueContainersPaths();
		childContainersSelectorPanel.add(new DropDownChoicePanel<QName>(ID_CHILD_CONTAINERS_LIST,
				Model.of(pathsList.size() > 0 ? pathsList.get(0) : null), Model.ofList(pathsList)));
		childContainersSelectorPanel.add(new AjaxButton(ID_ADD_BUTTON, createStringResource("prismValuePanel.add")) {
			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				addNewContainerValuePerformed(ajaxRequestTarget);
			}
		});
	}

	protected void addNewContainerValuePerformed(AjaxRequestTarget ajaxRequestTarget){
		isChildContainersSelectorPanelVisible = false;
		getModelObject().addNewChildContainerValue(getSelectedContainerQName(), getPageBase());
		ajaxRequestTarget.add(getChildContainersSelectorPanel().getParent());
	}

	private QName getSelectedContainerQName(){
		DropDownChoicePanel<QName> panel = (DropDownChoicePanel)getChildContainersSelectorPanel().get(ID_CHILD_CONTAINERS_LIST);
		return panel.getModel().getObject();
	}

	private WebMarkupContainer getChildContainersSelectorPanel(){
		return (WebMarkupContainer) get(ID_CHILD_CONTAINERS_SELECTOR_PANEL);
	}

	@Override
	protected String getLabel() {
		return getModel().getObject().getDisplayName();
	}
	
	private void onShowEmptyClick(AjaxRequestTarget target) {
		
		ContainerValueWrapper<C> wrapper = PrismContainerValueHeaderPanel.this.getModelObject();
		wrapper.setShowEmpty(!wrapper.isShowEmpty(), false);
			
		onButtonClick(target);
		
	}


}
