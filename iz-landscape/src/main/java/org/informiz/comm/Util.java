package org.informiz.comm;

/**
 * @author Nira Amit
 */
public class Util {
	public static final String QUEUE_HOST_KEY = "rabbit.host";
    public static final String LANDSCAPE_QUEUE_NAME = "lands_queue";
	public static final String DEFAULT_QUEUE_HOST = "localhost";
    
    public static String createJsonErrorResp(String err) {
    	return "{\"errors\":\"" + err + "\"}";
    }
}
