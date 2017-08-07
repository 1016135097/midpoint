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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Holder;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.Arrays;
import java.util.List;

/**
 * @author honchar
 * @author mederly
 */
public abstract class DataLanguagePanel<T> extends MultiStateHorizontalButton {

	public static final List<String> LANGUAGES = Arrays.asList(
			PrismContext.LANG_XML,
			PrismContext.LANG_JSON,
			PrismContext.LANG_YAML);

	public static final List<String> LABELS = Arrays.asList(
			"PageDebugView.xmlViewButton",
			"PageDebugView.xmlJsonButton",
			"PageDebugView.xmlYamlButton");

	private PageBase pageBase;
	private Class<T> dataType;
	private int currentLanguageIndex;       // always [0..N)

	public DataLanguagePanel(String id, String dataLanguage, Class<T> dataType, PageBase pageBase) {
		super(id, getIndexFor(dataLanguage), LABELS, pageBase);
		this.pageBase = pageBase;
		this.dataType = dataType;
		this.currentLanguageIndex = getIndexFor(dataLanguage);
		setOutputMarkupId(true);
	}

	private static int getIndexFor(String dataLanguage) {
		int i = LANGUAGES.indexOf(dataLanguage);
		return i >= 0 ? i : 0;
	}

	@Override
	protected void onStateChanged(int updatedIndex, AjaxRequestTarget target) {
		String updatedLanguage = updatedIndex >= 0 && updatedIndex < LANGUAGES.size()
				? LANGUAGES.get(updatedIndex) : LANGUAGES.get(0);

		String currentObjectString = getObjectStringRepresentation();
		if (StringUtils.isBlank(currentObjectString)) {
			onLanguageSwitched(target, updatedIndex, updatedLanguage, currentObjectString);
			return;
		}

		OperationResult result = new OperationResult(DataLanguagePanel.class.getName() + ".validateObject");
		Holder<T> objectHolder = new Holder<>(null);

		try {
			pageBase.validateObject(currentObjectString, objectHolder, LANGUAGES.get(currentLanguageIndex), isValidateSchema(), dataType, result);
			if (result.isAcceptable()) {

				Object updatedObject = objectHolder.getValue();
				String updatedObjectString;
				if (Objectable.class.isAssignableFrom(dataType)) {
					updatedObjectString = pageBase.getPrismContext().serializerFor(updatedLanguage)
							.serialize(((Objectable) updatedObject).asPrismObject());
				} else {
					updatedObjectString = pageBase.getPrismContext().serializerFor(updatedLanguage)
							.serializeRealValue(updatedObject);
				}
				setSelectedIndex(updatedIndex);
				currentLanguageIndex = updatedIndex;
				onLanguageSwitched(target, updatedIndex, updatedLanguage, updatedObjectString);
				target.add(this);
				target.add(pageBase.getFeedbackPanel());
			} else {
				pageBase.showResult(result);
				target.add(pageBase.getFeedbackPanel());
			}
		} catch (Exception ex) {
			result.recordFatalError("Couldn't change the language.", ex);
			pageBase.showResult(result);
			target.add(this);
			target.add(pageBase.getFeedbackPanel());
		}
	}

	protected abstract void onLanguageSwitched(AjaxRequestTarget target, int index, String updatedLanguage,
			String objectString);

	protected abstract String getObjectStringRepresentation();

	protected boolean isValidateSchema() {
		return false;
	}
}
