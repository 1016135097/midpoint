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
package com.evolveum.midpoint.common.refinery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaProcessorUtil;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.DepreactedAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PropertyLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAttributeDefinitionType;

/**
 * @author semancik
 */
public class RefinedAttributeDefinition extends ResourceAttributeDefinition implements Dumpable, DebugDumpable {

	private static LayerType DEFAULT_LAYER = LayerType.MODEL;
	
    private String displayName;
    private String description;
    private boolean tolerant = true;
    private List<String> intolerantValuePattern;
    private List<String> tolerantValuePattern;
    private ResourceAttributeDefinition attributeDefinition;
    private AttributeFetchStrategyType fetchStrategy;
    private MappingType outboundMappingType;
    private List<MappingType> inboundMappingTypes;
    private Map<LayerType,PropertyLimitations> limitationsMap = new HashMap<LayerType, PropertyLimitations>();
    private QName matchingRuleQName = null;

    protected RefinedAttributeDefinition(ResourceAttributeDefinition attrDef, PrismContext prismContext) {
        super(attrDef.getName(), attrDef.getTypeName(), prismContext);
        this.attributeDefinition = attrDef;
    }

    @Override
    public void setNativeAttributeName(String nativeAttributeName) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    public boolean isTolerant() {
		return tolerant;
	}

	public void setTolerant(boolean tolerant) {
		this.tolerant = tolerant;
	}
	
	@Override
    public boolean canCreate() {
		return canCreate(DEFAULT_LAYER);
    }
	
	public boolean canCreate(LayerType layer) {
        return limitationsMap.get(layer).getAccess().isCreate();
    }

	@Override
    public boolean canRead() {
		return canRead(DEFAULT_LAYER);
    }

    public boolean canRead(LayerType layer) {
        return limitationsMap.get(layer).getAccess().isRead();
    }

    @Override
    public boolean canUpdate() {
    	return canUpdate(DEFAULT_LAYER);
    }
    
    public boolean canUpdate(LayerType layer) {
        return limitationsMap.get(layer).getAccess().isUpdate();
    }

    @Override
    public void setReadOnly() {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public QName getValueType() {
        return attributeDefinition.getValueType();
    }

    @Override
    public void setMinOccurs(int minOccurs) {
    	throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setMaxOccurs(int maxOccurs) {
    	throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setRead(boolean read) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setUpdate(boolean update) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setCreate(boolean create) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public boolean isIgnored() {
        return isIgnored(DEFAULT_LAYER);
    }
    
    public boolean isIgnored(LayerType layer) {
        return limitationsMap.get(layer).isIgnore();
    }

    @Override
    public void setIgnored(boolean ignored) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setHelp(String help) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceAttributeDefinition getAttributeDefinition() {
        return attributeDefinition;
    }

    public void setAttributeDefinition(ResourceAttributeDefinition attributeDefinition) {
        this.attributeDefinition = attributeDefinition;
    }

    public MappingType getOutboundMappingType() {
        return outboundMappingType;
    }

    public void setOutboundMappingType(MappingType outboundMappingType) {
        this.outboundMappingType = outboundMappingType;
    }
    
    public boolean hasOutboundMapping() {
    	return outboundMappingType != null;
    }

    public List<MappingType> getInboundMappingTypes() {
        return inboundMappingTypes;
    }

    public void setInboundMappingTypes(List<MappingType> inboundAssignmentTypes) {
        this.inboundMappingTypes = inboundAssignmentTypes;
    }

    public QName getName() {
        return attributeDefinition.getName();
    }

    public QName getTypeName() {
        return attributeDefinition.getTypeName();
    }

    public String getNativeAttributeName() {
        return attributeDefinition.getNativeAttributeName();
    }

    public Object[] getAllowedValues() {
        return attributeDefinition.getAllowedValues();
    }
    
    public boolean isReturnedByDefault() {
		return attributeDefinition.isReturnedByDefault();
	}

	public void setReturnedByDefault(Boolean returnedByDefault) {
		throw new UnsupportedOperationException("Cannot change returnedByDefault");
	}

	@Override
    public int getMaxOccurs() {
    	return getMaxOccurs(DEFAULT_LAYER);
    }
    
    public int getMaxOccurs(LayerType layer) {
    	return limitationsMap.get(layer).getMaxOccurs();
    }

    @Override
    public int getMinOccurs() {
    	return getMinOccurs(DEFAULT_LAYER);
    }

    public int getMinOccurs(LayerType layer) {
    	return limitationsMap.get(layer).getMinOccurs();
    }
    
    public boolean isOptional(LayerType layer) {
    	return limitationsMap.get(layer).getMinOccurs() == 0;
    }

    public boolean isMandatory(LayerType layer) {
    	return limitationsMap.get(layer).getMinOccurs() > 0;
    }
    
    public boolean isMultiValue(LayerType layer) {
    	int maxOccurs = limitationsMap.get(layer).getMaxOccurs();
    	return maxOccurs < 0 || maxOccurs > 1;
    }
    
    public boolean isSingleValue(LayerType layer) {
    	return limitationsMap.get(layer).getMaxOccurs() == 1;
    }

    public PropertyLimitations getLimitations(LayerType layer) {
    	return limitationsMap.get(layer);
    }

    public String getHelp() {
        return attributeDefinition.getHelp();
    }

    public AttributeFetchStrategyType getFetchStrategy() {
		return fetchStrategy;
	}

	public void setFetchStrategy(AttributeFetchStrategyType fetchStrategy) {
		this.fetchStrategy = fetchStrategy;
	}

	public QName getMatchingRuleQName() {
		return matchingRuleQName;
	}

	public void setMatchingRuleQName(QName matchingRuleQName) {
		this.matchingRuleQName = matchingRuleQName;
	}
	
	
	public List<String> getTolerantValuePattern(){
		return tolerantValuePattern;
	}
	
	public List<String> getIntolerantValuePattern(){
		return intolerantValuePattern;
		
	}

	static RefinedAttributeDefinition parse(ResourceAttributeDefinition schemaAttrDef, ResourceAttributeDefinitionType schemaHandlingAttrDefType,
    		ObjectClassComplexTypeDefinition objectClassDef, PrismContext prismContext, 
                                            String contextDescription) throws SchemaException {

        RefinedAttributeDefinition rAttrDef = new RefinedAttributeDefinition(schemaAttrDef, prismContext);

        if (schemaHandlingAttrDefType != null && schemaHandlingAttrDefType.getDisplayName() != null) {
	            rAttrDef.setDisplayName(schemaHandlingAttrDefType.getDisplayName());
        } else {
            if (schemaAttrDef.getDisplayName() != null) {
                rAttrDef.setDisplayName(schemaAttrDef.getDisplayName());
            }
        }

        if (schemaHandlingAttrDefType != null) {
        	rAttrDef.fetchStrategy = schemaHandlingAttrDefType.getFetchStrategy();
        	rAttrDef.matchingRuleQName = schemaHandlingAttrDefType.getMatchingRule();
        }
        
        PropertyLimitations schemaLimitations = getOrCreateLimitations(rAttrDef.limitationsMap, LayerType.SCHEMA);
        schemaLimitations.setMinOccurs(schemaAttrDef.getMinOccurs());
        schemaLimitations.setMaxOccurs(schemaAttrDef.getMaxOccurs());
        schemaLimitations.setIgnore(schemaAttrDef.isIgnored());
        schemaLimitations.getAccess().setCreate(schemaAttrDef.canCreate());
        schemaLimitations.getAccess().setUpdate(schemaAttrDef.canUpdate());
        schemaLimitations.getAccess().setRead(schemaAttrDef.canRead());
        
        Integer deprecatedMinOccurs = null;
        Integer deprecatedMaxOccurs = null;
        Boolean deprecatedIgnored = null;
        Boolean deprecatedCreate = null;
        Boolean deprecatedRead = null;
        Boolean deprecatedUpdate = null;

        if (schemaHandlingAttrDefType != null) {
        	
        	if (schemaHandlingAttrDefType.getDescription() != null) {
            	rAttrDef.setDescription(schemaHandlingAttrDefType.getDescription());
        	}

        	if (schemaHandlingAttrDefType.isTolerant() == null) {
        		rAttrDef.tolerant = true;
        	} else {
        		rAttrDef.tolerant = schemaHandlingAttrDefType.isTolerant();
        	}
        	
        	rAttrDef.tolerantValuePattern = schemaHandlingAttrDefType.getTolerantValuePattern();
        	rAttrDef.intolerantValuePattern = schemaHandlingAttrDefType.getIntolerantValuePattern();
        	
            if (schemaHandlingAttrDefType.getOutbound() != null) {
                rAttrDef.setOutboundMappingType(schemaHandlingAttrDefType.getOutbound());
            }

            if (schemaHandlingAttrDefType.getInbound() != null) {
                rAttrDef.setInboundMappingTypes(schemaHandlingAttrDefType.getInbound());
            }
        
            deprecatedMinOccurs = SchemaProcessorUtil.parseMultiplicity(schemaHandlingAttrDefType.getMinOccurs());
            deprecatedMaxOccurs = SchemaProcessorUtil.parseMultiplicity(schemaHandlingAttrDefType.getMaxOccurs());
            deprecatedIgnored = schemaHandlingAttrDefType.isIgnore();
            
            deprecatedCreate = parseDeprecatedAccess(schemaHandlingAttrDefType, DepreactedAccessType.CREATE);
            deprecatedRead = parseDeprecatedAccess(schemaHandlingAttrDefType, DepreactedAccessType.READ);
            deprecatedUpdate = parseDeprecatedAccess(schemaHandlingAttrDefType, DepreactedAccessType.UPDATE);
            
        }

        PropertyLimitations previousLimitations = null;
        for (LayerType layer: LayerType.values()) {
        	PropertyLimitations limitations = getOrCreateLimitations(rAttrDef.limitationsMap, layer);
        	if (previousLimitations != null) {
        		limitations.setMinOccurs(previousLimitations.getMinOccurs());
        		limitations.setMaxOccurs(previousLimitations.getMaxOccurs());
        		limitations.setIgnore(previousLimitations.isIgnore());
        		limitations.getAccess().setCreate(previousLimitations.getAccess().isCreate());
        		limitations.getAccess().setRead(previousLimitations.getAccess().isRead());
        		limitations.getAccess().setUpdate(previousLimitations.getAccess().isUpdate());
        	}
        	previousLimitations = limitations;
        	if (layer == DEFAULT_LAYER) {
        		if (deprecatedMinOccurs != null) {
        			limitations.setMinOccurs(deprecatedMinOccurs);
        		}
        		if (deprecatedMaxOccurs != null) {
        			limitations.setMaxOccurs(deprecatedMaxOccurs);
        		}
        		if (deprecatedIgnored != null) {
        			limitations.setIgnore(deprecatedIgnored);
        		}
        		if (deprecatedCreate != null) {
        			limitations.getAccess().setCreate(deprecatedCreate);
        		}
        		if (deprecatedRead != null) {
        			limitations.getAccess().setRead(deprecatedRead);
        		}
        		if (deprecatedUpdate != null) {
        			limitations.getAccess().setUpdate(deprecatedUpdate);
        		}
        	}
        	if (schemaHandlingAttrDefType != null) {
        		if (layer != LayerType.SCHEMA) {
        			// SCHEMA is a pseudo-layer. It cannot be overriden ... unless specified explicitly
	        		PropertyLimitationsType genericLimitationsType = getLimitationsType(schemaHandlingAttrDefType.getLimitations(), null);
	        		if (genericLimitationsType != null) {
	        			applyLimitationsType(limitations, genericLimitationsType);
	        		}
        		}
        		PropertyLimitationsType layerLimitationsType = getLimitationsType(schemaHandlingAttrDefType.getLimitations(), layer);
        		if (layerLimitationsType != null) {
        			applyLimitationsType(limitations, layerLimitationsType);
        		}
        	}
        }

        return rAttrDef;

    }

	private static void applyLimitationsType(PropertyLimitations limitations, PropertyLimitationsType layerLimitationsType) {
		if (layerLimitationsType.getMinOccurs() != null) {
			limitations.setMinOccurs(SchemaProcessorUtil.parseMultiplicity(layerLimitationsType.getMinOccurs()));
		}
		if (layerLimitationsType.getMaxOccurs() != null) {
			limitations.setMaxOccurs(SchemaProcessorUtil.parseMultiplicity(layerLimitationsType.getMaxOccurs()));
		}
		if (layerLimitationsType.isIgnore() != null) {
			limitations.setIgnore(layerLimitationsType.isIgnore());
		}
		if (layerLimitationsType.getAccess() != null) {
			PropertyAccessType accessType = layerLimitationsType.getAccess();
			if (accessType.isCreate() != null) {
				limitations.getAccess().setCreate(accessType.isCreate());
			}
			if (accessType.isRead() != null) {
				limitations.getAccess().setRead(accessType.isRead());
			}
			if (accessType.isUpdate() != null) {
				limitations.getAccess().setUpdate(accessType.isUpdate());
			}
		}
	}

	private static PropertyLimitationsType getLimitationsType(List<PropertyLimitationsType> limitationsTypes, LayerType layer) throws SchemaException {
		PropertyLimitationsType found = null;
		for (PropertyLimitationsType limitType: limitationsTypes) {
			if (contains(limitType.getLayer(),layer)) {
				if (found == null) {
					found = limitType;
				} else {
					throw new SchemaException("Duplicate definition of limitations for layer '"+layer+"'");
				}
			}
		}
		return found;
	}

	private static boolean contains(List<LayerType> layers, LayerType layer) {
		if (layers == null || layers.isEmpty()) {
			if (layer == null) {
				return true;
			} else {
				return false;
			}
		}
		return layers.contains(layer);
	}

	private static PropertyLimitations getOrCreateLimitations(Map<LayerType, PropertyLimitations> limitationsMap,
			LayerType layer) {
		PropertyLimitations limitations = limitationsMap.get(layer);
		if (limitations == null) {
			limitations = new PropertyLimitations();
			limitationsMap.put(layer, limitations);
		}
		return limitations;
	}

	private static Boolean parseDeprecatedAccess(ResourceAttributeDefinitionType attrDefType, DepreactedAccessType access) {
		if (attrDefType == null) {
			return null;
		}
		List<DepreactedAccessType> accessList = attrDefType.getAccess();
		if (accessList == null || accessList.isEmpty()) {
			return null;
		}
		for (DepreactedAccessType acccessEntry: accessList) {
			if (acccessEntry == access) {
				return true;
			}
		}
		return false;
	}

	public static boolean isIgnored(ResourceAttributeDefinitionType attrDefType) {
        if (attrDefType.isIgnore() == null) {
            return false;
        }
        return attrDefType.isIgnore();
    }
    
    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		if (getDisplayName() != null) {
            sb.append(",Disp");
        }
        if (getDescription() != null) {
            sb.append(",Desc");
        }
        if (getOutboundMappingType() != null) {
            sb.append(",OUT");
        }
        if (getInboundMappingTypes() != null) {
            sb.append(",IN");
        }
		return sb.toString();
	}
    
    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "rRAD";
    }

    @Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(super.debugDump(indent));
		sb.append("\n");
		DebugUtil.debugDumpMapSingleLine(sb, limitationsMap, indent + 1);
		return sb.toString();
	}

}
