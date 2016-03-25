package org.informiz.executor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nira Amit
 */
public class JdbcCypherExecutor implements CypherExecutor {

	public static final String DEFAULT_DB_HOST = "localhost";
	public static final String DEFAULT_DB_PORT = "7474";
	
	public static final String APP_DB_USER_KEY = "neo4j.user";
	public static final String APP_DB_PASS_KEY = "neo4j.pass";
	public static final String APP_DB_HOST_KEY = "graph.host";
	public static final String APP_DB_PORT_KEY = "graph.port";

	static Logger logger = LoggerFactory.getLogger(JdbcCypherExecutor.class);

	private final Connection conn;

	
    public JdbcCypherExecutor(Properties appProps) {
        try {
        	final Properties props = new Properties();
            props.put("user", appProps.getOrDefault(APP_DB_USER_KEY, ""));
            props.put("password", appProps.getOrDefault(APP_DB_PASS_KEY, ""));
            String host = appProps.getOrDefault(APP_DB_HOST_KEY, DEFAULT_DB_HOST).toString();
            String port = appProps.getOrDefault(APP_DB_PORT_KEY, DEFAULT_DB_PORT).toString();
            String url = "jdbc:neo4j://" + host + ":" + port;
            conn = DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public QueryResultIterator query(String query, Map<String, Object> params) {
        try {
            final PreparedStatement statement = conn.prepareStatement(query);
            setParameters(statement, params);
            final ResultSet result = statement.executeQuery();
            return new QueryResultIterator(statement, result);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setParameters(PreparedStatement statement, Map<String, Object> params) throws SQLException {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            int index = Integer.parseInt(entry.getKey());
            statement.setObject(index, entry.getValue());
        }
    }
    
    @Override
    public void update(String statement) {
        try {
        	conn.prepareStatement(statement).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public void shutdown() {
		try {
			conn.close();
		} catch (SQLException e) {
			logger.warn("Failed to close connection", e);
		}
		
	}
}
