package org.fiteagle.north.proprietary.rest;

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
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDFS;

@Path("/resources")
public class NorthboundAPI {
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  @SuppressWarnings("unused")
  private static Logger LOGGER = Logger.getLogger(NorthboundAPI.class.toString());
  
  //TODO: make dynamic
  public final static String INSTANCE_PREFIX = "http://federation.av.tu-berlin.de/about#";
  
  @GET
  @Path("/")
  @Produces("text/turtle")
  public Response getAllResourcesTTL() throws JMSException {
    String query = "DESCRIBE ?resource WHERE {?resource <"+RDFS.subClassOf.getURI()+"> <"+ MessageBusOntologyModel.classResource + ">. }";
    String requestModel = MessageUtil.createSerializedSPARQLQueryModel(query);
    final Message request = MessageUtil.createRDFMessage(requestModel, IMessageBus.TYPE_REQUEST, IMessageBus.SERIALIZATION_DEFAULT, context);
    context.createProducer().send(topic, request);
    
    Message rcvMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getRDFResult(rcvMessage);
    resultString = MessageUtil.getTTLResultModelFromSerializedModel(resultString);
    
    return createRESTResponse(resultString, null);
  }
  
  @GET
  @Path("/{resourceName}")
  @Produces("text/turtle")
  public Response describeResource(@PathParam("resourceName") String resourceName) throws JMSException {
    String query = "DESCRIBE <"+INSTANCE_PREFIX+resourceName+">";
    String requestModel = MessageUtil.createSerializedSPARQLQueryModel(query);
    final Message request = MessageUtil.createRDFMessage(requestModel, IMessageBus.TYPE_REQUEST, IMessageBus.SERIALIZATION_DEFAULT, context);
    context.createProducer().send(topic, request);
    
    Message rcvMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getRDFResult(rcvMessage);
    resultString = MessageUtil.getTTLResultModelFromSerializedModel(resultString);
    
    return createRESTResponse(resultString, null);
  }
  
  @GET
  @Path("/{adapterName}/instances")
  @Produces("text/turtle")
  public Response describeAdapterManagedInstances(@PathParam("adapterName") String adapterName) throws JMSException {
    
    String query = "DESCRIBE ?resource "
        + "WHERE {"
        + "?resource a ?resourceType .  "
        + "?resourceType <http://open-multinet.info/ontology/omn#implementedBy> ?adapterType . "
        + "<"+INSTANCE_PREFIX+adapterName+"> a ?adapterType}";
    
    
    String requestModel = MessageUtil.createSerializedSPARQLQueryModel(query);
    final Message request = MessageUtil.createRDFMessage(requestModel, IMessageBus.TYPE_REQUEST, IMessageBus.SERIALIZATION_DEFAULT, context);
    context.createProducer().send(topic, request);
    
    Message rcvMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getRDFResult(rcvMessage);
    resultString = MessageUtil.getTTLResultModelFromSerializedModel(resultString);
    
    return createRESTResponse(resultString, null);
  }
  
  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces("text/html")
  public Response createResourceInstanceWithRDF(String rdfInput) {
    
    Model inputModel = MessageUtil.createMsgCreate(MessageUtil.parseSerializedModel(rdfInput));
    
    Message request = MessageUtil.createRDFMessage(inputModel, IMessageBus.TYPE_CREATE, IMessageBus.SERIALIZATION_DEFAULT, context);
    context.createProducer().send(topic, request);
    Message result = MessageUtil.waitForResult(request, context, topic);
    return createRESTResponse(MessageUtil.getRDFResult(result), Response.Status.CREATED);
  }
  
  @POST
  @Path("/{adapterName}")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces("text/html")
  public Response configureResourceInstance(@PathParam("adapterName") String adapterName, String rdfInput) {
    
    Model inputModel = MessageUtil.createMsgConfigure(MessageUtil.parseSerializedModel(rdfInput));
    
    Message request = MessageUtil.createRDFMessage(inputModel, IMessageBus.TYPE_CONFIGURE, IMessageBus.SERIALIZATION_DEFAULT, context);
    context.createProducer().send(topic, request);
    Message result = MessageUtil.waitForResult(request, context, topic);
    return createRESTResponse(MessageUtil.getRDFResult(result), null);
  }
  
  @DELETE
  @Path("/{adapterName}")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces("text/html")
  public Response releaseResourceInstance(@PathParam("adapterName") String adapterName, String rdfInput) {
    
    Model inputModel = MessageUtil.createMsgRelease(MessageUtil.parseSerializedModel(rdfInput));
    
    if (inputModel != null) {
      Message request = MessageUtil.createRDFMessage(inputModel, IMessageBus.TYPE_RELEASE, IMessageBus.SERIALIZATION_DEFAULT, context);
      context.createProducer().send(topic, request);
      Message result = MessageUtil.waitForResult(request, context, topic);
      return createRESTResponse(MessageUtil.getRDFResult(result), null);
    }
    
    return createRESTResponse(null, null);
  }
  
  @GET
  @Path("/discover")
  @Produces("text/turtle")
  public Response discoverAllTTL() throws JMSException {
    
    Model inputModel =  MessageUtil.createMsgDiscover(null);
    
    if (inputModel != null) {
      Message request = MessageUtil.createRDFMessage(inputModel, IMessageBus.TYPE_DISCOVER, IMessageBus.SERIALIZATION_DEFAULT, context);
      context.createProducer().send(topic, request);
      //TODO: needs to be refactored, currenty gets just result from 1 adapter
      Message result = MessageUtil.waitForResult(request, context, topic);
      return createRESTResponse(MessageUtil.getRDFResult(result), null);
    }
    
    return createRESTResponse(null, null);
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
      if (successResponseStatus != null) {
        return Response.status(successResponseStatus).entity(responseString).build();
      }
      return Response.ok(responseString, "text/turtle").build();
    }
  }
  
}
