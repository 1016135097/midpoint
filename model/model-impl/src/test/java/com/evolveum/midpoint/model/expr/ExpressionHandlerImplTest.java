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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.expr;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-audit.xml" })
public class ExpressionHandlerImplTest extends AbstractTestNGSpringContextTests {

	private static final Trace LOGGER = TraceManager.getTrace(ExpressionHandlerImplTest.class);
	private static final File TEST_FOLDER = new File("./src/test/resources");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	@Autowired
	private ExpressionHandler expressionHandler;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConfirmUser() throws Exception {
		PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(
				TEST_FOLDER, "account-xpath-evaluation.xml"));
		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_FOLDER, "user-new.xml"));

		ExpressionType expression = PrismTestUtil.unmarshalObject(
						"<object xsi:type=\"ExpressionType\" xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" "
								+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
								+ "<code>declare namespace c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\";\n"
								+ "declare namespace dj=\"http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ\";\n"
								+ "$c:user/c:givenName = $c:account/c:attributes/dj:givenName</code></object>", ExpressionType.class);

		OperationResult result = new OperationResult("testConfirmUser");
		boolean confirmed = expressionHandler.evaluateConfirmationExpression(user.asObjectable(), account.asObjectable(), expression,
				result);
		LOGGER.info(result.dump());

		assertTrue("Wrong expression result (expected true)", confirmed);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEvaluateExpression() throws Exception {
		PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(TEST_FOLDER, "expr/account.xml"));
		AccountShadowType accountType = account.asObjectable();
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON, "resource-opendj.xml"));
		ResourceType resourceType = resource.asObjectable();
		accountType.setResource(resourceType);

		Element valueExpressionElement = findChildElement(resourceType.getSynchronization().getCorrelation()
				.getFilter(), SchemaConstants.NS_C, "valueExpression");
		ExpressionType expression = PrismTestUtil.getPrismContext().getPrismJaxbProcessor()
				.toJavaValue(valueExpressionElement, ExpressionType.class);
		LOGGER.debug(SchemaDebugUtil.prettyPrint(expression));

		OperationResult result = new OperationResult("testCorrelationRule");
		String name = expressionHandler.evaluateExpression(accountType, expression, "test expression", result);
		LOGGER.info(result.dump());

		assertEquals("Wrong expression result", "hbarbossa", name);
	}

	private Element findChildElement(Element element, String namespace, String name) {
		NodeList list = element.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && namespace.equals(node.getNamespaceURI())
					&& name.equals(node.getLocalName())) {
				return (Element) node;
			}
		}
		return null;
	}
}
