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
package com.evolveum.midpoint.model.intest.password;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractPasswordTest extends AbstractInitializedModelIntegrationTest {
		
	protected static final String USER_PASSWORD_1_CLEAR = "d3adM3nT3llN0Tal3s";
	protected static final String USER_PASSWORD_2_CLEAR = "bl4ckP3arl";
	protected static final String USER_PASSWORD_3_CLEAR = "wh3r3sTheRum?";
	protected static final String USER_PASSWORD_4_CLEAR = "sh1v3rM3T1mb3rs";
	protected static final String USER_PASSWORD_5_CLEAR = "s3tSa1al";
	protected static final String USER_PASSWORD_A_CLEAR = "A"; // too short
	protected static final String USER_PASSWORD_JACK_CLEAR = "12jAcK34"; // contains username
	protected static final String USER_PASSWORD_SPARROW_CLEAR = "saRRow123"; // contains familyName
	protected static final String USER_PASSWORD_VALID_1 = "abcd123";
	protected static final String USER_PASSWORD_VALID_2 = "abcd223";
	protected static final String USER_PASSWORD_VALID_3 = "abcd323";
	protected static final String USER_PASSWORD_VALID_4 = "abcd423";

	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "password");

	protected static final File RESOURCE_DUMMY_UGLY_FILE = new File(TEST_DIR, "resource-dummy-ugly.xml");
	protected static final String RESOURCE_DUMMY_UGLY_OID = "10000000-0000-0000-0000-000000344104";
	protected static final String RESOURCE_DUMMY_UGLY_NAME = "ugly";

	protected static final File PASSWORD_POLICY_UGLY_FILE = new File(TEST_DIR, "password-policy-ugly.xml");
	protected static final String PASSWORD_POLICY_UGLY_OID = "cfb3fa9e-027a-11e7-8e2c-dbebaacaf4ee";
	
	protected static final File SECURITY_POLICY_DEFAULT_STORAGE_HASHING_FILE = new File(TEST_DIR, "security-policy-default-storage-hashing.xml");
	protected static final String SECURITY_POLICY_DEFAULT_STORAGE_HASHING_OID = "0ea3b93c-0425-11e7-bbc1-73566dc53d59";
	
	protected static final File SECURITY_POLICY_PASSWORD_STORAGE_NONE_FILE = new File(TEST_DIR, "security-policy-password-storage-none.xml");
	protected static final String SECURITY_POLICY_PASSWORD_STORAGE_NONE_OID = "2997a20a-0423-11e7-af65-a7ab7d19442c";
	
	protected static final String USER_JACK_EMPLOYEE_NUMBER_NEW_BAD = "No1";
	protected static final String USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD = "pir321";
	
	protected DummyResource dummyResourceUgly;
	protected DummyResourceContoller dummyResourceCtlUgly;
	protected ResourceType resourceDummyUglyType;
	protected PrismObject<ResourceType> resourceDummyUgly;

	protected String accountJackOid;
	protected String accountJackRedOid;
	protected String accountJackUglyOid;
	protected String accountJackYellowOid;
	protected XMLGregorianCalendar lastPasswordChangeStart;
	protected XMLGregorianCalendar lastPasswordChangeEnd;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		importObjectFromFile(PASSWORD_POLICY_UGLY_FILE);
		importObjectFromFile(SECURITY_POLICY_DEFAULT_STORAGE_HASHING_FILE);
		importObjectFromFile(SECURITY_POLICY_PASSWORD_STORAGE_NONE_FILE);
		
		setGlobalSecurityPolicy(getSecurityPolicyOid(), initResult);

		dummyResourceCtlUgly = DummyResourceContoller.create(RESOURCE_DUMMY_UGLY_NAME, resourceDummyUgly);
		dummyResourceCtlUgly.extendSchemaPirate();
		dummyResourceUgly = dummyResourceCtlUgly.getDummyResource();
		resourceDummyUgly = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_UGLY_FILE, RESOURCE_DUMMY_UGLY_OID, initTask, initResult);
		resourceDummyUglyType = resourceDummyUgly.asObjectable();
		dummyResourceCtlUgly.setResource(resourceDummyUgly);

		login(USER_ADMINISTRATOR_USERNAME);
	}
	
	protected abstract String getSecurityPolicyOid();

	@Test
    public void test010AddPasswordPolicy() throws Exception {
		final String TEST_NAME = "test010AddPasswordPolicy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
		// WHEN
        PrismObject<ObjectType> passwordPolicy = addObject(PASSWORD_POLICY_GLOBAL_FILE, task, result);
		
		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong OID after add", PASSWORD_POLICY_GLOBAL_OID, passwordPolicy.getOid());

		// Check object
        PrismObject<ValuePolicyType> accountShadow = repositoryService.getObject(ValuePolicyType.class, PASSWORD_POLICY_GLOBAL_OID, null, result);

        // TODO: more asserts
	}
	
	@Test
    public void test050CheckJackPassword() throws Exception {
		final String TEST_NAME = "test050CheckJackPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN, WHEN
        // this happens during test initialization when user-jack.xml is added
        
        // THEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		// Password still encrypted. We haven't changed it yet.
		assertUserPassword(userJack, USER_JACK_PASSWORD, CredentialsStorageTypeType.ENCRYPTION);
	}
	

	@Test
    public void test051ModifyUserJackPassword() throws Exception {
		final String TEST_NAME = "test051ModifyUserJackPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_1_CLEAR, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		assertPasswordMetadata(userJack, false, startCal, endCal);
		// Password policy is not active yet. No history should be kept.
		assertPasswordHistoryEntries(userJack);
	}
	
	@Test
    public void test060CheckJackPasswordModelInteraction() throws Exception {
		final String TEST_NAME = "test060CheckJackPasswordModelInteraction";
        TestUtil.displayTestTile(this, TEST_NAME);

        if (getPasswordStorageType() == CredentialsStorageTypeType.NONE) {
        	// Nothing to check in this case
        	return;
        }
        
        // GIVEN
        Task task = createTask(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN, THEN
        ProtectedStringType userPasswordPsGood = new ProtectedStringType();
        userPasswordPsGood.setClearValue(USER_PASSWORD_1_CLEAR);
        assertTrue("Good password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsGood, task, result));
        
        ProtectedStringType userPasswordPsBad = new ProtectedStringType();
        userPasswordPsBad.setClearValue("this is not a password");        
        assertFalse("Bad password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsBad, task, result));

        ProtectedStringType userPasswordPsEmpty = new ProtectedStringType();
        assertFalse("Empty password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsEmpty, task, result));
        
        assertFalse("Null password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, null, task, result));

	}
	
	@Test
    public void test070AddUserHerman() throws Exception {
		final String TEST_NAME = "test070AddUserHerman";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(USER_HERMAN_FILE, task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userAfter = getUser(USER_HERMAN_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_HERMAN_OID, USER_HERMAN_USERNAME, 
				USER_HERMAN_FULL_NAME, USER_HERMAN_GIVEN_NAME, USER_HERMAN_FAMILY_NAME);
        
		assertUserPassword(userAfter, USER_HERMAN_PASSWORD);
		assertPasswordMetadata(userAfter, true, startCal, endCal);
		// Password policy is not active yet. No history should be kept.
		assertPasswordHistoryEntries(userAfter);
	}

	@Test
    public void test100ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackAssignAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertShadowLifecycle(accountShadow, false);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertShadowLifecycle(accountModel, false);
        
        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
        assertDummyPasswordConditional(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
	}
	
	/**
	 * This time user has assigned account. Account password should be changed as well.
	 */
	@Test
    public void test110ModifyUserJackPassword() throws Exception {
		final String TEST_NAME = "test110ModifyUserJackPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();
                        
		// WHEN
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_2_CLEAR, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertUserPassword(userJack, USER_PASSWORD_2_CLEAR);
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_2_CLEAR);
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
	}
	
	/**
	 * Modify account password. User's password should be unchanged
	 */
	@Test
    public void test111ModifyAccountJackPassword() throws Exception {
		final String TEST_NAME = "test111ModifyAccountJackPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyAccountChangePassword(accountJackOid, USER_PASSWORD_3_CLEAR, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		// User should still have old password
		assertUserPassword(userJack, USER_PASSWORD_2_CLEAR);
		// Account has new password
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_3_CLEAR);
		
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
	}
	
	/**
	 * Modify both user and account password. As password outbound mapping is weak the user should have its own password
	 * and account should have its own password.
	 */
	@Test
    public void test112ModifyJackPasswordUserAndAccount() throws Exception {
		final String TEST_NAME = "test112ModifyJackPasswordUserAndAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs4 = new ProtectedStringType();
        userPasswordPs4.setClearValue(USER_PASSWORD_4_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs4);
        
        ProtectedStringType userPasswordPs5 = new ProtectedStringType();
        userPasswordPs5.setClearValue(USER_PASSWORD_5_CLEAR);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountJackOid, getDummyResourceObject(), 
        		PASSWORD_VALUE_PATH, userPasswordPs5);        
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);
        
        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		// User should still have old password
		assertUserPassword(userJack, USER_PASSWORD_4_CLEAR);
		// Account has new password
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_5_CLEAR);
		
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
	}
		
	/**
	 * Add red and ugly dummy resource to the mix. This would be fun.
	 */
	@Test
    public void test120ModifyUserJackAssignAccountDummyRedAndUgly() throws Exception {
		final String TEST_NAME = "test120ModifyUserJackAssignAccountDummyRedAndUgly";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_UGLY_OID, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 3);
        accountJackRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        accountJackUglyOid = getLinkRefOid(userJack, RESOURCE_DUMMY_UGLY_OID);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_4_CLEAR);

		assertDummyAccount(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null, true);
		assertDummyPasswordConditional(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

		// User and default dummy account should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_4_CLEAR);
     	assertDummyPassword("jack", USER_PASSWORD_5_CLEAR);
     	
     	assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
	}
	
	/**
	 * Modify both user and account password. Red dummy has a strong password mapping. User change should override account
	 * change.
	 */
	@Test
    public void test121ModifyJackPasswordUserAndAccountRed() throws Exception {
		final String TEST_NAME = "test121ModifyJackPasswordUserAndAccountRed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs1 = new ProtectedStringType();
        userPasswordPs1.setClearValue(USER_PASSWORD_1_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs1);
        
        ProtectedStringType userPasswordPs2 = new ProtectedStringType();
        userPasswordPs2.setClearValue(USER_PASSWORD_2_CLEAR);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountJackRedOid, getDummyResourceObject(),
        		PASSWORD_VALUE_PATH, userPasswordPs2);        
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, USER_JACK_FULL_NAME);
        
		// User should still have old password
		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		// Red account has the same account as user
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// ... and default account has also the same password as user now. There was no other change on default dummy instance 
		// so even the weak mapping took place.
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

		assertLinks(userJack, 3);
		
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
	}
	
	/**
	 * Jack employee number is mapped to ugly resource password.
	 * Change employee number to something that does NOT comply with ugly resource password policy.
	 * MID-3769
	 */
	@Test
    public void test125ModifyJackEmployeeNumberBad() throws Exception {
		final String TEST_NAME = "test125ModifyJackEmployeeNumberBad";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
               
        try {
			// WHEN
	        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result, 
	        		USER_JACK_EMPLOYEE_NUMBER_NEW_BAD);
	        assertNotReached();
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
                
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after", userJack);
        
		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// ugly password should be changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

		assertLinks(userJack, 3);
		
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
	}
	
	/**
	 * Jack employee number is mapped to ugly resource password.
	 * Change employee number to something that does comply with ugly resource password policy.
	 * MID-3769
	 */
	@Test
    public void test128ModifyJackEmployeeNumberGood() throws Exception {
		final String TEST_NAME = "test128ModifyJackEmployeeNumberGood";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                        
		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result, 
        		USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
                
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after", userJack);
        
		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// ugly password should be changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertLinks(userJack, 3);
		
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
	}
	
	/**
	 * Yellow resource has minimum password length constraint. But this time the password is OK.
	 */
	@Test
    public void test130ModifyUserJackAssignAccountDummyYellow() throws Exception {
		final String TEST_NAME = "test130ModifyUserJackAssignAccountDummyYellow";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 4);
        accountJackYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource (yellow)
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        
        // Check account in dummy resource (red)
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        
        // User and default dummy account should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
		
		assertPasswordHistoryEntries(userJack);
	}
	
	/**
	 * Yellow resource has minimum password length constraint. Change password to something shorter.
	 * MID-3033, MID-2134
	 */
	@Test
    public void test132ModifyUserJackPasswordA() throws Exception {
		final String TEST_NAME = "test132ModifyUserJackPasswordA";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_A_CLEAR, task, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertPartialError(result);
		
		lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 4);
        accountJackYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        
        // Check account in dummy resource (red)
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_A_CLEAR);
        
        // User and default dummy account should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_A_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_A_CLEAR);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
		
		assertPasswordHistoryEntries(userJack);
	}
	
	@Test
    public void test200ApplyPasswordPolicy() throws Exception {
		final String TEST_NAME = "test200ApplyPasswordPolicy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		PrismReferenceValue passPolicyRef = new PrismReferenceValue(PASSWORD_POLICY_GLOBAL_OID, ValuePolicyType.COMPLEX_TYPE);
		// WHEN
		modifyObjectReplaceReference(SecurityPolicyType.class, getSecurityPolicyOid(),
				new ItemPath(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_PASSWORD_POLICY_REF),
        		task, result, passPolicyRef);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	/**
	 * Reconcile user after password policy change. Nothing should be changed in the user.
	 * Password history should still be empty. We haven't changed the password yet.
	 * MID-3567
	 */
	@Test
    public void test202ReconcileUserJack() throws Exception {
		final String TEST_NAME = "test202ReconcileUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertLinks(userBefore, 4);
        
		// WHEN
        reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 4);
        accountJackYellowOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        
        // Check account in dummy resource (red)
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_A_CLEAR);
        
        // User and default dummy account should have unchanged passwords
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_A_CLEAR);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
		
		assertPasswordHistoryEntries(userAfter);
	}
	
	/**
	 * Change to password that complies with password policy.
	 */
	@Test
    public void test210ModifyUserJackPasswordGood() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test210ModifyUserJackPasswordGood",
				USER_PASSWORD_VALID_1, USER_PASSWORD_A_CLEAR);
	}
	
	/**
	 * Reconcile user. Nothing should be changed.
	 * MID-3567
	 */
	@Test
    public void test212ReconcileUserJack() throws Exception {
		final String TEST_NAME = "test212ReconcileUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_A_CLEAR);
	}
	
	/**
	 * Recompute user. Nothing should be changed.
	 * MID-3567
	 */
	@Test
    public void test214RecomputeUserJack() throws Exception {
		final String TEST_NAME = "test214RecomputeUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        recomputeUser(USER_JACK_OID, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_A_CLEAR);
	}
	
	/**
	 * Change to password that violates the password policy (but is still OK for yellow resource).
	 */
	@Test
    public void test220ModifyUserJackPasswordBadA() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test220ModifyUserJackPasswordBadA",
				USER_PASSWORD_1_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_A_CLEAR);
	}
	
	/**
	 * Change to password that violates the password policy (but is still OK for yellow resource).
	 * Use a different delta (container delta instead of property delta).
	 * MID-2857
	 */
	@Test
    public void test222ModifyUserJackPasswordBadContainer() throws Exception {
		final String TEST_NAME = "test222ModifyUserJackPasswordBadContainer";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
		userPasswordPs.setClearValue(USER_PASSWORD_1_CLEAR);
		PasswordType passwordType = new PasswordType();
		passwordType.setValue(userPasswordPs);
		
		ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationReplaceContainer(UserType.class, USER_JACK_OID, 
				new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD),
						prismContext, passwordType);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        
        try {
			// WHEN
    		modelService.executeChanges(deltas, null, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
	        
        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Exected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_A_CLEAR);
	}
	
	/**
	 * Change to password that violates the password policy (contains username)
	 * MID-1657
	 */
	@Test
    public void test224ModifyUserJackPasswordBadJack() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test224ModifyUserJackPasswordBadJack",
				USER_PASSWORD_JACK_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_A_CLEAR);
	}
	
	/**
	 * Change to password that violates the password policy (contains family name)
	 * MID-1657
	 */
	@Test
    public void test226ModifyUserJackPasswordBadSparrow() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test226ModifyUserJackPasswordBadSparrow",
				USER_PASSWORD_SPARROW_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_A_CLEAR);
	}
	
	/**
	 * Change to password that complies with password policy. Again. See that 
	 * the change is applied correctly and that it is included in the history.
	 */
	@Test
    public void test230ModifyUserJackPasswordGoodAgain() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test230ModifyUserJackPasswordGoodAgain",
				USER_PASSWORD_VALID_2, USER_PASSWORD_A_CLEAR, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * Change to password that is good but it is the same as current password.
	 */
	@Test
    public void test235ModifyUserJackPasswordGoodSameAsCurrent() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test235ModifyUserJackPasswordGoodSameAsCurrent",
				USER_PASSWORD_VALID_2, USER_PASSWORD_VALID_2, USER_PASSWORD_A_CLEAR, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * Change to password that is good but it is already in the history.
	 */
	@Test
    public void test236ModifyUserJackPasswordGoodInHistory() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test236ModifyUserJackPasswordGoodInHistory",
				USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_2, USER_PASSWORD_A_CLEAR, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * Change to password that is bad and it is already in the history.
	 */
	@Test
    public void test237ModifyUserJackPasswordBadInHistory() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test237ModifyUserJackPasswordBadInHistory",
				USER_PASSWORD_A_CLEAR, USER_PASSWORD_VALID_2, USER_PASSWORD_A_CLEAR, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * Change to password that complies with password policy. Again.
	 * This time there are enough passwords in the history. So the history should
	 * be truncated.
	 */
	@Test
    public void test240ModifyUserJackPasswordGoodAgainOverHistory() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test240ModifyUserJackPasswordGoodAgainOverHistory",
				USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_2);
	}
	
	/**
	 * Change to password that complies with password policy. Again.
	 * This time there are enough passwords in the history. So the history should
	 * be truncated.
	 */
	@Test
    public void test241ModifyUserJackPasswordGoodAgainOverHistoryAgain() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test241ModifyUserJackPasswordGoodAgainOverHistoryAgain",
				USER_PASSWORD_VALID_4, USER_PASSWORD_VALID_2, USER_PASSWORD_VALID_3);
	}
	
	/**
	 * Reuse old password. Now the password should be out of the history, so
	 * the system should allow its reuse.
	 */
	@Test
    public void test248ModifyUserJackPasswordGoodReuse() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test248ModifyUserJackPasswordGoodReuse",
				USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_4);
	}
	
	private void doTestModifyUserJackPasswordSuccessWithHistory(final String TEST_NAME, 
			String newPassword, String... expectedPasswordHistory) throws Exception {
		TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();
                        
		// WHEN
        modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();
        
        assertJackPasswordsWithHistory(newPassword, expectedPasswordHistory);
	}
	
	private void doTestModifyUserJackPasswordFailureWithHistory(final String TEST_NAME, 
			String newPassword, String oldPassword, String... expectedPasswordHistory) throws Exception {
		TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        try {
			// WHEN
	        modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
	        
        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Exected exception", e);
        }
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertJackPasswordsWithHistory(oldPassword, expectedPasswordHistory);
	}
	
	private void assertJackPasswordsWithHistory(String expectedCurrentPassword, String... expectedPasswordHistory) throws Exception {
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 4);
        accountJackYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        assertUserPassword(userJack, expectedCurrentPassword);
        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);
        
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);
        
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);
        
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
		
		assertPasswordHistoryEntries(userJack, expectedPasswordHistory);
	}
	
	// TODO: add user with password that violates the policy

	/**
	 * Create an org, and create two parentOrgRefs for jack (MID-3099).
	 * Change to password that violates the password policy.
	 */
	@Test
	public void test300TwoParentOrgRefs() throws Exception {
		final String TEST_NAME = "test300TwoParentOrgRefs";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		assignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, null);
		assignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);

		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		assertEquals("Wrong # of parentOrgRefs", 2, jack.getParentOrgRef().size());

		ObjectDelta<OrgType> orgDelta = (ObjectDelta<OrgType>) DeltaBuilder.deltaFor(OrgType.class, prismContext)
				.item(OrgType.F_PASSWORD_POLICY_REF).replace(new PrismReferenceValue(PASSWORD_POLICY_GLOBAL_OID))
				.asObjectDelta(ORG_GOVERNOR_OFFICE_OID);
		executeChanges(orgDelta, null, task, result);

		OrgType govOffice = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID).asObjectable();
		display("governor's office", govOffice);
		assertEquals("Wrong OID of password policy ref", PASSWORD_POLICY_GLOBAL_OID, govOffice.getPasswordPolicyRef().getOid());

		try {
			// WHEN
			modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_1_CLEAR, task, result);

			AssertJUnit.fail("Unexpected success");
		} catch (PolicyViolationException e) {
			// This is expected
			display("Exected exception", e);
		}

		// THEN
		result.computeStatus();
		TestUtil.assertFailure(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 4);
		accountJackYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

		// Make sure that the password is unchanged

		assertUserPassword(userJack, USER_PASSWORD_VALID_1);
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

		assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
	}

	/*
	 *  Remove password. It should be removed from red resource as well. (MID-3111)
	 *  Also unassign yellow resource (requires non-empty password), all orgs, and remove default password policy.
	 */
	@Test
	public void test310RemovePassword() throws Exception {
		final String TEST_NAME = "test310RemovePassword";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
		unassignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, null, task, result);
		unassignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER, task, result);
		modifyObjectReplaceReference(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF, task, result);

		// WHEN
		modifyUserReplace(USER_JACK_OID, PASSWORD_VALUE_PATH, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);

		assertNull("User password is not null", userJack.asObjectable().getCredentials().getPassword().getValue());
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);			// password mapping is weak here - so no change is expected

		assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);	// password mapping is strong here

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
	}

	@Test
	public void test320ChangeEmployeeNumber() throws Exception {
		final String TEST_NAME = "test320ChangeEmployeeNumber";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
		modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result, "emp0000");

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		//assertUserJack(userJack, "Jack Sparrow");			// we changed employeeNumber, so this would fail

		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);			// password mapping is weak here - so no change is expected

		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "emp0000");
	}

	@Test
	public void test330RemoveEmployeeNumber() throws Exception {
		final String TEST_NAME = "test330RemoveEmployeeNumber";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		//assertUserJack(userJack, "Jack Sparrow");					// we changed employeeNumber, so this would fail

		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);			// password mapping is weak here - so no change is expected

		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);
	}
	
	/**
	 * Add user with password and an assignment. check that the account is provisioned and has password.
	 * Tests proper initial cleartext password handling in all cases.
	 */
	@Test
	public void test400AddUserRappWithAssignment() throws Exception {
		final String TEST_NAME = "test400AddUserRappWithAssignment";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_RAPP_FILE);
		AssignmentType assignmentType = createConstructionAssignment(RESOURCE_DUMMY_OID, null, null);
		UserType userBeforeType = userBefore.asObjectable();
		userBeforeType.setFullName(createPolyStringType(USER_RAPP_FULLNAME));
		userBeforeType.getAssignment().add(assignmentType);
		setPassword(userBefore, USER_PASSWORD_VALID_1);
		display("User before", userBefore);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		addObject(userBefore, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		String accountOid = getSingleLinkOid(userAfter);
		
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, true);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, true);
        
        // Check account in dummy resource
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * Make sure recompute does not destroy the situation.
	 */
	@Test
	public void test401UserRappRecompute() throws Exception {
		final String TEST_NAME = "test401UserRappRecompute";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_RAPP_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		String accountOid = getSingleLinkOid(userAfter);
		
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, true);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, true);
        
        // Check account in dummy resource
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * add new assignment to the user, check that account is provisioned and has correct lifecycle
	 */
	@Test
	public void test402AssignRappDummyRed() throws Exception {
		final String TEST_NAME = "test402AssignRappDummyRed";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignAccount(USER_RAPP_OID, RESOURCE_DUMMY_RED_OID, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		
		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, false);
        
        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, false);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);
        
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);
        
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * Make sure recompute does not destroy the situation.
	 */
	@Test
	public void test403UserRappRecompute() throws Exception {
		final String TEST_NAME = "test403UserRappRecompute";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_RAPP_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		
		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, false);
        
        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, false);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);
        
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);
        
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
	}
	
	/**
	 * initialize the account (password and lifecycle delta), check accoutn password and lifecycle
	 */
	@Test
	public void test404InitializeRappDummyRed() throws Exception {
		final String TEST_NAME = "test404InitializeRappDummyRed";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);
		String accountRedOid = getLinkRefOid(userBefore, RESOURCE_DUMMY_RED_OID);

		ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountRedOid, prismContext);
		ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(USER_PASSWORD_VALID_1);
        shadowDelta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, passwordPs);
        shadowDelta.addModificationReplaceProperty(ObjectType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		executeChanges(shadowDelta, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		
		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, SchemaConstants.LIFECYCLE_ACTIVE);
        
        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, SchemaConstants.LIFECYCLE_ACTIVE);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);
        
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);
        
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
	}

	/**
	 * Make sure recompute does not destroy the situation.
	 */
	@Test
	public void test405UserRappRecompute() throws Exception {
		final String TEST_NAME = "test405UserRappRecompute";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_RAPP_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		
		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, SchemaConstants.LIFECYCLE_ACTIVE);
        
        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, SchemaConstants.LIFECYCLE_ACTIVE);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);
        
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);
        
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
	}


	private void assertDummyPassword(String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		assertDummyPassword(null, userId, expectedClearPassword);
	}
	
	protected void assertDummyPasswordConditional(String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		if (getPasswordStorageType() == CredentialsStorageTypeType.ENCRYPTION) {
			assertDummyPassword(null, userId, expectedClearPassword);
		}
	}
	
	protected void assertDummyPasswordConditional(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		if (getPasswordStorageType() == CredentialsStorageTypeType.ENCRYPTION) {
			super.assertDummyPassword(instance, userId, expectedClearPassword);
		}
	}
	
	protected abstract void assertShadowLifecycle(PrismObject<ShadowType> shadow, boolean focusCreated);
	
	protected void assertShadowLifecycle(PrismObject<ShadowType> shadow, String expectedLifecycle) {
		if (expectedLifecycle == null) {
			String actualLifecycle = shadow.asObjectable().getLifecycleState();
			if (actualLifecycle != null && !SchemaConstants.LIFECYCLE_ACTIVE.equals(actualLifecycle)) {
				fail("Expected default lifecycle for "+shadow+", but was "+actualLifecycle);
			}
		} else {
			PrismAsserts.assertPropertyValue(shadow, ObjectType.F_LIFECYCLE_STATE, expectedLifecycle);
		}
	}
}
