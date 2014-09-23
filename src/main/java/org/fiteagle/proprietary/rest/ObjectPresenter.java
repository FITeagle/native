package org.fiteagle.proprietary.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.IMessageBus;
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
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectPresenter {
  
  @Inject
  protected JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  protected Topic topic;
  
  protected static ObjectMapper objectMapper = new ObjectMapper();
  
  private final static Logger log = Logger.getLogger(AuthenticationFilter.class.toString());
  
  public ObjectPresenter() {
  }
  
  protected Message sendMessage(Message message, String requestType){
    String filter = "";
    try {
      message.setStringProperty(IMessageBus.TYPE_REQUEST, requestType);
      message.setStringProperty(IMessageBus.TYPE_TARGET, UserManager.TARGET);
      message.setJMSCorrelationID(UUID.randomUUID().toString());
      filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
    } catch (JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
    Message rcvMessage = null;
    while(rcvMessage == null){
      context.createProducer().send(topic, message);
      rcvMessage = context.createConsumer(topic, filter).receive(IMessageBus.TIMEOUT);
    }
    
    return rcvMessage;
  }
  
  protected String getResultString(Message message){
    String result;    
    try {        
      result = message.getStringProperty(IMessageBus.TYPE_RESULT);
    } catch (JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
    return result;
  }

  protected void checkForExceptions(Message message){
    if(message == null){
      log.error("JMS: timeout while waiting for response");
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
          log.error(exceptionMessage);
          throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exceptionMessage);
        }
      }
    } catch (JMSException e) {
      throw new FiteagleWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "JMS Error: "+e.getMessage());    
    }
  }
  
  protected String decode(String string){    
    try {
      return URLDecoder.decode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      log.error(e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }
  
  public static class FiteagleWebApplicationException extends WebApplicationException {  
    private static final long serialVersionUID = 5823637635206011675L;
    public FiteagleWebApplicationException(int status, String message){
      super(Response.status(status).entity(message).build()); 
    }   
  }   
  
}