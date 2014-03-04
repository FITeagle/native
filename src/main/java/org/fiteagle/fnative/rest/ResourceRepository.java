package org.fiteagle.fnative.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.api.core.IResourceRepository.Serialization;

@Path("/repo")
public class ResourceRepository {

	private static Logger LOGGER = Logger.getLogger(ResourceRepository.class
			.toString());
	private IResourceRepository repo;
	@Inject
	private JMSContext context;
	@Resource(mappedName = "java:/topic/core")
	private Topic topic;

	public ResourceRepository() throws NamingException {
		final Context context = new InitialContext();
		repo = (IResourceRepository) context
				.lookup("java:global/core-resourcerepository/ResourceRepositoryEJB");
	}

	@GET
	@Path("/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXML() {
		LOGGER.log(Level.INFO, "Getting resources as RDF...");
		return repo.listResources(Serialization.XML);
	}

	@GET
	@Path("/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTL() {
		LOGGER.log(Level.INFO, "Getting resources as TTL...");
		return repo.listResources(Serialization.TTL);
	}

	@GET
	@Path("/async/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXMLviaMDB() throws JMSException, InterruptedException {		
		LOGGER.log(Level.INFO, "Getting resources as RDF via MDB...");
		JMSProducer producer = context.createProducer();
		JMSConsumer consumer = context.createConsumer(topic, "foo = bar");
		Message message = context.createMessage();
		message.setStringProperty("method", "listResources");
		producer.send(topic, message);
		Message result = consumer.receive(1000);
		return result.getStringProperty("result");
	}
}
