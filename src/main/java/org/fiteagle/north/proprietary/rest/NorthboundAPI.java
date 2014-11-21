package org.fiteagle.north.proprietary.rest;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.ws.rs.core.Response.Status;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;
import org.fiteagle.api.core.MessageBusOntologyModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@Path("/resources")
public class NorthboundAPI {

    @Inject
    private JMSContext context;
    @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;

    private static Logger LOGGER = Logger.getLogger(NorthboundAPI.class.toString());

    // Need to refresh this when a adapter gets deployed/undeployed....
    private static HashMap<String, String[]> adapterSpecificParameters = new HashMap<String, String[]>();

    private void resetAdapterParameters() {
        adapterSpecificParameters.clear();
    }

    /**
     * This method will refresh the list of adapters available in the testbed It gets called every time an adapter is not found in the Map "adapterSpecificParameters"
     */
    private void refreshTestbedAdapterParameters() {

        resetAdapterParameters();

        String query = "DESCRIBE * {?s ?p ?o}";
        String requestModel = MessageBusMsgFactory.createSerializedSPARQLQueryModel(query);
        final Message request = createRDFMessage(requestModel, IMessageBus.TYPE_REQUEST);
        sendRequest(request);
        
        Message rcvMessage = waitForResult(request);
        String resultString = getResult(rcvMessage);
        String modelString = MessageBusMsgFactory.getTTLResultModelFromSerializedModel(resultString);
        Model responseModel = MessageBusMsgFactory.parseSerializedModel(modelString);

        if (responseModel != null) {

            // Get contained adapter names
            StmtIterator adapterIterator = responseModel.listStatements(null, RDFS.subClassOf, MessageBusOntologyModel.classAdapter);
            while (adapterIterator.hasNext()) {
                String[] adapterSpecificParams = new String[5];
                Statement currentAdapterTypeStatement = adapterIterator.next();

                // Find out what resource this adapter implements
                StmtIterator adapterImplementsIterator = responseModel.listStatements(currentAdapterTypeStatement.getSubject(), MessageBusOntologyModel.propertyFiteagleImplements,(RDFNode) null);
                while (adapterImplementsIterator.hasNext()) {
                    adapterSpecificParams[0] = currentAdapterTypeStatement.getSubject().toString();
                    adapterSpecificParams[1] = adapterImplementsIterator.next().getResource().toString();
                }

                // Find out the name of the adapter instance and its namespace/prefix
                StmtIterator adapterTypeIterator = responseModel.listStatements(null, RDF.type, currentAdapterTypeStatement.getSubject());
                while (adapterTypeIterator.hasNext()) {
                    Statement currentAdapterStatement = adapterTypeIterator.next();

                    String namespace = currentAdapterTypeStatement.getSubject().getNameSpace();
                    int posPrefix = namespace.lastIndexOf("/");
                    String prefix = namespace.substring(posPrefix + 1, namespace.length() - 1);
                    String adapterName = currentAdapterStatement.getSubject().getLocalName();

                    adapterSpecificParams[2] = prefix;
                    adapterSpecificParams[3] = namespace;
                    adapterSpecificParams[4] = adapterName;

                    adapterSpecificParameters.put(adapterName, adapterSpecificParams);
                }
            }
        }
    }
    
  @GET
  @Path("/")
  @Produces("text/turtle")
  public Response getAllResourcesTTL() throws JMSException {
    String query = "DESCRIBE ?resource WHERE {?resource <http://www.w3.org/2000/01/rdf-schema#subClassOf> <"+MessageBusOntologyModel.classResource+">. }";
    String requestModel = MessageBusMsgFactory.createSerializedSPARQLQueryModel(query);
    final Message request = createRDFMessage(requestModel, IMessageBus.TYPE_REQUEST);
    sendRequest(request);
    
    Message rcvMessage = waitForResult(request);
    String resultString = getResult(rcvMessage);
    String result = MessageBusMsgFactory.getTTLResultModelFromSerializedModel(resultString);
    
    Model resultModel = MessageBusMsgFactory.parseSerializedModel(result);
    result = MessageBusMsgFactory.serializeModel(resultModel);
    
    return createRESTResponse(result, null);
  }
  
    @GET
    @Path("/discover")
    @Produces("text/turtle")
    public Response discoverAllTTL() throws JMSException {

        Model inputModel = createDiscoverModel();

        if (inputModel != null) {
          Message request = createRDFMessage(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_DISCOVER);
          sendRequest(request);
          Message result = waitForResult(request);
          return createRESTResponse(getResult(result), null);
        }

        return createRESTResponse(null, null);
    }

    @GET
    @Path("/{adapterName}")
    @Produces("text/turtle")
    public Response requestAdapterResourceInstancesTTL(@PathParam("adapterName") String adapterName) throws JMSException {

        Model inputModel = createRequestModel(adapterName, null);

        if (inputModel != null) {
            Message request = createRDFMessage(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_REQUEST);
            sendRequest(request);
            Message result = waitForResult(request);
            return createRESTResponse(getResult(result), null);
        }

        return createRESTResponse(null, null);
    }

    @GET
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/turtle")
    public Response requestSingleResourceInstanceTTL(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = createRequestModel(adapterName, instanceName);

        if (inputModel != null) {
          Message request = createRDFMessage(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_REQUEST);
          sendRequest(request);
          Message result = waitForResult(request);
          return createRESTResponse(getResult(result), null);
        }

        return createRESTResponse(null, null);
    }

    @PUT
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/html")
    public Response createResourceInstance(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = createCreateModel(adapterName, instanceName);

        if (inputModel != null) {
            Message request = createRDFMessage(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CREATE);
            sendRequest(request);
            Message result = waitForResult(request);
            return createRESTResponse(getResult(result), Response.Status.CREATED);
        }

        return createRESTResponse(null, null);
    }

    @PUT
    @Path("/{adapterName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public Response createResourceInstanceWithRDF(@PathParam("adapterName") String adapterName, String rdfInput) {

        Model inputModel = createCreateModel(MessageBusMsgFactory.parseSerializedModel(rdfInput));

        Message request = createRDFMessage(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CREATE);
        sendRequest(request);
        Message result = waitForResult(request);
        return createRESTResponse(getResult(result), Response.Status.CREATED);
    }

    @POST
    @Path("/{adapterName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public Response configureResourceInstance(@PathParam("adapterName") String adapterName, String rdfInput) {

        Model inputModel = createConfigureModel(MessageBusMsgFactory.parseSerializedModel(rdfInput));

        Message request = createRDFMessage(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CONFIGURE);
        sendRequest(request);
        Message result = waitForResult(request);
        return createRESTResponse(getResult(result), null);
    }

    @DELETE
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/html")
    public Response adapterReleaseInstance(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = createReleaseModel(adapterName, instanceName);

        if (inputModel != null) {
            Message request = createRDFMessage(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_RELEASE);
            sendRequest(request);
            Message result = waitForResult(request);
            return createRESTResponse(getResult(result), null);
        }

        return createRESTResponse(null, null);
    }

    private Model createRequestModel(String adapterName, String instanceName) {

        String[] adapterParams = getAdapterParams(adapterName);

        if (adapterParams != null) {
            Model rdfModel = createDefaultModel();

            if (instanceName != null) {
                // If instance specified, request that resource instance
                addInstanceToModel(rdfModel, adapterParams[1], instanceName);
            } else {
                // If no instance specified, request whole adapter
                rdfModel = createDefaultModel(adapterParams);
            }
            setAdapterPrefix(rdfModel, adapterParams);

            return MessageBusMsgFactory.createMsgRequest(rdfModel);
        }

        return null;
    }

    private Model createDiscoverModel() {
        return MessageBusMsgFactory.createMsgDiscover(null);
    }

    private Model createReleaseModel(String adapterName, String instanceName) {

        String[] adapterParams = getAdapterParams(adapterName);

        if (adapterParams != null) {
            Model rdfModel = createDefaultModel(adapterParams);

            addInstanceToModel(rdfModel, adapterParams[1], instanceName);
            setAdapterPrefix(rdfModel, adapterParams);

            return MessageBusMsgFactory.createMsgRelease(rdfModel);
        }

        return null;
    }

    private Model createCreateModel(String adapterName, String instanceName) {

        String[] adapterParams = getAdapterParams(adapterName);

        if (adapterParams != null) {
            Model rdfModel = createDefaultModel(adapterParams);

            addInstanceToModel(rdfModel, adapterParams[1], instanceName);
            setAdapterPrefix(rdfModel, adapterParams);

            return MessageBusMsgFactory.createMsgCreate(rdfModel);
        }

        return null;
    }

    private Model createCreateModel(Model rdfModel) {
        return MessageBusMsgFactory.createMsgCreate(rdfModel);
    }

    private Model createConfigureModel(Model rdfModel) {
        return MessageBusMsgFactory.createMsgConfigure(rdfModel);
    }

    private Response createRESTResponse(String responseString, Status successResponseStatus) {
        if (responseString == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Adapter not found").build();
        } else if (responseString.equals(Response.Status.BAD_REQUEST.name())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Resource not processed").build();
        } else if (responseString.equals(Response.Status.NOT_FOUND.name())) {
            return Response.status(Response.Status.NOT_FOUND).entity("Resource not found").build();
        } else if (responseString.equals(Response.Status.REQUEST_TIMEOUT.name())) {
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("Time out, please try again").build();
        } else if (responseString.equals(Response.Status.CONFLICT.name())) {
          return Response.status(Response.Status.CONFLICT).entity("Conflict").build();
        } else {
            if(successResponseStatus != null){
                return Response.status(successResponseStatus).entity(responseString).build();
            }
            return Response.ok(responseString, "text/turtle").build();
        }
    }

    private String[] getAdapterParams(String paramAdapterName) {
        for (String adapterName : adapterSpecificParameters.keySet()) {
            if (paramAdapterName.equals(adapterName)) {
                return adapterSpecificParameters.get(adapterName);
            }
        }

        // Try refresh adapter list and search again
        refreshTestbedAdapterParameters();

        for (String adapterName : adapterSpecificParameters.keySet()) {
            if (paramAdapterName.equals(adapterName)) {
                return adapterSpecificParameters.get(adapterName);
            }
        }

        return null;
    }

    private void addInstanceToModel(Model rdfModel, String instanceType, String instanceName) {
        Resource resourceType = rdfModel.createResource(instanceType);
        Resource resourceInstance = rdfModel.createResource("http://federation.av.tu-berlin.de/about#" + instanceName);
        resourceInstance.addProperty(RDF.type, resourceType);
    }

    private void setAdapterPrefix(Model rdfModel, String[] adapterParams) {
        rdfModel.setNsPrefix(adapterParams[2], adapterParams[3]);
    }

    private Model createDefaultModel(String[] adapterParams) {
        Model rdfModel = ModelFactory.createDefaultModel();
        addInstanceToModel(rdfModel, adapterParams[0], adapterParams[4]);
        return rdfModel;
    }

    private static Model createDefaultModel() {
        return ModelFactory.createDefaultModel();
    }

    private Message createRDFMessage(final String rdfInput, final String methodType) {
        final Message message = this.context.createMessage();

        try {
          message.setStringProperty(IMessageBus.METHOD_TYPE, methodType);
          message.setStringProperty(IMessageBus.SERIALIZATION, IMessageBus.SERIALIZATION_DEFAULT);
          message.setStringProperty(IMessageBus.RDF, rdfInput);
          message.setJMSCorrelationID(UUID.randomUUID().toString());
        } catch (JMSException e) {
          LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return message;
    }

    private String getResult(final Message result) {
        String resources = Response.Status.REQUEST_TIMEOUT.name();

        NorthboundAPI.LOGGER.log(Level.INFO, "Received reply.");
        if (null != result) {
            try {
              resources = result.getStringProperty(IMessageBus.RDF);
            } catch (JMSException e) {
              LOGGER.log(Level.SEVERE, e.getMessage());
            }
            
            Model resultModel = MessageBusMsgFactory.parseSerializedModel(resources);
            if(resultModel != null && resultModel.size() != 0){
              resultModel.removeAll(resultModel.getResource(MessageBusOntologyModel.internalMessage.getURI()), null, null);
              resources = MessageBusMsgFactory.serializeModel(resultModel);
            }
        }
        return resources;
    }

    private void sendRequest(final Message message) {
        NorthboundAPI.LOGGER.log(Level.INFO, "Sending request...");
        this.context.createProducer().send(this.topic, message);
    }

    private Message waitForResult(final Message message) {
        String filter = null;
        try {
          filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
        } catch (JMSException e) {
          LOGGER.log(Level.SEVERE, e.getMessage());
        }
        final Message rcvMessage = this.context.createConsumer(this.topic, filter).receive(IMessageBus.TIMEOUT);
        return rcvMessage;
    }

}
