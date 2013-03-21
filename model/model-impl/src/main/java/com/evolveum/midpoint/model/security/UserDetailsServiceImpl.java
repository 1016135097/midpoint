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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.security;

import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.lens.Assignment;
import com.evolveum.midpoint.model.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.security.api.UserDetailsService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@Service(value = "userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Trace LOGGER = TraceManager.getTrace(UserDetailsServiceImpl.class);
    
    @Autowired(required = true)
    private transient RepositoryService repositoryService;
    
    @Autowired(required = true)
    private ObjectResolver objectResolver;
    
    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;
    
    @Autowired(required = true)
    private PrismContext prismContext;

    @Override
    public MidPointPrincipal getUser(String principal) {
    	MidPointPrincipal user = null;
        try {
            user = findByUsername(principal);
        } catch (Exception ex) {
            LOGGER.warn("Couldn't find user with name '{}', reason: {}.",
                    new Object[]{principal, ex.getMessage()});
        }

        return user;
    }

    @Override
    public void updateUser(MidPointPrincipal user) {
        try {
            save(user);
        } catch (RepositoryException ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.",
                    new Object[]{user.getFullName(), user.getOid(), ex.getMessage()});
        }
    }

    private MidPointPrincipal findByUsername(String username) throws SchemaException, ObjectNotFoundException {
        PolyString usernamePoly = new PolyString(username);
        usernamePoly.recompute(prismContext.getDefaultPolyStringNormalizer());

        ObjectQuery query = ObjectQuery.createObjectQuery(
                EqualsFilter.createEqual(UserType.class, prismContext, UserType.F_NAME, usernamePoly));
        LOGGER.trace("Looking for user, query:\n" + query.dump());

        List<PrismObject<UserType>> list = repositoryService.searchObjects(UserType.class, query,
                new OperationResult("Find by username"));
        if (list == null) {
            return null;
        }
        LOGGER.trace("Users found: {}.", new Object[]{list.size()});
        if (list.size() == 0 || list.size() > 1) {
            return null;
        }

        MidPointPrincipal principal = new MidPointPrincipal(list.get(0).asObjectable());
        addAuthorizations(principal);
        return principal;
    }

	private void addAuthorizations(MidPointPrincipal principal) {
		UserType userType = principal.getUser();
		if (userType.getAssignment().isEmpty()) {
			return;
		}

		Collection<Authorization> authorizations = principal.getAuthorities();
		
		
		AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
        assignmentEvaluator.setRepository(repositoryService);
        assignmentEvaluator.setUserOdo(new ObjectDeltaObject<UserType>(userType.asPrismObject(), null, userType.asPrismObject()));
        assignmentEvaluator.setChannel(null);
        assignmentEvaluator.setObjectResolver(objectResolver);
        assignmentEvaluator.setPrismContext(prismContext);
        assignmentEvaluator.setValueConstructionFactory(valueConstructionFactory);
		
        OperationResult result = new OperationResult(UserDetailsServiceImpl.class.getName() + ".addAuthorizations");
        for(AssignmentType assignmentType: userType.getAssignment()) {
        	try {
				Assignment assignment = assignmentEvaluator.evaluate(assignmentType, userType, userType.toString(), result);
				authorizations.addAll(assignment.getAuthorizations());
			} catch (SchemaException e) {
				LOGGER.error("Schema violation while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			} catch (ObjectNotFoundException e) {
				LOGGER.error("Object not found while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			} catch (ExpressionEvaluationException e) {
				LOGGER.error("Evaluation error while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			}
        }
	}

	private MidPointPrincipal save(MidPointPrincipal person) throws RepositoryException {
        try {
            UserType oldUserType = getUserByOid(person.getOid());
            PrismObject<UserType> oldUser = oldUserType.asPrismObject();

            PrismObject<UserType> newUser = person.getUser().asPrismObject();

            ObjectDelta<UserType> delta = oldUser.diff(newUser);
            repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(),
                    new OperationResult(OPERATION_UPDATE_USER));
        } catch (Exception ex) {
            throw new RepositoryException(ex.getMessage(), ex);
        }

        return person;
    }

    private UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        ObjectType object = repositoryService.getObject(UserType.class, oid,
                new OperationResult(OPERATION_GET_USER)).asObjectable();
        if (object != null && (object instanceof UserType)) {
            return (UserType) object;
        }

        return null;
    }
}
