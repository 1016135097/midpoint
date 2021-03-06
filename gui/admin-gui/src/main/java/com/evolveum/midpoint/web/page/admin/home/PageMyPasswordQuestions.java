/*
 * Portions Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.page.admin.home.component.MyPasswordQuestionsPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@PageDescriptor(url = "/PasswordQuestions", action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL,
                label = "PageSelfCredentials.auth.credentials.label",
                description = "PageSelfCredentials.auth.credentials.description") })
public class PageMyPasswordQuestions extends PageAdminHome {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageMyPasswordQuestions.class);

    private static final String DOT_CLASS = PageMyPasswordQuestions.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";
    private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "LOAD Question Policy";
    private static final String ID_PASSWORD_QUESTIONS_PANEL = "pwdQuestionsPanel";
    private static final String OPERATION_SAVE_QUESTIONS = "Save Security Questions";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";

    private LoadableModel<PrismObjectWrapper<UserType>> userModel;

    private List<MyPasswordQuestionsPanel> pqPanels;
    private IModel<PasswordQuestionsDto> model;
    private List<SecurityQuestionDefinitionType> policyQuestionList;
    int questionNumber;

    public PageMyPasswordQuestions() {
        model = new LoadableModel<PasswordQuestionsDto>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PasswordQuestionsDto load() {
                return loadPageModel();
            }
        };

        initLayout();

    }

    public PageMyPasswordQuestions(IModel<PasswordQuestionsDto> model) {
        this.model = model;
        initLayout();
    }

    public PageMyPasswordQuestions(final PrismObject<UserType> userToEdit) {
        userModel = new LoadableModel<PrismObjectWrapper<UserType>>(false) {

            @Override
            protected PrismObjectWrapper<UserType> load() {
                return loadUserWrapper(userToEdit);
            }
        };
        initLayout();
    }

    private PasswordQuestionsDto loadPageModel() {
        LOGGER.debug("Loading user for Security Question Page.");

        PasswordQuestionsDto dto = new PasswordQuestionsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        try {
            String userOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);

            PrismObject<UserType> user = getModelService().getObject(UserType.class, userOid, null, task, subResult);

            dto.setSecurityAnswers(createUsersSecurityQuestionsList(user));
            subResult.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get user Questions, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return dto;
    }

    public List<SecurityQuestionAnswerDTO> createUsersSecurityQuestionsList(PrismObject<UserType> user) {
        LOGGER.debug("Security Questions Loading for user: " + user.getOid());
        if (user.asObjectable().getCredentials() != null && user.asObjectable().getCredentials().getSecurityQuestions() != null) {
            List<SecurityQuestionAnswerType> secQuestAnsList = user.asObjectable().getCredentials().getSecurityQuestions().getQuestionAnswer();

            if (secQuestAnsList != null) {

                LOGGER.debug("User SecurityQuestion ANswer List is Not null");
                List<SecurityQuestionAnswerDTO> secQuestAnswListDTO = new ArrayList<>();
                for (SecurityQuestionAnswerType securityQuestionAnswerType : secQuestAnsList) {
                    Protector protector = getPrismContext().getDefaultProtector();
                    String decoded = "";
                    if (securityQuestionAnswerType.getQuestionAnswer().getEncryptedDataType() != null) {
                        try {
                            decoded = protector.decryptString(securityQuestionAnswerType.getQuestionAnswer());

                        } catch (EncryptionException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't decrypt user answer", e);

                        }
                    }
                    secQuestAnswListDTO.add(new SecurityQuestionAnswerDTO(
                            securityQuestionAnswerType.getQuestionIdentifier(), decoded));
                }

                return secQuestAnswListDTO;
            }
        }
        return null;
    }

    public void initLayout() {

        Form mainForm = new MidpointForm(ID_MAIN_FORM);

        //question panel list
        pqPanels = new ArrayList<>();
        OperationResult result = new OperationResult(OPERATION_LOAD_QUESTION_POLICY);
        try {

            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_QUESTION_POLICY);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_QUESTION_POLICY);
            try {

                CredentialsPolicyType credPolicy = getModelInteractionService().getCredentialsPolicy(null, null, result);

                //Global Policy set question numbers
                if (credPolicy != null && credPolicy.getSecurityQuestions() != null) {

                    // Actual Policy Question List
                    policyQuestionList = getEnabledSecurityQuestions(credPolicy);

                    questionNumber = policyQuestionList.size();
                } else {
                    questionNumber = 0;
                    policyQuestionList = new ArrayList<>();
                }
            } catch (Exception ex) {
                LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't load credentials for security questions", ex);
                showResult(result);
            }

            /*User's Pre-Set Question List*/
            List<SecurityQuestionAnswerDTO> userQuestionList = model.getObject().getSecurityAnswers();

            /* check if user's set number of
             * questions matches the policy or not*/

            //Case that policy have more than users's number of numbers
            if ((userQuestionList == null) || (questionNumber > userQuestionList.size())) {
                if (userQuestionList == null) {
                    executeAddingQuestions(questionNumber, 0, policyQuestionList);
                    //TODO same questions check should be implemented

                } else {
                    executePasswordQuestionsAndAnswers(userQuestionList, policyQuestionList, userQuestionList.size());
                    //QUESTION NUMBER BIGGER THAN QUESTION LIST
                    //rest of the questions
                    int difference = questionNumber - userQuestionList.size();
                    executeAddingQuestions(difference, userQuestionList.size(), policyQuestionList);

                }

            } else if (questionNumber == userQuestionList.size()) {
                //QUESTION NUMBER EQUALS TO QUESTION LIST
                executePasswordQuestionsAndAnswers(userQuestionList, policyQuestionList, 0);
                //TODO PART2: Case that policy have smaller than users's number of numbers
            } else if (questionNumber < userQuestionList.size()) {
                //QUESTION NUMBER SMALLER THAN QUESTION LIST
                executePasswordQuestionsAndAnswers(userQuestionList, policyQuestionList, 0);
            }

        } catch (Exception ex) {

            result.recordFatalError(getString("PageMyPasswordQuestions.message.couldNotLoadSysConfig"), ex);
        }

        add(mainForm);
        mainForm.add(getPanels(pqPanels));
        initButtons(mainForm);

    }

    private List<SecurityQuestionDefinitionType> getEnabledSecurityQuestions(CredentialsPolicyType credPolicy) {
        List<SecurityQuestionDefinitionType> actualQuestions = credPolicy.getSecurityQuestions().getQuestion();
        List<SecurityQuestionDefinitionType> enabledQuestions = new ArrayList<>();

        for (SecurityQuestionDefinitionType actualQuestion : actualQuestions) {
            if (!Boolean.FALSE.equals(actualQuestion.isEnabled())) {
                enabledQuestions.add(actualQuestion);
            }
        }
        return enabledQuestions;
    }

    /**
     * method for adding questions to user credentials
     *
     * @author oguzhan
     */
    public void executeAddingQuestions(int questionNumber, int panelNumber, List<SecurityQuestionDefinitionType> policyQuestionList) {
        LOGGER.debug("executeAddingQuestions");
        for (int i = 0; i < questionNumber; i++) {
            //LOGGER.info("\n\n Adding panel element");
            SecurityQuestionAnswerDTO a = new SecurityQuestionAnswerDTO(policyQuestionList.get(panelNumber).getIdentifier(), "", policyQuestionList.get(panelNumber).getQuestionText());
            MyPasswordQuestionsPanel panel = new MyPasswordQuestionsPanel(ID_PASSWORD_QUESTIONS_PANEL + panelNumber, a);
            pqPanels.add(panel);
            panelNumber++;

        }

    }

    /**
     * method for get existing questions and answer from user credentials
     *
     * @author oguzhan
     */
    public void executePasswordQuestionsAndAnswers(List<SecurityQuestionAnswerDTO> userQuestionList, List<SecurityQuestionDefinitionType> policyQuestionList, int panelNumber) {
        int userQuest = 0;
        LOGGER.debug("executePasswordQuestionsAndAnswers");
        for (SecurityQuestionDefinitionType securityQuestionDefinitionType : policyQuestionList) {
            /* Loop for finding the Existing Questions
             * and Answers according to Policy*/

            //user's question List loop to match the questions
            for (int i = userQuest; i < userQuestionList.size(); i++) {

                if (userQuestionList.get(i).getPwdQuestion().trim().compareTo(securityQuestionDefinitionType.getIdentifier().trim()) == 0) {

                    SecurityQuestionAnswerDTO a = new SecurityQuestionAnswerDTO(userQuestionList.get(i).getPwdQuestion(), userQuestionList.get(i).getPwdAnswer(), userQuestionList.get(i).getQuestionItself());

                    a = checkIfQuestionIsValidSingle(a, securityQuestionDefinitionType);
                    MyPasswordQuestionsPanel panel = new MyPasswordQuestionsPanel(ID_PASSWORD_QUESTIONS_PANEL + panelNumber, a);
                    pqPanels.add(panel);
                    panelNumber++;
                    userQuest++;
                    break;

                } else if (userQuestionList.get(i).getPwdQuestion().trim().compareTo(securityQuestionDefinitionType.getIdentifier().trim()) != 0) {

                    SecurityQuestionAnswerDTO a = new SecurityQuestionAnswerDTO(policyQuestionList.get(panelNumber).getIdentifier(), "", policyQuestionList.get(panelNumber).getQuestionText());
                    a.setQuestionItself(securityQuestionDefinitionType.getQuestionText());
                    userQuestionList.get(i).setPwdQuestion(securityQuestionDefinitionType.getIdentifier().trim());

                    MyPasswordQuestionsPanel panel = new MyPasswordQuestionsPanel(ID_PASSWORD_QUESTIONS_PANEL + panelNumber, a);
                    pqPanels.add(panel);
                    panelNumber++;

                    userQuest++;
                    break;

                }

            }

        }

    }

    public ListView<MyPasswordQuestionsPanel> getPanels(List<MyPasswordQuestionsPanel> p) {
        return new ListView<MyPasswordQuestionsPanel>(ID_PASSWORD_QUESTIONS_PANEL, p) {
            @Override
            protected void populateItem(ListItem item) {
                item.add((MyPasswordQuestionsPanel) item.getModelObject());
            }
        };
    }

    public void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                savePerformed(target);
            }
        };
        mainForm.add(save);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageBase.button.back")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);
    }

    private void savePerformed(AjaxRequestTarget target) {

        /*
         * Oguzhan: added target variable to the updateQuestions method.
         */
        updateQuestions(SecurityUtils.getPrincipalUser().getOid(), target);

    }

    private void cancelPerformed(AjaxRequestTarget target) {
        setResponsePage(getMidpointApplication().getHomePage());
    }

    private PrismObjectWrapper<UserType> loadUserWrapper(PrismObject<UserType> userToEdit) {
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        PrismObject<UserType> user = null;
        Task task = createSimpleTask(OPERATION_LOAD_USER);
        try {
            Collection options = getOperationOptionsBuilder()
                    .item(UserType.F_CREDENTIALS).retrieve()
                    .build();
            user = getModelService().getObject(UserType.class, SecurityUtils.getPrincipalUser().getOid(), options, task, result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError(getString("PageMyPasswordQuestions.message.loadUserWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load user PageMyQuestions", ex);
        }

        showResult(result, false);

        if (user == null) {

            throw new RestartResponseException(PageDashboardInfo.class);
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        PrismObjectWrapper<UserType> wrapper;
        PrismObjectWrapperFactory<UserType> factory = getPageBase().findObjectWrapperFactory(user.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        try {
            wrapper = factory.createObjectWrapper(user, ItemStatus.NOT_CHANGED, context);
        } catch (Exception ex) {
            result.recordFatalError(getString("PageMyPasswordQuestions.message.loadUserWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load user", ex);
            try {
                wrapper = factory.createObjectWrapper(user, ItemStatus.NOT_CHANGED, context);
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        }
        showResult(result, false);

        return wrapper;
    }

    private SecurityQuestionAnswerDTO checkIfQuestionIsValidSingle(
            SecurityQuestionAnswerDTO questionIdentifier, SecurityQuestionDefinitionType securityQuestion) {
        if (securityQuestion.getIdentifier().trim().compareTo(questionIdentifier.getPwdQuestion().trim()) == 0) {
            questionIdentifier.setQuestionItself(securityQuestion.getQuestionText());
            return questionIdentifier;
        } else {
            return null;
        }
    }

    private void updateQuestions(String useroid, AjaxRequestTarget target) {

        Task task = createSimpleTask(OPERATION_SAVE_QUESTIONS);
        OperationResult result = new OperationResult(OPERATION_SAVE_QUESTIONS);
        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
        SecurityQuestionAnswerType[] answerTypeList = new SecurityQuestionAnswerType[questionNumber];

        try {
            int listnum = 0;
            for (MyPasswordQuestionsPanel type : pqPanels) {
                SecurityQuestionAnswerType answerType = new SecurityQuestionAnswerType();
                ProtectedStringType answer = new ProtectedStringType();

                answer.setClearValue(((TextField<String>) type.get(MyPasswordQuestionsPanel.F_ANSWER)).getModelObject());
                answerType.setQuestionAnswer(answer);

                //used apache's unescapeHtml method for special chars like \'
                String results = StringEscapeUtils.unescapeHtml4(
                        type.get(MyPasswordQuestionsPanel.F_QUESTION).getDefaultModelObjectAsString());
                answerType.setQuestionIdentifier(getQuestionIdentifierFromQuestion(results));
                answerTypeList[listnum] = answerType;
                listnum++;

            }

            // fill in answerType data here
            ItemPath path = ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS, SecurityQuestionsCredentialsType.F_QUESTION_ANSWER);
            ObjectDelta<UserType> objectDelta = getPrismContext().deltaFactory().object()
                    .createModificationReplaceContainer(UserType.class, useroid,
                            path, answerTypeList);

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            getModelService().executeChanges(deltas, null, task, result);

            success(getString("message.success"));
            target.add(getFeedbackPanel());
        } catch (Exception ex) {

            error(getString("message.error"));
            target.add(getFeedbackPanel());
            ex.printStackTrace();
        }
    }

    private String getQuestionIdentifierFromQuestion(String questionItself) {
        for (SecurityQuestionDefinitionType securityQuestionDefinitionType : policyQuestionList) {
            if (questionItself.equalsIgnoreCase(securityQuestionDefinitionType.getQuestionText())) {
                return securityQuestionDefinitionType.getIdentifier();
            }

        }
        return null;
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

}
