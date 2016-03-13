package org.informiz.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.informiz.es.InformizESIndexManager;
import org.informiz.executor.CypherExecutor;
import org.informiz.executor.JdbcCypherExecutor;
import org.informiz.executor.QueryResultIterator;
import org.informiz.flume.GraphEventExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InformiExportManager {
	static Logger logger = LoggerFactory.getLogger(InformiExportManager.class);
	
	private static final String INFORMIZ_QUERY = "MATCH (node:Informi) RETURN node";

	private final CypherExecutor cypher;
	private final GraphEventExporter eventExporter;
	
	public InformiExportManager(String graphUser, String graphPass, String flumeHost, int flumePort) {
		// TODO get Neo4j url from input/properties/environment
		cypher = new JdbcCypherExecutor("http://localhost:7474", graphUser, graphPass);
		eventExporter = new GraphEventExporter(flumeHost, flumePort);
		InformizESIndexManager.init();
	}
	
	@SuppressWarnings("unchecked")
	public void exportInformiz() {
		try(QueryResultIterator result = cypher.query(INFORMIZ_QUERY, new HashMap<String, Object>())) {
			List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();

			while (result.hasNext()) {
				Map<String, Object> row = result.next();
				nodes.add((Map<String, Object>)row.get("node"));
			}

			eventExporter.sendNodesToFlume(nodes);
		}
	}
	
	public void close() {
		eventExporter.close();
		cypher.shutdown();		
	}

	public static void main(String[] args) {
		InformiExportManager exporter = null;
		try {
			// TODO - for testing, to remove, read user\pass from environment variable
			exporter = new InformiExportManager("neo4j", "neo4j", "localhost", 44444);
			exporter.exportInformiz();
		} finally {
			if (exporter != null) exporter.close();
		}
	}

}
