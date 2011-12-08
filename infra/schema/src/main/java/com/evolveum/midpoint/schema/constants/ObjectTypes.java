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
package com.evolveum.midpoint.schema.constants;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public enum ObjectTypes {

    ACCOUNT("schema.objectTypes.account", SchemaConstants.I_ACCOUNT_SHADOW_TYPE, SchemaConstants.I_ACCOUNT,
            AccountShadowType.class, true),

    CONNECTOR("schema.objectTypes.connector", SchemaConstants.I_CONNECTOR_TYPE, SchemaConstants.I_CONNECTOR,
            ConnectorType.class, true),

    CONNECTOR_HOST("schema.objectTypes.connectorHost", SchemaConstants.I_CONNECTOR_HOST_TYPE,
            SchemaConstants.I_CONNECTOR_HOST, ConnectorHostType.class, true),

    GENERIC_OBJECT("schema.objectTypes.genericObject", SchemaConstants.I_GENERIC_OBJECT_TYPE,
            SchemaConstants.I_GENERIC_OBJECT, GenericObjectType.class, false),

    RESOURCE("schema.objectTypes.resource", SchemaConstants.I_RESOURCE_TYPE, SchemaConstants.I_RESOURCE,
            ResourceType.class, true),

    USER("schema.objectTypes.user", SchemaConstants.I_USER_TYPE, SchemaConstants.I_USER, UserType.class,
            false),

    USER_TEMPLATE("schema.objectTypes.userTemplate", SchemaConstants.I_USER_TEMPLATE_TYPE,
            SchemaConstants.I_USER_TEMPLATE, UserTemplateType.class, false),

    SYSTEM_CONFIGURATION("schema.objectTypes.systemConfiguration",
            SchemaConstants.I_SYSTEM_CONFIGURATION_TYPE, SchemaConstants.I_SYSTEM_CONFIGURATION,
            SystemConfigurationType.class, false),

    TASK("schema.objectTypes.task", SchemaConstants.C_TASK_TYPE, SchemaConstants.C_TASK, TaskType.class,
            false),

    RESOURCE_OBJECT_SHADOW("schema.objectTypes.resourceObject",
            SchemaConstants.I_RESOURCE_OBJECT_SHADOW_TYPE, SchemaConstants.I_RESOURCE_OBJECT_SHADOW,
            ResourceObjectShadowType.class, true),

    OBJECT("schema.objectTypes.object", SchemaConstants.C_OBJECT_TYPE, SchemaConstants.C_OBJECT,
            ObjectType.class, false),

    ROLE("schema.objectTypes.role", SchemaConstants.ROLE_TYPE, SchemaConstants.ROLE, RoleType.class, false),

    PASSWORD_POLICY("schema.objectTypes.passwordPolicy", SchemaConstants.I_PASSWORD_POLICY_TYPE,
            SchemaConstants.I_PASSWORD_POLICY, PasswordPolicyType.class, false);

    private String localizationKey;
    private QName type;
    private QName name;
    private Class<? extends ObjectType> classDefinition;
    private boolean managedByProvisioning;

    private ObjectTypes(String key, QName type, QName name, Class<? extends ObjectType> classDefinition,
                        boolean managedByProvisioning) {
        this.localizationKey = key;
        this.type = type;
        this.name = name;
        this.classDefinition = classDefinition;
        this.managedByProvisioning = managedByProvisioning;
    }

    public boolean isManagedByProvisioning() {
        return managedByProvisioning;
    }

    public String getLocalizationKey() {
        return localizationKey;
    }

    public String getValue() {
        return type.getLocalPart();
    }

    public QName getQName() {
        return name;
    }

    public QName getTypeQName() {
        return type;
    }

    public Class<? extends ObjectType> getClassDefinition() {
        return classDefinition;
    }

    public String getObjectTypeUri() {
        return QNameUtil.qNameToUri(getTypeQName());
    }

    public static ObjectTypes getObjectType(String objectType) {
        for (ObjectTypes type : values()) {
            if (type.getValue().equals(objectType)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported object type " + objectType);
    }

    public static ObjectTypes getObjectTypeFromUri(String objectTypeUri) {
        for (ObjectTypes type : values()) {
            if (type.getObjectTypeUri().equals(objectTypeUri)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported object type uri " + objectTypeUri);
    }

    public static ObjectTypes getObjectTypeFromTypeQName(QName typeQName) {
        for (ObjectTypes type : values()) {
            if (type.getTypeQName().equals(typeQName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported object type qname " + typeQName);
    }

    public static String getObjectTypeUri(String objectType) {
        return getObjectType(objectType).getObjectTypeUri();
    }

    public static Class<? extends ObjectType> getObjectTypeClass(String objectType) {
        for (ObjectTypes type : values()) {
            if (type.getValue().equals(objectType)) {
                return type.getClassDefinition();
            }
        }

        throw new IllegalArgumentException("Unsupported object type " + objectType);
    }

    @SuppressWarnings("unchecked")
    public static ObjectTypes getObjectType(Class<? extends ObjectType> objectType) {
        for (ObjectTypes type : values()) {
            if (type.getClassDefinition().equals(objectType)) {
                return type;
            }
        }
        // No match. Try with superclass.
        Class<?> superclass = objectType.getSuperclass();
        if (superclass != null && !superclass.equals(ObjectType.class)) {
            return getObjectType((Class<? extends ObjectType>) superclass);
        }

        throw new IllegalArgumentException("Unsupported object type " + objectType);
    }

    public static boolean isManagedByProvisioning(ObjectType object) {
        Validate.notNull(object, "Object must not be null.");

        return isClassManagedByProvisioning(object.getClass());
    }

    public static boolean isClassManagedByProvisioning(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().isAssignableFrom(clazz)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static boolean isObjectTypeManagedByProvisioning(Class<? extends ObjectType> objectType) {
        Validate.notNull(objectType, "Object type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().equals(objectType)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static boolean isObjectTypeManagedByProvisioning(String objectType) {
        Validate.notEmpty(objectType, "Object type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getTypeQName().getLocalPart().equals(objectType)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }
}
