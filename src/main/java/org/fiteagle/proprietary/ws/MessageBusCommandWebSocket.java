package org.fiteagle.proprietary.ws;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Topic;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fiteagle.api.core.IMessageBus;

@ServerEndpoint("/api/bus/command")
public class MessageBusCommandWebSocket {

	@Inject
	private JMSContext jmsContext;
	@Resource(mappedName = "topic/core")
	private Topic topic;
	
	private static final Logger LOGGER = Logger
			.getLogger(MessageBusCommandWebSocket.class.getName());

	@OnMessage
	public String onMessage(final String command) throws JMSException,
			InterruptedException {
		LOGGER.log(Level.INFO, "Received WebSocket message: " + command);

		JMSProducer jmsProducer = jmsContext.createProducer();

		final Message jmsMessage = this.jmsContext.createMessage();
		jmsMessage.setStringProperty(IMessageBus.TYPE_REQUEST, command);

		LOGGER.log(Level.INFO, "Submitting command '" + command
				+ "' via JMS...");

		if (null != topic)
			jmsProducer.send(topic, jmsMessage);
		else
			LOGGER.log(Level.SEVERE, "No topic found! Debug me!");

		LOGGER.log(Level.INFO, "...done.");

		return "OK";
	}

	@OnOpen
	public void onOpen(final Session wsSession, final EndpointConfig config)
			throws IOException {
		LOGGER.log(Level.INFO, "Opening WebSocket connection...");
	}
}
