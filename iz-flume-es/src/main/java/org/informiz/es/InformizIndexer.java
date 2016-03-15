package org.informiz.es;

import java.io.IOException;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.conf.ComponentConfiguration;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.mapper.internal.IdFieldMapper;

import com.frontier45.flume.sink.elasticsearch2.ElasticSearchIndexRequestBuilderFactory;

public class InformizIndexer implements ElasticSearchIndexRequestBuilderFactory {
	@Override
	public void configure(Context arg0) {
		// no-op

	}

	@Override
	public void configure(ComponentConfiguration arg0) {
		// no-op

	}

	@Override
	public IndexRequestBuilder createIndexRequest(Client client, String indexName, String indexType, Event event)
			throws IOException {
		IndexRequestBuilder request = client.prepareIndex();
		request.setIndex(indexName)
		       .setType(indexType)
		       .setId(event.getHeaders().get(IdFieldMapper.NAME))
		       .setSource(event.getBody());

		return request;
	}
}
