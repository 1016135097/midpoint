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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.Holder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.test.util.ModelServiceUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model-unit-test.xml",
		"classpath:application-context-model.xml" })
public class ModelService2Test {

	private static final File TEST_FOLDER_CONTROLLER = new File("./src/test/resources/controller");
	@Autowired(required = true)
	ModelPortType modelService;
	@Autowired(required = true)
	ProvisioningService provisioningService;
	@Autowired(required = true)
	RepositoryService repositoryService;

	@Before
	public void before() {
		Mockito.reset(provisioningService, repositoryService);
	}

	@Test(expected = FaultMessage.class)
	public void addNullObject() throws FaultMessage {
		try {
			modelService.addObject(null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Add must fail.");
	}

	@Test(expected = FaultMessage.class)
	@SuppressWarnings("unchecked")
	public void addUserWithoutName() throws Exception {
		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml"))).getValue();
		try {
			modelService.addObject(expectedUser, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("add must fail.");
	}

	@Test(expected = FaultMessage.class)
	public void testGetNullOid() throws FaultMessage {
		try {
			modelService.getObject(null, new PropertyReferenceListType(), new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("get must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testGetEmptyOid() throws FaultMessage {
		try {
			modelService.getObject("", new PropertyReferenceListType(), new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("get must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testGetNullOidAndPropertyRef() throws FaultMessage {
		try {
			modelService.getObject(null, null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("get must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testGetNullPropertyRef() throws FaultMessage {
		try {
			modelService.getObject("001", null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("get must fail");
	}

	@Test(expected = FaultMessage.class)
	public void getNonexistingObject() throws FaultMessage, ObjectNotFoundException, SchemaException {
		try {
			final String oid = "abababab-abab-abab-abab-000000000001";
			when(
					repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class),
							any(OperationResult.class))).thenThrow(
					new ObjectNotFoundException("Object with oid '" + oid + "' not found."));

			modelService.getObject(oid, new PropertyReferenceListType(), new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertObjectNotFoundFault(ex);
		}
		fail("get must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteNullOid() throws FaultMessage {
		try {
			modelService.deleteObject(null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("delete must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteEmptyOid() throws FaultMessage {
		try {
			modelService.deleteObject("", null);
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("delete must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteNullResult() throws FaultMessage {
		try {
			modelService.deleteObject("1", null);
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("delete must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteNonExisting() throws FaultMessage, ObjectNotFoundException, SchemaException {
		try {
			final String oid = "abababab-abab-abab-abab-000000000001";
			when(
					repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class),
							any(OperationResult.class))).thenThrow(
					new ObjectNotFoundException("Object with oid '' not found."));

			modelService.deleteObject(oid, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertObjectNotFoundFault(ex);
		}
		fail("delete must fail");
	}

	@Test(expected = FaultMessage.class)
	public void nullObjectType() throws FaultMessage {
		try {
			modelService.listObjects(null, new PagingType(), new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void nullObjectTypeAndPaging() throws FaultMessage {
		try {
			modelService.listObjects(null, null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void nullPagingList() throws FaultMessage {
		try {
			modelService.listObjects("", null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void badPagingList() throws FaultMessage {
		PagingType paging = new PagingType();
		paging.setMaxSize(-1);
		paging.setOffset(-1);

		try {
			modelService.listObjects(ObjectTypes.USER.getObjectTypeUri(), paging,
					new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void nullOid() throws FaultMessage {
		try {
			modelService.getPropertyAvailableValues(null, new PropertyReferenceListType(),
					new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument excetion must be thrown");
	}

	@Test(expected = FaultMessage.class)
	public void emptyOid() throws FaultMessage {
		try {
			modelService.getPropertyAvailableValues("", new PropertyReferenceListType(),
					new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument excetion must be thrown");
	}

	@Test(expected = FaultMessage.class)
	public void nullQueryType() throws FaultMessage {
		try {
			modelService.searchObjects(null, new PagingType(), new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void nullQueryTypeAndPaging() throws FaultMessage {
		try {
			modelService
					.searchObjects(null, null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void nullPagingSearch() throws FaultMessage {
		try {
			modelService.searchObjects(new QueryType(), null, new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void badPagingSearch() throws FaultMessage {
		PagingType paging = new PagingType();
		paging.setMaxSize(-1);
		paging.setOffset(-1);

		try {
			modelService.searchObjects(new QueryType(), paging, new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("Illegal argument exception was not thrown.");
	}
}
