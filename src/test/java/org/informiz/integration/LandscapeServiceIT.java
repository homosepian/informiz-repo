package org.informiz.integration;

import static spark.Spark.port;
import static spark.Spark.stop;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.informiz.landscape.LandscapeRoutes;
import org.informiz.landscape.LandscapeService;
import org.informiz.util.Util;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.internal.ArrayComparisonFailure;

import static org.neo4j.helpers.collection.MapUtil.map;

import com.google.gson.Gson;

/**
 * In order to run these tests you need:
 * - A running neo4j server 
 *   - Configured with user/password (null credentials fail the tests)
 *   - With expected data 
 *   	- change the "file:///path/to/xxx.csv" in data.sql to point to the csv files in your local environment
 *   	- run the commands on your neo4j server 
 * - Environment variables NEO4J_USER and NEO4J_PASSWORD containing the credentials, available for the test. I.e
 *   - If you're running from eclipse, create the variables in the "Run as -> Run Configurations... -> Environment"
 *   - If you're running maven from command line 
 *     - On Linux export those variables in ~/.profile (and then "source ~/.profile")
 *     - On Windows create the variables in your system's advanced settings
 * - By default the app connects to neo4j on localhost:7474. You can change these settings by defining additional
 *   environment variables PORT and NEO4J_URL.
 *   
 * @author Nira Amit
 * 
 */
@Category(IntegrationTest.class)
public final class LandscapeServiceIT {
	LandscapeService service = null;

	@Before
	public void initService() {
		port(Util.getWebPort());
        String username = System.getenv("NEO4J_USER");
        String password = System.getenv("NEO4J_PASSWORD");
        try {
			new LandscapeService(Util.getNeo4jUrl(), username, password).process();
	        new LandscapeRoutes("localhost").init();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception while trying to init services: " + e.getMessage());
		}
	}
	
	@After
	public void stopService() {
		stop();
	}
	
	@Test
	public void getLandscapeFromService() {
        Map<String, Object> result = service.graph(869, 10);
		verifyResult(result);
	}

	@Test
	public void getLandscapeFromREST() {
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet("http://localhost:8080/graph?informi=869");
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");

            Gson gson = new Gson();
            LandscapeResponse landscape = gson.fromJson(json, LandscapeResponse.class);

            verifyResult(map("graph",landscape.getGraph(), "labels", landscape.getLabels()));

        } catch (IOException ex) {
        	Assert.fail("Could not get landscape from REST endpoint: " + ex.getMessage());
        }
    }

	@SuppressWarnings("unchecked")
	private void verifyResult(Map<String, Object> result) throws ArrayComparisonFailure {
		Map<String, Object> graph = (Map<String, Object>)result.get("graph");
		List<Map<String, Object>> nodes = (List<Map<String, Object>>)graph.get("nodes");
		List <Map<String, Object>> edges = (List<Map<String, Object>>)graph.get("edges");
		List<String> labels = (List<String>)result.get("labels");
		
		Assert.assertEquals("Graph should contain five nodes", 5, nodes.size());
		Assert.assertEquals("Graph should contain four edges", 4, edges.size());
		Assert.assertArrayEquals("Labels should be 'Informi'", new String[]{"Informi"}, labels.toArray());
	}

    public class LandscapeResponse{
    	private Map<String, List<Map<String, Object>>> graph;
    	private List<String> labels;
    	private String errors;

        public Map<String, List<Map<String, Object>>> getGraph() {
            return graph;
        }

        public List<String> getLabels() {
            return labels;
        }
        
        public String getErrors() {
            return errors;
        }
    }

}
