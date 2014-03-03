package org.fiteagle.fnative.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fiteagle.api.core.IResourceRepository;


@Path("/repo")
public class ResourceRepository{
  
  private static Logger log = Logger.getLogger(ResourceRepository.class.toString());
  
  private IResourceRepository repo;
  
  public ResourceRepository() throws NamingException{
    final Context context = new InitialContext();
    repo = (IResourceRepository) context.lookup("java:global/core-resourcerepository/ResourceRepository");
  }
  
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public String listResources() {
	  log.log(Level.INFO, "Getting resources...");
      return repo.listResources();
  }
}
