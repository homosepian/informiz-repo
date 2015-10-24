package org.informiz.executor;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Nira Amit
 */
public interface CypherExecutor {
    Iterator<Map<String,Object>> query(String statement, Map<String,Object> params);
}
