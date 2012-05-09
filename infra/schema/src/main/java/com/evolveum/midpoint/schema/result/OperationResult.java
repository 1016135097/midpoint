/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Viliam repan
 * Portions Copyrighted 2011 Radovan Semancik
 * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.schema.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ParamsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UnknownJavaObjectType;

/**
 * Nested Operation Result.
 * 
 * This class provides informations for better error handling in complex
 * operations. It contains a status (success, failure, warning, ...) and an
 * error message. It also contains a set of sub-results - results on inner
 * operations.
 * 
 * This object can be used by GUI to display smart (and interactive) error
 * information. It can also be used by the client code to detect deeper problems
 * in the invocations, retry or otherwise compensate for the errors or decide
 * how severe the error was and it is possible to proceed.
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
public class OperationResult implements Serializable, Dumpable {

	private static final long serialVersionUID = -2467406395542291044L;
	private static final String INDENT_STRING = "    ";
	
	public static final String CONTEXT_IMPLEMENTATION_CLASS = "implementationClass";
	public static final String CONTEXT_PROGRESS = "progress";
	public static final String CONTEXT_OID = "oid";
	public static final String CONTEXT_OBJECT = "object";
	public static final String CONTEXT_ITEM = "item";
	public static final String CONTEXT_TASK = "task";
	
	public static final String PARAM_OID = "oid";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_TASK = "task";
	public static final String PARAM_OBJECT = "object";
	
	public static final String RETURN_COUNT = "count";
	
	private static long TOKEN_COUNT = 1000000000000000000L;
	private String operation;
	private OperationResultStatus status;
	private Map<String, Object> params;
	private Map<String, Object> context;
	private Map<String, Object> returns;
	private long token;
	private String messageCode;
	private String message;
	private String localizationMessage;
	private List<Object> localizationArguments;
	private Throwable cause;
	private List<OperationResult> subresults;
	private List<String> details;
	private boolean summarizeErrors;
	private boolean summarizePartialErrors;
	private boolean summarizeSuccesses;
	private OperationResult summarizeTo;
	
	private static final Trace LOGGER = TraceManager.getTrace(OperationResult.class);

	public OperationResult(String operation) {
		this(operation, null, OperationResultStatus.UNKNOWN, 0, null, null, null, null, null);
	}

	public OperationResult(String operation, String messageCode, String message) {
		this(operation, null, OperationResultStatus.SUCCESS, 0, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, long token, String messageCode, String message) {
		this(operation, null, OperationResultStatus.SUCCESS, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, String message) {
		this(operation, null, status, 0, null, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, String messageCode, String message) {
		this(operation, null, status, 0, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, long token, String messageCode,
			String message) {
		this(operation, null, status, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, long token, String messageCode,
			String message, Throwable cause) {
		this(operation, null, status, token, messageCode, message, null, cause, null);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message) {
		this(operation, params, status, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message, List<OperationResult> subresults) {
		this(operation, params, status, token, messageCode, message, null, null, subresults);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message, String localizationMessage, Throwable cause,
			List<OperationResult> subresults) {
		this(operation, params, status, token, messageCode, message, localizationMessage, null, cause,
				subresults);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message, String localizationMessage,
			List<Object> localizationArguments, Throwable cause, List<OperationResult> subresults) {
		if (StringUtils.isEmpty(operation)) {
			throw new IllegalArgumentException("Operation argument must not be null or empty.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Operation status must not be null.");
		}
		this.operation = operation;
		this.params = params;
		this.status = status;
		this.token = token;
		this.messageCode = messageCode;
		this.message = message;
		this.localizationMessage = localizationMessage;
		this.localizationArguments = localizationArguments;
		this.cause = cause;
		this.subresults = subresults;
		this.details = new ArrayList<String>();
	}

	public OperationResult createSubresult(String operation) {
		OperationResult subresult = new OperationResult(operation);
		addSubresult(subresult);
		return subresult;
	}

	/**
	 * Contains operation name. Operation name must be defined as {@link String}
	 * constant in module interface with description and possible parameters. It
	 * can be used for further processing. It will be used as key for
	 * translation in admin-gui.
	 * 
	 * @return always return non null, non empty string
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * Method returns list of operation subresults @{link
	 * {@link OperationResult}.
	 * 
	 * @return never returns null
	 */
	public List<OperationResult> getSubresults() {
		if (subresults == null) {
			subresults = new ArrayList<OperationResult>();
		}
		return subresults;
	}

    /**
     * @return last subresult, or null if there are no subresults.
     */
    public OperationResult getLastSubresult() {
        if (subresults == null || subresults.isEmpty()) {
            return null;
        } else {
            return subresults.get(subresults.size()-1);
        }
    }

    /**
     * @return last subresult status, or null if there are no subresults.
     */
    public OperationResultStatus getLastSubresultStatus() {
        OperationResult last = getLastSubresult();
        return last != null ? last.getStatus() : null;
    }

	public void addSubresult(OperationResult subresult) {
		getSubresults().add(subresult);
	}

	/**
	 * Contains operation status as defined in {@link OperationResultStatus}
	 * 
	 * @return never returns null
	 */
	public OperationResultStatus getStatus() {
		return status;
	}

	/**
	 * Returns true if the result is success.
	 * 
	 * This returns true if the result is absolute success. Presence of partial
	 * failures or warnings fail this test.
	 * 
	 * @return true if the result is success.
	 */
	public boolean isSuccess() {
		return (status == OperationResultStatus.SUCCESS);
	}

	public boolean isWarning() {
		return status == OperationResultStatus.WARNING;
	}

	/**
	 * Returns true if the result is acceptable for further processing.
	 * 
	 * In other words: if there were no fatal errors. Warnings and partial
	 * errors are acceptable. Yet, this test also fails if the operation state
	 * is not known.
	 * 
	 * @return true if the result is acceptable for further processing.
	 */
	public boolean isAcceptable() {
		return (status != OperationResultStatus.FATAL_ERROR);
	}

	public boolean isUnknown() {
		return (status == OperationResultStatus.UNKNOWN);
	}
	
	public boolean isError() {
		return (status == OperationResultStatus.FATAL_ERROR) ||
					(status == OperationResultStatus.PARTIAL_ERROR);
	}


	/**
	 * Computes operation result status based on subtask status and sets an
	 * error message if the status is FATAL_ERROR.
	 * 
	 * @param errorMessage
	 *            error message
	 */
	public void computeStatus(String errorMessage) {
		computeStatus(errorMessage, errorMessage);
	}

	public void computeStatus(String errorMessage, String warnMessage) {
		Validate.notEmpty(errorMessage, "Error message must not be null.");

		// computeStatus sets a message if none is set,
		// therefore we need to check before calling computeStatus
		boolean noMessage = StringUtils.isEmpty(message);
		computeStatus();
		
		switch (status) {
			case FATAL_ERROR:
			case PARTIAL_ERROR:
				if (noMessage) {
					message = errorMessage;
				}
				break;
			case UNKNOWN:
			case WARNING:
			case NOT_APPLICABLE:
				if (noMessage) {
					if (StringUtils.isNotEmpty(warnMessage)) {
						message = warnMessage;
					} else {
						message = errorMessage;
					}
				}
				break;
		}
	}

	/**
	 * Computes operation result status based on subtask status.
	 */
	public void computeStatus() {
		if (getSubresults().isEmpty()) {
			if (status == OperationResultStatus.UNKNOWN) {
				status = OperationResultStatus.SUCCESS;
			}
			return;
		}

		OperationResultStatus newStatus = OperationResultStatus.UNKNOWN;
		boolean allSuccess = true;
		boolean allNotApplicable = true;
		for (OperationResult sub : getSubresults()) {
			if (sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
				allNotApplicable = false;
			}
			if (sub.getStatus() == OperationResultStatus.FATAL_ERROR) {
				status = OperationResultStatus.FATAL_ERROR;
				if (message == null) {
					message = sub.getMessage();
				}
				return;
			}
			if (sub.getStatus() == OperationResultStatus.IN_PROGRESS) {
				status = OperationResultStatus.IN_PROGRESS;
				if (message == null) {
					message = sub.getMessage();
				}
				return;
			}
			if (sub.getStatus() != OperationResultStatus.SUCCESS
					&& sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
				allSuccess = false;
			}
			if (sub.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
				newStatus = OperationResultStatus.PARTIAL_ERROR;
				if (message == null) {
					message = sub.getMessage();
				}
			}
			if (newStatus != OperationResultStatus.PARTIAL_ERROR) {
				if (sub.getStatus() == OperationResultStatus.WARNING) {
					newStatus = OperationResultStatus.WARNING;
					if (message == null) {
						message = sub.getMessage();
					}
				}
			}
		}

		if (allNotApplicable && !getSubresults().isEmpty()) {
			status = OperationResultStatus.NOT_APPLICABLE;
		}
		if (allSuccess && !getSubresults().isEmpty()) {
			status = OperationResultStatus.SUCCESS;
		} else {
			status = newStatus;
		}
	}

	public void recomputeStatus() {
		// Only recompute if there are subresults, otherwise keep original
		// status
		if (subresults != null && !subresults.isEmpty()) {
			computeStatus();
		}
	}
	
	public void recomputeStatus(String message) {
		// Only recompute if there are subresults, otherwise keep original
		// status
		if (subresults != null && !subresults.isEmpty()) {
			computeStatus(message);
		}
	}

	public void recomputeStatus(String errorMessage, String warningMessage) {
		// Only recompute if there are subresults, otherwise keep original
		// status
		if (subresults != null && !subresults.isEmpty()) {
			computeStatus(errorMessage, warningMessage);
		}
	}

	public void recordSuccessIfUnknown() {
		if (isUnknown()) {
			recordSuccess();
		}
	}

	/**
	 * Method returns {@link Map} with operation parameters. Parameters keys are
	 * described in module interface for every operation.
	 * 
	 * @return never returns null
	 */
	public Map<String, Object> getParams() {
		if (params == null) {
			params = new HashMap<String, Object>();
		}
		return params;
	}

	public void addParam(String paramName, Object paramValue) {
		getParams().put(paramName, paramValue);
	}

	public void addParams(String[] names, Object... objects) {
		if (names.length != objects.length) {
			throw new IllegalArgumentException("Bad result parameters size, names '" + names.length
					+ "', objects '" + objects.length + "'.");
		}

		for (int i = 0; i < names.length; i++) {
			addParam(names[i], objects[i]);
		}
	}

	public Map<String, Object> getContext() {
		if (context == null) {
			context = new HashMap<String, Object>();
		}
		return context;
	}

	@SuppressWarnings("unchecked")
	public <T> T getContext(Class<T> type, String contextName) {
		return (T) getContext().get(contextName);
	}

	public void addContext(String contextName, Object value) {
		getContext().put(contextName, value);
	}

	public Map<String, Object> getReturns() {
		if (returns == null) {
			returns = new HashMap<String, Object>();
		}
		return returns;
	}

	public void addReturn(String returnName, Object value) {
		getReturns().put(returnName, value);
	}

	public Object getReturn(String returnName) {
		return getReturns().get(returnName);
	}

	/**
	 * @return Contains random long number, for better searching in logs.
	 */
	public long getToken() {
		if (token == 0) {
			token = TOKEN_COUNT++;
		}
		return token;
	}

	/**
	 * Contains mesage code based on module error catalog.
	 * 
	 * @return Can return null.
	 */
	public String getMessageCode() {
		return messageCode;
	}

	/**
	 * @return Method returns operation result message. Message is required. It
	 *         will be key for translation in admin-gui.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return Method returns message key for translation, can be null.
	 */
	public String getLocalizationMessage() {
		return localizationMessage;
	}

	/**
	 * @return Method returns arguments if needed for localization, can be null.
	 */
	public List<Object> getLocalizationArguments() {
		return localizationArguments;
	}

	/**
	 * @return Method returns operation result exception. Not required, can be
	 *         null.
	 */
	public Throwable getCause() {
		return cause;
	}

	public void recordSuccess() {
		// Success, no message or other explanation is needed.
		status = OperationResultStatus.SUCCESS;
	}

	public void recordFatalError(Throwable cause) {
		recordStatus(OperationResultStatus.FATAL_ERROR, cause.getMessage(), cause);
	}

	public void recordPartialError(Throwable cause) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, cause.getMessage(), cause);
	}

	public void recordWarning(Throwable cause) {
		recordStatus(OperationResultStatus.WARNING, cause.getMessage(), cause);
	}

	public void recordStatus(OperationResultStatus status, Throwable cause) {
		this.status = status;
		this.cause = cause;
		// No other message was given, so use message from the exception
		// not really correct, but better than nothing.
		message = cause.getMessage();
	}

	public void recordFatalError(String message, Throwable cause) {
		recordStatus(OperationResultStatus.FATAL_ERROR, message, cause);
	}

	public void recordPartialError(String message, Throwable cause) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, message, cause);
	}

	public void recordWarning(String message, Throwable cause) {
		recordStatus(OperationResultStatus.WARNING, message, cause);
	}

	public void recordStatus(OperationResultStatus status, String message, Throwable cause) {
		this.status = status;
		this.message = message;
		this.cause = cause;
	}

	public void recordFatalError(String message) {
		recordStatus(OperationResultStatus.FATAL_ERROR, message);
	}

	public void recordPartialError(String message) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, message);
	}

	public void recordWarning(String message) {
		recordStatus(OperationResultStatus.WARNING, message);
	}

	/**
	 * Records result from a common exception type. This automatically
	 * determines status and also sets appropriate message.
	 * 
	 * @param exception
	 *            common exception
	 */
	public void record(CommonException exception) {
		// TODO: switch to a localized message later
		// Exception is a fatal error in this context
		recordFatalError(exception.getOperationResultMessage(), exception);
	}

	public void recordStatus(OperationResultStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	/**
	 * Returns true if result status is UNKNOWN or any of the subresult status
	 * is unknown (recursive).
	 * 
	 * May come handy in tests to check if all the operations fill out the
	 * status as they should.
	 */
	public boolean hasUnknownStatus() {
		if (status == OperationResultStatus.UNKNOWN) {
			return true;
		}
		for (OperationResult subresult : getSubresults()) {
			if (subresult.hasUnknownStatus()) {
				return true;
			}
		}
		return false;
	}

	public void appendDetail(String detailLine) {
		// May be switched to a more structured method later
		details.add(detailLine);
	}

	public List<String> getDetail() {
		return details;
	}

	@Override
	public String toString() {
		return "R(" + operation + " " + status + " " + message + ")";
	}

	public String dump() {
		return dump(true);
	}

	public String dump(boolean withStack) {
		StringBuilder sb = new StringBuilder();
		dumpIndent(sb, 0, withStack);
		return sb.toString();
	}

	private void dumpIndent(StringBuilder sb, int indent, boolean printStackTrace) {
		for (int i = 0; i < indent; i++) {
			sb.append(INDENT_STRING);
		}
		sb.append("*op* ");
		sb.append(operation);
		sb.append(", st: ");
		sb.append(status);
		sb.append(", msg: ");
		sb.append(message);
		sb.append("\n");
		if (cause != null) {
			for (int i = 0; i < indent + 2; i++) {
				sb.append(INDENT_STRING);
			}
			sb.append("[cause]");
			sb.append(cause.getClass().getSimpleName());
			sb.append(":");
			sb.append(cause.getMessage());
			sb.append("\n");
			if (printStackTrace) {
				dumpStackTrace(sb, cause.getStackTrace(), indent + 4);
				dumpInnerCauses(sb, cause.getCause(), indent + 3);
			}
		}

		for (Map.Entry<String, Object> entry : getParams().entrySet()) {
			for (int i = 0; i < indent + 2; i++) {
				sb.append(INDENT_STRING);
			}
			sb.append("[p]");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(dumpEntry(entry.getValue()));
			sb.append("\n");
		}

		for (Map.Entry<String, Object> entry : getContext().entrySet()) {
			for (int i = 0; i < indent + 2; i++) {
				sb.append(INDENT_STRING);
			}
			sb.append("[c]");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(dumpEntry(entry.getValue()));
			sb.append("\n");
		}
		
		for (Map.Entry<String, Object> entry : getReturns().entrySet()) {
			for (int i = 0; i < indent + 2; i++) {
				sb.append(INDENT_STRING);
			}
			sb.append("[r]");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(dumpEntry(entry.getValue()));
			sb.append("\n");
		}

		for (String line : details) {
			for (int i = 0; i < indent + 2; i++) {
				sb.append(INDENT_STRING);
			}
			sb.append("[d]");
			sb.append(line);
			sb.append("\n");
		}

		for (OperationResult sub : getSubresults()) {
			sub.dumpIndent(sb, indent + 1, printStackTrace);
		}
	}

	private String dumpEntry(Object value) {
		if (value instanceof Element) {
			Element element = (Element)value;
			if (SchemaConstants.C_VALUE.equals(DOMUtil.getQName(element))) {
				try {
					String cvalue = null;
					if (value == null) {
						cvalue = "null";
					} else if (value instanceof Element) {
						cvalue = SchemaDebugUtil.prettyPrint(XmlTypeConverter.toJavaValue((Element)value));
					} else {
						cvalue = SchemaDebugUtil.prettyPrint(value);
					}
					return cvalue;
				} catch (Exception e) {
					return "value: "+element.getTextContent();
				}
			}
		}
		return SchemaDebugUtil.prettyPrint(value);
	}

	private void dumpInnerCauses(StringBuilder sb, Throwable innerCause, int indent) {
		if (innerCause == null) {
			return;
		}
		for (int i = 0; i < indent; i++) {
			sb.append(INDENT_STRING);
		}
		sb.append("Caused by ");
		sb.append(innerCause.getClass().getName());
		sb.append(": ");
		sb.append(innerCause.getMessage());
		sb.append("\n");
		dumpStackTrace(sb, innerCause.getStackTrace(), indent + 1);
		dumpInnerCauses(sb, innerCause.getCause(), indent);
	}

	private static void dumpStackTrace(StringBuilder sb, StackTraceElement[] stackTrace, int indent) {
		for (int i = 0; i < stackTrace.length; i++) {
			for (int j = 0; j < indent; j++) {
				sb.append(INDENT_STRING);
			}
			StackTraceElement element = stackTrace[i];
			sb.append(element.toString());
			sb.append("\n");
		}
	}

	public static OperationResult createOperationResult(OperationResultType result) {
		Validate.notNull(result, "Result type must not be null.");

		Map<String, Object> params = null;
		if (result.getParams() != null) {
			params = new HashMap<String, Object>();
			for (EntryType entry : result.getParams().getEntry()) {
				params.put(entry.getKey(), entry.getAny());
			}
		}

		List<OperationResult> subresults = null;
		if (!result.getPartialResults().isEmpty()) {
			subresults = new ArrayList<OperationResult>();
			for (OperationResultType subResult : result.getPartialResults()) {
				subresults.add(createOperationResult(subResult));
			}
		}

		LocalizedMessageType message = result.getLocalizedMessage();
		String localizedMessage = message == null ? null : message.getKey();
		List<Object> localizedArguments = message == null ? null : message.getArgument();

		return new OperationResult(result.getOperation(), params,
				OperationResultStatus.parseStatusType(result.getStatus()), result.getToken(),
				result.getMessageCode(), result.getMessage(), localizedMessage, localizedArguments, null,
				subresults);
	}

	public OperationResultType createOperationResultType() {
		return createOperationResultType(this);
	}

	private OperationResultType createOperationResultType(OperationResult opResult) {
		OperationResultType result = new OperationResultType();
		result.setToken(opResult.getToken());
		result.setStatus(OperationResultStatus.createStatusType(opResult.getStatus()));
		result.setOperation(opResult.getOperation());
		result.setMessage(opResult.getMessage());
		result.setMessageCode(opResult.getMessageCode());

		if (opResult.getCause() != null || !opResult.details.isEmpty()) {
			StringBuilder detailsb = new StringBuilder();

			// Record text messages in details (if present)
			if (opResult.details.isEmpty()) {
				for (String line : opResult.details) {
					detailsb.append(line);
					detailsb.append("\n");
				}
			}

			// Record stack trace in details if a cause is present
			if (opResult.getCause() != null) {
				Throwable ex = opResult.getCause();
				detailsb.append(ex.getClass().getName());
				detailsb.append(": ");
				detailsb.append(ex.getMessage());
				detailsb.append("\n");
				StackTraceElement[] stackTrace = ex.getStackTrace();
				for (int i = 0; i < stackTrace.length; i++) {
					detailsb.append(stackTrace[i].toString());
					detailsb.append("\n");
				}
			}

			result.setDetails(details.toString());
		}

		if (StringUtils.isNotEmpty(opResult.getLocalizationMessage())) {
			LocalizedMessageType message = new LocalizedMessageType();
			message.setKey(opResult.getLocalizationMessage());
			if (opResult.getLocalizationArguments() != null) {
				message.getArgument().addAll(opResult.getLocalizationArguments());
			}
			result.setLocalizedMessage(message);
		}

		Set<Entry<String, Object>> params = opResult.getParams().entrySet();
		if (!params.isEmpty()) {
			ParamsType paramsType = new ParamsType();
			result.setParams(paramsType);

			for (Entry<String, Object> entry : params) {
				paramsType.getEntry().add(createEntryElement(entry.getKey(),entry.getValue()));
			}
		}

		for (OperationResult subResult : opResult.getSubresults()) {
			result.getPartialResults().add(opResult.createOperationResultType(subResult));
		}

		return result;
	}

	/**
	 * Temporary workaround, brutally hacked -- so that the conversion 
	 * of OperationResult into OperationResultType 'somehow' works, at least to the point
	 * where when we:
	 * - have OR1
	 * - serialize it into ORT1
	 * - then deserialize into OR2
	 * - serialize again into ORT2
	 * so we get ORT1.equals(ORT2) - at least in our simple test case :)
	 * 
	 * FIXME: this should be definitely reworked
	 * 
	 * @param entry
	 * @return
	 */
	private EntryType createEntryElement(String key, Object value) {
		EntryType entryType = new EntryType();
		entryType.setKey(key);
		if (value != null) {
			Document doc = DOMUtil.getDocument();
			if (value instanceof ObjectType && ((ObjectType)value).getOid() != null) {
				// Store only reference on the OID. This is faster and getObject can be used to retrieve
				// the object if needed. Although is does not provide 100% accuracy, it is a good tradeoff.
				setObjectReferenceEntry(entryType, ((ObjectType)value));
			// these values should be put 'as they are', in order to be deserialized into themselves
			} else if (value instanceof String || value instanceof Integer || value instanceof Long) {
				entryType.setAny(new JAXBElement<Object>(SchemaConstants.C_VALUE, Object.class, value));
			} else if (XmlTypeConverter.canConvert(value.getClass())) {
				try {
					entryType.setAny(XmlTypeConverter.toXsdElement(value, SchemaConstants.C_VALUE, doc, true));
				} catch (SchemaException e) {
					LOGGER.error("Cannot convert value {} to XML: {}",value,e.getMessage());
					setUnknownJavaObjectEntry(entryType, value);
				}
			} else if (value instanceof Element || value instanceof JAXBElement<?>) {
				entryType.setAny(value);
			// FIXME: this is really bad code ... it means that 'our' JAXB object should be put as is
			} else if ("com.evolveum.midpoint.xml.ns._public.common.common_1".equals(value.getClass().getPackage().getName())) {
				Object o = new JAXBElement<Object>(SchemaConstants.C_VALUE, Object.class, value);
				entryType.setAny(o);
			} else {
				setUnknownJavaObjectEntry(entryType, value);
			}
		}
		return entryType;
	}

	private void setObjectReferenceEntry(EntryType entryType, ObjectType objectType) {
		ObjectReferenceType objRefType = new ObjectReferenceType();
		objRefType.setOid(objectType.getOid());
		ObjectTypes type = ObjectTypes.getObjectType(objectType.getClass());
		if (type != null) {
			objRefType.setType(type.getTypeQName());
		}
		JAXBElement<ObjectReferenceType> element = new JAXBElement<ObjectReferenceType>(
				SchemaConstants.C_OBJECT_REF, ObjectReferenceType.class, objRefType);
		entryType.setAny(element);
	}

	private void setUnknownJavaObjectEntry(EntryType entryType, Object value) {
		UnknownJavaObjectType ujo = new UnknownJavaObjectType();
		ujo.setClazz(value.getClass().getName());
		ujo.setToString(value.toString());
		entryType.setAny(new ObjectFactory().createUnknownJavaObject(ujo));
	}

}
