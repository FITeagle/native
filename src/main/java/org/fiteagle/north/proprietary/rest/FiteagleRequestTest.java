package org.fiteagle.north.proprietary.rest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@Path("/resources2")
public class FiteagleRequestTest {
	
	/*
	 * DELETE THIS CLASS WHEN REAL REQUESTS ARE READY!
	 * just for testing purposes
	 * fires a REQUEST message with sparql query that asks for EVERYTHING in the repo
	 * curl -i -X GET http://localhost:8080/native/api/resources2/ to test it
	 */

    @Inject
    private JMSContext context;
    @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;

    private static Logger LOGGER = Logger.getLogger(FiteagleRequestTest.class.toString());

    @GET
    @Path("/")
    @Produces("text/turtle")
    public String gimme() throws JMSException {
        String serialization =  "TURTLE";
        String sparqlQuery = "Select * {?s ?p ?o}";

        Model rdfModel = ModelFactory.createDefaultModel();
        
        com.hp.hpl.jena.rdf.model.Resource message = rdfModel.createResource("http://fiteagleinternal#Message");
        message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleRequest);
        message.addProperty(RDFS.comment, sparqlQuery); // SPARQL Query is expected in this form atm

        rdfModel.setNsPrefix("", "http://fiteagleinternal#");
        rdfModel.setNsPrefix("fiteagle", "http://fiteagle.org/ontology#");

        String response = "";
        try {
            Message request = this.createRequest(modelToString(rdfModel, serialization), serialization, IMessageBus.TYPE_REQUEST);
            sendRequest(request);
            Message result = waitForResult(request);
            response = getResult(result);
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }   
    
    private String modelToString(Model model, String serialization){
        StringWriter writer = new StringWriter();

        model.write(writer, serialization);
        
        return writer.toString();
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

        FiteagleRequestTest.LOGGER.log(Level.INFO, "Received resources via MDB...");
        if (null != result) {
            resources = result.getStringProperty(IMessageBus.RDF);
        }
        return resources;
    }

    private void sendRequest(final Message message) {
    	FiteagleRequestTest.LOGGER.log(Level.INFO, "Getting resources via MDB...");
        this.context.createProducer().send(this.topic, message);
    }

    private Message waitForResult(final Message message) throws JMSException {
    	FiteagleRequestTest.LOGGER.log(Level.INFO, "Waiting for an answer...");
        final String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
        final Message rcvMessage = this.context.createConsumer(this.topic, filter).receive(5000);
        return rcvMessage;
    }

}
