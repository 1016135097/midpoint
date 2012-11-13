/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiConnector extends AbstractModelIntegrationTest {
			
	private String connectorDummyOid;
	private String connectorDummyFakeOid;
	
	private PrismObject<ResourceType> resourceDummy;
	private PrismObject<ResourceType> resourceDummyFake;
	
	protected static DummyResource dummyResource;

	public TestMultiConnector() throws JAXBException {
		super();
	}
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// Make sure that the connectors are discovered
		modelService.postInit(initResult);
		
		// Make sure to call postInit first. This add system config to repo.
		// If system is initialized after that then the logging config from system config
		// will be used instead of test logging config
		super.initSystem(initTask, initResult);
		
		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();
		
		addDummyAccount(dummyResource, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot");
		addDummyAccount(dummyResource, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood");
		addDummyAccount(dummyResource, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");
	}
	
	@Test
    public void test010ListConnectors() throws Exception {
        displayTestTile(this, "test010ListConnectors");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test010ListConnectors");
        OperationResult result = task.getResult();
        
		// WHEN
        List<PrismObject<ConnectorType>> connectors = modelService.searchObjects(ConnectorType.class, null, null, task, result);
		
		// THEN
        display("Connectors", connectors);
        result.computeStatus();
        IntegrationTestTools.assertSuccess("getObject result", result);
        
        assertEquals("Unexpected number of connectors", 5, connectors.size());
        for(PrismObject<ConnectorType> connector: connectors) {
        	display("Connector", connector);
        	ConnectorType connectorType = connector.asObjectable();
        	if (CONNECTOR_DUMMY_TYPE.equals(connectorType.getConnectorType())) {
        		String connectorVersion = connectorType.getConnectorVersion();
        		if (connectorVersion.contains("fake")) {
        			display("Fake Dummy Connector OID", connector.getOid());
        			connectorDummyFakeOid = connector.getOid();
        		} else {
        			display("Dummy Connector OID", connector.getOid());
        			connectorDummyOid = connector.getOid();
        		}
        	}
        }
        
        assertNotNull("No dummy connector", connectorDummyOid);
        assertNotNull("No fake dummy connector", connectorDummyFakeOid);

	}
	
	@Test
    public void test020ImportFakeResource() throws Exception {
        displayTestTile(this, "test020ImportFakeResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test020ImportFakeResource");
        OperationResult result = task.getResult();
        
		// WHEN
        importObjectFromFile(RESOURCE_DUMMY_FAKE_FILENAME, result);
		
		// THEN
        result.computeStatus();
        display("Import result", result);
        IntegrationTestTools.assertSuccess("import result", result, 2);
        
        resourceDummyFake = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, null, task, result);
        display("Imported resource", resourceDummyFake);
        assertNotNull("Null fake resource after getObject", resourceDummyFake);
        assertEquals("Wrong connectorRef in fake resource", connectorDummyFakeOid, 
        		resourceDummyFake.asObjectable().getConnectorRef().getOid());

	}
	
	@Test
    public void test021TestFakeResource() throws Exception {
        displayTestTile(this, "test021TestFakeResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test021TestFakeResource");
        OperationResult result = task.getResult();
        
		// WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_FAKE_OID, task);
		
		// THEN
 		display("testResource result", testResult);
        IntegrationTestTools.assertSuccess("testResource result", testResult);
	}
	
	@Test
    public void test022ListAccountsFakeResource() throws Exception {
        displayTestTile(this, "test022ListAccountsFakeResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test022ListAccountsFakeResource");
        OperationResult result = task.getResult();
        
		// WHEN
        Collection<PrismObject<AccountShadowType>> accounts = listAccounts(resourceDummyFake, task, result);
		
		// THEN
        result.computeStatus();
 		display("listAccounts result", result);
        IntegrationTestTools.assertSuccess("listAccounts result", result);
        
        assertEquals("Unexpected number of accounts: "+accounts, 1, accounts.size());
	}

	@Test
    public void test030ImportDummyResource() throws Exception {
        displayTestTile(this, "test030ImportDummyResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test030ImportDummyResource");
        OperationResult result = task.getResult();
        
		// WHEN
        importObjectFromFile(RESOURCE_DUMMY_FILENAME, result);
		
		// THEN
        result.computeStatus();
        display("Import result", result);
        IntegrationTestTools.assertSuccess("import result", result, 2);
        
        resourceDummy = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        display("Imported resource", resourceDummy);
        assertNotNull("Null fake resource after getObject", resourceDummy);
        assertEquals("Wrong connectorRef in fake resource", connectorDummyOid, 
        		resourceDummy.asObjectable().getConnectorRef().getOid());

	}
	
	@Test
    public void test031TestFakeResource() throws Exception {
        displayTestTile(this, "test031TestFakeResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test031TestFakeResource");
        OperationResult result = task.getResult();
        
		// WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OID, task);
		
		// THEN
 		display("testResource result", testResult);
        IntegrationTestTools.assertSuccess("testResource result", testResult);
	}
	
	@Test
    public void test032ListAccountsDummyResource() throws Exception {
        displayTestTile(this, "test032ListAccountsDummyResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test032ListAccountsDummyResource");
        OperationResult result = task.getResult();
        
		// WHEN
        Collection<PrismObject<AccountShadowType>> accounts = listAccounts(resourceDummy, task, result);
		
		// THEN
        result.computeStatus();
 		display("listAccounts result", result);
        IntegrationTestTools.assertSuccess("listAccounts result", result);
        
        assertEquals("Unexpected number of accounts: "+accounts, 3, accounts.size());
	}
	
	@Test
    public void test100Upgrade() throws Exception {
        displayTestTile(this, "test100Upgrade");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test100Upgrade");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaDel = ReferenceDelta.createModificationDelete(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyFakeOid);
        resourceDelta.addModification(connectorRefDeltaDel);
        ReferenceDelta connectorRefDeltaAdd = ReferenceDelta.createModificationAdd(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyOid);
		resourceDelta.addModification(connectorRefDeltaAdd);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        // Check if the changes went well in the repo
        PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, result);
        display("Upgraded fake resource (repo)", repoResource);
        assertNotNull("Null fake resource after getObject (repo)", repoResource);
        assertEquals("Oooops. The OID of fake resource mysteriously changed. Call the police! (repo)", RESOURCE_DUMMY_FAKE_OID, repoResource.getOid());
        assertEquals("Wrong connectorRef in fake resource (repo)", connectorDummyOid, 
        		repoResource.asObjectable().getConnectorRef().getOid());
        
        // Check if resource view of the model has changed as well
        resourceDummyFake = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, null, task, result);
        display("Upgraded fake resource (model)", resourceDummyFake);
        assertNotNull("Null fake resource after getObject (model)", resourceDummyFake);
        assertEquals("Oooops. The OID of fake resource mysteriously changed. Call the police! (model)", RESOURCE_DUMMY_FAKE_OID, resourceDummyFake.getOid());
        assertEquals("Wrong connectorRef in fake resource (model)", connectorDummyOid, 
        		resourceDummyFake.asObjectable().getConnectorRef().getOid());
        
        // Check if the other resource is still untouched
        PrismObject<ResourceType> dummyResourceModelAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        dummyResourceModelBefore.asObjectable().setFetchResult(null);
        dummyResourceModelAfter.asObjectable().setFetchResult(null);
        ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(dummyResourceModelBefore, dummyResourceModelAfter);
        display("Dummy resource diff", dummyResourceDiff);
        assertTrue("Ha! Someone touched the other resource! Off with his head! diff:"+dummyResourceDiff, dummyResourceDiff.isEmpty());
	}
	
	// TODO: downgrade
	// TODO: replace
	// TODO: model.modify RAW

	
}
