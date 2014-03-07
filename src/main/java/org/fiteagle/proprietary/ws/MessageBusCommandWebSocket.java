package org.fiteagle.proprietary.ws;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.proprietary.messagebus.MessageBusSenderSessionBean;

@Named
@ServerEndpoint("/api/bus/command")
public class MessageBusCommandWebSocket {

	private static final Logger LOGGER = Logger
			.getLogger(MessageBusCommandWebSocket.class.getName());

	private MessageBusSenderSessionBean senderBean;

	@Inject
	public MessageBusCommandWebSocket(MessageBusSenderSessionBean senderBean) {
		this.senderBean = senderBean;
	}

	@OnMessage
	public String onMessage(final String command) throws JMSException {
		LOGGER.log(Level.INFO, "Received WebSocket message3: " + command);
		
		final Message message = senderBean.createMessage();
		message.setStringProperty(IMessageBus.TYPE_REQUEST, command);

		senderBean.sendRequest(message);
		return "";
	}

	@OnOpen
	public void onOpen(final Session wsSession, final EndpointConfig config)
			throws IOException {
		LOGGER.log(Level.INFO, "Opening WebSocket connection...");
	}
}
