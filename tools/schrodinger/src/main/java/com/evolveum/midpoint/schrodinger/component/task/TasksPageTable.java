/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.task.EditTaskPage;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by matus on 6/25/2018.
 */
public class TasksPageTable extends AssignmentHolderObjectListTable<ListTasksPage, EditTaskPage> {
    public TasksPageTable(ListTasksPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public EditTaskPage clickByName(String name) {
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();


        return new EditTaskPage();
    }

    @Override
    public TableWithPageRedirect selectCheckboxByName(String name) {
        //TODO implement

        return null;
    }

    @Override
    public EditTaskPage getObjectDetailsPage(){
        return new EditTaskPage();
    }

}
