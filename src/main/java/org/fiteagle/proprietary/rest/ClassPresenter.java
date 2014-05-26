package org.fiteagle.proprietary.rest;

import java.util.List;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.usermanagement.Class;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserManager.CourseNotFoundException;
import org.fiteagle.proprietary.rest.UserPresenter.FiteagleWebApplicationException;

@Path("/class")
public class ClassPresenter {
  
private UserManager manager;
  
  public ClassPresenter() throws NamingException{
    final Context context = new InitialContext();
    manager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager!org.fiteagle.api.core.usermanagement.UserManager");
  }
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Class get(@PathParam("id") long id) {
  try {
      return manager.get(id);
    } catch (EJBException e) {
      if(e.getCausedByException() instanceof CourseNotFoundException){
        throw new FiteagleWebApplicationException(404, e.getMessage());
      }
      return null;
    }   
  }
  
  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public long add(Class targetClass) {
    long id = manager.addClass(targetClass.getOwner().getUsername(), targetClass).getId();
    return id;
  }
  
  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") long id) {
    manager.delete(id);
    return Response.status(200).build();
  }
  
  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Class> getAllClasses(){
    return manager.getAllClasses();
  }
  
}
