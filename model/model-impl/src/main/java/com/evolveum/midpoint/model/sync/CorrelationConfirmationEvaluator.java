package com.evolveum.midpoint.model.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.camel.spi.Required;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationParameters;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

@Component
public class CorrelationConfirmationEvaluator {

	private static transient Trace LOGGER = TraceManager.getTrace(CorrelationConfirmationEvaluator.class);

	@Autowired(required = true)
	private RepositoryService repositoryService;

	@Autowired(required = true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

//	@Autowired
//	private ExpressionHandler expressionHandler;

	public List<PrismObject<UserType>> findUsersByCorrelationRule(ResourceObjectShadowType currentShadow,
			QueryType query, ResourceType resourceType, OperationResult result)
			throws SynchronizationException {

		if (query == null) {
			LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
					+ "returning empty list of users.", resourceType);
			return null;
		}

		Element element = query.getFilter();
		if (element == null) {
			LOGGER.warn("Correlation rule for resource '{}' doesn't contain query filter, "
					+ "returning empty list of users.", resourceType);
			return null;
		}

		ObjectQuery q = null;
		try {
			q = QueryConvertor.createObjectQuery(UserType.class, query, prismContext);
			q = updateFilterWithAccountValues(currentShadow, resourceType, q, "Correlation expression", result);
			if (q == null) {
				// Null is OK here, it means that the value in the filter
				// evaluated
				// to null and the processing should be skipped
				return null;
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
					SchemaDebugUtil.prettyPrint(query));
			throw new SynchronizationException("Couldn't convert query.", ex);
		}
		List<PrismObject<UserType>> users = null;
		try {
			// query = new QueryType();
			// query.setFilter(filter);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for results in filter\n{}",
						new Object[] { currentShadow, SchemaDebugUtil.prettyPrint(query) });
			}
			PagingType paging = new PagingType();
			// ObjectQuery q = QueryConvertor.createObjectQuery(UserType.class,
			// query, prismContext);
			users = repositoryService.searchObjects(UserType.class, q, result);

			if (users == null) {
				users = new ArrayList<PrismObject<UserType>>();
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER,
					"Couldn't search users in repository, based on filter (simplified)\n{}.", ex, q.dump());
			throw new SynchronizationException(
					"Couldn't search users in repository, based on filter (See logs).", ex);
		}

		LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} returned {} users: {}", new Object[] {
				currentShadow, users.size(), PrettyPrinter.prettyPrint(users, 3) });
		return users;
	}

	public boolean matchUserCorrelationRule(PrismObject<AccountShadowType> currentShadow, PrismObject<UserType> userType, ResourceType resourceType, OperationResult result){

		ObjectSynchronizationType synchronization = ResourceTypeUtil.determineSynchronization(resourceType, UserType.class);
		
		if (synchronization == null){
			LOGGER.warn(
					"Resource does not support synchornization. Skipping evaluation correlation/confirmation for user {} and account {}",
					userType, currentShadow);
			return false;
		}
		
		QueryType query = synchronization.getCorrelation();
		
		if (query == null) {
			LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
					+ "returning empty list of users.", resourceType);
			return false;
		}

		Element element = query.getFilter();
		if (element == null) {
			LOGGER.warn("Correlation rule for resource '{}' doesn't contain query filter, "
					+ "returning empty list of users.", resourceType);
			return false;
		}

		ObjectQuery q = null;
		try {
			q = QueryConvertor.createObjectQuery(UserType.class, query, prismContext);
			q = updateFilterWithAccountValues(currentShadow.asObjectable(), resourceType, q, "Correlation expression", result);
			if (q == null) {
				// Null is OK here, it means that the value in the filter
				// evaluated
				// to null and the processing should be skipped
				return false;
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
					SchemaDebugUtil.prettyPrint(query));
			throw new SystemException("Couldn't convert query.", ex);
		}
//		List<PrismObject<UserType>> users = null;
//		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for results in filter\n{}",
						new Object[] { currentShadow, SchemaDebugUtil.prettyPrint(query) });
			}
//			PagingType paging = new PagingType();
			boolean match = ObjectQuery.match(userType, q.getFilter());

//			if (users == null) {
//				users = new ArrayList<PrismObject<UserType>>();
//			}
//		} catch (Exception ex) {
//			LoggingUtils.logException(LOGGER,
//					"Couldn't search users in repository, based on filter (simplified)\n{}.", ex, q.dump());
//			throw new SynchronizationException(
//					"Couldn't search users in repository, based on filter (See logs).", ex);
//		}

		LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} match user: {}", new Object[] {
				currentShadow, userType });
		return match;
	}

	
	public List<PrismObject<UserType>> findUserByConfirmationRule(List<PrismObject<UserType>> users,
			ResourceObjectShadowType currentShadow, ResourceType resource, ExpressionType expression, OperationResult result)
			throws SynchronizationException {

		List<PrismObject<UserType>> list = new ArrayList<PrismObject<UserType>>();
		for (PrismObject<UserType> user : users) {
			try {
				UserType userType = user.asObjectable();
				boolean confirmedUser = evaluateConfirmationExpression(userType,
						currentShadow, resource, expression, result);
				if (user != null && confirmedUser) {
					list.add(user);
				}
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
				throw new SynchronizationException("Couldn't confirm user " + user.getName(), ex);
			}
		}

		LOGGER.debug("SYNCHRONIZATION: CONFIRMATION: expression for {} matched {} users.", new Object[] {
				currentShadow, list.size() });
		return list;
	}

	private ObjectQuery updateFilterWithAccountValues(ResourceObjectShadowType currentShadow, ResourceType resource,
			ObjectQuery query, String shortDesc, OperationResult result) throws SynchronizationException {
		LOGGER.trace("updateFilterWithAccountValues::begin");
		if (query.getFilter() == null) {
			return null;
		}

		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Transforming search filter from:\n{}", query.dump());
			}

			Element valueExpressionElement = query.getFilter().getExpression();
			if (valueExpressionElement == null
					&& (((PropertyValueFilter) query.getFilter()).getValues() == null || ((PropertyValueFilter) query
							.getFilter()).getValues().isEmpty())) {
				LOGGER.warn("No valueExpression in rule for {}", currentShadow);
				return null;
			}
			ExpressionType valueExpression = prismContext.getPrismJaxbProcessor().toJavaValue(
					valueExpressionElement, ExpressionType.class);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Filter transformed to expression\n{}", valueExpression);
			}
						
			PrismPropertyValue expressionResult = evaluateExpression(currentShadow, resource, valueExpression, query, shortDesc, result);
			
			if (expressionResult == null || expressionResult.isEmpty()) {
				LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
						valueExpression);
				return null;
			}
			// TODO: log more context
			LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.", new Object[] {
					currentShadow, expressionResult });
			if (query.getFilter() instanceof EqualsFilter) {
				((EqualsFilter) query.getFilter()).setValue(expressionResult);
				query.getFilter().setExpression(null);
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Transforming filter to:\n{}", query.getFilter().dump());
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't transform filter.", ex);
			throw new SynchronizationException("Couldn't transform filter, reason: " + ex.getMessage(), ex);
		}

		LOGGER.trace("updateFilterWithAccountValues::end");
		return query;
	}

	
	public static Map<QName, Object> getDefaultXPathVariables(UserType user,
			ResourceObjectShadowType shadow, ResourceType resource) {
		
		Map<QName, Object> variables = new HashMap<QName, Object>();
		if (user != null) {
			variables.put(SchemaConstants.I_USER, user.asPrismObject());
		}

		if (shadow != null) {
			variables.put(SchemaConstants.I_ACCOUNT, shadow.asPrismObject());
		}

		if (resource != null) {
			variables.put(SchemaConstants.I_RESOURCE, resource.asPrismObject());
		}

		return variables;
	}
	
	private PrismPropertyValue evaluateExpression(ResourceObjectShadowType currentShadow,
			ResourceType resource, ExpressionType valueExpression, ObjectQuery query, String shortDesc,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
	Map<QName, Object> variables = getDefaultXPathVariables(null, currentShadow, resource);
		
		//TODO rafactor after new query engine is implemented
		ItemDefinition outputDefinition = null;
		if (query.getFilter() instanceof ValueFilter){
			outputDefinition = ((ValueFilter)query.getFilter()).getDefinition();
		}
		
		if (outputDefinition == null){
			outputDefinition =  new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
					DOMUtil.XSD_STRING, prismContext);
		}
		
		Expression<PrismPropertyValue> expression = expressionFactory.makeExpression(valueExpression,
				outputDefinition, shortDesc, parentResult);

		ExpressionEvaluationParameters params = new ExpressionEvaluationParameters(null, variables, shortDesc, parentResult);
		PrismValueDeltaSetTriple<PrismPropertyValue> outputTriple = expression.evaluate(params);
		if (outputTriple == null) {
			return null;
		}
		Collection<PrismPropertyValue> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			return null;
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }

        return nonNegativeValues.iterator().next();
//		String expressionResult = expressionHandler.evaluateExpression(currentShadow, valueExpression,
//				shortDesc, result);
   	}
	
	public boolean evaluateConfirmationExpression(UserType user, ResourceObjectShadowType shadow, ResourceType resource,
			ExpressionType expressionType, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(shadow, "Resource object shadow must not be null.");
		Validate.notNull(expressionType, "Expression must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		Map<QName, Object> variables = getDefaultXPathVariables(user, shadow, resource);
		String shortDesc = "confirmation expression for "+resource.asPrismObject();
		
		PrismPropertyDefinition outputDefinition = new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, 
				outputDefinition, shortDesc, result);

		ExpressionEvaluationParameters params = new ExpressionEvaluationParameters(null, variables, shortDesc, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(params);
		Collection<PrismPropertyValue<Boolean>> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        PrismPropertyValue<Boolean> resultpval = nonNegativeValues.iterator().next();
        if (resultpval == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        Boolean resultVal = resultpval.getValue();
        if (resultVal == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
		return resultVal;
	}

}
