package org.fiteagle.proprietary.rest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.Node;
import org.fiteagle.api.core.usermanagement.UserManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Path("/node")
public class NodePresenter extends ObjectPresenter{
  
  public NodePresenter() {
  }
  
  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public long add(Node node) throws JMSException {
    Message message = context.createMessage();
    String nodeJSON = new Gson().toJson(node);
    message.setStringProperty(UserManager.TYPE_PARAMETER_NODE_JSON, nodeJSON);
    final String filter = sendMessage(message, UserManager.ADD_NODE);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return rcvMessage.getLongProperty(IMessageBus.TYPE_RESULT);
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
  
}
