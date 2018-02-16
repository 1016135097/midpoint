/**
 * Copyright (c) 2015-2018 Evolveum
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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import sun.security.provider.SHA;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class ConstructionDetailsPanel<C extends Containerable, IW extends ItemWrapper> extends BasePanel<ContainerValueWrapper<C>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_KIND_FIELD = "kindField";
    private static final String ID_INTENT_FIELD = "intentField";

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionDetailsPanel.class);
    private static final String DOT_CLASS = ConstructionDetailsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    private LoadableDetachableModel<PrismObject<ResourceType>> resourceModel;

    public ConstructionDetailsPanel(String id, IModel<ContainerValueWrapper<C>> constructionWrapperModel){
        super(id, constructionWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels(){
        resourceModel = new LoadableDetachableModel<PrismObject<ResourceType>>() {
            @Override
            protected PrismObject<ResourceType> load() {
                ConstructionType construction = (ConstructionType)getModelObject().getContainerValue().asContainerable();
                ObjectReferenceType resourceRef = construction.getResourceRef();
                Task loadResourceTask = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
                OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
                PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef, getPageBase(), loadResourceTask, result);
                result.computeStatusIfUnknown();
                if (!result.isAcceptable()) {
                    LOGGER.error("Cannot find resource referenced from construction. {}", result.getMessage());
                    result.recordPartialError("Could not find resource referenced from construction.");
                    return null;
                }
                return resource;
            }
        };
    }

    private void initLayout(){
        DropDownChoicePanel kindDropDown = WebComponentUtil.createEnumPanel(ShadowKindType.class, ID_KIND_FIELD,
                getPropertyModelForConstructionAttribute(ConstructionType.F_KIND), ConstructionDetailsPanel.this);
        kindDropDown.setOutputMarkupId(true);
        kindDropDown.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getKindDropdownComponent());
                target.add(getIntentDropdownComponent());
            }
        });
        add(kindDropDown);

        DropDownChoicePanel intentDropDown = new DropDownChoicePanel(ID_INTENT_FIELD,
        getPropertyModelForConstructionAttribute(ConstructionType.F_INTENT), getIntentAvailableValuesModel());
        intentDropDown.setOutputMarkupId(true);
        add(intentDropDown);
    }

    private IModel<List<String>> getIntentAvailableValuesModel(){
        return new LoadableModel<List<String>>(true){
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load(){
                List<String> availableIntentValues = new ArrayList<>();
                if (resourceModel.getObject() == null){
                    return availableIntentValues;
                }
                try {
                    RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceModel.getObject());
                    if (refinedSchema != null){
                        ShadowKindType kind = (ShadowKindType) ConstructionDetailsPanel.this.getKindDropdownComponent().getBaseFormComponent().getModelObject();
                        List<? extends RefinedObjectClassDefinition> definitions = refinedSchema.getRefinedDefinitions(kind);
                        for (RefinedObjectClassDefinition def : definitions){
                            availableIntentValues.add(def.getIntent());
                        }
                    }
                } catch (SchemaException ex){
                    LOGGER.error("Cannot get refined resource schema for resource {}. {}", resourceModel.getObject().getName().getOrig(), ex.getLocalizedMessage());
                }

                return availableIntentValues;
            }
        };
    }

    private PropertyModel getPropertyModelForConstructionAttribute(QName attributeName){
        //todo should be refactored. 1) relocate 2) wrap by some new kind of model
        PropertyModel<List<IW>> propertiesModel = new PropertyModel<>(getModel(), "properties");
        List<IW> propertiesList = propertiesModel.getObject();
        for (final IW property : propertiesList){
            if (property.getName().equals(attributeName)){
                IModel<IW> itemWrapperModel = new IModel<IW>() {
                    @Override
                    public IW getObject() {
                        return property;
                    }

                    @Override
                    public void setObject(IW iw) {
                        propertiesList.set(propertiesList.indexOf(property), iw);
                    }

                    @Override
                    public void detach() {

                    }
                };
                PropertyModel<List<ValueWrapper>> valuesModel = new PropertyModel<>(itemWrapperModel, "values");
                List<ValueWrapper> valuesList = valuesModel.getObject();
                if (valuesList.size() > 0) {
                    IModel<ValueWrapper> valueWrapperModel = new IModel<ValueWrapper>() {
                        @Override
                        public ValueWrapper getObject() {
                            return valuesList.get(0);
                        }

                        @Override
                        public void setObject(ValueWrapper valueWrapper) {
                            valuesList.set(0, valueWrapper);
                        }

                        @Override
                        public void detach() {

                        }
                    };
                    return new PropertyModel<>(valueWrapperModel, "value.value");
                }

            }
        }
        return null;
    }

    private DropDownChoicePanel getKindDropdownComponent(){
        return (DropDownChoicePanel) get(ID_KIND_FIELD);
    }

    private DropDownChoicePanel getIntentDropdownComponent(){
        return (DropDownChoicePanel) get(ID_INTENT_FIELD);
    }
}
