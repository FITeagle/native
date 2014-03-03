package org.fiteagle.fnative.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.api.core.IResourceRepository.Serialization;


@Path("/repo")
public class ResourceRepository{
  
  private static Logger log = Logger.getLogger(ResourceRepository.class.toString());
  
  private IResourceRepository repo;
  
  public ResourceRepository() throws NamingException{
    final Context context = new InitialContext();
    repo = (IResourceRepository) context.lookup("java:global/core-resourcerepository/ResourceRepository");
  }
  
  @GET
  @Path("/resources.rdf")
  @Produces("application/rdf+xml")
  public String listResourcesXML() {
	  log.log(Level.INFO, "Getting resources as RDF...");
      return repo.listResources(Serialization.XML);
  }

  @GET
  @Path("/resources.ttl")
  @Produces("text/turtle")
  public String listResourcesTTL() {
	  log.log(Level.INFO, "Getting resources as TTL...");
      return repo.listResources(Serialization.TTL);
  }

}
