package org.informiz.es;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.conf.ComponentConfiguration;
import com.frontier45.flume.sink.elasticsearch2.ElasticSearchEventSerializer;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

public class InformizEventSerializer implements ElasticSearchEventSerializer {

	@Override
	public void configure(Context arg0) {
		// no-op

	}

	@Override
	public void configure(ComponentConfiguration arg0) {
		// no-op

	}

	@Override
	public BytesStream getContentBuilder(Event event) throws IOException {
		XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(event.getBody());
		parser.close();
		return jsonBuilder().copyCurrentStructure(parser);
		
	}

}
