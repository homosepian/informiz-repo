package org.informiz.es;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
//import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InformizESIndexManager {
	public static final String DEFAULT_CLUSTER_NAME = "elasticsearch";
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 9300;

	public static final String INFORMIZ_INDEX = "informiz";
	public static final String INFORMI_TYPE = "informi";

	static Logger logger = LoggerFactory.getLogger(InformizESIndexManager.class);

	public static class HostProps {
		String hostname;
		int port;

		public HostProps(String hostname, int port) {
			this.hostname = hostname;
			this.port = port;
		}
	}

	public static void init() {
		init(Settings.settingsBuilder()
				.put("cluster.name", DEFAULT_CLUSTER_NAME)
				/*.put("client.transport.sniff", true)*/.build(),
				Arrays.asList(new HostProps(DEFAULT_HOST, DEFAULT_PORT)));
	}

	public static void init(Settings settings, List<HostProps> hosts) {
		try (TransportClient client = TransportClient.builder().settings(settings).build()) {
			for (HostProps host : hosts) {
				try {
					client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host.hostname), host.port));
				} catch (UnknownHostException e) {
					logger.error("Failed to establish transport address", e);
				}
			}
			if (client.transportAddresses() == null || client.transportAddresses().isEmpty()) {
				throw new RuntimeException("No transport addresses established for ElasticSearch");
			}
			initIndex(client);
		}
	}

	private static void initIndex(Client client) {
		
		IndicesExistsRequest request = new IndicesExistsRequest(INFORMIZ_INDEX);
		IndicesExistsResponse response = client.admin().indices().exists(request).actionGet();
		if (response.isExists()) {
			return;
		}

		try {
			client.admin().indices().create(new
					CreateIndexRequest(INFORMIZ_INDEX)).actionGet();
			XContentBuilder xbMapping = 
					jsonBuilder()
					.startObject()
					.startObject(INFORMI_TYPE)
					.startObject("properties")

					.startObject("title")
					.field("type", "string")
					.endObject()

					.startObject("description")
					.field("type", "string")
					.endObject()

					.startObject("id")
					.field("type", "long")
					.endObject()

					.startObject("media_type")
					.field("type", "string")
					.endObject()

					// TODO do i want this indexed?
					.startObject("media_source")
					.field("type", "string")
					.field("index", "not_analyzed")
					.endObject()

					.startObject("created")
					.field("type", "date")
					.field("format", "yyyy-MM-dd HH:mm:ss")
					.endObject()

					.endObject()
					.endObject()
					.endObject();

			client.admin().indices().preparePutMapping(INFORMIZ_INDEX).setType(INFORMI_TYPE)
			.setSource(xbMapping).execute().actionGet();

		} catch (Exception e) {
			throw new RuntimeException("Failed to create informiz index", e);
		}
	}
}
