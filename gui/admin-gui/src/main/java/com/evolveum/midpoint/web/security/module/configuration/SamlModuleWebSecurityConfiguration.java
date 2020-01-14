/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.configuration;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.saml.key.KeyType;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.config.NetworkConfiguration;
import org.springframework.security.saml.provider.config.RotatingKeys;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.saml2.signature.AlgorithmMethod;
import org.springframework.security.saml.saml2.signature.DigestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfiguration extends ModuleWebSecurityConfigurationImpl {

    private static final transient Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfiguration.class);
    private static Protector protector;

    private SamlServerConfiguration samlConfiguration;
    private Map<String, String> namesOfUsernameAttributes = new HashMap<String, String>();

    private SamlModuleWebSecurityConfiguration() {
    }

    public static void setProtector(Protector protector) {
        SamlModuleWebSecurityConfiguration.protector = protector;
    }

    public static SamlModuleWebSecurityConfiguration build(AuthenticationModuleSaml2Type modelType, String prefixOfSequence, ServletRequest request){
        Validate.notNull(request);
        SamlModuleWebSecurityConfiguration configuration = buildInternal((AuthenticationModuleSaml2Type)modelType, prefixOfSequence, request);
        configuration.validate();
        return configuration;
    }

    private static SamlModuleWebSecurityConfiguration buildInternal(AuthenticationModuleSaml2Type modelType, String prefixOfSequence, ServletRequest request){
        SamlModuleWebSecurityConfiguration configuration = new SamlModuleWebSecurityConfiguration();
        build(configuration, modelType, prefixOfSequence);
        SamlServerConfiguration samlConfiguration = new SamlServerConfiguration();
        AuthenticationModuleSaml2NetworkType networkType = modelType.getNetwork();
        if (networkType != null) {
            NetworkConfiguration network = new NetworkConfiguration();
            if (networkType.getConnectTimeout() != 0) {
                network.setConnectTimeout(networkType.getConnectTimeout());
            }
            if (networkType.getReadTimeout() != 0) {
                network.setReadTimeout(networkType.getReadTimeout());
            }
            samlConfiguration.setNetwork(network);
        }
        AuthenticationModuleSaml2ServiceProviderType serviceProviderType = modelType.getServiceProvider();
        LocalServiceProviderConfiguration serviceProvider = new LocalServiceProviderConfiguration();
        serviceProvider.setEntityId(serviceProviderType.getEntityId())
                .setSignMetadata(Boolean.TRUE.equals(serviceProviderType.isSignRequests()))
                .setSignRequests(Boolean.TRUE.equals(serviceProviderType.isSignRequests()))
                .setWantAssertionsSigned(Boolean.TRUE.equals(serviceProviderType.isWantAssertionsSigned()))
                .setSingleLogoutEnabled(Boolean.TRUE.equals(serviceProviderType.isSingleLogoutEnabled()))
                .setBasePath(getBasePath(((HttpServletRequest) request)));
        List<Object> objectList = new ArrayList<Object>();
        for (AuthenticationModuleSaml2NameIdType nameIdType : serviceProviderType.getNameId()) {
            objectList.add(nameIdType.value());
        }
        serviceProvider.setNameIds(objectList);
        if (serviceProviderType.getDefaultDigest() != null) {
            serviceProvider.setDefaultDigest(DigestMethod.fromUrn(serviceProviderType.getDefaultDigest().value()));
        }
        if (serviceProviderType.getDefaultSigningAlgorithm() != null) {
            serviceProvider.setDefaultSigningAlgorithm(AlgorithmMethod.fromUrn(serviceProviderType.getDefaultSigningAlgorithm().value()));
        }
        AuthenticationModuleSaml2KeyType keysType = serviceProviderType.getKeys();
//        if (keysType != null) {
            RotatingKeys key = new RotatingKeys();
            AuthenticationModuleSaml2SimpleKeyType activeKeyType = keysType.getActive();
//            if (activeKeyType != null) {
                try {
                    key.setActive(createSimpleKey(activeKeyType));
                } catch (EncryptionException e) {
                    LOGGER.error("Couldn't obtain clear string for configuration of SimpleKey from " + activeKeyType);
                }
//            }

//            if (keysType.getStandBy() != null && !keysType.getStandBy().isEmpty()) {
                for (AuthenticationModuleSaml2SimpleKeyType standByKey : keysType.getStandBy()) {
                    try {
                        key.getStandBy().add(createSimpleKey(standByKey));
                    } catch (EncryptionException e) {
                        LOGGER.error("Couldn't obtain clear string for configuration of SimpleKey from " + standByKey);
                    }
                }
//            }
            serviceProvider.setKeys(key);
//        }

        List<ExternalIdentityProviderConfiguration> providers = new ArrayList<ExternalIdentityProviderConfiguration>();
        List<AuthenticationModuleSaml2ProviderType> providersType = serviceProviderType.getProvider();
        for (AuthenticationModuleSaml2ProviderType providerType : providersType) {
            ExternalIdentityProviderConfiguration provider = new ExternalIdentityProviderConfiguration();
            provider.setAlias(providerType.getAlias())
                    .setSkipSslValidation(Boolean.TRUE.equals(providerType.isSkipSslValidation()))
                    .setMetadataTrustCheck(Boolean.TRUE.equals(providerType.isMetadataTrustCheck()))
                    .setAuthenticationRequestBinding(URI.create(providerType.getAuthenticationRequestBinding()));
            if (StringUtils.isNotBlank(providerType.getLinkText())) {
                provider.setLinktext(providerType.getLinkText());
            }
            List<String> verificationKeys = new ArrayList<String>();
            for (ProtectedStringType verificationKeyProtected : providerType.getVerificationKeys()) {
                try {
                    String verificationKey = protector.decryptString(verificationKeyProtected);
                } catch (EncryptionException e) {
                    LOGGER.error("Couldn't obtain clear string for provider verification key");
                }
            }
            if (verificationKeys != null && !verificationKeys.isEmpty()) {
                provider.setVerificationKeys(verificationKeys);
            }
            try {
                provider.setMetadata(createMetadata(providerType.getMetadata(), true));
            } catch (Exception e) {
                LOGGER.error("Couldn't obtain metadata as string from " + providerType.getMetadata());
            }
            providers.add(provider);
            configuration.addNameOfUsernameAttributeOfIP(providerType.getEntityId(), providerType.getNameOfUsernameAttribute());
        }
        serviceProvider.setProviders(providers);
        try {
            serviceProvider.setMetadata(createMetadata(serviceProviderType.getMetadata(), false));
        } catch (Exception e) {
            LOGGER.error("Couldn't obtain metadata as string from " + serviceProviderType.getMetadata());
        }
        serviceProvider.setPrefix(configuration.getPrefix());
        samlConfiguration.setServiceProvider(serviceProvider);
        configuration.setSamlConfiguration(samlConfiguration);
        return configuration;
    }

    private static String createMetadata(AuthenticationModuleSaml2ProviderMetadataType metadata, boolean required) throws IOException {
        if (metadata != null) {
            String metadataUrl = metadata.getMetadataUrl();
            if (StringUtils.isNotBlank(metadataUrl)) {
                return metadataUrl;
            }
            String pathToFile = metadata.getPathToFile();
            if (StringUtils.isNotBlank(pathToFile)) {
                return readFile(pathToFile);
            }
            byte[] xml = metadata.getXml();
            if (xml != null && xml.length != 0) {
                return new String(xml);
            }
        }
        if (required) {
            throw new IllegalArgumentException("Metadata is not present");
        }
        return null;
    }

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

    private static SimpleKey createSimpleKey(AuthenticationModuleSaml2SimpleKeyType simpleKeyType) throws EncryptionException {
        SimpleKey key = new SimpleKey();
        key.setName(simpleKeyType.getName());
//        Protector protector = ((MidPointApplication) Application.get()).getProtector();
        String privateKey = protector.decryptString(simpleKeyType.getPrivateKey());
        key.setPrivateKey(privateKey);
        String passphrase = protector.decryptString(simpleKeyType.getPassphrase());
        key.setPassphrase(passphrase);
        String certificate = protector.decryptString(simpleKeyType.getCertificate());
        key.setCertificate(certificate);
        if (simpleKeyType.getType() != null) {
            key.setType(KeyType.fromTypeName(simpleKeyType.getType().name()));
        }
        return key;
    }

    private static String getBasePath(HttpServletRequest request) {
        boolean includePort = true;
        if (443 == request.getServerPort() && "https".equals(request.getScheme())) {
            includePort = false;
        }
        else if (80 == request.getServerPort() && "http".equals(request.getScheme())) {
            includePort = false;
        }
        return request.getScheme() +
                "://" +
                request.getServerName() +
                (includePort ? (":" + request.getServerPort()) : "") +
                request.getContextPath();
    }

    public SamlServerConfiguration getSamlConfiguration() {
        return samlConfiguration;
    }

    public void setSamlConfiguration(SamlServerConfiguration samlConfiguration) {
        this.samlConfiguration = samlConfiguration;
    }

    public Map<String, String> getNamesOfUsernameAttributes() {
        return namesOfUsernameAttributes;
    }

    public void addNameOfUsernameAttributeOfIP(String aliasOfIP, String nameOfAttribute){
        if (StringUtils.isBlank(aliasOfIP) || StringUtils.isBlank(nameOfAttribute)) {
            throw new IllegalArgumentException("Couldn't use attribute name '" + nameOfAttribute +"'" + " for alias '" + aliasOfIP +"'");
        }
        getNamesOfUsernameAttributes().put(aliasOfIP, nameOfAttribute);
    }

    public void setNamesOfUsernameAttributes(Map<String, String> namesOfUsernameAttributes) {
        this.namesOfUsernameAttributes = namesOfUsernameAttributes;
    }

    @Override
    protected void validate() {
        super.validate();
        if (getSamlConfiguration() == null) {
            throw new IllegalArgumentException("Saml configuration is null");
        }
    }
}
