package org.fiteagle.proprietary.rest;

import java.io.IOException;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

@Path("/node")
public class NodePresenter extends ObjectPresenter{
  
  public NodePresenter() {
  }
  
  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public long add(Node node) throws JMSException, JsonProcessingException {
    Message message = context.createMessage();
    String nodeJSON = objectMapper.writeValueAsString(node);
    message.setStringProperty(UserManager.TYPE_PARAMETER_NODE_JSON, nodeJSON);
    Message rcvMessage = sendMessage(message, UserManager.ADD_NODE);
    
    checkForExceptions(rcvMessage);
    return rcvMessage.getLongProperty(IMessageBus.TYPE_RESULT);
  }
  
  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Node> getAllNodes() throws JsonParseException, JsonMappingException, IOException{
    Message message = context.createMessage();
    Message rcvMessage = sendMessage(message, UserManager.GET_ALL_NODES);
    
    checkForExceptions(rcvMessage);
    return objectMapper.readValue(getResultString(rcvMessage), new TypeReference<List<Node>>(){});
  }
  
}
