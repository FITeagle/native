package org.fiteagle.core.repo.dm;

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
import javax.ws.rs.Produces;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;

@Path("/repo/")
public class ResourceRepositoryREST {

	private static final int TIMEOUT = 2000;
	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;

	private static Logger LOGGER = Logger
			.getLogger(ResourceRepositoryREST.class.toString());

	@GET
	@Path("/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXML() throws JMSException {
		return this
				.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	@GET
	@Path("/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTL() throws JMSException {
		return this.listResources(IResourceRepository.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/resources.jsonld")
	@Produces("application/ld+json")
	public String listResourcesJSON() throws JMSException {
		return this.listResources(IResourceRepository.SERIALIZATION_JSONLD);
	}

	private String listResources(final String serialization)
			throws JMSException {
		
		final Message request = this.createRequest(serialization);
		sendRequest(request);
		final Message result = waitForResult(request);
		final String resources = getResources(result);

		return resources;
	}

	private String getResources(final Message result) throws JMSException {
		String resources = "timeout";
		
		ResourceRepositoryREST.LOGGER.log(Level.INFO,
				"Received resources via MDB...");
		if (null != result) {
			resources = result.getStringProperty(IMessageBus.TYPE_RESULT);
		}
		return resources;
	}

	private void sendRequest(final Message message) {
		ResourceRepositoryREST.LOGGER.log(Level.INFO,
				"Getting resources via MDB...");
		this.context.createProducer().send(this.topic, message);
	}

	private Message waitForResult(final Message message) throws JMSException {
		ResourceRepositoryREST.LOGGER.log(Level.INFO,
				"Waiting for an answer...");
		final String filter = "JMSCorrelationID='"
				+ message.getJMSCorrelationID() + "'";
		final Message rcvMessage = this.context.createConsumer(this.topic,
				filter).receive(ResourceRepositoryREST.TIMEOUT);
		return rcvMessage;
	}

	private Message createRequest(final String serialization)
			throws JMSException {
		final Message message = this.context.createMessage();
		message.setStringProperty(IMessageBus.TYPE_REQUEST,
				IResourceRepository.LIST_RESOURCES);
		message.setStringProperty(IResourceRepository.PROP_SERIALIZATION,
				serialization);
		message.setJMSCorrelationID(UUID.randomUUID().toString());
		return message;
	}
}
