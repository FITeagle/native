package org.fiteagle.north.proprietary.rest;

import java.util.UUID;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;

@Path("/rest")
public class ResourceRepository {

	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;

	private static Logger LOGGER = Logger
			.getLogger(ResourceRepository.class.toString());

	@GET
	@Path("/resources")
	@Produces("text/turtle")
	public String listResourcesTTL() throws JMSException {
		return this.listResources(IMessageBus.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/resources")
	@Produces("application/ld+json")
	public String listResourcesJSON() throws JMSException {
		return this.listResources(IMessageBus.SERIALIZATION_JSONLD);
	}
	
	@GET
	@Path("/resources/{resource : (.+)?}")
	@Produces("text/turtle")
	public String listSpecificResourceTTL(@PathParam("resource") String resource) throws JMSException {
		return this.getResource(resource, IMessageBus.SERIALIZATION_TURTLE);
	}
	
	@GET
	@Path("/resources/{resource : (.+)?}")
	@Produces("application/ld+json")
	public String listSpecificResourceLD(@PathParam("resource") String resource) throws JMSException {
		return this.getResource(resource, IMessageBus.SERIALIZATION_JSONLD);
	}

	private String getResource(String resource, String serialization) throws JMSException {
		//@todo: find a good solution for the namespace naming
		final Message request = this.createRequest("DESCRIBE <http://mynamespace/"+resource+">", serialization);
		sendRequest(request);
		final Message result = waitForResult(request);
		final String resources = getResult(result);

		return resources;
	}

	private String listResources(final String serialization)
			throws JMSException {
		
		//todo: update as soon as we have a proper ontology
		final Message request = this.createRequest("DESCRIBE ?s WHERE { ?s a <http://fiteagle.org/ontology#resource> }", serialization);
		sendRequest(request);
		final Message result = waitForResult(request);
		final String resources = getResult(result);

		return resources;
	}

	private String getResult(final Message result) throws JMSException {
		String resources = "timeout";
		
		ResourceRepository.LOGGER.log(Level.INFO,
				"Received resources via MDB...");
		if (null != result) {
			resources = result.getStringProperty(IMessageBus.RESULT);
		}
		return resources;
	}

	private void sendRequest(final Message message) {
		ResourceRepository.LOGGER.log(Level.INFO,
				"Getting resources via MDB...");
		this.context.createProducer().send(this.topic, message);
	}

	private Message waitForResult(final Message message) throws JMSException {
		ResourceRepository.LOGGER.log(Level.INFO,
				"Waiting for an answer...");
		final String filter = "JMSCorrelationID='"
				+ message.getJMSCorrelationID() + "'";
		final Message rcvMessage = this.context.createConsumer(this.topic,
				filter).receive(IMessageBus.TIMEOUT);
		return rcvMessage;
	}

	private Message createRequest(final String query, final String serialization)
			throws JMSException {
		final Message message = this.context.createMessage();
		message.setStringProperty(IMessageBus.TYPE, IMessageBus.REQUEST);
		message.setStringProperty(IMessageBus.SERIALIZATION, serialization);
		message.setStringProperty(IMessageBus.TARGET, IResourceRepository.SERVICE_NAME);
		message.setStringProperty(IMessageBus.QUERY, query);
		message.setJMSCorrelationID(UUID.randomUUID().toString());
		return message;
	}
}
