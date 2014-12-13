package org.fiteagle.north.proprietary.rest;

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
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;

@Path("/rest")
public class ResourceRepository {

	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;

	@SuppressWarnings("unused")
  private static Logger LOGGER = Logger.getLogger(ResourceRepository.class.toString());

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
    // TODO: find a good solution for the namespace naming
    
    String query = "DESCRIBE <" + NorthboundAPI.INSTANCE_PREFIX + resource + ">";
    String requestModel = MessageUtil.createSerializedSPARQLQueryModel(query);
    final Message request = MessageUtil.createRDFMessage(requestModel, IMessageBus.TYPE_REQUEST, serialization, context);
    context.createProducer().send(topic, request);
    
    final Message result = MessageUtil.waitForResult(request, context, topic);
    final String resources = MessageUtil.getRDFResult(result);
    
    return resources;
  }

	private String listResources(final String serialization) {
		
	  String query = "DESCRIBE ?s WHERE { ?s a <"+MessageBusOntologyModel.classResource+"> }";
    String requestModel = MessageUtil.createSerializedSPARQLQueryModel(query);
    final Message request = MessageUtil.createRDFMessage(requestModel, IMessageBus.TYPE_REQUEST, serialization, context);
    context.createProducer().send(topic, request);
		final Message result = MessageUtil.waitForResult(request, context, topic);
		final String resources = MessageUtil.getRDFResult(result);

		return resources;
	}
}
