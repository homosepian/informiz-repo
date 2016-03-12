package org.informiz.executor;

import java.util.Map;

import org.neo4j.graphdb.ResourceIterator;

/**
 * @author Nira Amit
 */
public interface CypherExecutor {
	QueryResultIterator query(String statement, Map<String,Object> params);
	
	void shutdown();
}
