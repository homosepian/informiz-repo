package org.informiz.landscape;

import static spark.Spark.get;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.servlet.SparkApplication;

public class LandscapeRoutes implements SparkApplication {
	Logger logger = LoggerFactory.getLogger(SparkApplication.class);

    private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private LandscapeService service;

    public LandscapeRoutes(LandscapeService service) {
        this.service = service;
    }
    
    public void init() {
    	
        get("/graph", new Route() {
        	
            /* 
             * Return a landscape graph for the informi given in the URL.
             * e.g http://server.com/graph?informi=123
             * 
             * @see spark.Route#handle(spark.Request, spark.Response)
             */
            public Object handle(Request request, Response response) {
            	int informi = -1;
                try {
					informi = Integer.valueOf(request.queryParams("informi"));
				} catch (NumberFormatException e) {
            		throw new IllegalArgumentException("Invalid informi id " + request.queryParams("informi"));
				}
                int limit = 30;
                if (request.queryParams("limit") != null) {
                	try {
						limit = Integer.valueOf(request.queryParams("limit"));
					} catch (NumberFormatException e) {
						logger.warn("Query limit parameter not a number " + request.queryParams("limit"));
					}
                }
                response.header("Content-Type", "application/json");
                return gson.toJson(service.graph(informi, limit));
            }
        });

        // Currently allowed headers and methods
        Spark.options("/*", new Route() {
            public Object handle(Request request, Response response) {
        	 
    	        String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
    	        if (accessControlRequestHeaders != null) {
    	            response.header("Access-Control-Allow-Headers", "Accept, Content-Type");
    	        }
    	     
    	        String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
    	        if(accessControlRequestMethod != null){
    	        	response.header("Access-Control-Allow-Methods", "OPTIONS, GET");
    	        }
    	     
    	        return "OK";
            }
        });
         
        // Allows cross-origin requests, only the database itself is secured
        Spark.before(new Filter()  {
            public void handle(Request request, Response response) {
            	response.header("Access-Control-Allow-Origin", "*");
            }
        });

    }
}
