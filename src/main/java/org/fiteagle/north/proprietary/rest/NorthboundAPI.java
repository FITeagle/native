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
import com.hp.hpl.jena.rdf.model.SimpleSelector;
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
        String[] testbedAdapterParams = { "http://fiteagle.org/ontology#Testbed", "http://fiteagle.org/ontology#Testbed", "fiteagle", "http://fiteagle.org/ontology#", "FITEAGLE_Testbed" };
        adapterSpecificParameters.put("testbed", testbedAdapterParams);
    }

    /**
     * This method will refresh the list of adapters available in the testbed It gets called every time an adapter is not found in the Map "adapterSpecificParameters"
     */
    private void refreshTestbedAdapterParameters() {

        resetAdapterParameters();

        Model inputModel = createRequestModel("testbed", "FITEAGLE_Testbed");

        if (inputModel != null) {
            try {
                Message request = this.createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_REQUEST);
                sendRequest(request);
                Message result = waitForResult(request);

                Model response = MessageBusMsgFactory.parseSerializedModel(getResult(result));

                // Get contained adapter names
                // :FITEAGLE_Testbed fiteagle:containsAdapter :ADeployedMotorAdapter1.
                StmtIterator adapterIterator = response.listStatements(new SimpleSelector(null, RDFS.subClassOf, MessageBusOntologyModel.classAdapter));
                while (adapterIterator.hasNext()) {
                    String[] adapterSpecificParams = new String[5];
                    Statement currentAdapterTypeStatement = adapterIterator.next();

                    // Find out what resource this adapter implements
                    StmtIterator adapterImplementsIterator = response.listStatements(new SimpleSelector(currentAdapterTypeStatement.getSubject(), MessageBusOntologyModel.propertyFiteagleImplements,
                            (RDFNode) null));
                    while (adapterImplementsIterator.hasNext()) {
                        adapterSpecificParams[0] = currentAdapterTypeStatement.getSubject().toString();
                        adapterSpecificParams[1] = adapterImplementsIterator.next().getResource().toString();
                    }

                    // Find out the name of the adapter instance and its namespace/prefix
                    StmtIterator adapterTypeIterator = response.listStatements(new SimpleSelector(null, RDF.type, currentAdapterTypeStatement.getSubject()));
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
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
    
  @GET
  @Path("/")
  @Produces("text/turtle")
  public Response getAllResourcesTTL() throws JMSException {
    
    Model requestModel = createDefaultModel();
    Resource resource = requestModel.createResource();
    resource.addProperty(RDFS.subClassOf, MessageBusOntologyModel.classResource);
    
    requestModel = MessageBusMsgFactory.createMsgRequest(requestModel);
    
    try {
      Message request = createRequest(MessageBusMsgFactory.serializeModel(requestModel), IMessageBus.TYPE_REQUEST);
      sendRequest(request);
      Message result = waitForResult(request);
      return createRESTResponse(getResult(result), null);
    } catch (JMSException e) {
      e.printStackTrace();
    }
    
    return createRESTResponse(null, null);
  }

    @GET
    @Path("/discover")
    @Produces("text/turtle")
    public Response discoverAllTTL() throws JMSException {

        Model inputModel = createDiscoverModel();

        if (inputModel != null) {
            try {
                Message request = createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_DISCOVER);
                sendRequest(request);
                Message result = waitForResult(request);
                return createRESTResponse(getResult(result), null);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return createRESTResponse(null, null);
    }

    @GET
    @Path("/{adapterName}")
    @Produces("text/turtle")
    public Response requestAdapterResourceInstancesTTL(@PathParam("adapterName") String adapterName) throws JMSException {

        Model inputModel = createRequestModel(adapterName, null);

        if (inputModel != null) {
            try {
                Message request = createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_REQUEST);
                sendRequest(request);
                Message result = waitForResult(request);
                return createRESTResponse(getResult(result), null);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return createRESTResponse(null, null);
    }

    @GET
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/turtle")
    public Response requestSingleResourceInstanceTTL(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = createRequestModel(adapterName, instanceName);

        if (inputModel != null) {
            try {
                Message request = createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_REQUEST);
                sendRequest(request);
                Message result = waitForResult(request);
                return createRESTResponse(getResult(result), null);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return createRESTResponse(null, null);
    }

    @PUT
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/html")
    public Response createResourceInstance(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = createCreateModel(adapterName, instanceName);

        if (inputModel != null) {
            try {
                Message request = createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CREATE);
                sendRequest(request);
                Message result = waitForResult(request);
                return createRESTResponse(getResult(result), Response.Status.CREATED);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        return createRESTResponse(null, null);
    }

    @PUT
    @Path("/{adapterName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public Response createResourceInstanceWithRDF(@PathParam("adapterName") String adapterName, String rdfInput) {

        Model inputModel = createCreateModel(MessageBusMsgFactory.parseSerializedModel(rdfInput));

        try {
            Message request = createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CREATE);
            sendRequest(request);
            Message result = waitForResult(request);
            return createRESTResponse(getResult(result), Response.Status.CREATED);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        return createRESTResponse(null, null);
    }

    @POST
    @Path("/{adapterName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/html")
    public Response configureResourceInstance(@PathParam("adapterName") String adapterName, String rdfInput) {

        Model inputModel = createConfigureModel(MessageBusMsgFactory.parseSerializedModel(rdfInput));

        try {
            Message request = createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_CONFIGURE);
            sendRequest(request);
            Message result = waitForResult(request);
            return createRESTResponse(getResult(result), null);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        return createRESTResponse(null, null);
    }

    @DELETE
    @Path("/{adapterName}/{instanceName}")
    @Produces("text/html")
    public Response adapterReleaseInstance(@PathParam("adapterName") String adapterName, @PathParam("instanceName") String instanceName) {

        Model inputModel = createReleaseModel(adapterName, instanceName);

        if (inputModel != null) {
            try {
                Message request = createRequest(MessageBusMsgFactory.serializeModel(inputModel), IMessageBus.TYPE_RELEASE);
                sendRequest(request);
                Message result = waitForResult(request);
                return createRESTResponse(getResult(result), null);
            } catch (JMSException e) {
                e.printStackTrace();
            }
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
        } else if (responseString.equals(IMessageBus.STATUS_400)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Resource not processed").build();
        } else if (responseString.equals(IMessageBus.STATUS_404)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Resource not found").build();
        } else if (responseString.equals(IMessageBus.STATUS_408)) {
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("Time out, please try again").build();
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
        Resource resourceInstance = rdfModel.createResource("http://fiteagleinternal#" + instanceName);
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

    private Model createDefaultModel() {
        return ModelFactory.createDefaultModel();
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
        final String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
        final Message rcvMessage = this.context.createConsumer(this.topic, filter).receive(IMessageBus.TIMEOUT);
        return rcvMessage;
    }

}
