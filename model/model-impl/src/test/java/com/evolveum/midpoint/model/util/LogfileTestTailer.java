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
package com.evolveum.midpoint.model.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.util.aspect.MidpointAspect;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class LogfileTestTailer {
	
	private static final String LOG_FILENAME = "target/test.log";
	private static final String MARKER = "_M_A_R_K_E_R_";
	
	public static final String LEVEL_ERROR = "ERROR";
	public static final String LEVEL_WARN = "WARN";
	public static final String LEVEL_INFO = "INFO";
	public static final String LEVEL_DEBUG = "DEBUG";
	public static final String LEVEL_TRACE = "TRACE";
	
	private static final Pattern markerPattern = Pattern.compile(".*\\[[^]]*\\]\\s+(\\w+)\\s+\\S+\\s+"+MARKER+"\\s+(\\w+).*");
	public static final Pattern auditPattern = 
			Pattern.compile(".*\\[[^]]*\\]\\s+(\\w+)\\s+\\("+LoggingConfigurationManager.AUDIT_LOGGER_NAME+"\\):\\s*(.*)");
	
	final static Trace LOGGER = TraceManager.getTrace(LogfileTestTailer.class);
	
	private FileReader fileReader;
	private BufferedReader reader;
	private boolean seenMarker;
	private Set<String> loggedMarkers;
	private List<String> auditMessages = new ArrayList<String>();
	private String expectedMessage;
	private String expectedMessageLine;
	
	public LogfileTestTailer() throws IOException {
		reset();
		File file = new File(LOG_FILENAME);
		fileReader = new FileReader(file);
		reader = new BufferedReader(fileReader);
		reader.skip(file.length());
	}
	
	public void close() throws IOException {
		reader.close();
		fileReader.close();
	}
	
	public void reset() {
		seenMarker = false;
		loggedMarkers = new HashSet<String>();
		auditMessages = new ArrayList<String>();
		expectedMessageLine = null;
	}

	public boolean isSeenMarker() {
		return seenMarker;
	}

	public void setSeenMarker(boolean seenMarker) {
		this.seenMarker = seenMarker;
	}

	public void tail() throws IOException {
		while (true) {
		    String line = reader.readLine();
		    if (line == null) {
		    	break;
		    }
		    processLogLine(line);
		}
	}

	private void processLogLine(String line) {
		Matcher matcher = markerPattern.matcher(line);
		while (matcher.find()) {
			seenMarker = true;
			String level = matcher.group(1);
			String subsystemName = matcher.group(2);
			recordMarker(level,subsystemName);
		}
		matcher = auditPattern.matcher(line);
		while (matcher.find()) {
			String level = matcher.group(1);
			String message = matcher.group(2);
			recordAuditMessage(level,message);
		}
		if (expectedMessage != null && line.contains(expectedMessage)) {
			expectedMessageLine = line;
		}
	}
	
	private void recordMarker(String level, String subsystemName) {
		String key = constructKey(level, subsystemName);
		loggedMarkers.add(key);
	}
	
	private void recordAuditMessage(String level, String message) {
		auditMessages.add(message);
	}

	private String constructKey(String level, String subsystemName) {
		return level+":"+subsystemName;
	}
	
	public void assertMarkerLogged(String level, String subsystemName) {
		assert loggedMarkers.contains(constructKey(level, subsystemName)) : level + " in " + subsystemName + " was not logged";
	}
	
	public void assertMarkerNotLogged(String level, String subsystemName) {
		assert !loggedMarkers.contains(constructKey(level, subsystemName)) : level + " in " + subsystemName + " was logged (while not expecting it)";
	}
	
	public void setExpecteMessage(String expectedMessage) {
		this.expectedMessage = expectedMessage;
		this.expectedMessageLine = null;
	}
	
	public void assertExpectedMessage() {
		assert expectedMessageLine != null : "The expected message was not seen";
	}
	
	public void assertNoAudit() {
		assert auditMessages.isEmpty() : "Audit messages not empty: "+auditMessages;
	}
	
	public void assertAudit() {
		assert !auditMessages.isEmpty() : "No audit message";
	}

	public void assertAudit(String message) {
		assert auditMessages.contains(message) : "No audit message: "+message;
	}
	
	public void assertAuditRequest() {
		for (String message: auditMessages) {
			if (message.contains("stage REQUEST")) {
				return;
			}
			if (message.contains("es=REQUEST")) {
				return;
			}
		}
		assert false: "No request audit message";
	}

	public void assertAuditExecution() {
		for (String message: auditMessages) {
			if (message.contains("stage EXECUTION")) {
				return;
			}
			if (message.contains("es=EXECUTION")) {
				return;
			}
		}
		assert false: "No execution audit message";
	}

	public void assertAudit(int messageCount) {
		assert auditMessages.size() == messageCount : "Wrong number of audit messages, expected "+messageCount+", was "+auditMessages.size();
	}

	/**
	 * Log all levels in all subsystems.
	 */
	public void log() {
		for (String subsystemName: MidpointAspect.SUBSYSTEMS) {
			logAllLevels(LOGGER, subsystemName);
		}
		logAllLevels(LOGGER, null);
	}
	
	private void logAllLevels(Trace logger, String subsystemName) {
		String message = MARKER+" "+subsystemName;
		String previousSubsystem = MidpointAspect.swapSubsystemMark(subsystemName);
		logger.trace(message);
		logger.debug(message);
		logger.info(message);
		logger.warn(message);
		logger.error(message);
		MidpointAspect.swapSubsystemMark(previousSubsystem);
	}
	
	public void logAndTail() throws IOException {
		log();
		// Some pause here?
		tail();
	}

}
