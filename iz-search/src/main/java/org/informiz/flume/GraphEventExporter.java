package org.informiz.flume;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.FlumeException;
import org.apache.flume.api.RpcClient;
import org.apache.flume.api.RpcClientFactory;
import org.apache.flume.event.EventBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder; 

public class GraphEventExporter {

	public static final int BATCH_SIZE = 100; 
	public static final int ATTEMPT_DELAY = 500; 
	public static final int MAX_ATTEMPS = 3; 

	private RpcClient client;
	private String hostname;
	private int port;


	public GraphEventExporter(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
		connect();
	}

	public void sendNodesToFlume(List<Map<String, Object>> nodes) {
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		List<Event> batch = new ArrayList<Event>();
		int size = 0;
		for (Map<String, Object> node: nodes) {
			Event event = EventBuilder.withBody(gson.toJson(node), Charset.forName("UTF-8"));
			batch.add(event);
			size++;
			if (size == BATCH_SIZE) {
				sendBatch(batch);
				batch.clear();
				size=0;
			}
		}
		if (! batch.isEmpty()) sendBatch(batch);
	}

	private void sendBatch(List<Event> batch) {
		sendBatch(batch, 0);
	}

	private void sendBatch(List<Event> batch, int attempt) {
		try {
			client.appendBatch(new ArrayList<Event>(batch));
		} catch (EventDeliveryException e) {
			if (attempt < MAX_ATTEMPS) {
				connect();
				sendBatch(batch, attempt+1);
			} else {
				throw new RuntimeException(String.format("Failed to send events to flume after %d attempts", MAX_ATTEMPS));
			}
		} 
	}

	private void connect() {
		cleanUp();
		try { 
			this.client = RpcClientFactory.getDefaultInstance(hostname, port);
		} catch (final FlumeException fe) { 
			throw new RuntimeException("Unable to create Avro client", fe); 
		} 
	} 

	public void close() {
		cleanUp();
	}

	private void cleanUp() {
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				// TODO log
			}
		}
		client = null;
	}

}
