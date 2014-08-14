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

@Path("/resources")
public class NorthboundAPI {

    @Inject
    private JMSContext context;
    @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;

    private static Logger LOGGER = Logger.getLogger(NorthboundAPI.class.toString());

    @GET
    @Path("/")
    @Produces("text/turtle")
    public String discoverResourcesTTL() throws JMSException {
        return discoverMotorTTL();
    }
    
    @GET
    @Path("/garage")
    @Produces("text/turtle")
    public String discoverMotorTTL() throws JMSException {
        String serialization = "TURTLE";
        
        Model inputModel = getDiscoverModel();

        String response = "";
        try {
            Message request = this.createRequest(modelToString(inputModel, serialization), serialization, IMessageBus.TYPE_DISCOVER);
            sendRequest(request);
            Message result = waitForResult(request);
            response = getResult(result);
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }
    
    @GET
    @Path("/garage/{instanceName}")
    @Produces("text/turtle")
    public String discoverMotorInstanceTTL(@PathParam("instanceName") String instanceName) {
        String serialization = "TURTLE";
        
        Model inputModel = getDiscoverModel(instanceName);

        String response = "";
        try {
            Message request = this.createRequest(modelToString(inputModel, serialization), serialization, IMessageBus.TYPE_DISCOVER);
            sendRequest(request);
            Message result = waitForResult(request);
            response = getResult(result);
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }
    
    private Model getDiscoverModel(String instanceName){
        Model rdfModel = getDiscoverModel();
        
        com.hp.hpl.jena.rdf.model.Resource motorResourceType = rdfModel.createResource("http://fiteagle.org/ontology/adapter/motor#Motor");
        com.hp.hpl.jena.rdf.model.Resource motor = rdfModel.createResource("http://fiteagleinternal#" + instanceName);
        motor.addProperty(RDF.type, motorResourceType);
        
        rdfModel.setNsPrefix("motor", "http://fiteagle.org/ontology/adapter/motor#");
        
        return rdfModel;
    }
        
        
    private Model getDiscoverModel(){
        Model rdfModel = ModelFactory.createDefaultModel();
        
        com.hp.hpl.jena.rdf.model.Resource message = rdfModel.createResource("http://fiteagleinternal#Message");
        message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleDiscover);

        rdfModel.setNsPrefix("", "http://fiteagleinternal#");
        rdfModel.setNsPrefix("fiteagle", "http://fiteagle.org/ontology#");
        
        return rdfModel;
    }    
        
    
    @PUT
    @Path("/garage/{instanceName}")
    @Produces("text/html")
    public String motorCreateInstance(@PathParam("instanceName") String instanceName) {
        
        String serialization = "TURTLE";
        
        Model inputModel = getCreateModel(instanceName);

        String response = "";
        try {
            Message request = this.createRequest(modelToString(inputModel, serialization), serialization, IMessageBus.TYPE_CREATE);
            sendRequest(request);
            Message result = waitForResult(request);
            response = getResult(result);
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }

    @PUT
    @Path("/garage/{instanceName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public String motorCreateInstanceWithRDF(@PathParam("instanceName") String instanceName, String rdfInput) {

        String serialization = "TURTLE";
        
        Model inputModel = getCreateModel(readModel(rdfInput, serialization));

        String response = "";
        try {
            Message request = this.createRequest(modelToString(inputModel, serialization), serialization, IMessageBus.TYPE_CREATE);
            sendRequest(request);
            Message result = waitForResult(request);
            response = getResult(result);
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }
    
    private Model getReleaseModel(String instanceName){
        Model rdfModel = ModelFactory.createDefaultModel();
        com.hp.hpl.jena.rdf.model.Resource motorResourceType = rdfModel.createResource("http://fiteagle.org/ontology/adapter/motor#Motor");
        com.hp.hpl.jena.rdf.model.Resource motor = rdfModel.createResource("http://fiteagleinternal#" + instanceName);
        motor.addProperty(RDF.type, motorResourceType);
        
        rdfModel.setNsPrefix("motor", "http://fiteagle.org/ontology/adapter/motor#");

        com.hp.hpl.jena.rdf.model.Resource message = rdfModel.createResource("http://fiteagleinternal#Message");
        message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleRelease);

        rdfModel.setNsPrefix("", "http://fiteagleinternal#");
        rdfModel.setNsPrefix("fiteagle", "http://fiteagle.org/ontology#");
        
        return rdfModel;
    }
    
    private Model getCreateModel(String instanceName){
        Model rdfModel = ModelFactory.createDefaultModel();
        com.hp.hpl.jena.rdf.model.Resource motorResourceType = rdfModel.createResource("http://fiteagle.org/ontology/adapter/motor#Motor");
        com.hp.hpl.jena.rdf.model.Resource motor = rdfModel.createResource("http://fiteagleinternal#" + instanceName);
        motor.addProperty(RDF.type, motorResourceType);
        
        rdfModel.setNsPrefix("motor", "http://fiteagle.org/ontology/adapter/motor#");

        return getCreateModel(rdfModel);
    }
    
    private Model getCreateModel(Model rdfModel){
        
        com.hp.hpl.jena.rdf.model.Resource message = rdfModel.createResource("http://fiteagleinternal#Message");
        message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleCreate);

        rdfModel.setNsPrefix("", "http://fiteagleinternal#");
        rdfModel.setNsPrefix("fiteagle", "http://fiteagle.org/ontology#");
        
        return rdfModel;
    }
    
    private Model getConfigureModel(Model rdfModel){
        
        com.hp.hpl.jena.rdf.model.Resource message = rdfModel.createResource("http://fiteagleinternal#Message");
        message.addProperty(RDF.type, MessageBusOntologyModel.propertyFiteagleConfigure);

        rdfModel.setNsPrefix("", "http://fiteagleinternal#");
        rdfModel.setNsPrefix("fiteagle", "http://fiteagle.org/ontology#");
        
        return rdfModel;
    }
    
    private Model readModel(String modelString, String serialization){
        Model rdfModel = ModelFactory.createDefaultModel();

        InputStream is = new ByteArrayInputStream(modelString.getBytes());

        // read the RDF/XML file
        rdfModel.read(is, null, serialization);
        
        return rdfModel;
    }
    
    private String modelToString(Model model, String serialization){
        StringWriter writer = new StringWriter();

        model.write(writer, serialization);
        
        return writer.toString();
    }

    @POST
    @Path("/garage/{instanceName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public String motorConfigureInstance(@PathParam("instanceName") String instanceName, String rdfInput) {
        String serialization = "TURTLE";
        
        Model inputModel = getConfigureModel(readModel(rdfInput, serialization));

        String response = "";
        try {
            Message request = this.createRequest(modelToString(inputModel, serialization), serialization, IMessageBus.TYPE_CONFIGURE);
            sendRequest(request);
            Message result = waitForResult(request);
            response = getResult(result);
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }

    @DELETE
    @Path("/garage/{instanceName}")
    @Produces("text/html")
    public String motorReleaseInstance(@PathParam("instanceName") String instanceName) {
        String serialization = "TURTLE";
        
        Model inputModel = getReleaseModel(instanceName);

        String response = "";
        try {
            Message request = this.createRequest(modelToString(inputModel, serialization), serialization, IMessageBus.TYPE_RELEASE);
            sendRequest(request);
            Message result = waitForResult(request);
            response = getResult(result);
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
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

        NorthboundAPI.LOGGER.log(Level.INFO, "Received resources via MDB...");
        if (null != result) {
            resources = result.getStringProperty(IMessageBus.RDF);
        }
        return resources;
    }

    private void sendRequest(final Message message) {
        NorthboundAPI.LOGGER.log(Level.INFO, "Getting resources via MDB...");
        this.context.createProducer().send(this.topic, message);
    }

    private Message waitForResult(final Message message) throws JMSException {
        NorthboundAPI.LOGGER.log(Level.INFO, "Waiting for an answer...");
        final String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
        final Message rcvMessage = this.context.createConsumer(this.topic, filter).receive(IMessageBus.TIMEOUT);
        return rcvMessage;
    }

}
