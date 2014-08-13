package org.fiteagle.proprietary.rest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.Node;
import org.fiteagle.api.core.usermanagement.User.InValidAttributeException;
import org.fiteagle.api.core.usermanagement.User.NotEnoughAttributesException;
import org.fiteagle.api.core.usermanagement.User.PublicKeyNotFoundException;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicateEmailException;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicatePublicKeyException;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicateUsernameException;
import org.fiteagle.api.core.usermanagement.UserManager.FiteagleClassNotFoundException;
import org.fiteagle.api.core.usermanagement.UserManager.NodeNotFoundException;
import org.fiteagle.api.core.usermanagement.UserManager.UserNotFoundException;
import org.fiteagle.proprietary.rest.UserPresenter.FiteagleWebApplicationException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Path("/node")
public class NodePresenter {
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private final static int TIMEOUT_TIME_MS = 10000;

  public NodePresenter() {
  }
  
  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Node> getAllNodes(){
    Message message = context.createMessage();
    final String filter = sendMessage(message, UserManager.GET_ALL_NODES);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    Type listType = new TypeToken<ArrayList<Node>>() {}.getType();
    return new Gson().fromJson(getResultString(rcvMessage), listType);
  }

  private String sendMessage(Message message, String requestType){
    String filter = "";
    try {
      message.setStringProperty(IMessageBus.TYPE_REQUEST, requestType);
      message.setStringProperty(IMessageBus.TYPE_TARGET, UserManager.TARGET);
      message.setJMSCorrelationID(UUID.randomUUID().toString());
      filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
    } catch (JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
    context.createProducer().send(topic, message);
    return filter;
  }
  
  private String getResultString(Message message){
    String result;    
    try {        
      result = message.getStringProperty(IMessageBus.TYPE_RESULT);
    } catch (JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
    return result;
  }
  
  private void checkForExceptions(Message message){
    if(message == null){
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "timeout while waiting for answer from JMS message bus");    
    }
    try {
      String exceptionMessage = message.getStringProperty(IMessageBus.TYPE_EXCEPTION);
      if(exceptionMessage != null){
        if(exceptionMessage.startsWith(UserNotFoundException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.NOT_FOUND.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(FiteagleClassNotFoundException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.NOT_FOUND.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(PublicKeyNotFoundException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.NOT_FOUND.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(NodeNotFoundException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.NOT_FOUND.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(FiteagleClassNotFoundException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.NOT_FOUND.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(DuplicateUsernameException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.CONFLICT.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(DuplicateEmailException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.CONFLICT.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(DuplicatePublicKeyException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(Response.Status.CONFLICT.getStatusCode(), exceptionMessage);    
        }
        if(exceptionMessage.startsWith(InValidAttributeException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(422, exceptionMessage);    
        }
        if(exceptionMessage.startsWith(NotEnoughAttributesException.class.getSimpleName())){
          throw new FiteagleWebApplicationException(422, exceptionMessage);    
        }
        else{
          throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exceptionMessage);
        }
      }
    } catch (JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
  }
  
}
