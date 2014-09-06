package org.fiteagle.north.proprietary.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by vju on 9/6/14.
 */
@Path("/lodlive")
public class LodLiveREST {
    @GET
    @Path("/query")
    @Produces("application/sparql-results+json")
    public String handleLodLiveRequest(){
        return "HAllo";
    }
}
