package org.fiteagle.north.proprietary.rest;

import info.openmultinet.ontology.vocabulary.Omn;

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
    String query = "DESCRIBE ?resource WHERE {?resource <"+RDFS.subClassOf.getURI()+"> <"+ Omn.Resource + ">. }";
    return processQuery(query, IMessageBus.SERIALIZATION_TURTLE);
  }
  
  @GET
  @Path("/")
  @Produces("application/ld+json")
  public Response getAllResourcesJSON() throws JMSException {
    String query = "DESCRIBE ?resource WHERE {?resource <"+RDFS.subClassOf.getURI()+"> <"+ Omn.Resource + ">. }";
    return processQuery(query, IMessageBus.SERIALIZATION_JSONLD);
  }
  
  @GET
  @Path("/{resourceName}")
  @Produces("text/turtle")
  public Response describeResourceTTL(@PathParam("resourceName") String resourceName) throws JMSException {
    String query = "DESCRIBE <"+INSTANCE_PREFIX+resourceName+">";
    return processQuery(query, IMessageBus.SERIALIZATION_TURTLE);
  }
  
  @GET
  @Path("/{resourceName}")
  @Produces("application/ld+json")
  public Response describeResourceJSON(@PathParam("resourceName") String resourceName) throws JMSException {
    String query = "DESCRIBE <"+INSTANCE_PREFIX+resourceName+">";
    return processQuery(query, IMessageBus.SERIALIZATION_JSONLD);
  }
  
  @GET
  @Path("/{adapterName}/instances")
  @Produces("text/turtle")
  public Response describeAdapterManagedInstancesTTL(@PathParam("adapterName") String adapterName) throws JMSException {
    String query = "DESCRIBE ?resource "
        + "WHERE {"
        + "?resource a ?resourceType .  "
        + "?resourceType <http://open-multinet.info/ontology/omn#implementedBy> ?adapterType . "
        + "<"+INSTANCE_PREFIX+adapterName+"> a ?adapterType}";
    return processQuery(query, IMessageBus.SERIALIZATION_TURTLE);
  }
  
  @GET
  @Path("/{adapterName}/instances")
  @Produces("application/ld+json")
  public Response describeAdapterManagedInstancesJSON(@PathParam("adapterName") String adapterName) throws JMSException {
    String query = "DESCRIBE ?resource "
        + "WHERE {"
        + "?resource a ?resourceType .  "
        + "?resourceType <http://open-multinet.info/ontology/omn#implementedBy> ?adapterType . "
        + "<"+INSTANCE_PREFIX+adapterName+"> a ?adapterType}";
    return processQuery(query, IMessageBus.SERIALIZATION_JSONLD);
  }
  
  private Response processQuery(String query, String serialization){
    Message request = MessageUtil.createSPARQLQueryMessage(query, IMessageBus.TARGET_RESOURCE_ADAPTER_MANAGER, serialization, context);
    context.createProducer().send(topic, request);
    
    Message rcvMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getStringBody(rcvMessage);
    return createRESTResponse(resultString, null);
  }
  
  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces("text/html")
  public Response createResourceInstanceWithRDF(String rdfInput) {
    
    Model inputModel =MessageUtil.parseSerializedModel(rdfInput, IMessageBus.SERIALIZATION_TURTLE);
    
    Message request = MessageUtil.createRDFMessage(inputModel, IMessageBus.TYPE_CREATE, IMessageBus.TARGET_ORCHESTRATOR, IMessageBus.SERIALIZATION_DEFAULT, null, context);
    context.createProducer().send(topic, request);
    
    Message receivedMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getStringBody(receivedMessage);
    return createRESTResponse(resultString, Response.Status.CREATED);
  }
  
  @POST
  @Path("")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces("text/html")
  public Response configureResourceInstance(String rdfInput) {
    Model inputModel =MessageUtil.parseSerializedModel(rdfInput, IMessageBus.SERIALIZATION_TURTLE);
    
    Message request = MessageUtil.createRDFMessage(inputModel, IMessageBus.TYPE_CONFIGURE, IMessageBus.TARGET_ORCHESTRATOR, IMessageBus.SERIALIZATION_DEFAULT, null, context);
    context.createProducer().send(topic, request);
    
    Message receivedMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getStringBody(receivedMessage);
    return createRESTResponse(resultString, null);
  }
  
  @DELETE
  @Path("")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces("text/html")
  public Response releaseResourceInstance(String rdfInput) {
    Model inputModel = MessageUtil.parseSerializedModel(rdfInput, IMessageBus.SERIALIZATION_TURTLE);
    
    Message request = MessageUtil.createRDFMessage(inputModel, IMessageBus.TYPE_DELETE, IMessageBus.TARGET_ORCHESTRATOR, IMessageBus.SERIALIZATION_DEFAULT, null, context);
    context.createProducer().send(topic, request);
    
    Message receivedMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getStringBody(receivedMessage);
    return createRESTResponse(resultString, null);
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
