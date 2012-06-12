/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.NodeErrorStatus;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.NodeType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.Validate;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Takes care about node registration in repository.
 *
 * @author Pavol Mederly
 */
public class NodeRegistrar {

    private static final transient Trace LOGGER = TraceManager.getTrace(NodeRegistrar.class);

    private TaskManagerQuartzImpl taskManager;
    private ClusterManager clusterManager;

    private PrismObject<NodeType> nodePrism;

    public NodeRegistrar(TaskManagerQuartzImpl taskManager, ClusterManager clusterManager) {
        Validate.notNull(taskManager);
        Validate.notNull(clusterManager);

        this.taskManager = taskManager;
        this.clusterManager = clusterManager;
    }

    /**
     * Executes node startup registration: if Node object with a give name (node ID) exists, deletes it.
     * Then creates a new Node with the information relevant to this node.
     *
     * @param result Node prism to be used for periodic re-registrations.
     */
    PrismObject<NodeType> createNodeObject(OperationResult result) throws TaskManagerInitializationException {

        nodePrism = createNodePrism(taskManager.getConfiguration());
        NodeType node = nodePrism.asObjectable();

        LOGGER.info("Registering this node in the repository as " + node.getNodeIdentifier() + " at " + node.getHostname() + ":" + node.getJmxPort());

        List<PrismObject<NodeType>> nodes = null;
        try {
            nodes = findNodesWithGivenName(result, node.getName());
        } catch (SchemaException e) {
            throw new TaskManagerInitializationException("Node registration failed because of schema exception", e);
        }

        for (PrismObject<NodeType> n : nodes) {
            LOGGER.trace("Removing existing NodeType with oid = {}, name = {}", n.getOid(), n.getName());
            try {
                getRepositoryService().deleteObject(NodeType.class, n.getOid(), result);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Cannot remove NodeType with oid = {}, name = {}, because it does not exist.", e, n.getOid(), n.getName());
                // continue, because the error is not that severe (we hope so)
            }
        }

        try {
            String oid = getRepositoryService().addObject(nodePrism, result);
            nodePrism.setOid(oid);
        } catch (ObjectAlreadyExistsException e) {
            taskManager.setNodeErrorStatus(NodeErrorStatus.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node, because it already exists (this should not happen, as nodes with such a name were just removed)", e);
        } catch (SchemaException e) {
            taskManager.setNodeErrorStatus(NodeErrorStatus.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node because of schema exception", e);
        }

        LOGGER.trace("Node was successfully registered in the repository.");
        return nodePrism;
    }

    private PrismObject<NodeType> createNodePrism(TaskManagerConfiguration configuration) {

        PrismObjectDefinition<NodeType> nodeTypeDef = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(NodeType.class);
        PrismObject<NodeType> nodePrism = nodeTypeDef.instantiate();

        NodeType node = nodePrism.asObjectable();

        node.setNodeIdentifier(configuration.getNodeId());
        node.setName(configuration.getNodeId());
        node.setHostname(getMyAddress());
        node.setJmxPort(configuration.getJmxPort());
        node.setClustered(configuration.isClustered());
        node.setRunning(true);
        node.setLastCheckInTime(getCurrentTime());

        generateInternalNodeIdentifier(node);

        return nodePrism;
    }

    /**
     * Generates an identifier that is used to ensure that this Node object is not (by mistake) overwritten
     * by another node in cluster. ClusterManager thread periodically checks if this identifier has not been changed.
     *
     * @param node
     */

    private void generateInternalNodeIdentifier(NodeType node) {

        String id = node.getNodeIdentifier() + ":" + node.getJmxPort() + ":" + Math.round(Math.random() * 10000000000000.0);
        LOGGER.trace("internal node identifier generated: " + id);
        node.setInternalNodeIdentifier(id);
    }

    private XMLGregorianCalendar getCurrentTime() {

        try {
            // AFAIK the DatatypeFactory is not thread safe, so we have to create an instance every time
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        } catch (DatatypeConfigurationException e) {
            // this should not happen
            throw new SystemException("Cannot create DatatypeFactory (to create XMLGregorianCalendar instance).", e);
        }
    }

    private PropertyDelta<NodeType> createCheckInTimeDelta() {
        return PropertyDelta.createReplaceDelta(nodePrism.getDefinition(), NodeType.F_LAST_CHECK_IN_TIME, getCurrentTime());
    }


    /**
     * Removes current node from the repository (currently not used; recordNodeShutdown is used instead).
     *
     * @param result
     */
    @Deprecated
    void removeNodeObject(OperationResult result) {

        String oid = nodePrism.getOid();
        String name = nodePrism.asObjectable().getNodeIdentifier();

        LOGGER.trace("Removing this node from the repository (name {}, oid {})", name, oid);
        try {
            getRepositoryService().deleteObject(NodeType.class, oid, result);
            LOGGER.trace("Node successfully unregistered (removed).");
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot unregister (remove) this node (name {}, oid {}), because it does not exist.", e,
                    name, oid);
        }

    }

    /**
     * Registers the node going down (sets running attribute to false).
     *
     * @param result
     */
    void recordNodeShutdown(OperationResult result) {

        LOGGER.trace("Registering this node shutdown (name {}, oid {})", nodePrism.asObjectable().getName(), nodePrism.getOid());

        List<PropertyDelta<NodeType>> modifications = new ArrayList<PropertyDelta<NodeType>>();
        modifications.add(PropertyDelta.createReplaceDelta(nodePrism.getDefinition(), NodeType.F_RUNNING, false));
        modifications.add(createCheckInTimeDelta());

        try {
            getRepositoryService().modifyObject(NodeType.class, nodePrism.getOid(), modifications, result);
            LOGGER.trace("Node shutdown successfully registered.");
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}), because it does not exist.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            // we do not set error flag here, because we hope that on a node startup the registration would (perhaps) succeed
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}).", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}) due to schema exception.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
        }
    }

    /**
     * Updates registration of this node (runs periodically within ClusterManager thread).
     *
     * @param result
     */
    void updateNodeObject(OperationResult result) {

        LOGGER.trace("Updating this node registration (name {}, oid {})", nodePrism.asObjectable().getName(), nodePrism.getOid());

        List<PropertyDelta<NodeType>> modifications = new ArrayList<PropertyDelta<NodeType>>();
        modifications.add(PropertyDelta.createReplaceDelta(nodePrism.getDefinition(), NodeType.F_HOSTNAME, getMyAddress()));
        modifications.add(createCheckInTimeDelta());

        try {
            getRepositoryService().modifyObject(NodeType.class, nodePrism.getOid(), modifications, result);
            LOGGER.trace("Node registration successfully updated.");
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot update registration of this node (name {}, oid {}), because it does not exist in repository. It is probably caused by cluster misconfiguration (other node rewriting the Node object?) Stopping the scheduler.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatus.OK) {
                registerNodeError(NodeErrorStatus.NODE_REGISTRATION_FAILED);
            }
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logException(LOGGER, "Cannot update registration of this node (name {}, oid {}).", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatus.OK) {
                registerNodeError(NodeErrorStatus.NODE_REGISTRATION_FAILED);
            }
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Cannot update registration of this node (name {}, oid {}) due to schema exception. Stopping the scheduler.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatus.OK) {
                registerNodeError(NodeErrorStatus.NODE_REGISTRATION_FAILED);
            }
        }
    }

    /**
     * Checks whether this Node object was not overwritten by another node (implying there is duplicate node ID in cluster).
     *
     * @param result
     */
    void verifyNodeObject(OperationResult result) {

        PrismObject<NodeType> nodeInRepo;

        String oid = nodePrism.getOid();
        String myName = nodePrism.asObjectable().getName();
        LOGGER.trace("Verifying node record with OID = " + oid);

        // first, let us check the record of this node - whether it exists and whether the internalNodeIdentifier is OK
        try {
            nodeInRepo = getRepositoryService().getObject(NodeType.class, oid, result);
        } catch (ObjectNotFoundException e) {
            if (doesNodeExist(result, myName)) {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found), but " +
                        "another node record with the name '{}' exists. It seems that in this cluster " +
                        "there are two or more nodes with the same name '{}'. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                registerNodeError(NodeErrorStatus.DUPLICATE_NODE_ID_OR_NAME);
                return;
            } else {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found). It  " +
                        "seems it was deleted in the meantime. Please check the reason. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                // actually we could re-register the node, but it is safer (and easier for now :) to stop the node instead
                registerNodeError(NodeErrorStatus.NODE_REGISTRATION_FAILED);
                return;
            }
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Cannot check the record of this node (OID = {}) because of schema exception. Stopping the scheduler.", e, oid);
            registerNodeError(NodeErrorStatus.NODE_REGISTRATION_FAILED);
            return;
        }

        // check the internalNodeIdentifier
        String existingId = nodePrism.asObjectable().getInternalNodeIdentifier();
        String idInRepo = nodeInRepo.asObjectable().getInternalNodeIdentifier();
        if (!existingId.equals(idInRepo)) {
            LOGGER.error("Internal node identifier has been overwritten in the repository. " +
                    "Probably somebody has overwritten it in the meantime, i.e. another node with the name of '" +
                    nodePrism.asObjectable().getName() + "' is running. Stopping the scheduler.");
            registerNodeError(NodeErrorStatus.DUPLICATE_NODE_ID_OR_NAME);
            return;
        }
    }

    /**
     * There may be either exactly one non-clustered node (and no other nodes), or clustered nodes only.
     * @param result
     */
    public void checkNonClusteredNodes(OperationResult result) {

        LOGGER.trace("Checking non-clustered nodes.");

        List<String> clustered = new ArrayList<String>();
        List<String> nonClustered = new ArrayList<String>();

        List<PrismObject<NodeType>> allNodes = clusterManager.getAllNodes(result);
        for (PrismObject<NodeType> nodePrism : allNodes) {
            NodeType n = nodePrism.asObjectable();
            if (isUp(n)) {
                if (n.isClustered()) {
                    clustered.add(n.getNodeIdentifier());
                } else {
                    nonClustered.add(n.getNodeIdentifier());
                }
            }
        }

        LOGGER.trace("Clustered nodes: " + clustered);
        LOGGER.trace("Non-clustered nodes: " + nonClustered);

        int all = clustered.size() + nonClustered.size();

        if (!taskManager.getConfiguration().isClustered() && all > 1) {
            LOGGER.error("This node is a non-clustered one, mixed with other nodes. In this system, there are " +
                    nonClustered.size() + " non-clustered nodes (" + nonClustered + ") and " +
                    clustered.size() + " clustered ones (" + clustered + "). Stopping this node.");
            registerNodeError(NodeErrorStatus.NON_CLUSTERED_NODE_WITH_OTHERS);
        }

    }

    boolean isUp(NodeType n) {
        return n.getLastCheckInTime() != null &&
                (System.currentTimeMillis() - n.getLastCheckInTime().toGregorianCalendar().getTimeInMillis())
                        <= (taskManager.getConfiguration().getNodeTimeout() * 1000L);
    }


    private boolean doesNodeExist(OperationResult result, String myName) {
        try {
            List<PrismObject<NodeType>> nodes = findNodesWithGivenName(result, myName);
            return nodes != null && !nodes.isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Existence of a Node cannot be checked due to schema exception.", e);
            return false;
        }
    }

    private List<PrismObject<NodeType>> findNodesWithGivenName(OperationResult result, String name) throws SchemaException {

        QueryType q = QueryUtil.createNameQuery(name);
        return getRepositoryService().searchObjects(NodeType.class, q, new PagingType(), result);
    }


    /**
     * Sets node error status and shuts down the scheduler (used when an error occurs after initialization).
     *
     * @param status Error status to be set.
     */
    private void registerNodeError(NodeErrorStatus status) {
        taskManager.setNodeErrorStatus(status);
        if (taskManager.getServiceThreadsActivationState()) {
            taskManager.getExecutionManager().stopSchedulerAndTasksLocally(0L, new OperationResult("nodeError"));
        }
        taskManager.getExecutionManager().shutdownLocalSchedulerChecked();
        LOGGER.warn("Scheduler stopped, please check your cluster configuration as soon as possible; kind of error = " + status);
    }

    private String getMyAddress() {

        if (taskManager.getConfiguration().getJmxHostName() != null) {
            return taskManager.getConfiguration().getJmxHostName();
        } else {
            try {
                InetAddress address = InetAddress.getLocalHost();
                return address.getHostAddress();
            } catch (UnknownHostException e) {
                LoggingUtils.logException(LOGGER, "Cannot get local IP address", e);
                return "unknown-host";
            }
        }
    }

    public PrismObject<NodeType> getNodePrism() {
        return nodePrism;
    }

    public String getNodeId() {
        return nodePrism.asObjectable().getNodeIdentifier();
    }

    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return getNodeId().equals(node.asObjectable().getNodeIdentifier());
    }

    boolean isCurrentNode(String nodeIdentifier) {
        return nodeIdentifier == null || getNodeId().equals(nodeIdentifier);
    }

    private RepositoryService getRepositoryService() {
        return taskManager.getRepositoryService();
    }

    private PrismContext getPrismContext() {
        return taskManager.getPrismContext();
    }

    public void deleteNode(String nodeIdentifier, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(NodeRegistrar.class.getName() + ".deleteNode");
        result.addParam("nodeIdentified", nodeIdentifier);

        boolean deleted = false;

        List<PrismObject<NodeType>> nodes = clusterManager.getAllNodes(result);
        for (PrismObject<NodeType> nodePrism : nodes) {
            if (nodeIdentifier.equals(nodePrism.asObjectable().getNodeIdentifier())) {
                deleted = true;
                if (isUp(nodePrism.asObjectable())) {
                    result.recordFatalError("Node " + nodeIdentifier + " cannot be deleted, because it is currently up.");
                } else {
                    try {
                        taskManager.getRepositoryService().deleteObject(NodeType.class, nodePrism.getOid(), result);
                        result.recordSuccess();
                    } catch (ObjectNotFoundException e) {
                        // should not occur
                        result.recordFatalError("Node " + nodeIdentifier + " cannot be deleted, because it does not exist in repository.");
                    }
                }
            }
        }
        if (!deleted) {
            result.recordFatalError("Node " + nodeIdentifier + " cannot be deleted, because it does not exist in repository.");
        }
    }
}
