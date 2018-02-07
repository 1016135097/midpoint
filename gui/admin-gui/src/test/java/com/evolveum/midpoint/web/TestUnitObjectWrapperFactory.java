/**
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class TestUnitObjectWrapperFactory extends AbstractGuiUnitTest {

	@Test
    public void testCreateWrapperUser() throws Exception {
		final String TEST_NAME = "testCreateWrapperUser";
		TestUtil.displayTestTitle(TEST_NAME);

		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_REPO_FILE);
		PrismObjectDefinition<UserType> objDef = user.getDefinition();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator());
		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user,
				objDef, null, ContainerStatus.MODIFYING);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "user display name", "user description", user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 7, objectWrapper.getContainers().size());

		ContainerWrapper<UserType> mainContainerWrapper = objectWrapper.findContainerWrapper(ItemPath.EMPTY_PATH);
		WrapperTestUtil.assertWrapper(mainContainerWrapper, "prismContainer.mainPanelDisplayName", (ItemPath) null, user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+mainContainerWrapper, 1, mainContainerWrapper.getValues().size());
		ContainerValueWrapper<UserType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString("jack"));
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_TIMEZONE, null);

		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ActivationType.activation", UserType.F_ACTIVATION, user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
		ContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);
	}

	// We cannot unit test shadow wrapper. It requires initialized resource, resource schema, capabilities, working model service, etc.


}
