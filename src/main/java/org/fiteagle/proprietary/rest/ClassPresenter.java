package org.fiteagle.proprietary.rest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.Class;
import org.fiteagle.api.core.usermanagement.UserManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Path("/class")
public class ClassPresenter extends ObjectPresenter {
  
  public ClassPresenter() {
  }
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Class get(@PathParam("id") long id) throws JMSException {
    Message message = context.createMessage();
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, id);
    final String filter = sendMessage(message, UserManager.GET_CLASS);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    String resultJSON = getResultString(rcvMessage);
    return new Gson().fromJson(resultJSON, Class.class);
  }
  
  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public long add(Class targetClass) throws JMSException {
    Message message = context.createMessage();
    String classJSON = new Gson().toJson(targetClass);
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, targetClass.getOwner().getUsername());
    message.setStringProperty(UserManager.TYPE_PARAMETER_CLASS_JSON, classJSON);
    final String filter = sendMessage(message, UserManager.ADD_CLASS);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return rcvMessage.getLongProperty(IMessageBus.TYPE_RESULT);
  }
  
  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") long id) throws JMSException {
    Message message = context.createMessage();
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, id);
    final String filter = sendMessage(message, UserManager.DELETE_CLASS);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Class> getAllClasses(){
    Message message = context.createMessage();
    final String filter = sendMessage(message, UserManager.GET_ALL_CLASSES);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    Type listType = new TypeToken<ArrayList<Class>>() {}.getType();
    return new Gson().fromJson(getResultString(rcvMessage), listType);
  }

}
