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

	static Logger logger = LoggerFactory.getLogger(JdbcCypherExecutor.class);

	private final Connection conn;

    public JdbcCypherExecutor(String url) {
        this(url,null,null);
    }
    
    public JdbcCypherExecutor(String url,String username, String password) {
        try {
        	final Properties props = new Properties();
            props.put("user", username);
            props.put("password", password);
            conn = DriverManager.getConnection(url.replace("http://","jdbc:neo4j://"), props);
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
}
