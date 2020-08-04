/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.report.impl.ReportJasperCreateTaskHandler;
import com.evolveum.midpoint.report.impl.ReportTaskHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractReportIntegrationTest extends AbstractModelIntegrationTest {

    protected static final File TEST_DIR_COMMON = new File("src/test/resources/common");
    protected static final File EXPORT_DIR = new File("target/midpoint-home/export");

    protected static final File TEST_REPORTS_DIR = new File("src/test/resources/reports");

    protected static final File REPORT_USER_LIST_FILE = new File(TEST_REPORTS_DIR, "report-user-list.xml");
    protected static final String REPORT_USER_LIST_OID = "00000000-0000-0000-0000-000000000110";

    protected static final File REPORT_USER_LIST_EXPRESSIONS_CSV_FILE = new File(TEST_REPORTS_DIR, "report-user-list-expressions-csv.xml");
    protected static final String REPORT_USER_LIST_EXPRESSIONS_CSV_OID = "8fa48180-4f17-11e9-9eed-3fb4721a135e";

    protected static final File REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_FILE = new File(TEST_REPORTS_DIR, "report-user-list-expressions-poisonous-query-csv.xml");
    protected static final String REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_OID = "5c5af02a-4fe9-11e9-bb07-7b4e52fe05cd";

    protected static final File REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_FILE = new File(TEST_REPORTS_DIR, "report-user-list-expressions-poisonous-field-csv.xml");
    protected static final String REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_OID = "76c58132-4fe9-11e9-86fe-ff36d221f673";

    protected static final File REPORT_USER_LIST_SCRIPT_FILE = new File(TEST_REPORTS_DIR, "report-user-list-script.xml");
    protected static final String REPORT_USER_LIST_SCRIPT_OID = "222bf2b8-c89b-11e7-bf36-ebd4e4d45a80";

    protected static final File REPORT_AUDIT_CSV_FILE = new File(TEST_REPORTS_DIR, "report-audit-csv.xml");
    protected static final String REPORT_AUDIT_CSV_OID = "66dbbecc-a9fc-11e9-b75c-03927bebc9f7";

    protected static final File REPORT_AUDIT_CSV_LEGACY_FILE = new File(TEST_REPORTS_DIR, "report-audit-csv-legacy.xml");
    protected static final String REPORT_AUDIT_CSV_LEGACY_OID = "78faa28c-a9ff-11e9-8c60-e7843d75831e";

    protected static final File USER_JACK_FILE = new File(TEST_DIR_COMMON, "user-jack.xml");
    protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

    protected static final File USERS_MONKEY_ISLAND_FILE = new File(TEST_DIR_COMMON, "users-monkey-island.xml");

    protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR_COMMON, "system-configuration.xml");
    protected static final File SYSTEM_CONFIGURATION_SAFE_FILE = new File(TEST_DIR_COMMON, "system-configuration-safe.xml");

    protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR_COMMON, "resource-opendj.xml");
    protected static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

    protected static final File CONNECTOR_DUMMY_FILE = new File(TEST_DIR_COMMON, "connector-ldap.xml");
    protected static final String CONNECTOR_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3eedd";

    protected static final File ROLE_SUPERUSER_FILE = new File(TEST_DIR_COMMON, "role-superuser.xml");

    protected static final File USER_ADMINISTRATOR_FILE = new File(TEST_DIR_COMMON, "user-administrator.xml");

    protected static final File USER_READER_FILE = new File(TEST_DIR_COMMON, "user-reader.xml");
    protected static final String USER_READER_USERNAME = "reader";
    protected static final File USER_RUNNER_FILE = new File(TEST_DIR_COMMON, "user-runner.xml");
    protected static final String USER_RUNNER_USERNAME = "runner";
    protected static final File USER_READER_RUNNER_FILE = new File(TEST_DIR_COMMON, "user-reader-runner.xml");
    protected static final String USER_READER_RUNNER_USERNAME = "reader-runner";
    protected static final File ROLE_READER_FILE = new File(TEST_DIR_COMMON, "role-reader.xml");
    protected static final File ROLE_RUNNER_FILE = new File(TEST_DIR_COMMON, "role-runner.xml");
    public static final File ARCHETYPE_TASK_FILE = new File(COMMON_DIR, "archetype-task-report.xml");

    protected static final String OP_CREATE_REPORT = ReportTaskHandler.class.getName() + "createReport";
    protected static final String OP_IMPORT_REPORT = ReportTaskHandler.class.getName() + "importReport";

    @Autowired protected ReportManager reportManager;
    @Autowired
    @Qualifier("reportJasperCreateTaskHandler")
    protected ReportJasperCreateTaskHandler reportTaskHandler;

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        // System Configuration
        modelService.postInit(initResult);
        try {
            repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        // User administrator
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);

        login(userAdministrator);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        importObjectFromFile(ARCHETYPE_TASK_FILE, initResult);
    }

    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }
}
