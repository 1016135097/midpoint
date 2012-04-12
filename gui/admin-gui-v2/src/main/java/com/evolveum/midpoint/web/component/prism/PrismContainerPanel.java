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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class PrismContainerPanel extends Panel {

    private boolean showHeader;

    public PrismContainerPanel(String id, IModel<ContainerWrapper> model) {
        this(id, model, true);
    }

    public PrismContainerPanel(String id, IModel<ContainerWrapper> model, boolean showHeader) {
        super(id);
        this.showHeader = showHeader;

        add(new AttributeAppender("class", new Model<String>("attributeComponent"), " "));

        initLayout(model);
    }

    private void initLayout(final IModel<ContainerWrapper> model) {
        WebMarkupContainer header = new WebMarkupContainer("header");
        header.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !model.getObject().isMain();
            }
        });
        add(header);

        header.add(new Label("label", new PropertyModel<Object>(model, "displayName")));

        ListView<PropertyWrapper> properties = new ListView<PropertyWrapper>("properties",
                new PropertyModel(model, "properties")) {

            @Override
            protected void populateItem(ListItem<PropertyWrapper> item) {
                item.add(new PrismPropertyPanel("property", item.getModel()));
            }
        };
        add(properties);
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }
}
