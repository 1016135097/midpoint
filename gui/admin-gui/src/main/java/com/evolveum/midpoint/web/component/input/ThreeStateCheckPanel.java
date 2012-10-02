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

package com.evolveum.midpoint.web.component.input;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.threeStateCheckBox.ThreeStateCheckBox;

/**
 * @author mserbak
 */
public class ThreeStateCheckPanel extends InputPanel {
	private ThreeStateCheckBox check;
	private Boolean state;
	
	public ThreeStateCheckPanel(String id, final IModel<Boolean> model) {
		super(id);
		state = model.getObject();
		
		WebMarkupContainer inputElement = new WebMarkupContainer("inputElement");
		add(inputElement);
		
		check = new ThreeStateCheckBox("input", model);
		check.setOutputMarkupId(true);
		inputElement.add(check);

		inputElement.setMarkupId(check.getMarkupId() + "Element");
		
		Label inputImg = new Label("inputImg");
		inputImg.setMarkupId(check.getMarkupId() + ".Img");
		inputElement.add(inputImg);
		
		inputElement.add(new AjaxEventBehavior("onMouseUp") {
			
			@Override
			protected void onEvent(AjaxRequestTarget target) {
				updateModel(model);
			}
		});
	}
	
	private void updateModel(IModel<Boolean> model){
		if(state == null) {
			state = false;
		} else if(!state) {
			state = true;
		} else {
			state = null;
		}
		model.setObject(state);
	}

	@Override
	public FormComponent getBaseFormComponent() {
		return check;
	}
}
