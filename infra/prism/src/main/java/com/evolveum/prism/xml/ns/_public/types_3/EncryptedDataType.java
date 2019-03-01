//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.02.04 at 01:34:24 PM CET
//


package com.evolveum.prism.xml.ns._public.types_3;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 *
 * 				TODO
 * 				Contains data protected by (reversible) encryption.
 *
 * 				Loosely based on XML encryption standard. But we cannot use full
 * 				standard as we are not bound to XML. We need this to work also for
 * 				JSON and YAML and other languages.
 *
 *
 * <p>Java class for EncryptedDataType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EncryptedDataType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="encryptionMethod" type="{http://prism.evolveum.com/xml/ns/public/types-3}EncryptionMethodType" minOccurs="0"/&gt;
 *         &lt;element name="keyInfo" type="{http://prism.evolveum.com/xml/ns/public/types-3}KeyInfoType" minOccurs="0"/&gt;
 *         &lt;element name="cipherData" type="{http://prism.evolveum.com/xml/ns/public/types-3}CipherDataType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EncryptedDataType", propOrder = {
    "encryptionMethod",
    "keyInfo",
    "cipherData"
})
public class EncryptedDataType implements Serializable, Cloneable {

    protected EncryptionMethodType encryptionMethod;
    protected KeyInfoType keyInfo;
    protected CipherDataType cipherData;

    /**
     * Gets the value of the encryptionMethod property.
     *
     * @return
     *     possible object is
     *     {@link EncryptionMethodType }
     *
     */
    public EncryptionMethodType getEncryptionMethod() {
        return encryptionMethod;
    }

    /**
     * Sets the value of the encryptionMethod property.
     *
     * @param value
     *     allowed object is
     *     {@link EncryptionMethodType }
     *
     */
    public void setEncryptionMethod(EncryptionMethodType value) {
        this.encryptionMethod = value;
    }

    /**
     * Gets the value of the keyInfo property.
     *
     * @return
     *     possible object is
     *     {@link KeyInfoType }
     *
     */
    public KeyInfoType getKeyInfo() {
        return keyInfo;
    }

    /**
     * Sets the value of the keyInfo property.
     *
     * @param value
     *     allowed object is
     *     {@link KeyInfoType }
     *
     */
    public void setKeyInfo(KeyInfoType value) {
        this.keyInfo = value;
    }

    /**
     * Gets the value of the cipherData property.
     *
     * @return
     *     possible object is
     *     {@link CipherDataType }
     *
     */
    public CipherDataType getCipherData() {
        return cipherData;
    }

    /**
     * Sets the value of the cipherData property.
     *
     * @param value
     *     allowed object is
     *     {@link CipherDataType }
     *
     */
    public void setCipherData(CipherDataType value) {
        this.cipherData = value;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cipherData == null) ? 0 : cipherData.hashCode());
		result = prime * result + ((encryptionMethod == null) ? 0 : encryptionMethod.hashCode());
		result = prime * result + ((keyInfo == null) ? 0 : keyInfo.hashCode());
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
		EncryptedDataType other = (EncryptedDataType) obj;
		if (cipherData == null) {
			if (other.cipherData != null)
				return false;
		} else if (!cipherData.equals(other.cipherData))
			return false;
		if (encryptionMethod == null) {
			if (other.encryptionMethod != null)
				return false;
		} else if (!encryptionMethod.equals(other.encryptionMethod))
			return false;
		if (keyInfo == null) {
			if (other.keyInfo != null)
				return false;
		} else if (!keyInfo.equals(other.keyInfo))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EncryptedDataType(encryptionMethod=" + encryptionMethod + ", keyInfo=" + keyInfo
				+ ", cipherData=" + cipherData + ")";
	}

    @Override
    public EncryptedDataType clone() {
        EncryptedDataType cloned = new EncryptedDataType();
        cloned.setCipherData(getCipherData().clone());
        cloned.setEncryptionMethod(getEncryptionMethod().clone());
        cloned.setKeyInfo(getKeyInfo().clone());
        return cloned;
    }
}