package org.fiteagle.proprietary.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.api.core.IResourceRepository.Serialization;

@Path("/repo")
public class ResourceRepositoryServlet {

	private static final String EJB_NAME = "java:global/core-resourcerepository/ResourceRepositoryEJB";
	private static final int TIMEOUT = 1000;
	private static Logger LOGGER = Logger.getLogger(ResourceRepositoryServlet.class
			.toString());
	private IResourceRepository repo;
	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE)
	private Topic topic;

	public ResourceRepositoryServlet() throws NamingException {
			this.repo = (IResourceRepository) new InitialContext()
					.lookup(ResourceRepositoryServlet.EJB_NAME);
	}

	@GET
	@Path("/ejb/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXML() {
		LOGGER.log(Level.INFO, "Getting resources as RDF...");
		return repo.listResources(Serialization.XML);
	}

	@GET
	@Path("/ejb/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTL() {
		LOGGER.log(Level.INFO, "Getting resources as TTL...");
		return repo.listResources(Serialization.TTL);
	}

	@GET
	@Path("/mdb/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXMLviaMDB() throws JMSException,
			InterruptedException {
		return mdbListResources(IResourceRepository.SERIALIZATION_XML);
	}

	@GET
	@Path("/mdb/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTLviaMDB() throws JMSException,
			InterruptedException {
		return mdbListResources(IResourceRepository.SERIALIZATION_TTL);
	}

	private String mdbListResources(final String serialization)
			throws JMSException {
		String result = "unkown";
		JMSProducer producer = context.createProducer();
		JMSConsumer consumer = context.createConsumer(topic,
				IMessageBus.TYPE_RESPONSE + " = '"
						+ IResourceRepository.LIST_RESOURCES + "'");
		Message message = context.createMessage();
		message.setStringProperty(IMessageBus.TYPE_REQUEST,
				IResourceRepository.LIST_RESOURCES);
		
		message.setStringProperty(IResourceRepository.PROP_SERIALIZATION,
				serialization);

		LOGGER.log(Level.INFO, "Getting resources as RDF via MDB...");
		producer.send(topic, message);
		Message rcvMessage = consumer.receive(TIMEOUT);

		if (null != rcvMessage)
			result = rcvMessage.getStringProperty(IMessageBus.TYPE_RESULT);
		return result;
	}

	

}
