
    create table m_account_shadow (
        accountType varchar(255),
        allowedIdmAdminGuiAccess boolean,
        passwordXml text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_any (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        ownerType int4 not null,
        primary key (owner_id, owner_oid, ownerType)
    );

    create table m_any_clob (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        ownerType int4 not null,
        clobValue text,
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int4
    );

    create table m_any_date (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        ownerType int4 not null,
        dateValue timestamp,
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int4
    );

    create table m_any_long (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        ownerType int4 not null,
        longValue int8,
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int4
    );

    create table m_any_reference (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        ownerType int4 not null,
        oidValue varchar(255),
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int4
    );

    create table m_any_string (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        ownerType int4 not null,
        stringValue varchar(255),
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int4
    );

    create table m_assignment (
        accountConstruction text,
        enabled boolean,
        validFrom timestamp,
        validTo timestamp,
        description text,
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        targetRef_description text,
        targetRef_filter text,
        targetRef_relationLocalPart varchar(255),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type int4,
        id int8 not null,
        oid varchar(36) not null,
        extId int8,
        extOid varchar(36),
        extType int4,
        primary key (id, oid)
    );

    create table m_audit_delta (
        RAuditEventRecord_id int8 not null,
        deltas text
    );

    create table m_audit_event (
        id int8 not null,
        channel varchar(255),
        eventIdentifier varchar(255),
        eventStage int4,
        eventType int4,
        hostIdentifier varchar(255),
        initiator text,
        outcome int4,
        sessionIdentifier varchar(255),
        target text,
        targetOwner text,
        taskIdentifier varchar(255),
        taskOID varchar(255),
        timestampValue int8,
        primary key (id)
    );

    create table m_connector (
        connectorBundle varchar(255),
        connectorType varchar(255),
        connectorVersion varchar(255),
        framework varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        xmlSchema text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        protectConnection boolean,
        sharedSecret text,
        timeout int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_connector_target_system (
        connector_id int8 not null,
        connector_oid varchar(36) not null,
        targetSystemType varchar(255)
    );

    create table m_container (
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_exclusion (
        description text,
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        policy int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_generic_object (
        name_norm varchar(255),
        name_orig varchar(255),
        objectType varchar(255),
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_node (
        clusteredNode boolean,
        hostname varchar(255),
        internalNodeIdentifier varchar(255),
        jmxPort int4,
        lastCheckInTime timestamp,
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        running boolean,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_object (
        description text,
        version int8 not null,
        id int8 not null,
        oid varchar(36) not null,
        extId int8,
        extOid varchar(36),
        extType int4,
        primary key (id, oid)
    );

    create table m_object_org_ref (
        object_id int8 not null,
        object_oid varchar(36) not null,
        description text,
        filter text,
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type int4
    );

    create table m_operation_result (
        owner_oid varchar(36) not null,
        owner_id int8 not null,
        details text,
        localizedMessage text,
        message text,
        messageCode varchar(255),
        operation text,
        params text,
        partialResults text,
        status int4,
        token int8,
        primary key (owner_oid, owner_id)
    );

    create table m_org (
        costCenter varchar(255),
        displayName_norm varchar(255),
        displayName_orig varchar(255),
        identifier varchar(255),
        locality_norm varchar(255),
        locality_orig varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_org_closure (
        id int8 not null,
        depthValue int4,
        ancestor_id int8,
        ancestor_oid varchar(36),
        descendant_id int8,
        descendant_oid varchar(36),
        primary key (id)
    );

    create table m_org_org_type (
        org_id int8 not null,
        org_oid varchar(36) not null,
        orgType varchar(255)
    );

    create table m_org_sys_config (
        org_id int8 not null,
        org_oid varchar(36) not null,
        description text,
        filter text,
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type int4
    );

    create table m_password_policy (
        lifetime text,
        name_norm varchar(255),
        name_orig varchar(255),
        stringPolicy text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_reference (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        targetOid varchar(36) not null,
        description text,
        filter text,
        reference_relationLocalPart varchar(255),
        reference_relationNamespace varchar(255),
        type int4,
        primary key (owner_id, owner_oid, targetOid)
    );

    create table m_resource (
        business_administrativeState int4,
        capabilities_cachingMetadata text,
        capabilities_configured text,
        capabilities_native text,
        configuration text,
        connectorRef_description text,
        connectorRef_filter text,
        connectorRef_relationLocalPart varchar(255),
        connectorRef_relationNamespace varchar(255),
        connectorRef_targetOid varchar(36),
        connectorRef_type int4,
        consistency text,
        lastAvailabilityStatus int4,
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        o16_lastAvailabilityStatus int4,
        schemaHandling text,
        scripts text,
        synchronization text,
        xmlSchema text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_resource_approver_ref (
        user_id int8 not null,
        user_oid varchar(36) not null,
        description text,
        filter text,
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type int4
    );

    create table m_resource_shadow (
        enabled boolean,
        validFrom timestamp,
        validTo timestamp,
        attemptNumber int4,
        dead boolean,
        failedOperationType int4,
        intent varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        objectChange text,
        class_namespace varchar(255),
        class_localPart varchar(255),
        synchronizationSituation int4,
        synchronizationTimestamp timestamp,
        id int8 not null,
        oid varchar(36) not null,
        attrId int8,
        attrOid varchar(36),
        attrType int4,
        primary key (id, oid)
    );

    create table m_role (
        approvalExpression text,
        approvalProcess varchar(255),
        approvalSchema text,
        automaticallyApproved text,
        name_norm varchar(255),
        name_orig varchar(255),
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_sync_situation_description (
        shadow_id int8 not null,
        shadow_oid varchar(36) not null,
        chanel varchar(255),
        situation int4,
        timestamp timestamp
    );

    create table m_system_configuration (
        connectorFramework text,
        g36 text,
        g23_description text,
        globalPasswordPolicyRef_filter text,
        g23_relationLocalPart varchar(255),
        g23_relationNamespace varchar(255),
        g23_targetOid varchar(36),
        globalPasswordPolicyRef_type int4,
        logging text,
        modelHooks text,
        name_norm varchar(255),
        name_orig varchar(255),
        notificationConfiguration text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_task (
        binding int4,
        canRunOnNode varchar(255),
        category varchar(255),
        claimExpirationTimestamp timestamp,
        exclusivityStatus int4,
        executionStatus int4,
        handlerUri varchar(255),
        lastRunFinishTimestamp timestamp,
        lastRunStartTimestamp timestamp,
        modelOperationState text,
        name_norm varchar(255),
        name_orig varchar(255),
        nextRunStartTime timestamp,
        node varchar(255),
        objectRef_description text,
        objectRef_filter text,
        objectRef_relationLocalPart varchar(255),
        objectRef_relationNamespace varchar(255),
        objectRef_targetOid varchar(36),
        objectRef_type int4,
        otherHandlersUriStack text,
        ownerRef_description text,
        ownerRef_filter text,
        ownerRef_relationLocalPart varchar(255),
        ownerRef_relationNamespace varchar(255),
        ownerRef_targetOid varchar(36),
        ownerRef_type int4,
        parent varchar(255),
        progress int8,
        recurrence int4,
        resultStatus int4,
        schedule text,
        taskIdentifier varchar(255),
        threadStopAction int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_user (
        enabled boolean,
        validFrom timestamp,
        validTo timestamp,
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        allowedIdmAdminGuiAccess boolean,
        passwordXml text,
        emailAddress varchar(255),
        employeeNumber varchar(255),
        familyName_norm varchar(255),
        familyName_orig varchar(255),
        fullName_norm varchar(255),
        fullName_orig varchar(255),
        givenName_norm varchar(255),
        givenName_orig varchar(255),
        honorificPrefix_norm varchar(255),
        honorificPrefix_orig varchar(255),
        honorificSuffix_norm varchar(255),
        honorificSuffix_orig varchar(255),
        locale varchar(255),
        locality_norm varchar(255),
        locality_orig varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        nickName_norm varchar(255),
        nickName_orig varchar(255),
        preferredLanguage varchar(255),
        telephoneNumber varchar(255),
        timezone varchar(255),
        title_norm varchar(255),
        title_orig varchar(255),
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_user_employee_type (
        user_id int8 not null,
        user_oid varchar(36) not null,
        employeeType varchar(255)
    );

    create table m_user_organizational_unit (
        user_id int8 not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    );

    create table m_user_template (
        accountConstruction text,
        name_norm varchar(255),
        name_orig varchar(255),
        propertyConstruction text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    alter table m_account_shadow 
        add constraint fk_account_shadow 
        foreign key (id, oid) 
        references m_resource_shadow;

    alter table m_any_clob 
        add constraint fk_any_clob 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iDate on m_any_date (dateValue);

    alter table m_any_date 
        add constraint fk_any_date 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iLong on m_any_long (longValue);

    alter table m_any_long 
        add constraint fk_any_long 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iOid on m_any_reference (oidValue);

    alter table m_any_reference 
        add constraint fk_any_reference 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iString on m_any_string (stringValue);

    alter table m_any_string 
        add constraint fk_any_string 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iAssignmentEnabled on m_assignment (enabled);

    alter table m_assignment 
        add constraint fk_assignment 
        foreign key (id, oid) 
        references m_container;

    alter table m_assignment 
        add constraint fk_assignment_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_audit_delta 
        add constraint fk_audit_delta 
        foreign key (RAuditEventRecord_id) 
        references m_audit_event;

    create index iConnectorName on m_connector (name_norm);

    alter table m_connector 
        add constraint fk_connector 
        foreign key (id, oid) 
        references m_object;

    alter table m_connector_host 
        add constraint fk_connector_host 
        foreign key (id, oid) 
        references m_object;

    alter table m_connector_target_system 
        add constraint fk_connector_target_system 
        foreign key (connector_id, connector_oid) 
        references m_connector;

    alter table m_exclusion 
        add constraint fk_exclusion 
        foreign key (id, oid) 
        references m_container;

    alter table m_exclusion 
        add constraint fk_exclusion_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_generic_object 
        add constraint fk_generic_object 
        foreign key (id, oid) 
        references m_object;

    alter table m_node 
        add constraint fk_node 
        foreign key (id, oid) 
        references m_object;

    alter table m_object 
        add constraint fk_container 
        foreign key (id, oid) 
        references m_container;

    alter table m_object_org_ref 
        add constraint fk_object_org_ref 
        foreign key (object_id, object_oid) 
        references m_object;

    alter table m_operation_result 
        add constraint fk_result_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_org 
        add constraint fk_org 
        foreign key (id, oid) 
        references m_object;

    create index iDescendant on m_org_closure (descendant_oid, descendant_id);

    create index iAncestor on m_org_closure (ancestor_oid, ancestor_id);

    alter table m_org_closure 
        add constraint fk_descendant 
        foreign key (descendant_id, descendant_oid) 
        references m_object;

    alter table m_org_closure 
        add constraint fk_ancestor 
        foreign key (ancestor_id, ancestor_oid) 
        references m_object;

    alter table m_org_org_type 
        add constraint fk_org_org_type 
        foreign key (org_id, org_oid) 
        references m_org;

    alter table m_org_sys_config 
        add constraint fk_org_unit 
        foreign key (org_id, org_oid) 
        references m_system_configuration;

    alter table m_password_policy 
        add constraint fk_password_policy 
        foreign key (id, oid) 
        references m_object;

    alter table m_reference 
        add constraint fk_reference_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    alter table m_resource 
        add constraint fk_resource 
        foreign key (id, oid) 
        references m_object;

    alter table m_resource_approver_ref 
        add constraint fk_resource_approver_ref 
        foreign key (user_id, user_oid) 
        references m_resource;

    create index iResourceObjectShadowEnabled on m_resource_shadow (enabled);

    create index iResourceShadowName on m_resource_shadow (name_norm);

    alter table m_resource_shadow 
        add constraint fk_resource_object_shadow 
        foreign key (id, oid) 
        references m_object;

    alter table m_role 
        add constraint fk_role 
        foreign key (id, oid) 
        references m_object;

    alter table m_sync_situation_description 
        add constraint fk_shadow_sync_situation 
        foreign key (shadow_id, shadow_oid) 
        references m_resource_shadow;

    alter table m_system_configuration 
        add constraint fk_system_configuration 
        foreign key (id, oid) 
        references m_object;

    create index iTaskName on m_task (name_norm);

    alter table m_task 
        add constraint fk_task 
        foreign key (id, oid) 
        references m_object;

    create index iFullName on m_user (fullName_norm);

    create index iLocality on m_user (locality_norm);

    create index iHonorificSuffix on m_user (honorificSuffix_norm);

    create index iEmployeeNumber on m_user (employeeNumber);

    create index iGivenName on m_user (givenName_norm);

    create index iFamilyName on m_user (familyName_norm);

    create index iAdditionalName on m_user (additionalName_norm);

    create index iHonorificPrefix on m_user (honorificPrefix_norm);

    create index iUserEnabled on m_user (enabled);

    alter table m_user 
        add constraint fk_user 
        foreign key (id, oid) 
        references m_object;

    alter table m_user_employee_type 
        add constraint fk_user_employee_type 
        foreign key (user_id, user_oid) 
        references m_user;

    alter table m_user_organizational_unit 
        add constraint fk_user_org_unit 
        foreign key (user_id, user_oid) 
        references m_user;

    alter table m_user_template 
        add constraint fk_user_template 
        foreign key (id, oid) 
        references m_object;

    create sequence hibernate_sequence start 1 increment 1;
