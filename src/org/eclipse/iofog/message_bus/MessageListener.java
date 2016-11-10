package org.eclipse.iofog.message_bus;

import org.eclipse.iofog.local_api.MessageCallback;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.MessageHandler;

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
