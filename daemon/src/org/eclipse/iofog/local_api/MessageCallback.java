package org.eclipse.iofog.local_api;

import org.eclipse.iofog.message_bus.Message;

/**
 * Interface for the message bus to send real-time messages 
 * @author ashita
 * @since 2016
 */
public class MessageCallback {
	private final String name;
	
	public MessageCallback(String name) {
		this.name = name;
	}
	
	/**
	 * Method called from message bus to send real-time messages to the containers
	 * @param Message
	 * @return void
	 */
	public void sendRealtimeMessage(Message message) {
		MessageWebsocketHandler handler = new MessageWebsocketHandler();
		handler.sendRealTimeMessage(name, message);
	}
}