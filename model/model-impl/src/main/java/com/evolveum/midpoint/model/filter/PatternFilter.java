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

package com.evolveum.midpoint.model.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * Pattern-based filter. Can replace portions of input matched by patterns with
 * a static values. Works only on strings now.
 * 
 * @author Igor Farinic
 * @author Radovan Semancik
 * 
 */
public class PatternFilter extends AbstractFilter {

	public static final QName ELEMENT_REPLACE = new QName(SchemaConstants.NS_FILTER, "replace");
	public static final QName ELEMENT_PATTERN = new QName(SchemaConstants.NS_FILTER, "pattern");
	public static final QName ELEMENT_REPLACEMENT = new QName(SchemaConstants.NS_FILTER, "replacement");
	private static final Trace LOGGER = TraceManager.getTrace(PatternFilter.class);

	@Override
	public Node apply(Node node) {
		Validate.notNull(node, "Node must not be null.");
		String value = getValue(node);
		if (StringUtils.isEmpty(value)) {
			return node;
		}

		Validate.notEmpty(getParameters(), "Parameters must not be null or empty.");
		List<Replace> replaces = getReplaces();
		for (Replace replace : replaces) {
			Matcher matcher = replace.getPattern().matcher(value);
			value = matcher.replaceAll(replace.getReplacement());
		}

		return createReturnNode(node, value);
	}

	private List<Replace> getReplaces() {
		List<Replace> replaces = new ArrayList<Replace>();

		List<Object> parameters = getParameters();
		for (Object object : parameters) {
			if (!(object instanceof Element)) {
				continue;
			}

			Element element = (Element) object;
			if (!ELEMENT_REPLACE.getLocalPart().equals(element.getLocalName())) {
				LOGGER.debug("Ignoring unknown parameter {} in PatternFilter",
						new Object[] { element.getLocalName() });
				continue;
			}

			NodeList patternNodeList = element.getElementsByTagNameNS(ELEMENT_PATTERN.getNamespaceURI(),
					ELEMENT_PATTERN.getLocalPart());
			if (patternNodeList.getLength() != 1) {
				throw new IllegalArgumentException("Wrong number of " + ELEMENT_PATTERN + " elements ("
						+ patternNodeList.getLength() + ")");
			}
			String patternStr = ((Element) patternNodeList.item(0)).getTextContent();
			Pattern pattern = Pattern.compile(patternStr);

			NodeList replacementNodeList = element.getElementsByTagNameNS(
					ELEMENT_REPLACEMENT.getNamespaceURI(), ELEMENT_REPLACEMENT.getLocalPart());
			if (replacementNodeList.getLength() != 1) {
				throw new IllegalArgumentException("Wrong number of " + ELEMENT_REPLACEMENT + " elements ("
						+ replacementNodeList.getLength() + ")");
			}
			String replacement = ((Element) replacementNodeList.item(0)).getTextContent();

			replaces.add(new Replace(pattern, replacement));
		}

		return replaces;
	}

	private static class Replace {

		private String replacement;
		private Pattern pattern;

		public Replace(Pattern pattern, String replacement) {
			this.replacement = replacement;
			this.pattern = pattern;
		}

		/**
		 * Get the value of replacement
		 * 
		 * @return the value of replacement
		 */
		public String getReplacement() {
			return replacement;
		}

		/**
		 * Get the value of pattern
		 * 
		 * @return the value of pattern
		 */
		public Pattern getPattern() {
			return pattern;
		}
	}
}
