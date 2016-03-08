package org.informiz.util;

/**
 * @author Nira Amit
 */
public class Util {
    public static final String DEFAULT_DB_URL = "http://localhost:7474";
    public static final String LANDSCAPE_QUEUE_NAME = "lands_queue";
    
    public static int getWebPort() {
        String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) {
            return 8080;
        }
        return Integer.parseInt(webPort);
    }

    public static String getNeo4jUrl() {
        String urlVar = System.getenv("NEO4J_REST_URL");
        if (urlVar==null) urlVar = "NEO4J_URL";
        String url =  System.getenv(urlVar);
        if(url == null || url.isEmpty()) {
            return DEFAULT_DB_URL;
        }
        return url;
    }
    
    public static String createJsonErrorResp(String err) {
    	return "{\"errors\":\"" + err + "\"}";
    }
}
