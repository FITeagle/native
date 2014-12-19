package org.fiteagle.north.proprietary.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;

@Path("/lodlive")
public class LodLiveREST {
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private static Logger LOGGER = Logger.getLogger(LodLiveREST.class.toString());
  
  @GET
  @Path("/query")
  @Produces("application/sparql-results+json")
  public String handleLodLiveRequest(@QueryParam("query") String sparqlQuery) {
    LOGGER.log(Level.INFO, "Query from Lodlive: " + sparqlQuery);
    
    String serializedRequest = MessageUtil.createSerializedSPARQLQueryModel(sparqlQuery, IMessageBus.SERIALIZATION_JSONLD);  
    
    Message request = MessageUtil.createRDFMessage(serializedRequest, IMessageBus.TYPE_REQUEST, IMessageBus.SERIALIZATION_JSONLD, null, context);
    
    this.context.createProducer().send(topic, request);
    Message resultMessage = MessageUtil.waitForResult(request, context, topic);
    
    String response = MessageUtil.getRDFResult(resultMessage);
    if(response == null){
      LOGGER.log(Level.SEVERE, MessageUtil.getError(resultMessage));
      return "";
    }
    return response;
  }
  
}
