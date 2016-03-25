package org.informiz.populate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import org.informiz.executor.CypherExecutor;
import org.informiz.executor.JdbcCypherExecutor;

public class GraphImporter {
	public static final String GRAPH_URL = "graph.url";
	public static final String SCRIPT_FILE_NAME = "script.filename";
	
	private final CypherExecutor cypher;
	private Properties props;
	
	public GraphImporter(Properties props) {
		this.props = props;
		cypher = new JdbcCypherExecutor(props);
	}
	
	public void initGraph() {
		try (Stream<String> stream = Files.lines(Paths.get(props.getProperty(SCRIPT_FILE_NAME)))) {

			stream.forEach(cypher::update);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

	public void close() {
		cypher.shutdown();		
	}

	public static void main(String[] args) {
		GraphImporter importer = null;
		try (FileInputStream in = new FileInputStream(args[0])) {
			Properties props = new Properties();
			props.load(in);
			importer = new GraphImporter(props);
			importer.initGraph();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			if (importer != null) importer.close();
		}
	}

}
