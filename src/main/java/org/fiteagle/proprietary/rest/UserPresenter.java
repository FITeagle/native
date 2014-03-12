package org.fiteagle.proprietary.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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

import org.fiteagle.api.usermanagement.User;
import org.fiteagle.api.usermanagement.User.Role;
import org.fiteagle.api.usermanagement.UserManager;
import org.fiteagle.api.usermanagement.UserPublicKey;
import org.fiteagle.api.usermanagement.User.InValidAttributeException;
import org.fiteagle.api.usermanagement.User.NotEnoughAttributesException;
import org.fiteagle.api.usermanagement.User.PublicKeyNotFoundException;
import org.fiteagle.api.usermanagement.UserManager.DuplicateEmailException;
import org.fiteagle.api.usermanagement.UserManager.DuplicatePublicKeyException;
import org.fiteagle.api.usermanagement.UserManager.DuplicateUsernameException;
import org.fiteagle.api.usermanagement.UserManager.UserNotFoundException;
import org.fiteagle.core.aaa.authentication.KeyManagement;
import org.fiteagle.core.aaa.authentication.KeyManagement.CouldNotParse;


@Path("/user")
public class UserPresenter{
  
  private UserManager manager;
  
  public UserPresenter() throws NamingException{
    final Context context = new InitialContext();
    manager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager!org.fiteagle.api.usermanagement.UserManager");
  }
  
  @GET
  @Path("{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public User getUser(@PathParam("username") String username, @QueryParam("setCookie") boolean setCookie) {
	try {
      return manager.get(username);
    } catch (EJBException e) {
    	if(e.getCausedByException() instanceof UserNotFoundException){
    	  throw new FiteagleWebApplicationException(404, e.getMessage());
    	}
    	return null;
//    } catch (DatabaseException e) {
//      log.error(e.getMessage());
//      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }   
  }
  
  @PUT
  @Path("{username}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addUser(@PathParam("username") String username, NewUser user) {
    user.setUsername(username);
    try {
      manager.add(createUser(user));
    } catch(EJBException e){
    	if(e.getCausedByException() instanceof DuplicateUsernameException){
		  throw new FiteagleWebApplicationException(409, e.getMessage());
    	}
    	else if(e.getCausedByException() instanceof DuplicateEmailException){
    	  throw new FiteagleWebApplicationException(409, e.getMessage());
    	}
    	else if(e.getCausedByException() instanceof DuplicatePublicKeyException){
    	  throw new FiteagleWebApplicationException(409, e.getMessage());
    	}
    	if(e.getCausedByException() instanceof InValidAttributeException || e.getCausedByException() instanceof NotEnoughAttributesException){
        throw new FiteagleWebApplicationException(422, e.getMessage());
      }
    } catch(NotEnoughAttributesException | InValidAttributeException e){
        throw new FiteagleWebApplicationException(422, e.getMessage());
    }
    return Response.status(201).build();
  }

  @POST
  @Path("{username}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateUser(@PathParam("username") String username, NewUser user) {
    try {
      List<UserPublicKey> publicKeys = createPublicKeys(user.getPublicKeys());  
      manager.update(username, user.getFirstName(), user.getLastName(), user.getEmail(), user.getAffiliation(), user.getPassword(), publicKeys);
    } catch(EJBException e){
      if(e.getCausedByException() instanceof UserNotFoundException){
        throw new FiteagleWebApplicationException(404, e.getMessage());
      }
      if(e.getCausedByException() instanceof DuplicateEmailException){
        throw new FiteagleWebApplicationException(409, e.getMessage());
      }
      if(e.getCausedByException() instanceof DuplicatePublicKeyException){
        throw new FiteagleWebApplicationException(409, e.getMessage());
      }
      if(e.getCausedByException() instanceof InValidAttributeException || e.getCausedByException() instanceof NotEnoughAttributesException){
        throw new FiteagleWebApplicationException(422, e.getMessage());
      }
    } catch(NotEnoughAttributesException | InValidAttributeException e){
        throw new FiteagleWebApplicationException(422, e.getMessage());
    }
    return Response.status(200).build();
  }

  @POST
  @Path("{username}/role/{role}")
  public Response setRole(@PathParam("username") String username, @PathParam("role") Role role) {
    try {
      manager.setRole(username, role);
    } catch(EJBException e){
      if(e.getCausedByException() instanceof UserNotFoundException){
        throw new FiteagleWebApplicationException(404, e.getMessage());
      }
    }
    return Response.status(200).build();
  }
  
  @POST
  @Path("{username}/pubkey/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addPublicKey(@PathParam("username") String username, NewPublicKey pubkey) {    
    PublicKey key;
    try {
      key = KeyManagement.getInstance().decodePublicKey(pubkey.getPublicKeyString());
    } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e1) {
      throw new FiteagleWebApplicationException(422, e1.getMessage());
    }
  
    try {
      manager.addKey(username, new UserPublicKey(key, pubkey.getDescription(), pubkey.getPublicKeyString()));
    } catch(EJBException e){
      if(e.getCausedByException() instanceof UserNotFoundException){
        throw new FiteagleWebApplicationException(404, e.getMessage());
      }
      if(e.getCausedByException() instanceof DuplicatePublicKeyException){
        throw new FiteagleWebApplicationException(409, e.getMessage());
      }
      if(e.getCausedByException() instanceof InValidAttributeException || e.getCausedByException() instanceof NotEnoughAttributesException || e.getCausedByException() instanceof CouldNotParse){
        throw new FiteagleWebApplicationException(422, e.getMessage());
      }
    } catch(NotEnoughAttributesException | InValidAttributeException | CouldNotParse e){
      throw new FiteagleWebApplicationException(422, e.getMessage());
    }
    return Response.status(200).build();
  }
  
  @DELETE
  @Path("{username}/pubkey/{description}")
  public Response deletePublicKey(@PathParam("username") String username, @PathParam("description") String description) {
    try {
      manager.deleteKey(username, decode(description));
    } catch(EJBException e){
      if(e.getCausedByException() instanceof UserNotFoundException){
        throw new FiteagleWebApplicationException(404, e.getMessage());
      }
      if(e.getCausedByException() instanceof InValidAttributeException){
        throw new FiteagleWebApplicationException(422, e.getMessage());
      }
    } catch(InValidAttributeException e){
      throw new FiteagleWebApplicationException(422, e.getMessage());
    }
    return Response.status(200).build();
  }
  
  @POST
  @Path("{username}/pubkey/{description}/description")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response renamePublicKey(@PathParam("username") String username, @PathParam("description") String description, String newDescription) {    
    try {
      manager.renameKey(username, decode(description), newDescription);
    } catch(EJBException e){
      if(e.getCausedByException() instanceof UserNotFoundException || e.getCausedByException() instanceof PublicKeyNotFoundException){
        throw new FiteagleWebApplicationException(404, e.getMessage());
      }
      if(e.getCausedByException() instanceof DuplicatePublicKeyException){
        throw new FiteagleWebApplicationException(409, e.getMessage());
      }
      if(e.getCausedByException() instanceof InValidAttributeException){
        throw new FiteagleWebApplicationException(422, e.getMessage());
      }
    } catch(InValidAttributeException e){
      throw new FiteagleWebApplicationException(422, e.getMessage());
    }
    return Response.status(200).build();
  }
  
  @DELETE
  @Path("{username}")
  public Response deleteUser(@PathParam("username") String username) {
    manager.delete(username);
    return Response.status(200).build();
  }
  
  @POST
  @Path("{username}/certificate")
  @Consumes(MediaType.TEXT_PLAIN)
  public String createUserCertAndPrivateKey(@PathParam("username") String username, String passphrase) {  
    try {      
      return manager.createUserKeyPairAndCertificate(username, decode(passphrase));
    } catch (Exception e) {
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }    
  }
  
  @GET
  @Path("{username}/pubkey/{description}/certificate")
  @Produces(MediaType.TEXT_PLAIN)
  public String getUserCertificateForPublicKey(@PathParam("username") String username, @PathParam("description") String description) {
    try {
      return manager.createUserCertificateForPublicKey(username, decode(description));
    } catch (PublicKeyNotFoundException e){
      throw new FiteagleWebApplicationException(404, e.getMessage());
    } catch (Exception e) {
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }    
  } 
//  
//  @DELETE
//  @Path("{username}/cookie")
//  public Response deleteCookie(@PathParam("username") String username){
//    UserAuthenticationFilter.getInstance().deleteCookie(username);
//    return Response.status(200).build();
//  }
  
  private String decode(String string){    
    try {
      return URLDecoder.decode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private User createUser(NewUser newUser){
    List<UserPublicKey> publicKeys = createPublicKeys(newUser.getPublicKeys());    
    return new User(newUser.getUsername(), newUser.getFirstName(), newUser.getLastName(), newUser.getEmail(), newUser.getAffiliation(), newUser.getPassword(), publicKeys);     
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
