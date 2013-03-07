/*
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AddOverwriteTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(AddOverwriteTest.class);
    private static final String ORG_STRUCT_OBJECTS = "src/test/resources/orgstruct/org-monkey-island.xml";
    private static final String IMPORT_OVERWRITE = "src/test/resources/basic/import-overwrite.xml";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void addWithOverwrite() throws Exception {
        List<PrismObject<?>> objects = prismContext.getPrismDomProcessor().parseObjects(new File(ORG_STRUCT_OBJECTS));
        OperationResult opResult = new OperationResult("Import file");
        for (PrismObject o : objects) {
            repositoryService.addObject(o, null, opResult);
        }
        opResult.recordSuccess();
        opResult.recomputeStatus();

        AssertJUnit.assertTrue(opResult.isSuccess());

        //get carla, check oid and version
        PrismObject carla = getCarla(opResult);
        final String oid = carla.getOid();
        PrismAsserts.assertPropertyValue(carla, ObjectType.F_NAME, PrismTestUtil.createPolyString("carla"));
        PrismAsserts.assertPropertyValue(carla, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Carla"));
        AssertJUnit.assertEquals("0", carla.getVersion());
        AssertJUnit.assertNotNull(oid);

        //reimport carla, oid should stay the same, version must be incremented
        objects = prismContext.getPrismDomProcessor().parseObjects(new File(IMPORT_OVERWRITE));
        PrismObject newCarla = objects.get(0);
        newCarla.setOid(oid);

        String newOid = repositoryService.addObject(newCarla, RepoAddOptions.createOverwrite(), opResult);
        AssertJUnit.assertEquals(oid, newOid);


        carla = getCarla(opResult);
        PrismAsserts.assertPropertyValue(carla, ObjectType.F_NAME, PrismTestUtil.createPolyString("carla"));
        PrismAsserts.assertPropertyValue(carla, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Carla imported"));
        AssertJUnit.assertEquals("1", carla.getVersion());
        AssertJUnit.assertEquals(oid, carla.getOid());
    }

    private PrismObject getCarla(OperationResult opResult) throws Exception {
        final String CARLA_NAME = "carla";
        final ObjectQuery query = new ObjectQuery();
        PrismObjectDefinition userObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        query.setFilter(EqualsFilter.createEqual(null, userObjectDef, ObjectType.F_NAME, CARLA_NAME));
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, opResult);
        AssertJUnit.assertEquals(1, users.size());
        return users.get(0);
    }
}
