package org.informiz.export;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.informiz.es.InformizESIndexManager;
import org.informiz.executor.CypherExecutor;
import org.informiz.executor.JdbcCypherExecutor;
import org.informiz.executor.QueryResultIterator;
import org.informiz.flume.GraphEventExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InformiExportManager {
	public static final String DEFAULT_FLUME_HOST = "localhost";
	public static final String DEFAULT_FLUME_PORT = "44444";

	public static final String FLUME_HOST_KEY = "flume.host";
	public static final String FLUME_PORT_KEY = "flume.port";

	static Logger logger = LoggerFactory.getLogger(InformiExportManager.class);
	
	private static final String INFORMIZ_QUERY = "MATCH (node:Informi) RETURN node";

	private final CypherExecutor cypher;
	private final GraphEventExporter eventExporter;
	
	public InformiExportManager(Properties props) {
		cypher = new JdbcCypherExecutor(props);
		String flumeHost = props.getOrDefault(FLUME_HOST_KEY, DEFAULT_FLUME_HOST).toString();
		int flumePort = Integer.valueOf(props.getOrDefault(FLUME_PORT_KEY, DEFAULT_FLUME_PORT).toString());
		
		eventExporter = new GraphEventExporter(flumeHost, flumePort);
		InformizESIndexManager.init(props);
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
		
		try (FileInputStream in = new FileInputStream(args[0])) {
			Properties props = new Properties();
			props.load(in);
			exporter = new InformiExportManager(props);
			exporter.exportInformiz();
		} catch (Exception e) {
			logger.error("Exception while trying to initialize landscape service: " + e.getMessage(), e);
			System.exit(1);
		} finally {
			if (exporter != null) exporter.close();
		}
	}

}
