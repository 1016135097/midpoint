/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

/**
 * The test of Provisioning service on the API level. The test is using dummy resource for speed and flexibility.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-provisioning.xml",
		"classpath:ctx-provisioning-test.xml",
		"classpath:ctx-task.xml",
        "classpath:ctx-audit.xml",
		"classpath:ctx-repository.xml",
		"classpath:ctx-repo-cache.xml",
		"classpath:ctx-configuration-test.xml" })
@DirtiesContext
public class TestDummyHacks extends AbstractIntegrationTest {
	
	private static final String TEST_DIR = "src/test/resources/impl/dummy-hacks/";

	private static final String CONNECTOR_DUMMY_FILENAME = TEST_DIR + "connector-dummy.xml";

	private static final String RESOURCE_DUMMY_FILENAME = TEST_DIR + "resource-dummy.xml";
	private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";	
		
	private static final Trace LOGGER = TraceManager.getTrace(TestDummyHacks.class);

	private PrismObject<ConnectorType> connector;
	private PrismObject<ResourceType> resource;
	private ResourceType resourceType;
	private static DummyResource dummyResource;
	private static Task syncTask;
	
	@Autowired(required=true)
	private ProvisioningService provisioningService;
	
	// Used to make sure that the connector is cached
	@Autowired(required=true)
	private ConnectorTypeManager connectorTypeManager;
	
	@Autowired(required=true)
	private SynchornizationServiceMock syncServiceMock; 
	

	/**
	 * @throws JAXBException
	 */
	public TestDummyHacks() throws JAXBException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// DO NOT DO provisioningService.postInit(..)
		// We want to avoid connector discovery and insert our own connector object
//		provisioningService.postInit(initResult);
		
		connector = addObjectFromFile(CONNECTOR_DUMMY_FILENAME, ConnectorType.class, initResult);
		
		resource = addObjectFromFile(RESOURCE_DUMMY_FILENAME, ResourceType.class, initResult);
		resourceType = resource.asObjectable();
		
		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();		
	}


	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test003Connection() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test003Connection");
		// GIVEN
		OperationResult result = new OperationResult(TestDummyHacks.class.getName()
				+ ".test003Connection");
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				result).asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore
				.getConnectorRef().getOid(), result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);

		// THEN
		display("Test result", testResult);
		assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable(); 
		display("Resource after test", resourceTypeRepoAfter);

		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		String resourceXml = prismContext.getPrismDomProcessor().serializeObjectToString(resourceRepoAfter);
		display("Resource XML", resourceXml);

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resourceBefore.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		// The useless configuration variables should be reflected to the resource now
		assertEquals("Wrong useless string", "Shiver me timbers!", dummyResource.getUselessString());
		assertEquals("Wrong guarded useless string", "Dead men tell no tales", dummyResource.getUselessGuardedString());
	}

}
