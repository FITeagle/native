package org.fiteagle.proprietary.rest;

import java.io.IOException;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
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
import org.fiteagle.api.core.usermanagement.Class;
import org.fiteagle.api.core.usermanagement.Task;
import org.fiteagle.api.core.usermanagement.User.VIEW;
import org.fiteagle.api.core.usermanagement.UserManager;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

@Path("/class")
public class ClassPresenter extends ObjectPresenter {
  
  public ClassPresenter() {
  }
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @JsonView(VIEW.PUBLIC.class)
  public Class get(@PathParam("id") long id) throws JMSException, JsonParseException, JsonMappingException, IOException {
    Message message = context.createMessage();
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, id);
    Message rcvMessage = sendMessage(message, UserManager.GET_CLASS);
    
    checkForExceptions(rcvMessage);
    String resultJSON = getResultString(rcvMessage);
    return objectMapper.readValue(resultJSON, Class.class);
  }
  
  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public long add(Class targetClass) throws JMSException, JsonProcessingException {
    Message message = context.createMessage();
    String classJSON = objectMapper.writeValueAsString(targetClass);
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, targetClass.getOwner().getUsername());
    message.setStringProperty(UserManager.TYPE_PARAMETER_CLASS_JSON, classJSON);
    Message rcvMessage = sendMessage(message, UserManager.ADD_CLASS);
    
    checkForExceptions(rcvMessage);
    return rcvMessage.getLongProperty(IMessageBus.TYPE_RESULT);
  }
  
  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") long id) throws JMSException {
    Message message = context.createMessage();
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, id);
    Message rcvMessage = sendMessage(message, UserManager.DELETE_CLASS);
    
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @POST
  @Path("{id}/task/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public long addTask(@PathParam("id") long id, Task task) throws JMSException, JsonProcessingException {    
    Message message = context.createMessage();
    String taskJSON = objectMapper.writeValueAsString(new Task(task.getName(), task.getDescription()));  
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, id);      
    message.setStringProperty(UserManager.TYPE_PARAMETER_TASK_JSON, taskJSON);
    Message rcvMessage = sendMessage(message, UserManager.ADD_TASK);
    
    checkForExceptions(rcvMessage);
    return rcvMessage.getLongProperty(IMessageBus.TYPE_RESULT);
  }
  
  @DELETE
  @Path("{classId}/task/{taskId}")
  public Response deletePublicKey(@PathParam("classId") long classId, @PathParam("taskId") long taskId) throws JMSException {
    Message message = context.createMessage();
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, classId);      
    message.setLongProperty(UserManager.TYPE_PARAMETER_TASK_ID, taskId);
    Message rcvMessage = sendMessage(message, UserManager.REMOVE_TASK);
    
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  @JsonView(VIEW.PUBLIC.class)
  public List<Class> getAllClasses() throws JsonParseException, JsonMappingException, IOException{
    Message message = context.createMessage();
    Message rcvMessage = sendMessage(message, UserManager.GET_ALL_CLASSES);
    
    checkForExceptions(rcvMessage);
    return objectMapper.readValue(getResultString(rcvMessage), new TypeReference<List<Class>>(){});
  }

}
