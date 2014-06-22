package org.fiteagle.proprietary.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.User.InValidAttributeException;
import org.fiteagle.api.core.usermanagement.User.NotEnoughAttributesException;
import org.fiteagle.api.core.usermanagement.User.PublicKeyNotFoundException;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserManager.FiteagleClassNotFoundException;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicateEmailException;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicatePublicKeyException;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicateUsernameException;
import org.fiteagle.api.core.usermanagement.UserManager.UserNotFoundException;
import org.fiteagle.api.core.usermanagement.UserPublicKey;
import org.fiteagle.core.aaa.authentication.KeyManagement;
import org.fiteagle.core.aaa.authentication.KeyManagement.CouldNotParse;
import org.fiteagle.core.aaa.authentication.PasswordUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


@Path("/user")
public class UserPresenter{
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private final static int TIMEOUT_TIME_MS = 10000;
  
  public UserPresenter() {
  }
  
  @GET
  @Path("{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public User getUser(@PathParam("username") String username, @QueryParam("setCookie") boolean setCookie) throws JMSException {
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);
    final String filter = sendMessage(message, UserManager.GET_USER);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    String resultJSON = getResultString(rcvMessage);
    return new Gson().fromJson(resultJSON, User.class);
  }
  
  @PUT
  @Path("{username}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response add(@PathParam("username") String username, NewUser user) throws JMSException {
    user.setUsername(username);
    Message message = context.createMessage();
    String userJSON = new Gson().toJson(createUser(user));
    message.setStringProperty(UserManager.TYPE_PARAMETER_USER_JSON, userJSON);
    final String filter = sendMessage(message, UserManager.ADD_USER);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(201).build();
  }

  @POST
  @Path("{username}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response update(@PathParam("username") String username, NewUser user) throws JMSException {
    String pubKeysJSON = new Gson().toJson(createPublicKeys(user.getPublicKeys()));
    Message message = context.createMessage();
    
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);
    message.setStringProperty(UserManager.TYPE_PARAMETER_FIRSTNAME, user.getFirstName());
    message.setStringProperty(UserManager.TYPE_PARAMETER_LASTNAME, user.getLastName());
    message.setStringProperty(UserManager.TYPE_PARAMETER_EMAIL, user.getEmail());
    message.setStringProperty(UserManager.TYPE_PARAMETER_AFFILIATION, user.getAffiliation());
    message.setStringProperty(UserManager.TYPE_PARAMETER_PASSWORD, user.getPassword());
    message.setStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEYS, pubKeysJSON);
    
    final String filter = sendMessage(message, UserManager.UPDATE_USER);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }

  @POST
  @Path("{username}/role/{role}")
  public Response setRole(@PathParam("username") String username, @PathParam("role") Role role) throws JMSException {
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);
    message.setStringProperty(UserManager.TYPE_PARAMETER_ROLE, role.toString());
    final String filter = sendMessage(message, UserManager.SET_ROLE);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @POST
  @Path("{username}/pubkey/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addPublicKey(@PathParam("username") String username, NewPublicKey pubkey) throws JMSException {    
    Message message = context.createMessage();
    PublicKey key;
    try {
      key = KeyManagement.getInstance().decodePublicKey(pubkey.getPublicKeyString());
    } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
      throw new FiteagleWebApplicationException(422, e.getMessage());
    }
    String pubKeyJSON = new Gson().toJson(new UserPublicKey(key, pubkey.getDescription(), pubkey.getPublicKeyString()));  
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    message.setStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY, pubKeyJSON);
    final String filter = sendMessage(message, UserManager.ADD_PUBLIC_KEY);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @DELETE
  @Path("{username}/pubkey/{description}")
  public Response deletePublicKey(@PathParam("username") String username, @PathParam("description") String description) throws JMSException {
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    message.setStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION, decode(description));
    final String filter = sendMessage(message, UserManager.DELETE_PUBLIC_KEY);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @POST
  @Path("{username}/pubkey/{description}/description")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response renamePublicKey(@PathParam("username") String username, @PathParam("description") String description, String newDescription) throws JMSException {    
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    message.setStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION, decode(description));
    message.setStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION_NEW, decode(newDescription));
    final String filter = sendMessage(message, UserManager.RENAME_PUBLIC_KEY);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @DELETE
  @Path("{username}")
  public Response delete(@PathParam("username") String username) throws JMSException {
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    final String filter = sendMessage(message, UserManager.DELETE_USER);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @POST
  @Path("{username}/certificate")
  @Consumes(MediaType.TEXT_PLAIN)
  public String createUserCertAndPrivateKey(@PathParam("username") String username, String passphrase) throws JMSException {  
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    message.setStringProperty(UserManager.TYPE_PARAMETER_PASSPHRASE, decode(passphrase));      
    final String filter = sendMessage(message, UserManager.CREATE_USER_CERT_AND_PRIVATE_KEY);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return getResultString(rcvMessage);
  }
  
  @GET
  @Path("{username}/pubkey/{description}/certificate")
  @Produces(MediaType.TEXT_PLAIN)
  public String getUserCertificateForPublicKey(@PathParam("username") String username, @PathParam("description") String description) throws JMSException {
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    message.setStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION, decode(description));      
    final String filter = sendMessage(message, UserManager.GET_USER_CERT_FOR_PUBLIC_KEY);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return getResultString(rcvMessage);
  } 
  
  @DELETE
  @Path("{username}/cookie")
  public Response deleteCookie(@PathParam("username") String username){
    AuthenticationFilter.getInstance().deleteCookie(username);
    return Response.status(200).build();
  }
  
  @GET
  @Path("{username}/classes")
  @Produces(MediaType.APPLICATION_JSON)
  public List<org.fiteagle.api.core.usermanagement.Class> getAllClassesFromUser(@PathParam("username") String username) throws JMSException{
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    final String filter = sendMessage(message, UserManager.GET_ALL_CLASSES_FROM_USER);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    Type listType = new TypeToken<ArrayList<org.fiteagle.api.core.usermanagement.Class>>() {}.getType();
    return new Gson().fromJson(getResultString(rcvMessage), listType);
  }
  
  @GET
  @Path("{username}/ownedclasses")
  @Produces(MediaType.APPLICATION_JSON)
  public List<org.fiteagle.api.core.usermanagement.Class> getAllClassesOwnedByUser(@PathParam("username") String username) throws JMSException{
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);      
    final String filter = sendMessage(message, UserManager.GET_ALL_CLASSES_OWNED_BY_USER);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    Type listType = new TypeToken<ArrayList<org.fiteagle.api.core.usermanagement.Class>>() {}.getType();
    return new Gson().fromJson(getResultString(rcvMessage), listType);
  }
  
  @POST
  @Path("{username}/class/{id}")
  public Response signUpForClass(@PathParam("username") String username, @PathParam("id") long id) throws JMSException {    
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);   
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, id); 
    final String filter = sendMessage(message, UserManager.SIGN_UP_FOR_CLASS);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  
  @DELETE
  @Path("{username}/class/{id}")
  public Response leaveClass(@PathParam("username") String username, @PathParam("id") long id) throws JMSException {    
    Message message = context.createMessage();
    message.setStringProperty(UserManager.TYPE_PARAMETER_USERNAME, username);   
    message.setLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID, id); 
    final String filter = sendMessage(message, UserManager.LEAVE_CLASS);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    return Response.status(200).build();
  }
  
  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public List<User> getAllUsers() throws JMSException{
    Message message = context.createMessage();
    final String filter = sendMessage(message, UserManager.GET_ALL_USERS);
    
    Message rcvMessage = context.createConsumer(topic, filter).receive(TIMEOUT_TIME_MS);
    checkForExceptions(rcvMessage);
    String resultJSON = getResultString(rcvMessage);
    Type listType = new TypeToken<ArrayList<User>>() {}.getType();
    return new Gson().fromJson(resultJSON, listType);
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
        if(exceptionMessage.startsWith(PublicKeyNotFoundException.class.getSimpleName())){
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
  
  private String decode(String string){    
    try {
      return URLDecoder.decode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private User createUser(NewUser newUser){
    List<UserPublicKey> publicKeys = createPublicKeys(newUser.getPublicKeys());    
    String[] passwordHashAndSalt = PasswordUtil.generatePasswordHashAndSalt(newUser.getPassword());
    User user = null;
    try{
      user = new User(newUser.getUsername(), newUser.getFirstName(), newUser.getLastName(), newUser.getEmail(), newUser.getAffiliation(), passwordHashAndSalt[0], passwordHashAndSalt[1], publicKeys);
    } catch(NotEnoughAttributesException | InValidAttributeException e){
       throw new FiteagleWebApplicationException(422, e.getMessage());
    }
    return user;     
  }
  
  private ArrayList<UserPublicKey> createPublicKeys(List<NewPublicKey> keys) {
    if(keys == null){
      return null;
    }
    ArrayList<UserPublicKey> publicKeys = new ArrayList<>();
    for(NewPublicKey key : keys){
      try {
        publicKeys.add(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key.getPublicKeyString()), key.getDescription(), key.getPublicKeyString()));
      } catch (CouldNotParse e) {
        throw new FiteagleWebApplicationException(422, e.getMessage());
      } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
      }
    }
    return publicKeys;
  }
  
  public static class FiteagleWebApplicationException extends WebApplicationException {  
    private static final long serialVersionUID = 5823637635206011675L;
    public FiteagleWebApplicationException(int status, String message){
      super(Response.status(status).entity(message).build()); 
    }   
  }   
  
}
