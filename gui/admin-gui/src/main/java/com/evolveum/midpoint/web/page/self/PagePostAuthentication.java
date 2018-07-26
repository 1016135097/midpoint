/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.web.page.self;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.users.PageAdminUsers;
import com.evolveum.midpoint.web.page.login.PageAbstractFlow;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@PageDescriptor(urls = {@Url(mountUrl = "/self/postAuthentication", matchUrlForSecurity="/self/postAuthentication")}, 
		action = {
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_POST_AUTHENTICATION_URL,
						label = "PagePostAuthentication.auth.postAuthentication.label",
						description = "PagePostAuthentication.auth.postAuthentication.description"),
		}
		)
public class PagePostAuthentication extends PageAbstractFlow {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(PagePostAuthentication.class);
	
	private static final String DOT_CLASS = PagePostAuthentication.class.getName() + ".";
	
	private static final String OPERATION_LOAD_WRAPPER = DOT_CLASS + "loadWrapper";
	private static final String ID_WRAPPER_CONTENT = "wrapperContent";

	private IModel<UserType> userModel;
	private ObjectWrapper<UserType> objectWrapper;
	
	public PagePostAuthentication(PageParameters pageParameters) {
		super(pageParameters);
	}
	
	@Override
	public void initalizeModel() {
		userModel = new LoadableModel<UserType>() {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected UserType load() {
				MidPointPrincipal principal = null;
				principal = SecurityUtils.getPrincipalUser();
				return principal.getUser();
			}
		};
		
	}

	@Override
	public IModel<UserType> getUserModel() {
		return userModel;
	}

	@Override
	public boolean isCustomFormDefined() {
		return getPostAuthenticationConfiguration().getFormRef() != null;
	}

	@Override
	protected WebMarkupContainer initStaticLayout() {
		Task task = createSimpleTask(OPERATION_LOAD_WRAPPER);
		ObjectWrapperFactory owf = new ObjectWrapperFactory(PagePostAuthentication.this);
		objectWrapper = owf.createObjectWrapper("Details", "User Details", userModel.getObject().asPrismObject(), ContainerStatus.MODIFYING, task);
		
		Form<?> form = getMainForm();
		PrismPanel<UserType> prismPanel = new PrismPanel<>(ID_WRAPPER_CONTENT, new ContainerWrapperListFromObjectWrapperModel(Model.of(objectWrapper), getVisibleContainers()), new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), form, null, this);
		return prismPanel;
	}

	private List<ItemPath> getVisibleContainers() {
		return Arrays.asList(ItemPath.EMPTY_PATH, SchemaConstants.PATH_PASSWORD);
		
	}
	
	@Override
	protected WebMarkupContainer initDynamicLayout() {
		Task task = createSimpleTask(OPERATION_LOAD_DYNAMIC_FORM);
		Form<?> form = getMainForm();
		return createDynamicPanel(form, task);
	}
	
	@Override
	protected boolean isSideMenuVisible(boolean visibleIfLoggedIn) {
		return false;
	}

	@Override
	protected void submitRegistration(AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_SAVE_USER);
		ObjectDelta<UserType> userDelta = null;
		try {
			if (!isCustomFormDefined()) {
				userDelta = objectWrapper.getObjectDelta();
			} else {
				userDelta = getDynamicFormPanel().getObjectDelta();
			}

			getPrismContext().adopt(userDelta);
			WebModelServiceUtils.save(userDelta, result, this);
			result.recordSuccessIfUnknown();
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Error during saving user.", e);
			result.recordFatalError("Could not save user.", e);
		}
		
		result.computeStatus();
		
		if (result.isAcceptable()) {
			runPrivileged(() -> {
				ObjectDelta<UserType> lifecycleDelta = ObjectDelta.createModificationDeleteProperty(UserType.class,
						userModel.getObject().getOid(), UserType.F_LIFECYCLE_STATE, getPrismContext(),
						getPostAuthenticationConfiguration().getRequiredLifecycleState());
				OperationResult opResult = new OperationResult(OPERATION_SAVE_USER);
				Task task = createAnonymousTask(OPERATION_SAVE_USER);
				WebModelServiceUtils.save(lifecycleDelta, opResult, task, PagePostAuthentication.this);
				opResult.recordSuccessIfUnknown();
				return opResult;
			});
		}
		
		result.computeStatus();
		showResult(result, true);
		if (!result.isAcceptable()) {
			target.add(PagePostAuthentication.this);
		} else {
			MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
			try {
				getModelInteractionService().refreshPrincipal(principal.getOid());
				navigateToNext(getMidpointApplication().getHomePage());
			} catch (ObjectNotFoundException | SchemaException e) {
				LOGGER.error("Errpr while refreshing user: ", e);
				target.add(PagePostAuthentication.this);
			}
			
		}
		
		target.add(getFeedbackPanel());
		
	}
		
	@Override
	protected boolean isBackButtonVisible() {
		return false;
	}

	@Override
	protected ObjectReferenceType getCustomFormRef() {
		return getPostAuthenticationConfiguration().getFormRef();
	}
	
	@Override
	protected boolean isLogoLinkEnabled() {
		return false;
	}
}
