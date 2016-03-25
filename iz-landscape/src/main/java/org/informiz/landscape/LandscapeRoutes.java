package org.informiz.landscape;

import static spark.Spark.get;
import static spark.Spark.port;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.UUID;

import org.informiz.comm.LandscapeRequest;
import static org.informiz.comm.Util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.servlet.SparkApplication;

public class LandscapeRoutes implements SparkApplication {
	
	public static final String DEFAULT_WEB_PORT = "8080";
	public static final String WEB_PORT_KEY = "web.port";
	
	public static final int DEFAULT_SIZE_LIMIT = 30;

	static Logger logger = LoggerFactory.getLogger(LandscapeRoutes.class);

    private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private Connection connection;
    private Channel channel;
    private String replyQueueName;
    private QueueingConsumer consumer;

    public LandscapeRoutes(Properties props) throws Exception{
		String hostname = props.getOrDefault(QUEUE_HOST_KEY, DEFAULT_QUEUE_HOST).toString();
		
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostname);
        connection = factory.newConnection();
        channel = connection.createChannel();

        replyQueueName = channel.queueDeclare().getQueue();
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);
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
					logger.error("Invalid informi id " + request.queryParams("informi"));
					return createJsonErrorResp("Invalid informi id"); // don't return the param in case it's malicious
				}
                int limit = DEFAULT_SIZE_LIMIT;
                if (request.queryParams("limit") != null) {
                	try {
						limit = Integer.valueOf(request.queryParams("limit"));
					} catch (NumberFormatException e) {
						logger.warn("Query limit parameter not a number " + request.queryParams("limit"));
					}
                }
                response.header("Content-Type", "application/json");
                String landscape = getLandscape(informi, limit);
				return landscape;
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
    
	/**
	 * Returns a JSON representation of the landscape around the given informi.
	 * @param informiId - The informi's ID
	 * @param maxSize - The maximum number of nodes in the landscape graph
	 * @return - A JSON representation of the landscape, or of an error if one occurred
	 */
	public String getLandscape(int informiId, int maxSize) {
		String response = null;
		try {
			String corrId = UUID.randomUUID().toString() + 
					"_" + informiId + "_" + maxSize; // id collision only if query is the same 

			BasicProperties props = new BasicProperties
					.Builder()
					.correlationId(corrId)
					.replyTo(replyQueueName)
					.build();

			LandscapeRequest req = new LandscapeRequest(informiId, maxSize);
			String body = gson.toJson(req);
			channel.basicPublish("", LANDSCAPE_QUEUE_NAME, props, body.getBytes("UTF-8"));

			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				if (delivery.getProperties().getCorrelationId().equals(corrId)) {
					response = new String(delivery.getBody(),"UTF-8");
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Error while attempting to retrieve a " + maxSize + " node landscape for informi " + informiId + " : " + e.getMessage(), e);
			response = createJsonErrorResp("Failed to retrieve landscape: " + e.getMessage());
		}

		return response;
	}

    public void close() {
		try {
		      connection.close();
		} catch (Exception ignore) {}
    }
    
    public static void main(String[] args) {
    	LandscapeRoutes routes = null;
    	
    	try (FileInputStream in = new FileInputStream(args[0])) {
			Properties props = new Properties();
			props.load(in);
			
			int portNum = Integer.valueOf(props.getOrDefault(WEB_PORT_KEY, DEFAULT_WEB_PORT).toString());

			port(portNum);
			routes = new LandscapeRoutes(props);
			routes.init();
    	} catch (Exception e) {
			logger.error("Exception while trying to initialize landscape endpoint: " + e.getMessage(), e);
			if (routes != null) {
				routes.close();
			}
			System.exit(1);
		}
    }

}
