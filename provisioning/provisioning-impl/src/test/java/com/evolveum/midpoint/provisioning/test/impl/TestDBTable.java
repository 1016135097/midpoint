/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

/**
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDBTable extends AbstractIntegrationTest {
	
	private static final String FILENAME_RESOURCE_DERBY = "src/test/resources/object/resource-derby.xml";
	private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";
	private static final String ACCOUNT_WILL_FILENAME = "src/test/resources/impl/account-derby.xml";
	private static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
	private static final String ACCOUNT_WILL_USERNAME = "will";
	private static final String ACCOUNT_WILL_FULLNAME = "Will Turner";
	private static final String ACCOUNT_WILL_PASSWORD = "3lizab3th";
	private static final String DB_TABLE_CONNECTOR_TYPE = "org.identityconnectors.databasetable.DatabaseTableConnector";
	
	private static final Trace LOGGER = TraceManager.getTrace(TestDBTable.class);

	private static DerbyController derbyController = new DerbyController();
	
	@Autowired
	private ProvisioningService provisioningService;
	
//	@Autowired
//	private TaskManager taskManager;
	
	@Autowired
	private SynchornizationServiceMock syncServiceMock;
	
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		addResourceFromFile(FILENAME_RESOURCE_DERBY, DB_TABLE_CONNECTOR_TYPE, initResult);
	}
	
	@BeforeClass
	public static void startDb() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  ProvisioningServiceImplDBTest");
		LOGGER.info("------------------------------------------------------------------------------");
		derbyController.startCleanServer();
	}

	@AfterClass
	public static void stopDb() throws Exception {
		derbyController.stop();
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  ProvisioningServiceImplDBTest");
		LOGGER.info("------------------------------------------------------------------------------");
	}

	
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");
		
		OperationResult result = new OperationResult(TestDBTable.class.getName()+".test000Integrity");
		
		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, result).asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, result).asObjectable();
		assertNotNull(connector);
		display("DB Connector",connector);
	}
	
	@Test
	public void test001Connection() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test001Connection");
		OperationResult result = new OperationResult(TestDBTable.class.getName()+".test001Connection");
		
		OperationResult testResult = provisioningService.testResource(RESOURCE_DERBY_OID);
		
		display("Test result",testResult);
		assertSuccess("Test resource failed (result)", testResult);
		
		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, result).asObjectable();
		display("Resource after test",resource);
	}
	
	@Test
	public void test002AddAccount() throws Exception {
		final String TEST_NAME = "test002AddAccount";
		displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDBTable.class.getName()
				+ "." + TEST_NAME);

		AccountShadowType account = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, AccountShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(account));
		System.out.println(account.asPrismObject().dump());

		Task task = taskManager.createTaskInstance();
		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, task, result);
		
		// THEN
		result.computeStatus();
		display("add object result",result);
		assertSuccess("addObject has failed (result)",result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, accountType.getName());
//		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, provisioningAccountType.getName());
//		assertEquals("will", provisioningAccountType.getName());
		
		// Check database content
		
		Connection conn = derbyController.getConnection();
		// Check if it empty
		Statement stmt = conn.createStatement();
		stmt.execute("select * from users");
		ResultSet rs = stmt.getResultSet();
		
		assertTrue("The \"users\" table is empty",rs.next());
		assertEquals(ACCOUNT_WILL_USERNAME,rs.getString(DerbyController.COLUMN_LOGIN));
		assertEquals(ACCOUNT_WILL_PASSWORD,rs.getString(DerbyController.COLUMN_PASSWORD));
		assertEquals(ACCOUNT_WILL_FULLNAME,rs.getString(DerbyController.COLUMN_FULL_NAME));
		
		assertFalse("The \"users\" table has more than one record",rs.next());
		rs.close();
		stmt.close();
	}
	
	// MID-1234
	@Test(enabled=false)
	public void test005GetAccount() throws Exception {
		final String TEST_NAME = "test005GetAccount";
		displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDBTable.class.getName()
				+ "." + TEST_NAME);

		Task task = taskManager.createTaskInstance();
		// WHEN
		PrismObject<AccountShadowType> account = provisioningService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, null, result);
		
		// THEN
		result.computeStatus();
		display(result);
		assertSuccess(result);

		PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, account.asObjectable().getName());

		assertNotNull("No credentials", account.asObjectable().getCredentials());
		assertNotNull("No password", account.asObjectable().getCredentials().getPassword());
		assertNotNull("No password value", account.asObjectable().getCredentials().getPassword().getValue());
		ProtectedStringType password = account.asObjectable().getCredentials().getPassword().getValue();
		display("Password", password);
		String clearPassword = protector.decryptString(password);
		assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, clearPassword);
	}
	
	
}
