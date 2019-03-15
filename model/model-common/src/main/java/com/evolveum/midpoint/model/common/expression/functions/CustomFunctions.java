/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.model.common.expression.functions;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;


public class CustomFunctions {
	
	Trace LOGGER = TraceManager.getTrace(CustomFunctions.class);
	
	private ExpressionFactory expressionFactory;
	private FunctionLibraryType library;
	private OperationResult result;
	private Task task;
	private PrismContext prismContext;
	
	public CustomFunctions(FunctionLibraryType library, ExpressionFactory expressionFactory, OperationResult result, Task task) {
		this.library = library;
		this.expressionFactory = expressionFactory;
		this.prismContext = expressionFactory.getPrismContext();
		this.result = result;
		this.task = task;
	}
	
	/**
	 * This method is invoked by the scripts. It is supposed to be only public method exposed
	 * by this class.
	 */
	public <V extends PrismValue, D extends ItemDefinition> Object execute(String functionName, Map<String, Object> params) throws ExpressionEvaluationException {
		Validate.notNull(functionName, "Function name must be specified");
		
		List<ExpressionType> functions = library.getFunction().stream().filter(expression -> functionName.equals(expression.getName())).collect(Collectors.toList());
		
		LOGGER.trace("functions {}", functions);
		ExpressionType expressionType = functions.iterator().next();

		LOGGER.trace("function to execute {}", expressionType);
		
		try {
			ExpressionVariables variables = new ExpressionVariables();
			if (MapUtils.isNotEmpty(params)) {
				for (Map.Entry<String, Object> entry : params.entrySet()) {
					variables.put(entry.getKey(), convertInput(entry, expressionType));
				};
			}
			
			QName returnType = expressionType.getReturnType();
			if (returnType == null) {
				returnType = DOMUtil.XSD_STRING;
			}
			
			D outputDefinition = (D) prismContext.getSchemaRegistry().findItemDefinitionByType(returnType);
			if (outputDefinition == null) {
				outputDefinition = (D) prismContext.definitionFactory().createPropertyDefinition(SchemaConstantsGenerated.C_VALUE, returnType);
			}
			if (expressionType.getReturnMultiplicity() != null && expressionType.getReturnMultiplicity() == ExpressionReturnMultiplicityType.MULTI) {
				outputDefinition.toMutable().setMaxOccurs(-1);
			} else {
				outputDefinition.toMutable().setMaxOccurs(1);
			}
			String shortDesc = "custom function execute";
			Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition,
					shortDesc, task, result);

			ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task,
					result);
			PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(context);

			LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

			if (outputTriple == null) {
				return null;
			}
			
			Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
			
			if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
				return null;
			}
			
			if (outputDefinition.isMultiValue()) {
				return PrismValueCollectionsUtil.getRealValuesOfCollection(nonNegativeValues);
			}
			
			
			if (nonNegativeValues.size() > 1) {
				throw new ExpressionEvaluationException("Expression returned more than one value ("
						+ nonNegativeValues.size() + ") in " + shortDesc);
			}

			return nonNegativeValues.iterator().next().getRealValue();
			
			
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
			throw new ExpressionEvaluationException(e.getMessage(), e);
		}
		
	}

	private TypedValue convertInput(Map.Entry<String, Object> entry, ExpressionType expression) throws SchemaException {
	
		ExpressionParameterType expressionParam = expression.getParameter().stream().filter(param -> param.getName().equals(entry.getKey())).findAny().orElseThrow(SchemaException :: new);
		
		QName paramType = expressionParam.getType();
		Class<?> expressionParameterClass = prismContext.getSchemaRegistry().determineClassForType(paramType);
			
		Object value = entry.getValue();
		if (expressionParameterClass != null && !DOMUtil.XSD_ANYTYPE.equals(paramType) && XmlTypeConverter.canConvert(expressionParameterClass)) {
			value = ExpressionUtil.convertValue(expressionParameterClass, null, entry.getValue(), prismContext.getDefaultProtector(), prismContext);
		}
		
		Class<?> valueClass;
		if (value == null) {
			valueClass = expressionParameterClass;
		} else {
			valueClass = value.getClass();
		}
		
		ItemDefinition def = ExpressionUtil.determineDefinitionFromValueClass(prismContext, entry.getKey(), valueClass, paramType);
		
		return new TypedValue(value, def);
	}

}
