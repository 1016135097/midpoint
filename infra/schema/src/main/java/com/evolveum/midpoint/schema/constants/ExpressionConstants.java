/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.schema.constants;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class ExpressionConstants {

	public static final QName VAR_INPUT = new QName(SchemaConstants.NS_C, "input");
	public static final QName VAR_FOCUS = new QName(SchemaConstants.NS_C, "focus");
	public static final QName VAR_USER = new QName(SchemaConstants.NS_C, "user");
	public static final QName VAR_ACCOUNT = new QName(SchemaConstants.NS_C, "account");
	public static final QName VAR_PROJECTION = new QName(SchemaConstants.NS_C, "projection");
	public static final QName VAR_SHADOW = new QName(SchemaConstants.NS_C, "shadow");
	public static final QName VAR_SOURCE = new QName(SchemaConstants.NS_C, "source");
	public static final QName VAR_ASSIGNMENT = new QName(SchemaConstants.NS_C, "assignment");
	public static final QName VAR_IMMEDIATE_ASSIGNMENT = new QName(SchemaConstants.NS_C, "immediateAssignment");
	public static final QName VAR_THIS_ASSIGNMENT = new QName(SchemaConstants.NS_C, "thisAssignment");
	public static final QName VAR_FOCUS_ASSIGNMENT = new QName(SchemaConstants.NS_C, "focusAssignment");
	public static final QName VAR_IMMEDIATE_ROLE = new QName(SchemaConstants.NS_C, "immediateRole");
	public static final QName VAR_OPERATION = new QName(SchemaConstants.NS_C, "operation");
	public static final QName VAR_RESOURCE = new QName(SchemaConstants.NS_C, "resource");
	public static final QName VAR_MODEL_CONTEXT = new QName(SchemaConstants.NS_C, "modelContext");
	public static final QName VAR_PRISM_CONTEXT = new QName(SchemaConstants.NS_C, "prismContext");
	
	public static final QName VAR_LEGAL = new QName(SchemaConstants.NS_C, "legal");
	public static final QName VAR_FOCUS_EXISTS = new QName(SchemaConstants.NS_C, "focusExists");
	public static final QName VAR_ADMINISTRATIVE_STATUS = new QName(SchemaConstants.NS_C, "administrativeStatus");
	
	/**
	 * Numeric value describing the current iteration. It starts with 0 and increments on every iteration.
	 * Iterations are used to find unique values for an account, to resolve naming conflicts, etc.
	 */
	public static final QName VAR_ITERATION = new QName(SchemaConstants.NS_C, "iteration");
	
	/**
	 * String value describing the current iteration. It is usually suffix that is appended to the username
	 * or a similar "extension" of the value. It should have different value for every iteration. The actual
	 * value is determined by the iteration settings.
	 */
	public static final QName VAR_ITERATION_TOKEN = new QName(SchemaConstants.NS_C, "iterationToken");
	
	public static final QName OUTPUT_ELMENT_NAME = new QName(SchemaConstants.NS_C, "output");
	
}
