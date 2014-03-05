package org.fiteagle.proprietary.ws;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fiteagle.api.core.IMessageBus;

@ServerEndpoint("/api/bus/logger")
@MessageDriven(name = "LoggerMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class MessageBusLoggerWebSocket implements MessageListener {

	@Inject
	private JMSContext jmsContext;
	@Resource(mappedName = "java:/topic/core")
	private Topic topic;

	private static final Logger LOGGER = Logger
			.getLogger(MessageBusLoggerWebSocket.class.getName());

	private Session wsSession;

	@OnMessage
	public String onMessage(final String command) throws JMSException,
			InterruptedException {
		LOGGER.log(Level.INFO, "Received WebSocket message: " + command);

		JMSProducer jmsProducer = jmsContext.createProducer();

		final Message jmsMessage = this.jmsContext.createMessage();
		jmsMessage.setStringProperty(IMessageBus.TYPE_REQUEST, command);

		LOGGER.log(Level.INFO, "Submitting command '" + command
				+ "' via JMS...");

		jmsProducer.send(topic, jmsMessage);

		return "OK";
	}

	@OnOpen
	public void onOpen(final Session wsSession, final EndpointConfig config)
			throws IOException {
		LOGGER.log(Level.INFO, "Opening WebSocket connection...");
		this.wsSession = wsSession;
	}

	public void onMessage(final Message message) {
		try {
			LOGGER.log(Level.INFO, "Logging JMS message...");
			if (null != this.wsSession && this.wsSession.isOpen()) {
				String request = message
						.getStringProperty(IMessageBus.TYPE_REQUEST);
				this.wsSession.getAsyncRemote().sendText(
						"Request: " + request + "\n");
				String result = message
						.getStringProperty(IMessageBus.TYPE_RESULT);
				this.wsSession.getAsyncRemote().sendText(
						"Result: " + result + "\n");
			} else {
				LOGGER.log(Level.INFO, "No client to talk to");
			}
		} catch (JMSException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}
}
