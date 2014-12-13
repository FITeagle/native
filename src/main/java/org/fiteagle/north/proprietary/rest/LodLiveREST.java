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
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

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
    
    Model rdfModel = ModelFactory.createDefaultModel();
    
    Resource message = rdfModel.createResource(MessageBusOntologyModel.internalMessage.getURI());
    message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleRequest);
    message.addProperty(MessageBusOntologyModel.propertySparqlQuery, sparqlQuery);
    
    String response = "";
    String resultSet = "";
    Message request = MessageUtil.createRDFMessage(rdfModel, IMessageBus.TYPE_REQUEST, IMessageBus.SERIALIZATION_JSONLD, context);
    
    this.context.createProducer().send(topic, request);
    Message resultMessage = MessageUtil.waitForResult(request, context, topic);
    
    response = MessageUtil.getRDFResult(resultMessage);
    
    Model modelAnswer = MessageUtil.parseSerializedModel(response);
    
    StmtIterator iter = modelAnswer.listStatements(null, RDF.type, MessageBusOntologyModel.propertyFiteagleInform);
    Statement rdfsComment = null;
    
    while (iter.hasNext()) {
      Statement currentStatement = iter.nextStatement();
      currentStatement.toString();
      rdfsComment = currentStatement.getSubject().getProperty(MessageBusOntologyModel.propertyJsonResult);
      if (rdfsComment != null) {
        resultSet = rdfsComment.getObject().toString();
        break;
      }
    }
    
    return resultSet;
  }
  
}
