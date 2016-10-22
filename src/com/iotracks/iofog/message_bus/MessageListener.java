package com.iotracks.iofog.message_bus;

import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.MessageHandler;

import com.iotracks.iofog.local_api.MessageCallback;

/**
 * listener for real-time receiving
 * 
 * @author saeid
 *
 */
public class MessageListener implements MessageHandler{
	private final MessageCallback callback;
	
	public MessageListener(MessageCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public void onMessage(ClientMessage msg) {
		try {
			msg.acknowledge();
		} catch (Exception e) {}
		
		Message message = new Message(msg.getBytesProperty("message"));
		callback.sendRealtimeMessage(message);
	}

}
