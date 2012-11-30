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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.audit.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class AuditEventRecord implements Dumpable, DebugDumpable {
	
	/**
	 * Timestamp in millis.
	 */
	private Long timestamp;
	
	/**
	 * Unique identification of the event.
	 */
	private String eventIdentifier;
	
	// session ID
	private String sessionIdentifier;
	
	// channel???? (e.g. web gui, web service, ...)
	
	// task ID (not OID!)
	private String taskIdentifier;
	private String taskOID;
	
	// host ID
	private String hostIdentifier;
	
	// initiator (subject, event "owner"): store OID, type(implicit?), name
	private PrismObject<UserType> initiator;
	
	// (primary) target (object, the thing acted on): store OID, type, name
	// OPTIONAL
	private PrismObject<? extends ObjectType> target;
	
	// user that the target "belongs to"????
	private PrismObject<UserType> targetOwner;
		
	// event type
	private AuditEventType eventType;
	
	// event stage (request, execution)
	private AuditEventStage eventStage;
	
	// delta
	private Collection<ObjectDelta<? extends ObjectType>> deltas;
	
	// delta order (primary, secondary)
	
	private String channel;
	
	// outcome (success, failure)
	private OperationResultStatus outcome;

	// result (e.g. number of entries, returned object???)

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	public AuditEventRecord() {
		this.deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
	}
	
	public AuditEventRecord(AuditEventType eventType) {
		this.deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		this.eventType = eventType;
	}

	public AuditEventRecord(AuditEventType eventType, AuditEventStage eventStage) {
		this.deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		this.eventType = eventType;
		this.eventStage = eventStage;
	}

	public Long getTimestamp() {
		return timestamp;
	}
	
	public void clearTimestamp() {
		timestamp = null;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getEventIdentifier() {
		return eventIdentifier;
	}

	public void setEventIdentifier(String eventIdentifier) {
		this.eventIdentifier = eventIdentifier;
	}

	public String getSessionIdentifier() {
		return sessionIdentifier;
	}

	public void setSessionIdentifier(String sessionIdentifier) {
		this.sessionIdentifier = sessionIdentifier;
	}

	public String getTaskIdentifier() {
		return taskIdentifier;
	}

	public void setTaskIdentifier(String taskIdentifier) {
		this.taskIdentifier = taskIdentifier;
	}

	public String getTaskOID() {
		return taskOID;
	}

	public void setTaskOID(String taskOID) {
		this.taskOID = taskOID;
	}

	public String getHostIdentifier() {
		return hostIdentifier;
	}

	public void setHostIdentifier(String hostIdentifier) {
		this.hostIdentifier = hostIdentifier;
	}

	public PrismObject<UserType> getInitiator() {
		return initiator;
	}

	public void setInitiator(PrismObject<UserType> initiator) {
		this.initiator = initiator;
	}

	public PrismObject<? extends ObjectType> getTarget() {
		return target;
	}

	public void setTarget(PrismObject<? extends ObjectType> target) {
		this.target = target;
	}

	public PrismObject<UserType> getTargetOwner() {
		return targetOwner;
	}

	public void setTargetOwner(PrismObject<UserType> targetOwner) {
		this.targetOwner = targetOwner;
	}

	public AuditEventType getEventType() {
		return eventType;
	}

	public void setEventType(AuditEventType eventType) {
		this.eventType = eventType;
	}

	public AuditEventStage getEventStage() {
		return eventStage;
	}

	public void setEventStage(AuditEventStage eventStage) {
		this.eventStage = eventStage;
	}

	public Collection<ObjectDelta<? extends ObjectType>> getDeltas() {
		return deltas;
	}
	
	public void addDelta(ObjectDelta<? extends ObjectType> delta) {
		deltas.add(delta);
	}

	public void addDeltas(Collection<ObjectDelta<? extends ObjectType>> deltasToAdd) {
		deltas.addAll(deltasToAdd);
	}
	
	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public void clearDeltas() {
		deltas.clear();
	}

	public OperationResultStatus getOutcome() {
		return outcome;
	}

	public void setOutcome(OperationResultStatus outcome) {
		this.outcome = outcome;
	}
	
	public void setResult(OperationResult result) {
		outcome = result.getStatus();
	}
	
	public void checkConsistence() {
		if (initiator != null) {
			initiator.checkConsistence();
		}
		if (target != null) {
			target.checkConsistence();
		}
		if (targetOwner != null) {
			targetOwner.checkConsistence();
		}
		if (deltas != null) {
			ObjectDelta.checkConsistence(deltas);
		}
	}
	
	public AuditEventRecord clone() {
		AuditEventRecord clone = new AuditEventRecord();
		clone.channel = this.channel;
		clone.deltas = MiscSchemaUtil.cloneObjectDeltaCollection(this.deltas);
		clone.eventIdentifier = this.eventIdentifier;
		clone.eventStage = this.eventStage;
		clone.eventType = this.eventType;
		clone.hostIdentifier = this.hostIdentifier;
		clone.initiator = this.initiator;
		clone.outcome = this.outcome;
		clone.sessionIdentifier = this.sessionIdentifier;
		clone.target = this.target;
		clone.targetOwner = this.targetOwner;
		clone.taskIdentifier = this.taskIdentifier;
		clone.taskOID = this.taskOID;
		clone.timestamp = this.timestamp;
		return clone;
	}

	@Override
	public String toString() {
		return "AUDIT[" + formatTimestamp(timestamp) + " eid=" + eventIdentifier
				+ " sid=" + sessionIdentifier + ", tid=" + taskIdentifier
				+ " toid=" + taskOID + ", hid=" + hostIdentifier + ", I=" + formatObject(initiator)
				+ ", T=" + formatObject(target) + ", TO=" + formatObject(targetOwner) + ", et=" + eventType
				+ ", es=" + eventStage + ", D=" + deltas + ", ch="+ channel +"o=" + outcome + "]";
	}

	private static String formatTimestamp(Long timestamp) {
		if (timestamp == null) {
			return "null";
		}
		return TIMESTAMP_FORMAT.format(new java.util.Date(timestamp));
	}
	
	private static String formatObject(PrismObject<? extends ObjectType> object) {
		if (object == null) {
			return "null";
		}
		return object.toString();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("AUDIT");
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Timestamp", formatTimestamp(timestamp), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Identifier", eventIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Session Identifier", sessionIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Task Identifier", taskIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Task OID", taskOID, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Host Identifier", hostIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Initiator", formatObject(initiator), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Target", formatObject(target), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Target Owner", formatObject(targetOwner), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Type", eventType, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Stage", eventStage, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Channel", channel, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Outcome", outcome, indent + 1);
		DebugUtil.debugDumpLabel(sb, "Deltas", indent + 1);
		if (deltas == null || deltas.isEmpty()) {
			sb.append(" none");
		} else {
			sb.append(" ").append(deltas.size()).append(" deltas\n");
			DebugUtil.debugDump(sb, deltas, indent + 2, false);
		}
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}
		
}
