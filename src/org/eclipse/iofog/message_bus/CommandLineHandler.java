package org.eclipse.iofog.message_bus;

import org.eclipse.iofog.command_line.CommandLineParser;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.MessageHandler;

/**
 * listener for command-line communications
 * 
 * @author saeid
 *
 */
public class CommandLineHandler implements MessageHandler {

	@Override
	public void onMessage(ClientMessage message) {
		try {
			message.acknowledge();
		} catch (HornetQException e) {
		}
		String command = message.getStringProperty("command");
		String result = CommandLineParser.parse(command);
		ClientMessage response = MessageBusServer.getSession().createMessage(false);
		response.putStringProperty("response", result);
		response.putObjectProperty("receiver", "iofog.commandline.response");
		
		try {
			MessageBusServer.getCommandlineProducer().send(response);
		} catch (Exception e) {
		}
	}

}
