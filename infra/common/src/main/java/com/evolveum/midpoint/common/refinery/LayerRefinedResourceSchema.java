/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common.refinery;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;

/**
 * @author semancik
 *
 */
public class LayerRefinedResourceSchema extends RefinedResourceSchema {
	
	private RefinedResourceSchema refinedResourceSchema;
	private LayerType layer;
	
	private LayerRefinedResourceSchema(RefinedResourceSchema refinedResourceSchema, LayerType layer) {
		super(refinedResourceSchema.getPrismContext());
		this.refinedResourceSchema = refinedResourceSchema;
		this.layer = layer;
	}
	
	static LayerRefinedResourceSchema wrap(RefinedResourceSchema rSchema, LayerType layer) {
		return new LayerRefinedResourceSchema(rSchema, layer);
	}

	public String getNamespace() {
		return refinedResourceSchema.getNamespace();
	}

	public Collection<? extends RefinedAccountDefinition> getAccountDefinitions() {
		return LayerRefinedAccountDefinition.wrapCollection(refinedResourceSchema.getAccountDefinitions(), layer);
	}

	public Collection<Definition> getDefinitions() {
		return refinedResourceSchema.getDefinitions();
	}

	public ResourceSchema getOriginalResourceSchema() {
		return refinedResourceSchema.getOriginalResourceSchema();
	}

	public <T extends Definition> Collection<T> getDefinitions(Class<T> type) {
		return refinedResourceSchema.getDefinitions(type);
	}

	public LayerRefinedAccountDefinition getAccountDefinition(AccountShadowType shadow) {
		return LayerRefinedAccountDefinition.wrap(refinedResourceSchema.getAccountDefinition(shadow),layer);
	}

	public LayerRefinedAccountDefinition getAccountDefinition(String intent) {
		return LayerRefinedAccountDefinition.wrap(refinedResourceSchema.getAccountDefinition(intent),layer);
	}

	public void add(Definition def) {
		refinedResourceSchema.add(def);
	}

	public PrismContext getPrismContext() {
		return refinedResourceSchema.getPrismContext();
	}

	public LayerRefinedAccountDefinition getDefaultAccountDefinition() {
		return LayerRefinedAccountDefinition.wrap(refinedResourceSchema.getDefaultAccountDefinition(),layer);
	}

	public PrismObjectDefinition<AccountShadowType> getObjectDefinition(String accountType) {
		return refinedResourceSchema.getObjectDefinition(accountType);
	}

	public PrismObjectDefinition<AccountShadowType> getObjectDefinition(AccountShadowType shadow) {
		return refinedResourceSchema.getObjectDefinition(shadow);
	}

	public PrismContainerDefinition findContainerDefinitionByType(QName typeName) {
		return refinedResourceSchema.findContainerDefinitionByType(typeName);
	}

	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByType(QName typeName) {
		return refinedResourceSchema.findObjectDefinitionByType(typeName);
	}

	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByElementName(QName elementName) {
		return refinedResourceSchema.findObjectDefinitionByElementName(elementName);
	}

	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByType(QName typeName, Class<T> type) {
		return refinedResourceSchema.findObjectDefinitionByType(typeName, type);
	}

	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByCompileTimeClass(Class<T> type) {
		return refinedResourceSchema.findObjectDefinitionByCompileTimeClass(type);
	}

	public PrismPropertyDefinition findPropertyDefinitionByElementName(QName elementName) {
		return refinedResourceSchema.findPropertyDefinitionByElementName(elementName);
	}

	public <T extends ItemDefinition> T findItemDefinition(QName definitionName, Class<T> definitionType) {
		return refinedResourceSchema.findItemDefinition(definitionName, definitionType);
	}

	public <T extends ItemDefinition> T findItemDefinition(String localName, Class<T> definitionType) {
		return refinedResourceSchema.findItemDefinition(localName, definitionType);
	}

	public <T extends ItemDefinition> T findItemDefinitionByType(QName typeName, Class<T> definitionType) {
		return refinedResourceSchema.findItemDefinitionByType(typeName, definitionType);
	}

	public PrismContainerDefinition createPropertyContainerDefinition(String localTypeName) {
		return refinedResourceSchema.createPropertyContainerDefinition(localTypeName);
	}

	public PrismContainerDefinition createPropertyContainerDefinition(String localElementName, String localTypeName) {
		return refinedResourceSchema.createPropertyContainerDefinition(localElementName, localTypeName);
	}

	public ComplexTypeDefinition createComplexTypeDefinition(QName typeName) {
		return refinedResourceSchema.createComplexTypeDefinition(typeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		return refinedResourceSchema.createPropertyDefinition(localName, typeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		return refinedResourceSchema.createPropertyDefinition(localName, localTypeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		return refinedResourceSchema.createPropertyDefinition(name, typeName);
	}

	public LayerRefinedAccountDefinition findAccountDefinitionByObjectClass(QName objectClass) {
		return LayerRefinedAccountDefinition.wrap(refinedResourceSchema.findAccountDefinitionByObjectClass(objectClass),layer);
	}

	public PrismContainerDefinition findContainerDefinitionByElementName(QName elementName) {
		return refinedResourceSchema.findContainerDefinitionByElementName(elementName);
	}

	public ComplexTypeDefinition findComplexTypeDefinition(QName typeName) {
		return refinedResourceSchema.findComplexTypeDefinition(typeName);
	}

	public void setNamespace(String namespace) {
		refinedResourceSchema.setNamespace(namespace);
	}

	public Document serializeToXsd() throws SchemaException {
		return refinedResourceSchema.serializeToXsd();
	}

	public boolean isEmpty() {
		return refinedResourceSchema.isEmpty();
	}

	public <T extends ResourceObjectShadowType> PrismObject<T> refine(PrismObject<T> shadow) throws SchemaException {
		return refinedResourceSchema.refine(shadow);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((layer == null) ? 0 : layer.hashCode());
		result = prime * result + ((refinedResourceSchema == null) ? 0 : refinedResourceSchema.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerRefinedResourceSchema other = (LayerRefinedResourceSchema) obj;
		if (layer != other.layer)
			return false;
		if (refinedResourceSchema == null) {
			if (other.refinedResourceSchema != null)
				return false;
		} else if (!refinedResourceSchema.equals(other.refinedResourceSchema))
			return false;
		return true;
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("LRSchema(layer=").append(layer).append(",\n");
		sb.append(refinedResourceSchema.debugDump(indent+1));
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}
	
}
