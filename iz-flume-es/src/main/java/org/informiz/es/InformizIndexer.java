package org.informiz.es;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.conf.ComponentConfiguration;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
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
		XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(event.getBody());
		parser.close();
		jsonBuilder().copyCurrentStructure(parser);
		IndexRequestBuilder request = client.prepareIndex();
		request.setIndex(indexName)
		       .setType(indexType)
		       .setId(event.getHeaders().get(IdFieldMapper.NAME))
		       .setSource(event.getBody());

		return request;
	}
}
