package org.informiz.landscape;

import static spark.Spark.port;

import org.informiz.util.Util;

/**
 * @author Nira Amit
 */
public class LandscapeServer {

    public static void main(String[] args) {
        port(Util.getWebPort());
        String username = null;
        String password = null;
        if (args.length > 0) {
        	username = args[0];
        }
        if (args.length > 1) {
        	password = args[1];
        }
        final LandscapeService service = new LandscapeService(Util.getNeo4jUrl(), username, password);
        new LandscapeRoutes(service).init();
    }
}
