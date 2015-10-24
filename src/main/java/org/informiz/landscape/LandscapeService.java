package org.informiz.landscape;

import static org.neo4j.helpers.collection.MapUtil.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.informiz.executor.CypherExecutor;
import org.informiz.executor.JdbcCypherExecutor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Nira Amit
 */
public class LandscapeService {

    private final CypherExecutor cypher;

    public LandscapeService(String uri) {
    	this(uri, null, null);
    }

    public LandscapeService(String uri, String user, String pass) {
        cypher = createCypherExecutor(uri,user,pass);
    }

    private CypherExecutor createCypherExecutor(String uri, String user, String pass) {
        if (user != null) {
            return new JdbcCypherExecutor(uri,user,pass);
        }
        return new JdbcCypherExecutor(uri);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> graph(int informiId, int limit) {
        Iterator<Map<String,Object>> result = cypher.query(
        		"MATCH (informi:Informi)-[r]->(other:Informi) "
        		+ "WHERE informi.id={1} OR other.id={1} "
        		+ "RETURN informi, r, other "
        		+ "LIMIT {2}", map("1",informiId, "2",limit));
        List<Map<String, Object>> nodes = Lists.newArrayList();
        List <Map<String, Object>> rels = Lists.newArrayList();
        List<Integer> ids = new ArrayList<Integer>();

        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Map<String, Object> source = (Map<String, Object>)row.get("informi");
			addNode(source, informiId, ids, nodes);
            Map<String, Object> target = (Map<String, Object>)row.get("other");
			addNode(target, informiId, ids, nodes);
            Map<String, Object> rel = (Map<String, Object>)row.get("r");
            rels.add(map("source", source.get("id"), "target", target.get("id"), 
            		"caption", rel.get("description"), "type", "Relation"));
        }

        return map("graph", map("nodes", nodes, "edges", rels), "labels", Arrays.asList("Informi"), "errors", "");
    }

	private void addNode(Map<String, Object> node, int rootId, List<Integer> ids, List<Map<String, Object>> nodes) {
		Map<String, Object> informi = Maps.newHashMap(node);
		Integer id = (Integer)informi.get("id");
		if (! ids.contains(id)) {
			ids.add(id);
			nodes.add(informi);
			informi.put("type", "Informi");
			if (id == rootId) {
				informi.put("root", true);
			}
		}
	}
}
