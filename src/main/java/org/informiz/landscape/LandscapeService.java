package org.informiz.landscape;

import static org.neo4j.helpers.collection.MapUtil.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.informiz.executor.CypherExecutor;
import org.informiz.executor.JdbcCypherExecutor;
import org.informiz.util.LandscapeRequest;
import org.informiz.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * @author Nira Amit
 */
public class LandscapeService {

	static Logger logger = LoggerFactory.getLogger(LandscapeService.class);

    private final CypherExecutor cypher;
    Connection connection = null;
    Channel channel = null;

    public LandscapeService(String hostname) throws Exception {
    	this(hostname, null, null);
    }

    public LandscapeService(String hostname, String user, String pass) throws Exception {
    	cypher = createCypherExecutor(Util.getNeo4jUrl(),user,pass);
    	ConnectionFactory factory = new ConnectionFactory();
    	factory.setHost(hostname);

    	connection = factory.newConnection();
    	channel = connection.createChannel();

    	channel.queueDeclare(Util.LANDSCAPE_QUEUE_NAME, false, false, false, null);

    	channel.basicQos(1);

    	QueueingConsumer consumer = new QueueingConsumer(channel);
    	channel.basicConsume(Util.LANDSCAPE_QUEUE_NAME, false, consumer);
    }

    private CypherExecutor createCypherExecutor(String uri, String user, String pass) {
        if (user != null) {
            return new JdbcCypherExecutor(uri,user,pass);
        }
        return new JdbcCypherExecutor(uri);
    }
    
    public void process() {
    	Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		logger.info("Landscape service ready");
		QueueingConsumer consumer = new QueueingConsumer(channel);
    	try {
    		channel.basicConsume(Util.LANDSCAPE_QUEUE_NAME, false, consumer);
    		while (true) {
    			Map<String, Object> response = null;

    			QueueingConsumer.Delivery delivery = consumer.nextDelivery();

    			BasicProperties props = delivery.getProperties();
    			BasicProperties replyProps = new BasicProperties
    					.Builder()
    					.correlationId(props.getCorrelationId())
    					.build();

    			try {
    				String message = new String(delivery.getBody(),"UTF-8");
    				LandscapeRequest req = gson.fromJson(message, LandscapeRequest.class);

    				response = graph(req.getInformiId(), req.getLimit());
    			}
    			catch (Exception e){
    				logger.error("Error while attempting to retrieve landscape: " + e.getMessage(), e);
    				response = map("errors", "Failed to retrieve landscape: " + e.getMessage());
    			}
    			finally {  
    				String asJson = gson.toJson(response);
    				channel.basicPublish( "", props.getReplyTo(), replyProps, asJson.getBytes("UTF-8"));
    				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    			}
    		}
    	}
    	catch  (Exception e) {
    		e.printStackTrace();
    	}
    	finally {
    		close();
    	}      		      
    }

	public void close() {
		if (connection != null) {
			try {
				connection.close();
			}
			catch (Exception ignore) {}
		}
	}

    @SuppressWarnings("unchecked")
    public Map<String, Object> graph(int informiId, int limit) {
        Iterator<Map<String,Object>> result = cypher.query(
        		"MATCH (informi:Informi)-[r]->(other:Informi) "
        		+ "WHERE informi.id={1} OR other.id={1} "
        		+ "RETURN informi, r, other "
        		+ "LIMIT {2}", map("1",informiId, "2",limit));
        List<Map<String, Object>> nodes = Lists.newArrayList();
        List <Map<String, Object>> rels = Lists.newArrayList();
        List<Integer> ids = new ArrayList<Integer>();

        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Map<String, Object> source = (Map<String, Object>)row.get("informi");
			addNode(source, informiId, ids, nodes);
            Map<String, Object> target = (Map<String, Object>)row.get("other");
			addNode(target, informiId, ids, nodes);
            Map<String, Object> rel = (Map<String, Object>)row.get("r");
            rels.add(map("source", source.get("id"), "target", target.get("id"), 
            		"caption", rel.get("description"), "type", "Relation"));
        }

        return map("graph", map("nodes", nodes, "edges", rels), "labels", Arrays.asList("Informi"), "errors", "");
    }

	private void addNode(Map<String, Object> node, int rootId, List<Integer> ids, List<Map<String, Object>> nodes) {
		Map<String, Object> informi = Maps.newHashMap(node);
		Integer id = (Integer)informi.get("id");
		if (! ids.contains(id)) {
			ids.add(id);
			nodes.add(informi);
			informi.put("type", "Informi");
			if (id == rootId) {
				informi.put("root", true);
			}
		}
	}
	
	public static void main(String[] args) {
		LandscapeService service = null;
		String username = null;
		String password = null;
		String hostname = "localhost";
		if (args.length > 0) {
			username = args[0];
		}
		if (args.length > 1) {
			password = args[1];
		}
		if (args.length > 2) {
			hostname = args[2];
		}
        try {
        	service = new LandscapeService(hostname, username, password);
			service.process();
		} catch (Exception e) {
			logger.error("Exception while trying to initialize landscape endpoint: " + e.getMessage(), e);
    		if (service != null) {
				service.close();
    		}
		}
	}
}
