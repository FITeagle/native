package start;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn_resource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.jena.atlas.logging.Log;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

@Startup
@Singleton
public class StartUp {
	
	private Model defaultModel;	


	@PostConstruct
    public void addNativeApi(){
    	Model model = ModelFactory.createDefaultModel();
    Resource resource = model.createResource("http://localhost/resource/Native");
    resource.addProperty(Omn_resource.hasInterface, "/native/api/lodlive");
    resource.addProperty(Omn_resource.hasInterface, "/native/api/resources");
    resource.addProperty(Omn_resource.hasInterface, "/native/api/resources/testbed");
    resource.addProperty(Omn_resource.hasInterface, "/native/api/resources/${resourceName}");
    resource.addProperty(Omn_resource.hasInterface, "/native/api/resources/${adapterName}/instances");
    try {
		TripletStoreAccessor.addResource(resource);
		defaultModel = model;
	} catch (ResourceRepositoryException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    }
	
	
	@PreDestroy
	public void deleteNativeApi() {
		try{	
	    TripletStoreAccessor.deleteModel(defaultModel);
		} catch (ResourceRepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
