package org.fiteagle.north.proprietary.rest;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by vju on 9/6/14.
 */

/**
 * REST endpoint for Lodlive
 */
@Path("/lodlive")
public class LodLiveREST {

    @Inject
    private JMSContext context;
    @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;

    private static Logger LOGGER = Logger.getLogger(LodLiveREST.class.toString());


    @GET
    @Path("/query")
    @Produces("application/sparql-results+json")
    public String handleLodLiveRequest(@QueryParam("query") String sparqlQuery) {
        String serialization = "TURTLE";


        LOGGER.log(Level.INFO, sparqlQuery);

        //  sparqlQuery = "SELECT DISTINCT * WHERE {?object <http://www.w3.org/2002/07/owl#sameAs> <http://fiteagleinternal#ARunningMotor1>}";
        Model rdfModel = ModelFactory.createDefaultModel();

        com.hp.hpl.jena.rdf.model.Resource message = rdfModel.createResource("http://fiteagleinternal#Message");
        message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleRequest);
        message.addProperty(MessageBusOntologyModel.propertySparqlQuery, sparqlQuery); // SPARQL Query is expected in this form atm

        rdfModel.setNsPrefix("", "http://fiteagleinternal#");
        rdfModel.setNsPrefix("fiteagle", "http://fiteagle.org/ontology#");
        String response = "";
        String resultSet = "";
        try {
            Message request = this.createRequest(modelToString(rdfModel, serialization), serialization, IMessageBus.TYPE_REQUEST);
            sendRequest(request);
            Message result = waitForResult(request);

            //response is now the string contained in the rdf message property
            response = getResult(result);
            LOGGER.log(Level.INFO, "Got result " + response);

            // create an empty model
            Model modelAnswer = ModelFactory.createDefaultModel();

            InputStream is = new ByteArrayInputStream(response.getBytes(Charset.defaultCharset()));
            modelAnswer.read(is, null, serialization);
            LOGGER.log(Level.INFO, "Pushed rdf into modelAnswer");


            StmtIterator iter = modelAnswer.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleInform));
            Statement currentStatement = null;
            Statement rdfsComment = null;


            while (iter.hasNext()) {
                currentStatement = iter.nextStatement();
                currentStatement.toString();
                LOGGER.log(Level.INFO, currentStatement.toString());
                rdfsComment = currentStatement.getSubject().getProperty(MessageBusOntologyModel.propertyJsonResult);
                if (rdfsComment != null) {
                    resultSet = rdfsComment.getObject().toString();
                    LOGGER.log(Level.INFO, "ResultSet is" + resultSet);
                    break;
                }
            }


        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resultSet;
    }

    private String modelToString(Model model, String serialization) {
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

        LOGGER.log(Level.INFO, "Received resources via MDB...");
        if (null != result) {
            resources = result.getStringProperty(IMessageBus.RDF);
        }
        return resources;
    }

    private void sendRequest(final Message message) {
        LOGGER.log(Level.INFO, "Getting resources via MDB...");
        this.context.createProducer().send(this.topic, message);
    }

    private Message waitForResult(final Message message) throws JMSException {
        LOGGER.log(Level.INFO, "Waiting for an answer...");
        final String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
        final Message rcvMessage = this.context.createConsumer(this.topic, filter).receive(5000);
        return rcvMessage;
    }

}
