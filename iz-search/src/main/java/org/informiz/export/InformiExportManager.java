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
	
	public InformiExportManager(String graphUrl, String graphUser, String graphPass, String flumeHost, int flumePort) {
		cypher = new JdbcCypherExecutor(graphUrl, graphUser, graphPass);
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

	/**
	 * @param args - 
	 *    args[0] - Neo4j cluster URL, e.g http://localhost:7474 
	 *    args[1] - Neo4j user, e.g neo4j 
	 *    args[2] - Neo4j password, e.g neo4j 
	 *    args[3] - Flume hostname, e.g localhost
	 *    args[4] - Flume port, e.g 44444
	 */
	public static void main(String[] args) {
		InformiExportManager exporter = null;
		try {
			exporter = new InformiExportManager(args[0], args[1], args[2], args[3], Integer.valueOf(args[4]));
			exporter.exportInformiz();
		} finally {
			if (exporter != null) exporter.close();
		}
	}

}
