package com.evolveum.midpoint.provisioning.ucf.util;

import javax.xml.namespace.QName;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;

public class UcfUtil {
	
	public static String convertAttributeNameToIcf(QName attrQName, String resourceSchemaNamespace, OperationResult parentResult)
			throws SchemaException {
		// Attribute QNames in the resource instance namespace are converted
		// "as is"
		if (attrQName.getNamespaceURI().equals(resourceSchemaNamespace)) {
			return attrQName.getLocalPart();
		}

		// Other namespace are special cases

		if (ConnectorFactoryIcfImpl.ICFS_NAME.equals(attrQName)) {
			return Name.NAME;
		}

		if (ConnectorFactoryIcfImpl.ICFS_UID.equals(attrQName)) {
			// UID is strictly speaking not an attribute. But it acts as an
			// attribute e.g. in create operation. Therefore we need to map it.
			return Uid.NAME;
		}

		// No mapping available

		throw new SchemaException("No mapping from QName " + attrQName + " to an ICF attribute name");
	}
	
	public static Object convertValueToIcf(Object value, Protector protector, QName propName) throws SchemaException {
		if (value == null) {
			return null;
		}

		if (value instanceof PrismPropertyValue) {
			return convertValueToIcf(((PrismPropertyValue) value).getValue(), protector, propName);
		}

		if (value instanceof ProtectedStringType) {
			ProtectedStringType ps = (ProtectedStringType) value;
			return toGuardedString(ps, protector, propName.toString());
		}
		return value;
	}
	
	public static GuardedString toGuardedString(ProtectedStringType ps, Protector protector, String propertyName) {
		if (ps == null) {
			return null;
		}
		if (!protector.isEncrypted(ps)) {
			if (ps.getClearValue() == null) {
				return null;
			}
//			LOGGER.warn("Using cleartext value for {}", propertyName);
			return new GuardedString(ps.getClearValue().toCharArray());
		}
		try {
			return new GuardedString(protector.decryptString(ps).toCharArray());
		} catch (EncryptionException e) {
//			LOGGER.error("Unable to decrypt value of element {}: {}",
//					new Object[] { propertyName, e.getMessage(), e });
			throw new SystemException("Unable to dectypt value of element " + propertyName + ": "
					+ e.getMessage(), e);
		}
	}


}
