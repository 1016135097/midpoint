/**
 * 
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import com.evolveum.midpoint.prism.*;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author Radovan Semancik
 *
 */
public class TestSchemaRegistry {

	private static final String FOO_NAMESPACE = "http://example.com/xml/ns/foo";
	private static final String USER_EXT_NAMESPACE = "http://example.com/xml/ns/user-extension";
	private static final String EXTENSION_SCHEMA_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/test/extension";

	/**
	 * Test whether the midpoint prism context was constructed OK and if it can validate
	 * ordinary user object.
	 */
	@Test
	public void testBasic() throws SAXException, IOException, SchemaException {
		
		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry reg = context.getSchemaRegistry();
		Schema javaxSchema = reg.getJavaxSchema();
		assertNotNull(javaxSchema);
		
		// Try to use the schema to validate Jack
		Document document = DOMUtil.parseFile("src/test/resources/common/user-jack.xml");
		Validator validator = javaxSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(document), validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}
	

	@Test
	public void testCommonSchema() throws SchemaException, SAXException, IOException {

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();
		
		PrismSchema commonSchema = schemaRegistry.getObjectSchema();
		assertNotNull("No parsed common schema", commonSchema);
		System.out.println("Parsed common schema:");
		System.out.println(commonSchema.dump());
		
		// TODO
	}

    @Test
    public void testReferenceInExtension() throws SchemaException, SAXException, IOException {

        MidPointPrismContextFactory factory = getContextFactory();
        PrismContext context = factory.createInitializedPrismContext();
        SchemaRegistry schemaRegistry = context.getSchemaRegistry();
        
        // Common schema should be parsed during creation of the context
        schemaRegistry.loadPrismSchemaResource("schema/extension.xsd");
        
        // Check that the extension schema was loaded
        PrismSchema extensionSchema = schemaRegistry.findSchemaByNamespace(EXTENSION_SCHEMA_NAMESPACE);
        assertNotNull("Extension schema not parsed", extensionSchema);

        ItemDefinition itemDefinition = schemaRegistry.findItemDefinitionByElementName(TestConstants.EXTENSION_USER_REF_ELEMENT);
        assertNotNull("userRef element definition was not found", itemDefinition);
        System.out.println("UserRef definition:");
        System.out.println(itemDefinition.dump());

        assertEquals("Wrong userRef definition class", PrismReferenceDefinition.class, itemDefinition.getClass());
    }

    @Test
	public void testUserType() throws SchemaException, SAXException, IOException {
		
		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();

		PrismObjectDefinition<UserType> userDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
		assertNotNull("No user definition", userDefinition);
		
		System.out.println("testCommonSchemaUserType:");
		System.out.println(userDefinition.dump());
		
		assertFalse("User definition is marked as runtime", userDefinition.isRuntimeSchema());
		
		PrismPropertyDefinition nameDef = userDefinition.findPropertyDefinition(ObjectType.F_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = userDefinition.findContainerDefinition(SchemaConstants.C_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue("Extension definition is NOT marked as runtime", extensionDef.isRuntimeSchema());
		
		PrismPropertyDefinition givenNameDef = userDefinition.findPropertyDefinition(UserType.F_GIVEN_NAME);
		assertNotNull("No givenName definition", givenNameDef);
	}
	
	@Test
	public void testCommonSchemaAccountType() throws SchemaException, SAXException, IOException {

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();
				
		PrismObjectDefinition<AccountShadowType> accountDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
		assertNotNull("No account definition", accountDef);

		System.out.println("testCommonSchemaAccountType:");
		System.out.println(accountDef.dump());
		
		PrismPropertyDefinition nameDef = accountDef.findPropertyDefinition(SchemaConstants.C_NAME);
		assertNotNull("No name definition", nameDef);
		
		PrismContainerDefinition extensionDef = accountDef.findContainerDefinition(SchemaConstants.C_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue("'extension' definition is not marked as runtime", extensionDef.isRuntimeSchema());
		
		PrismContainerDefinition attributesDef = accountDef.findContainerDefinition(SchemaConstants.I_ATTRIBUTES);
		assertNotNull("No 'attributes' definition", attributesDef);
		assertTrue("'attributes' definition is not marked as runtime", attributesDef.isRuntimeSchema());
	}
	
	private MidPointPrismContextFactory getContextFactory() {
		return new MidPointPrismContextFactory();
	}
	
}
