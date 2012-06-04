/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.XmlSchemaType;

/**
 * @author Radovan Semancik
 *
 */
public class ConnectorTypeUtil {
	
	public static String getConnectorHostTypeOid(ConnectorType connectorType) {
		if (connectorType.getConnectorHostRef() != null) {
			return connectorType.getConnectorHostRef().getOid();
		} else if (connectorType.getConnectorHost() != null) {
			return connectorType.getConnectorHost().getOid();
		} else {
			return null;
		}
	}
	
	public static Element getConnectorXsdSchema(ConnectorType connector) {
		XmlSchemaType xmlSchemaType = connector.getSchema();
		if (xmlSchemaType == null) {
			return null;
		}
		return ObjectTypeUtil.findXsdElement(xmlSchemaType);
	}
	
	public static Element getConnectorXsdSchema(PrismObject<ConnectorType> connector) {
		PrismContainer<XmlSchemaType> xmlSchema = connector.findContainer(ConnectorType.F_SCHEMA);
		if (xmlSchema == null) {
			return null;
		}
		return ObjectTypeUtil.findXsdElement(xmlSchema);
	}
	
	public static void setConnectorXsdSchema(ConnectorType connectorType, Element xsdElement) {
		PrismObject<ConnectorType> connector = connectorType.asPrismObject();
		setConnectorXsdSchema(connector, xsdElement);
	}
	
	public static void setConnectorXsdSchema(PrismObject<ConnectorType> connector, Element xsdElement) {
		PrismContainer<XmlSchemaType> schemaContainer;
		try {
			schemaContainer = connector.findOrCreateContainer(ConnectorType.F_SCHEMA);
			PrismProperty<Element> definitionProperty = schemaContainer.findOrCreateProperty(XmlSchemaType.F_DEFINITION);
			ObjectTypeUtil.setXsdSchemaDefinition(definitionProperty, xsdElement);
		} catch (SchemaException e) {
			// Should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		
	}

	/**
	 * Returns parsed connector schema
	 */
	public static PrismSchema getConnectorSchema(ConnectorType connectorType, PrismContext prismContext) throws SchemaException {
		Element connectorSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(connectorType);
		return PrismSchema.parse(connectorSchemaElement, "schema for " + connectorType, prismContext);
	}
	
	public static PrismContainerDefinition<?> findConfigurationContainerDefintion(ConnectorType connectorType, PrismSchema connectorSchema) {
		QName configContainerQName = new QName(connectorType.getNamespace(), ResourceType.F_CONFIGURATION.getLocalPart());
		return connectorSchema.findContainerDefinitionByElementName(configContainerQName);
	}
	
	public static PrismContainerDefinition<?> findConfigurationContainerDefintion(ConnectorType connectorType, PrismContext prismContext) throws SchemaException {
		PrismSchema connectorSchema = getConnectorSchema(connectorType, prismContext);
		return findConfigurationContainerDefintion(connectorType, connectorSchema);
	}
	
}
