/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */

public class MidPointAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private ModelInteractionService modelInteractionService;

    @Autowired
    private TaskManager taskManager;

    private String defaultTargetUrl;

    private String prefix = "";

    public MidPointAuthenticationSuccessHandler setPrefix(String prefix) {
        this.prefix = "/" + stripSlashes(prefix) + "/";
        return this;
    }

    public MidPointAuthenticationSuccessHandler() {
        setRequestCache(new HttpSessionRequestCache());
    }

    private RequestCache requestCache;

    @Override
    public void setRequestCache(RequestCache requestCache) {
        super.setRequestCache(requestCache);
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        String urlSuffix = "/self/dashboard";
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setState(StateOfModule.SUCCESSFULLY);
            if (mpAuthentication.getAuthenticationChannel() != null) {
                urlSuffix = mpAuthentication.getAuthenticationChannel().getPathAfterSuccessfulAuthentication();
            }
        }

        if (WebModelServiceUtils.isPostAuthenticationEnabled(taskManager, modelInteractionService)) {
            String requestUrl = request.getRequestURL().toString();
            String contextPath = request.getContextPath();
            if (requestUrl.contains("spring_security_login")) {
                String target = requestUrl.substring(0, requestUrl.indexOf(contextPath));
                target = target + contextPath + "self/postAuthentication";
//                        String target = requestUrl.replace("spring_security_login", "self/postAuthentication");
                getRedirectStrategy().sendRedirect(request, response, target);
                return;
            }
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null && savedRequest.getRedirectUrl().contains(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/")) {
            String target = savedRequest.getRedirectUrl().substring(0, savedRequest.getRedirectUrl().indexOf(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/")) + urlSuffix;
            getRedirectStrategy().sendRedirect(request, response, target);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected String getTargetUrlParameter() {


        return defaultTargetUrl;
    }


    @Override
    public void setDefaultTargetUrl(String defaultTargetUrl) {
        this.defaultTargetUrl = defaultTargetUrl;
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(defaultTargetUrl)) {
            return super.determineTargetUrl(request, response);
        }

        return defaultTargetUrl;
    }
}
