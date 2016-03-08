package org.informiz.landscape;

import static org.neo4j.helpers.collection.MapUtil.map;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.informiz.executor.JdbcCypherExecutor;
import org.informiz.executor.QueryResultIterator;
import org.junit.Assert;
import org.junit.Test;

import mockit.Expectations;
import mockit.Mocked;

public final class LandscapeServiceTest {

	@Mocked JdbcCypherExecutor cypher;
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGraphCreation() throws SQLException {
		
		final Map<String, Object> row = map("informi", map("id", 123), "other", map("id", 456),
				"r", map("description", "connected"));
		
		final QueryResultIterator res = new QueryResultIterator(null, null) {

			Iterator<Map<String, Object>> inner = Arrays.asList(row).iterator();
			@Override
			public boolean hasNext() {
				return inner.hasNext();
			}

			@Override
			public Map<String, Object> next() {
				return inner.next();
			}

			@Override
			public void close() {
				// no-op
				
			}			
		};

		new Expectations() {{
			new JdbcCypherExecutor(anyString);

			// query will return one row
			cypher.query(anyString, map("1",123, "2",10));
			      result = res;
	   }};

		LandscapeService service = null;
		try {
			service = new LandscapeService("localhost");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception while trying to init service: " + e.getMessage());
		}
		Map<String, Object> result = service.graph(123, 10);

		Map<String, Object> graph = (Map<String, Object>)result.get("graph");
		List<Map<String, Object>> nodes = (List<Map<String, Object>>)graph.get("nodes");
		List <Map<String, Object>> edges = (List<Map<String, Object>>)graph.get("edges");
		List<String> labels = (List<String>)result.get("labels");
		
		Assert.assertTrue("Graph should contain node 123 as root", nodes.contains(map("id", 123, "type", 
				"Informi", "root", true)));
		Assert.assertTrue("Graph should contain node 456", nodes.contains(map("id", 456, 
				"type", "Informi")));
		Assert.assertEquals("Graph should contain two nodes", 2, nodes.size());

		Assert.assertTrue("Graph should contain edge 123->456", edges.contains(map("source", 123, "target", 456, 
        		"caption", "connected", "type", "Relation")));
		Assert.assertEquals("Graph should contain one edge", 1, edges.size());
		
		Assert.assertArrayEquals("Labels should be 'Informi'", new String[]{"Informi"}, labels.toArray());

	
	}
}
