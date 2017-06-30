package com.evolveum.midpoint.report.impl;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;


public class ReportNodeUtils {

    private static final Trace LOGGER = TraceManager.getTrace(ReportNodeUtils.class);
    private static final String SPACE = "%20";
    private static final String HEADER_USERAGENT = "mp-cluser-peer-client";
    private static final String ENDPOINTURIPATH = "midpoint/report";
    private static String URLENCODING = "UTF-8";
    private static String FILENAMEPARAMETER = "fname";
    private static final Integer DEFAULTPORT = 80;

    public InputStream executeOperation(String host, String fileName, String intraClusterHttpUrlPattern, String operation) throws CommunicationException, SecurityViolationException, ObjectNotFoundException, ConfigurationException, IOException {
        fileName = fileName.replaceAll("\\s", SPACE);
        StringBuilder path = new StringBuilder(ENDPOINTURIPATH);
        InputStream inputStream = null;
        InputStream entityContent = null;
        LOGGER.trace("About to initiate connection with {}", host);
        try {
            if (intraClusterHttpUrlPattern != null && !(intraClusterHttpUrlPattern.isEmpty())) {
                LOGGER.trace("The cluster uri pattern: {} ", intraClusterHttpUrlPattern.toString());
                String[] splitted = intraClusterHttpUrlPattern.split("/");
                if (!(splitted.length > 3)) { // https://$host/midpoint
                    StringBuilder errorBuilder = new StringBuilder("Non valid IntraClusterHttpUrlPattern parameter value: ").append(intraClusterHttpUrlPattern);
                    throw new ConfigurationException(errorBuilder.toString());
                }
                URIBuilder ubilder = new URIBuilder();
                String scheme = splitted[0].substring(0, splitted[0].length() - 1);
                ubilder.setScheme(scheme).setHost(host);
                String hostPart = splitted[2];

                String[] hostAndPort = hostPart.split("\\:");//host:port
                if (hostAndPort.length > 1) {
                    String port = hostAndPort[1];
                    if (NumberUtils.isDigits(port)) {
                        ubilder.setPort(Integer.parseInt(port));
                    }
                } else {
                    ubilder.setPort(DEFAULTPORT);
                }
                ubilder.setPath(path.toString()).setParameter(FILENAMEPARAMETER,
                        fileName);
                URI requestUri = ubilder.build();
                fileName = URLDecoder.decode(fileName, URLENCODING);
                LOGGER.debug("Sending request to the following uri: {} ", requestUri.toString());
                HttpRequestBase httpRequest = buildHttpRequest(operation);
                httpRequest.setURI(requestUri);
                httpRequest.setHeader("User-Agent", HEADER_USERAGENT);
                HttpClient client = HttpClientBuilder.create().build();
                try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpRequest)) {
                    HttpEntity entity = response.getEntity();
                    Integer statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == HttpStatus.SC_OK) {
                        LOGGER.info("Response OK, the file successfully returned by the cluster peer. ");
                        if (entity != null) {
                            entityContent = entity.getContent();
                            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = entityContent.read(buffer)) > -1) {
                                arrayOutputStream.write(buffer, 0, len);
                            }
                            arrayOutputStream.flush();
                            inputStream = new ByteArrayInputStream(arrayOutputStream.toByteArray());
                        }
                    } else if (statusCode == HttpStatus.SC_NO_CONTENT) {
                        if (HttpDelete.METHOD_NAME.equals(operation)) {
                            LOGGER.info("Deletion of the file {} was successful.", fileName);
                        }

                    } else if (statusCode == HttpStatus.SC_FORBIDDEN) {

                        StringBuilder errorBuilder = new StringBuilder("The access to the report ").append(fileName)
                                .append(" is forbidden.");
                        LOGGER.error("The access to the report with the name {} is forbidden.", fileName);

                        throw new SecurityViolationException(errorBuilder.toString());
                    } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                        StringBuilder errorBuilder = new StringBuilder("The report file ").append(fileName)
                                .append(" was not found on the originating nodes filesystem.");
                        throw new ObjectNotFoundException(errorBuilder.toString());
                    }
                } catch (ClientProtocolException e) {

                    StringBuilder errorBuilder = new StringBuilder("An exception with the communication protocol has occurred during a query to the cluster peer. ")
                            .append(e.getLocalizedMessage());
                    throw new CommunicationException(errorBuilder.toString());
                }
            }
        } catch (URISyntaxException e1) {
            StringBuilder errorBuilder = new StringBuilder("Invalid uri syntax: ").append(e1.getLocalizedMessage());
            throw new CommunicationException(errorBuilder.toString());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Unhandled exception when listing nodes");
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing nodes", e);
        } finally {
            IOUtils.closeQuietly(entityContent);
        }
        return inputStream;
    }

    public HttpRequestBase buildHttpRequest(String typeOfRequest) {
        HttpRequestBase httpRequest;

        if (HttpDelete.METHOD_NAME.equals(typeOfRequest)) {
            httpRequest = new HttpDelete();

        } else {
            httpRequest = new HttpGet();
        }
        return httpRequest;
    }

}
