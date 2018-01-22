/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ObjectDeltaUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdater.class);

    @Autowired
    private EntityModificationRegistry entityModificationRegistry;

    /**
     * modify
     */
    public <T extends ObjectType> RObject<T> update(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
                                                    RObject<T> objectToMerge, Session session, OperationResult result) {

        if (1 == 1) {
            return tryHibernateMerge(objectToMerge, session);
        }

        // todo handle nameCopy/name correctly

        RObject object = session.byId(objectToMerge.getClass()).getReference(oid);
        object.setVersion(objectToMerge.getVersion());
        object.setFullObject(objectToMerge.getFullObject());

        ManagedType mainEntityType = entityModificationRegistry.getJaxbMapping(type);

        for (ItemDelta delta : modifications) {
            ManagedType managedType = mainEntityType;
            Object bean = object;

            ItemPath path = delta.getPath();
            Iterator<ItemPathSegment> segments = path.getSegments().iterator();
            while (segments.hasNext()) {
                ItemPathSegment segment = segments.next();
                if (!(segment instanceof NameItemPathSegment)) {
                    throw new SystemException("Segment '" + segment + "' in '" + path + "' is not a name item");
                }

                NameItemPathSegment nameSegment = (NameItemPathSegment) segment;
                String nameLocalPart = nameSegment.getName().getLocalPart();

                Attribute attribute = entityModificationRegistry.findAttribute(managedType, nameLocalPart);
                if (attribute == null) {
                    attribute = entityModificationRegistry.findAttributeOverride(managedType, nameLocalPart);
                }

                if (attribute == null) {
                    // there's no table/column that needs update
                    break;
                }

                if (segments.hasNext()) {
                    switch (attribute.getPersistentAttributeType()) {
                        case EMBEDDED:
                            managedType = entityModificationRegistry.getMapping(attribute.getJavaType());
                            try {
                                bean = ((Method) attribute.getJavaMember()).invoke(bean);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex); //todo error handling
                            }
                            break;
                        case ONE_TO_MANY:
                            try {
                                Method method = ((Method) attribute.getJavaMember());
                                ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
                                Class clazz = (Class) parameterizedType.getActualTypeArguments()[0];

                                IdItemPathSegment id = (IdItemPathSegment) segments.next();
                                // todo handle types correctly
                                Collection c = (Collection) ((Method) attribute.getJavaMember()).invoke(bean);
                                if (Container.class.isAssignableFrom(clazz)) {
                                    boolean found = false;
                                    for (Container o : (Collection<Container>) c) {
                                        long l = o.getId().longValue();
                                        if (l == id.getId()) {
                                            managedType = entityModificationRegistry.getMapping(clazz);
                                            bean = o;

                                            found = true;
                                            break;
                                        }
                                    }

                                    if (!found) {
                                        throw new RuntimeException("Couldn't find container"); // todo error handling
                                    }
                                } else {
                                    throw new RuntimeException("Can't go over collection"); // todo error handling
                                }
                                System.out.println(c);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex); // todo error handling
                            }
                            break;
                        default:
//                            throw new RuntimeException("Don't know what to do"); // todo error handling
                    }

                    continue;
                }

                switch (attribute.getPersistentAttributeType()) {
                    case BASIC:
                    case EMBEDDED:
                        // todo qnames
                        // todo how to handle add/delete/replace
                        try {
                            Object realValue = delta.getAnyValue().getRealValue();
                            if (realValue instanceof Enum) {
                                String className = realValue.getClass().getSimpleName();
                                className = StringUtils.left(className, className.length() - 4);
                                String repoEnumClass = "com.evolveum.midpoint.repo.sql.data.common.enums.R" + className;
                                Class clazz = Class.forName(repoEnumClass);
                                if (SchemaEnum.class.isAssignableFrom(clazz)) {
                                    realValue = RUtil.getRepoEnumValue(realValue, clazz);
                                } else {
                                    throw new SystemException("Can't translate enum value " + realValue);
                                }
                            } else if (realValue instanceof PolyString) {
                                PolyString p = (PolyString) realValue;
                                realValue = new RPolyString(p.getOrig(), p.getNorm());
                            } else if (realValue instanceof ActivationType) {
                                RActivation ractivation = new RActivation();
                                RActivation.copyFromJAXB((ActivationType) realValue, ractivation, null);
                                realValue = ractivation;
                            }
                            PropertyUtils.setSimpleProperty(bean, attribute.getName(), realValue);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex); //todo error handling
                        }
                        break;
                    case MANY_TO_MANY:
                        // not used in our mappings
                        // todo throw exception
                        break;
                    case ONE_TO_ONE:

                        break;
                    case MANY_TO_ONE:
                        // can't be in delta (probably)
                        // todo throw exception
                        break;
                    case ONE_TO_MANY:
                        try {
                            // todo handle types correctly
                            Collection c = (Collection) ((Method) attribute.getJavaMember()).invoke(bean);


                            System.out.println(c);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex); // todo error handling
                        }
                        break;
                    case ELEMENT_COLLECTION:
                        try {
                            // todo handle types correctly
                            Collection c = (Collection) ((Method) attribute.getJavaMember()).invoke(bean);
                            c.addAll((List) delta.getValuesToAdd().stream().map(i -> ((PrismPropertyValue) i).getRealValue()).collect(Collectors.toList()));
                        } catch (Exception ex) {
                            throw new RuntimeException(ex); // todo error handling
                        }
                        break;
                }
            }
        }

        session.save(object);

        return objectToMerge;
    }

    /**
     * add with overwrite
     */
    public <T extends ObjectType> RObject<T> update(PrismObject<T> object, RObject<T> objectToMerge, Session session,
                                                    OperationResult result) {

        return tryHibernateMerge(objectToMerge, session);
        // todo implement
    }

    private <T extends ObjectType> RObject<T> tryHibernateMerge(RObject<T> object, Session session) {
        return (RObject) session.merge(object);
    }
}
