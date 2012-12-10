/*
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
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;

/** 
 * @author Radovan Semancik
 * @author Katka Valalikova
 */

@ContextConfiguration(locations = { "classpath:ctx-provisioning.xml",
		"classpath:ctx-provisioning-test.xml",
        "classpath:ctx-audit.xml",
		"classpath:ctx-task.xml",
		"classpath:ctx-repository.xml",
		"classpath:ctx-repo-cache.xml",
		"classpath:ctx-configuration-test.xml" })
@DirtiesContext
public class TestConnectorDiscovery extends AbstractIntegrationTest {

	private static final String LDAP_CONNECTOR_TYPE = "org.identityconnectors.ldap.LdapConnector";

	@Autowired
	private ProvisioningService provisioningService;

	private static Trace LOGGER = TraceManager.getTrace(TestConnectorDiscovery.class);

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
	}
		
	
	/**
	 * Check whether the connectors were discovered correctly and were added to the repository.
	 * @throws SchemaException
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
		displayTestTile("test001Connectors");
		
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".test001Connectors");
		
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, null, result);
		
		assertFalse("No connector found",connectors.isEmpty());
		display("Found "+connectors.size()+" discovered connector");
		
		for (PrismObject<ConnectorType> connector : connectors) {
			ConnectorType conn = connector.asObjectable();
			display("Found connector " +conn, conn);
//			if (conn.getConnectorType().equals("org.identityconnectors.ldap.LdapConnector")) {
//				// This connector is loaded manually, it has no schema
//				continue;
//			}
			ProvisioningTestUtil.assertConnectorSchemaSanity(conn, prismContext);
		}
		
		assertEquals("Unexpected number of connectors found", 4, connectors.size());
	}
		
	@Test
	public void testListConnectors() throws Exception{
		displayTestTile("testListConnectors");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".listConnectorsTest");
		
		List<PrismObject<ConnectorType>> connectors = provisioningService.searchObjects(ConnectorType.class, null, result);
		assertNotNull(connectors);
		
		for (PrismObject<ConnectorType> connector : connectors){
			ConnectorType conn = connector.asObjectable();
			System.out.println(conn.toString());
			System.out.println("connector name: "+ conn.getName());
			System.out.println("connector type: "+ conn.getConnectorType());
			System.out.println("-----\n");
		}
		
		assertEquals("Unexpected number of connectors found", 4, connectors.size());
	}
	
	@Test
	public void testSearchConnectorSimple() throws SchemaException{
		displayTestTile("testSearchConnectorSimple");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".testSearchConnector");
		
		PrismObject<ConnectorType> ldapConnector = findConnectorByType(LDAP_CONNECTOR_TYPE, result);
		assertEquals("Type does not match", LDAP_CONNECTOR_TYPE, ldapConnector.asObjectable().getConnectorType());
	}
	
	
	@Test
	public void testSearchConnectorAnd() throws SchemaException{
		displayTestTile("testSearchConnectorAnd");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".testSearchConnector");
		
		Document doc = DOMUtil.getDocument();
//		Element filter = QueryUtil
//				.createAndFilter(
//						doc,
//						QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_FRAMEWORK,
//								ConnectorFactoryIcfImpl.ICF_FRAMEWORK_URI),
//						QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE,
//								LDAP_CONNECTOR_TYPE));
//	
//		QueryType query = new QueryType();
//		query.setFilter(filter);
		AndFilter filter = AndFilter.createAnd(
				EqualsFilter.createEqual(ConnectorType.class, prismContext, SchemaConstants.C_CONNECTOR_FRAMEWORK, ConnectorFactoryIcfImpl.ICF_FRAMEWORK_URI),
				EqualsFilter.createEqual(ConnectorType.class, prismContext, SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE, LDAP_CONNECTOR_TYPE));
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		
		System.out.println("Query:\n"+query.dump());

		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, result);
		
		assertEquals("Unexpected number of results", 1, connectors.size());
		PrismObject<ConnectorType> ldapConnector = connectors.get(0);
		assertEquals("Type does not match", LDAP_CONNECTOR_TYPE, ldapConnector.asObjectable().getConnectorType());
		assertEquals("Framework does not match", ConnectorFactoryIcfImpl.ICF_FRAMEWORK_URI, ldapConnector.asObjectable().getFramework());
	}
}
