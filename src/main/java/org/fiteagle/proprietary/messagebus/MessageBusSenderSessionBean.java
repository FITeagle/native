package org.fiteagle.proprietary.messagebus;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;

@Stateless
public class MessageBusSenderSessionBean {

	@Inject
	private JMSContext jmsContext;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;
	private static final Logger LOGGER = Logger
			.getLogger(MessageBusSenderSessionBean.class.getName());
	private static final long TIMEOUT = 1000;

	public void sendRequest(Message request) throws JMSException {
		LOGGER.log(Level.INFO, "Submitting request to JMS...");
		jmsContext.createProducer().send(topic, request);
	}
	
	public Message sendRequestSync(Message request, String filter) throws JMSException {
		LOGGER.log(Level.INFO, "Submitting synchronous request to JMS...");
		jmsContext.createProducer().send(topic, request);
		return jmsContext.createConsumer(topic, filter).receive(TIMEOUT);
	}

	public Message createMessage() {
		return jmsContext.createMessage();
	}

}
