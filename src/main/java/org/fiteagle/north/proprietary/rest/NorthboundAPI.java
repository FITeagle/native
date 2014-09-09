package org.fiteagle.north.proprietary.rest;

import java.util.HashMap;
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
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;

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

    // TODO: Temporary solution, Refactor
    private static HashMap<String, String[]> adapterNameToResourceName = new HashMap<String, String[]>();

    static {
        // TODO: Add robot, stopwatch etc.
        String[] motorAdapterParams = { "http://fiteagle.org/ontology/adapter/motor#Motor", "motor", "http://fiteagle.org/ontology/adapter/motor#" };

        adapterNameToResourceName.put("motor", motorAdapterParams);

    }

    @GET
    @Path("/")
    @Produces("text/turtle")
    public Response discoverAllTTL() throws JMSException {

        // TODO
        for (String adapterName : adapterNameToResourceName.keySet()) {
            return discoverAdapterInstanceTTL(adapterName);
        }

        return getRESTResponse(null);
    }

    @GET
    @Path("/{adapterName}")
    @Produces("text/turtle")
    public Response discoverAdapterInstanceTTL(@PathParam("adapterName") String adapterName) throws JMSException {

        Model inputModel = getDiscoverModel(adapterName, null);

        if (inputModel != null) {
            try {
                Message request = this.createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_DISCOVER);
                sendRequest(request);
                Message result = waitForResult(request);
                return getRESTResponse(getResult(result));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return getRESTResponse(null);
    }

    @GET
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/turtle")
    public Response discoverAdapterResourceInstanceTTL(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = getDiscoverModel(adapterName, instanceName);

        if (inputModel != null) {
            try {
                Message request = this.createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_DISCOVER);
                sendRequest(request);
                Message result = waitForResult(request);
                return getRESTResponse(getResult(result));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        
        return getRESTResponse(null);
    }
    
    private Response getRESTResponse(String responseString){        
        if (responseString == null){
            return Response.status(Response.Status.NOT_FOUND).entity("Adapter not found").build();
        } else if(responseString.equals(IMessageBus.STATUS_400)){
            return Response.status(Response.Status.BAD_REQUEST).entity("Resource not processed").build();
        } else if (responseString.equals(IMessageBus.STATUS_404)){
            return Response.status(Response.Status.NOT_FOUND).entity("Resource not found").build();
        } else if (responseString.equals(IMessageBus.STATUS_408)){
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("Time out").build();
        } else {
            return Response.ok(responseString, "text/turtle").build();
        }
    }

    @PUT
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/html")
    public Response createResourceInstance(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = getCreateModel(adapterName, instanceName);

        if (inputModel != null) {
            try {
                Message request = this.createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CREATE);
                sendRequest(request);
                Message result = waitForResult(request);
                return getRESTResponse(getResult(result));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return getRESTResponse(null);
    }

    @PUT
    @Path("/{adapterName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public Response createResourceInstanceWithRDF(@PathParam("adapterName") String adapterName, String rdfInput) {

        Model inputModel = getCreateModel(MessageBusMsgFactory.parseSerializedModel(rdfInput));

        try {
            Message request = this.createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CREATE);
            sendRequest(request);
            Message result = waitForResult(request);
            return getRESTResponse(getResult(result));
        } catch (JMSException e) {
            e.printStackTrace();
        }

        return getRESTResponse(null);
    }

    @POST
    @Path("/{adapterName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public Response configureResourceInstance(@PathParam("adapterName") String adapterName, String rdfInput) {

        Model inputModel = getConfigureModel(MessageBusMsgFactory.parseSerializedModel(rdfInput));

        try {
            Message request = this.createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CONFIGURE);
            sendRequest(request);
            Message result = waitForResult(request);
            return getRESTResponse(getResult(result));
        } catch (JMSException e) {
            e.printStackTrace();
        }

        return getRESTResponse(null);
    }

    @DELETE
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/html")
    public Response motorReleaseInstance(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = getReleaseModel(adapterName, instanceName);

        if (inputModel != null) {
            try {
                Message request = this.createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_RELEASE);
                sendRequest(request);
                Message result = waitForResult(request);
                return getRESTResponse(getResult(result));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return getRESTResponse(null);
    }

    private Model getDiscoverModel(String adapterName, String instanceName) {
        Model rdfModel = ModelFactory.createDefaultModel();

        String[] adapterParams = getAdapterParams(adapterName);

        if (adapterParams != null) {

            if (instanceName != null) {
                addResourceInstanceToModel(rdfModel, adapterParams, instanceName);
            }
            setAdapterPrefix(rdfModel, adapterParams);

            return MessageBusMsgFactory.createMsgDiscover(rdfModel);
        }

        return null;
    }

    private Model getReleaseModel(String adapterName, String instanceName) {

        Model rdfModel = ModelFactory.createDefaultModel();

        String[] adapterParams = getAdapterParams(adapterName);

        if (adapterParams != null) {

            addResourceInstanceToModel(rdfModel, adapterParams, instanceName);
            setAdapterPrefix(rdfModel, adapterParams);

            return MessageBusMsgFactory.createMsgRelease(rdfModel);
        }

        return null;
    }

    private Model getCreateModel(String adapterName, String instanceName) {

        Model rdfModel = ModelFactory.createDefaultModel();

        String[] adapterParams = getAdapterParams(adapterName);

        if (adapterParams != null) {

            addResourceInstanceToModel(rdfModel, adapterParams, instanceName);
            setAdapterPrefix(rdfModel, adapterParams);

            return MessageBusMsgFactory.createMsgCreate(rdfModel);
        }

        return null;
    }

    private Model getCreateModel(Model rdfModel) {
        return MessageBusMsgFactory.createMsgCreate(rdfModel);
    }

    private Model getConfigureModel(Model rdfModel) {
        return MessageBusMsgFactory.createMsgConfigure(rdfModel);
    }

    private String[] getAdapterParams(String paramAdapterName) {
        for (String adapterName : adapterNameToResourceName.keySet()) {
            if (paramAdapterName.equals(adapterName)) {
                return adapterNameToResourceName.get(adapterName);
            }
        }
        return null;
    }

    private void addResourceInstanceToModel(Model rdfModel, String[] adapterParams, String instanceName) {
        com.hp.hpl.jena.rdf.model.Resource resourceType = rdfModel.createResource(adapterParams[0]);
        com.hp.hpl.jena.rdf.model.Resource resourceInstance = rdfModel.createResource("http://fiteagleinternal#" + instanceName);
        resourceInstance.addProperty(RDF.type, resourceType);
    }

    private void setAdapterPrefix(Model rdfModel, String[] adapterParams) {
        rdfModel.setNsPrefix(adapterParams[1], adapterParams[2]);
    }

    private Message createRequest(final String rdfInput, final String methodType) throws JMSException {
        final Message message = this.context.createMessage();

        message.setStringProperty(IMessageBus.METHOD_TYPE, methodType);
        message.setStringProperty(IMessageBus.SERIALIZATION, IMessageBus.SERIALIZATION_DEFAULT);
        message.setStringProperty(IMessageBus.RDF, rdfInput);
        message.setJMSCorrelationID(UUID.randomUUID().toString());

        return message;
    }

    private String getResult(final Message result) throws JMSException {
        String resources = IMessageBus.STATUS_408;

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
        final Message rcvMessage = this.context.createConsumer(this.topic, filter).receive(5000);
        return rcvMessage;
    }

}
