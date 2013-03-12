/**
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
package com.evolveum.midpoint.model.intest.negative;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertFailure;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.TestModelServiceContract;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * Tests the model service contract by using a broken CSV resource. Tests for negative test cases, mostly
 * correct handling of connector exceptions.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentErrors extends AbstractInitializedModelIntegrationTest {
	
	private static final String TEST_DIR = "src/test/resources/negative";
	private static final String TEST_TARGET_DIR = "target/test/negative";
		
	protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentErrors.class);
	
	private PrismObject<ResourceType> resource;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);		
	}
	
	/**
	 * The "while" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
	 * this results in account without any attributes. It should fail.
	 */
	@Test
    public void test100UserJackAssignBlankAccount() throws Exception {
		final String TEST_NAME = "test100UserJackAssignBlankAccount";
        displayTestTile(this, "TEST_NAME");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_WHITE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
//        try {
			// WHEN
			modelService.executeChanges(deltas, null, task, result);
			//not expected that it fails, insted the fatal error in the result is excpected
//			AssertJUnit.fail("Unexpected success of modelService.executeChanges(), expected an exception");
//        } catch (SchemaException e) {
//        	// This is expected
//        	display("Expected exception", e);
//        }
        
        result.computeStatus();
        
        assertFailure(result);
		
	}
	
	/**
	 * The "while" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
	 * this results in account without any attributes. It should fail.
	 */
	@Test
    public void test101AddUserCharlesAssignBlankAccount() throws Exception {
		final String TEST_NAME = "test101AddUserCharlesAssignBlankAccount";
        displayTestTile(this, "TEST_NAME");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        PrismObject<UserType> userCharles = createUser("charles", "Charles L. Charles");
        fillinUserAssignmentAccountConstruction(userCharles, RESOURCE_DUMMY_WHITE_OID);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(userCharles);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
//        try {
			// WHEN
			modelService.executeChanges(deltas, null, task, result);
			//not expected that it fails, insted the fatal error in the result is excpected
//			AssertJUnit.fail("Unexpected success of modelService.executeChanges(), expected an exception");
//        } catch (SchemaException e) {
//        	// This is expected
//        	display("Expected exception", e);
//        }
        
        result.computeStatus();
        assertFailure(result);
        
        // Even though the operation failed the addition of a user should be successful. Let's check if user was really added.
        String userOid = userDelta.getOid();
        assertNotNull("No user OID in delta after operation", userOid);
        
        PrismObject<UserType> userAfter = getUser(userOid);
        assertUser(userAfter, userOid, "charles", "Charles L. Charles", null, null);
		
	}
		

}
