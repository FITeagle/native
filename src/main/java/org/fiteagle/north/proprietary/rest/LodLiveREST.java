package org.fiteagle.north.proprietary.rest;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;
import org.fiteagle.api.core.MessageBusOntologyModel;

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
        LOGGER.log(Level.INFO, "Query from Lodlive: "+ sparqlQuery);

        Model rdfModel = ModelFactory.createDefaultModel();

        Resource message = rdfModel.createResource("http://fiteagleinternal#Message");
        message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleRequest);
        message.addProperty(MessageBusOntologyModel.propertySparqlQuery, sparqlQuery);

        String response = "";
        String resultSet = "";
        try {
          String serializedModel = MessageBusMsgFactory.serializeModel(rdfModel, IMessageBus.SERIALIZATION_TURTLE);
          
            Message request = createRequest(serializedModel, IMessageBus.SERIALIZATION_JSONLD, IMessageBus.TYPE_REQUEST);
            sendRequest(request);
            Message resultMessage = waitForResult(request);

            response = getResult(resultMessage);

            Model modelAnswer = MessageBusMsgFactory.parseSerializedModel(response);

            StmtIterator iter = modelAnswer.listStatements(null, RDF.type, MessageBusOntologyModel.propertyFiteagleInform);
            Statement currentStatement = null;
            Statement rdfsComment = null;

            while (iter.hasNext()) {
                currentStatement = iter.nextStatement();
                currentStatement.toString();
                rdfsComment = currentStatement.getSubject().getProperty(MessageBusOntologyModel.propertyJsonResult);
                if (rdfsComment != null) {
                    resultSet = rdfsComment.getObject().toString();
                    break;
                }
            }

        } catch (JMSException e) {
          LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return resultSet;
    }

    private Message createRequest(final String rdfInput, final String serialization, String methodType) throws JMSException {
        final Message message = this.context.createMessage();

        message.setStringProperty(IMessageBus.METHOD_TYPE, methodType);
        message.setStringProperty(IMessageBus.SERIALIZATION, serialization);
        message.setStringProperty(IMessageBus.RDF, rdfInput);
        message.setJMSCorrelationID(UUID.randomUUID().toString());

        return message;
    }

    private String getResult(final Message result) throws JMSException {
        String resources = "timeout";
        if (null != result) {
          resources = result.getStringProperty(IMessageBus.RDF);
        }
        return resources;
    }

    private void sendRequest(final Message message) {
        this.context.createProducer().send(this.topic, message);
    }

    private Message waitForResult(final Message message) throws JMSException {
        final String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
        final Message rcvMessage = this.context.createConsumer(this.topic, filter).receive(IMessageBus.TIMEOUT);
        return rcvMessage;
    }
}
