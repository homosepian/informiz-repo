package org.informiz.executor;

import java.util.Map;

/**
 * @author Nira Amit
 */
public interface CypherExecutor {
	
	QueryResultIterator query(String statement, Map<String,Object> params);
	
	void update(String statement);
	
	void shutdown();
}
