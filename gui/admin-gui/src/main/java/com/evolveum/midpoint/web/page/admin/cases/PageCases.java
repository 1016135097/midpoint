package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by acope on 9/14/17.
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/admin/cases", matchUrlForSecurity = "/admin/cases")
}, action = {
        @AuthorizationAction(actionUri = PageAdminCases.AUTH_CASES_ALL_LABEL,
                label = PageAdminCases.AUTH_CASES_ALL_LABEL,
                description = PageAdminCases.AUTH_CASES_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_URL,
                label = "PageCases.auth.cases.label",
                description = "PageCases.auth.cases.description")})
public class PageCases extends PageAdminCases {

    private static final Trace LOGGER = TraceManager.getTrace(PageCases.class);

    private static final String DOT_CLASS = PageCases.class.getName() + ".";

    private static final long serialVersionUID = 1L;

    public static final String ID_MAIN_FORM = "mainForm";
    public static final String ID_CASES_TABLE = "table";


    public PageCases() {
        this(true);
    }

    public PageCases(boolean clearPagingInSession) {
        LOGGER.trace("initLayout()");

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        LOGGER.trace("Creating casePanel");
        MainObjectListPanel<CaseType> casePanel = new MainObjectListPanel<CaseType>(
                ID_CASES_TABLE,
                CaseType.class,
                UserProfileStorage.TableId.TABLE_CASES,
                null,
                this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
                PageCases.this.caseDetailsPerformed(target, caseInstance);
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target) {
                navigateToNext(PageCases.class);
            }

            @Override
            protected List<IColumn<SelectableBean<CaseType>, String>> createColumns() {
                return PageCases.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return new ArrayList<>();
            }

            @Override
            protected IColumn<SelectableBean<CaseType>, String> createActionsColumn() {
                return PageCases.this.createActionsColumn();
            }
        };
        casePanel.setOutputMarkupId(true);
        mainForm.add(casePanel);

    }

    private void caseDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
        LOGGER.trace("caseDetailsPerformed()");
        navigateToNext(PageCase.class, pageParameters);
    }

    private List<IColumn<SelectableBean<CaseType>, String>> initColumns() {
        LOGGER.trace("initColumns()");


        List<IColumn<SelectableBean<CaseType>, String>> columns = new ArrayList<IColumn<SelectableBean<CaseType>, String>>();

        IColumn column = new PropertyColumn(createStringResource("pageCases.table.description"), "value.description");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageCases.table.state"), "value.state");
        columns.add(column);

//        column = new PropertyColumn(createStringResource("CaseType.event"),
//                CaseType.F_EVENT.getLocalPart(),  SelectableBean.F_VALUE + ".event");
//        columns.add(column);
//
//        column = new PropertyColumn(createStringResource("CaseType.closeTime"),
//                CaseType.F_CLOSE_TIMESTAMP.getLocalPart(),  SelectableBean.F_VALUE + ".closeTime");
//        columns.add(column);

        return columns;
    }


    private IColumn<SelectableBean<CaseType>, String> createActionsColumn() {
        LOGGER.trace("createActionsColumn()");

        return new InlineMenuButtonColumn<SelectableBean<CaseType>>(createInlineMenu(), 1, this) {
            @Override
            protected List<InlineMenuItem> getHeaderMenuItems() {
                return new ArrayList<>();
            }

            @Override
            protected int getHeaderNumberOfButtons() {
                return 0;
            }
        };
    }

    private List<InlineMenuItem> createInlineMenu() {
        LOGGER.trace("createInlineMenu()");
        List<InlineMenuItem> menu = new ArrayList<>();
        menu.add(new InlineMenuItem(createStringResource("pageCases.button.delete"),
                new Model<Boolean>(true), new Model<Boolean>(true), false,
                new ColumnMenuAction() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                    }
                }, 0,
                GuiStyleConstants.CLASS_DELETE_MENU_ITEM,
                DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()));

        return menu;

    }
}
