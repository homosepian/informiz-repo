package org.informiz.executor;

import java.util.Map;

import org.neo4j.graphdb.ResourceIterator;

/**
 * @author Nira Amit
 */
public interface CypherExecutor {
	ResourceIterator<Map<String,Object>> query(String statement, Map<String,Object> params);
}
