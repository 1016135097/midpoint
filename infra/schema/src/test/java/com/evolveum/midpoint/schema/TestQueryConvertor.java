package com.evolveum.midpoint.schema;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.test.TestUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class TestQueryConvertor {

	private static final Trace LOGGER = TraceManager.getTrace(TestQueryConvertor.class);

	private static final File TEST_DIR = new File("src/test/resources/queryconvertor");

	private PrismContext prismContext;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		// PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		PrismContextFactory contextFactory = MidPointPrismContextFactory.FACTORY;
		prismContext = contextFactory.createPrismContext();
		prismContext.initialize();
	}

	@Test
	public void testAccountQuery() throws Exception {
		LOGGER.info("===[ testAccountQuery ]===");
		File[] queriesToTest = new File[] { new File(TEST_DIR + "/query.xml"),
				new File(TEST_DIR + "/query-account-by-attributes-and-resource-ref.xml"), new File(TEST_DIR + "/query-or-composite.xml")};

		for (File file : queriesToTest) {
			LOGGER.info("Start to convert query {} ", file.getName());
			QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(file, QueryType.class);
			LOGGER.info("===[ query type parsed ]===");
			ObjectQuery query = null;
			try {
				query = QueryConvertor.createObjectQuery(AccountShadowType.class, queryType,
						prismContext);
				LOGGER.info("query converted: ");

				LOGGER.info("QUERY DUMP: {}", query.dump());
				
				QueryType convertedQueryType = QueryConvertor.createQueryType(query);
				LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));
				
			} catch (SchemaException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			} catch (RuntimeException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			} catch (Exception ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			}

		
		}
		//TODO: add some asserts
	}
	
	@Test
	public void testConnectorQuery() throws Exception{
		LOGGER.info("===[ testConnectorQuery ]===");
		File connectorQueryToTest = new File(TEST_DIR + "/query-connector-by-type.xml");
		QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(connectorQueryToTest,
				QueryType.class);
		LOGGER.info("===[ query type parsed ]===");
		ObjectQuery query = null;
		try {
			query = QueryConvertor.createObjectQuery(ConnectorType.class, queryType, prismContext);
			LOGGER.info("query converted: ");

			LOGGER.info("QUERY DUMP: {}", query.dump());
			AssertJUnit.assertNotNull(query);
			AssertJUnit.assertEquals(EqualsFilter.class, query.getFilter().getClass());
			List<? extends PrismValue> values = ((EqualsFilter) query.getFilter()).getValues();
			AssertJUnit.assertEquals(1, values.size());
			AssertJUnit.assertEquals(PrismPropertyValue.class, values.get(0).getClass());
			AssertJUnit.assertEquals("org.identityconnectors.ldap.LdapConnector", ((PrismPropertyValue) values.get(0)).getValue());
			AssertJUnit.assertEquals(ConnectorType.F_CONNECTOR_TYPE, ((EqualsFilter)query.getFilter()).getDefinition().getName());
			AssertJUnit.assertEquals(DOMUtil.XSD_STRING, ((EqualsFilter)query.getFilter()).getDefinition().getTypeName());
			
			QueryType convertedQueryType = QueryConvertor.createQueryType(query);
			LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));
		} catch (SchemaException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		}
		
	}
	
	@Test
	public void testGenericQuery() throws Exception{
		LOGGER.info("===[ testGenericQuery ]===");
		File genericsQueryToTest = new File(TEST_DIR + "/query-and-generic.xml");
		QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(genericsQueryToTest, QueryType.class);
		LOGGER.info("===[ query type parsed ]===");
		ObjectQuery query = null;
		try {
			query = QueryConvertor.createObjectQuery(GenericObjectType.class, queryType,
					prismContext);
			LOGGER.info("query converted: ");

			LOGGER.info("QUERY DUMP: {}", query.dump());
			//check parent filter
			AssertJUnit.assertNotNull(query);
			AssertJUnit.assertEquals(AndFilter.class, query.getFilter().getClass());
			AssertJUnit.assertEquals(2, ((AndFilter)query.getFilter()).getCondition().size());
			//check first condition
			ObjectFilter first = ((AndFilter) query.getFilter()).getCondition().get(0);
			AssertJUnit.assertEquals(EqualsFilter.class, first.getClass());
			List<? extends PrismValue> values = ((EqualsFilter) first).getValues();
			AssertJUnit.assertEquals(1, values.size());
			AssertJUnit.assertEquals(PrismPropertyValue.class, values.get(0).getClass());
			AssertJUnit.assertEquals(PrismTestUtil.createPolyString("generic object"), ((PrismPropertyValue) values.get(0)).getValue());
			AssertJUnit.assertEquals(GenericObjectType.F_NAME, ((EqualsFilter)first).getDefinition().getName());
			AssertJUnit.assertEquals(PolyStringType.COMPLEX_TYPE, ((EqualsFilter)first).getDefinition().getTypeName());
			//check second condition
			ObjectFilter second = ((AndFilter) query.getFilter()).getCondition().get(1);
			AssertJUnit.assertEquals(EqualsFilter.class, second.getClass());
			List<? extends PrismValue> secValues = ((EqualsFilter) second).getValues();
			AssertJUnit.assertEquals(1, secValues.size());
			AssertJUnit.assertEquals(PrismPropertyValue.class, secValues.get(0).getClass());
			AssertJUnit.assertEquals(123, ((PrismPropertyValue) secValues.get(0)).getValue());
			AssertJUnit.assertEquals(new QName("http://midpoint.evolveum.com/xml/ns/test/extension", "intType"), ((EqualsFilter)second).getDefinition().getName());
			AssertJUnit.assertEquals(DOMUtil.XSD_INT, ((EqualsFilter)second).getDefinition().getTypeName());
			
			QueryType convertedQueryType = QueryConvertor.createQueryType(query);
			LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));
		} catch (SchemaException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@Test
	public void testUserQuery() throws Exception{
		LOGGER.info("===[ testUserQuery ]===");
		File[] userQueriesToTest = new File[] { new File(TEST_DIR + "/query-user-by-fullName.xml"),
				new File(TEST_DIR + "/query-user-by-name.xml"),
				new File(TEST_DIR + "/query-user-substring-fullName.xml") };
		// prismContext.silentMarshalObject(queryTypeNew, LOGGER);
		for (File file : userQueriesToTest) {
			QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(file, QueryType.class);
			LOGGER.info("===[ query type parsed ]===");
			ObjectQuery query = null;
			try {
				query = QueryConvertor.createObjectQuery(UserType.class, queryType, prismContext);
				LOGGER.info("query converted: ");

				LOGGER.info("QUERY DUMP: {}", query.dump());
				
				QueryType convertedQueryType = QueryConvertor.createQueryType(query);
				LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));
			} catch (SchemaException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
				throw ex;
			} catch (RuntimeException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
				throw ex;
			} catch (Exception ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
				throw ex;
			}
		}
	}
}
