package org.informiz.executor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResultIterator implements Iterator<Map<String, Object>>, AutoCloseable {
	
	static Logger logger = LoggerFactory.getLogger(QueryResultIterator.class);
	
	PreparedStatement statement;
	ResultSet result;
    boolean hasNext;
    public List<String> columns;
	
	public QueryResultIterator(PreparedStatement statement, ResultSet result) throws SQLException {
		this.statement = statement;
		this.result = result;
		this.hasNext = result == null ? false : result.next();
	}

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private List<String> getColumns() throws SQLException {
        if (columns != null) return columns;
        ResultSetMetaData metaData = result.getMetaData();
        int count = metaData.getColumnCount();
        List<String> cols = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) cols.add(metaData.getColumnName(i));
        return columns = cols;
    }

    @Override
    public Map<String, Object> next() {
        try {
            if (hasNext) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (String col : getColumns()) map.put(col, result.getObject(col));
                hasNext = result.next();
                if (!hasNext) {
                    result.close();
                    statement.close();
                }
                return map;
            } else throw new NoSuchElementException();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
    }

	@Override
	public void close() {
        if (result != null) try {
			result.close();
		} catch (SQLException e) {
			// best effort
			logger.warn("Exception while closing query result-set", e);
		}
        if (statement != null) try {
			statement.close();
		} catch (SQLException e) {
			// best effort
			logger.warn("Exception while closing prepared-statement", e);
		}
	}
	
}
