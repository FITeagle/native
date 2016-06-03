package org.fiteagle.Native.start;

import info.openmultinet.ontology.vocabulary.Omn_resource;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.TimerHelper;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

@Startup
@Singleton
public class StartUp {

    private static final Logger LOGGER = Logger.getLogger(StartUp.class
            .getName());

    @javax.annotation.Resource
    private TimerService timerService;
    
    private Boolean rdfReady;
    private Boolean allreadySearchingForTripletStore;
    private InitialContext initialContext;

//	@Inject
//	private TimerHelper helper;

    Model defaultModel;
    private int failureCounter = 0;
    private static String resourceUri = "http://localhost/resource/Native";

    @PostConstruct
    public void addNativeApi() {
        setDefaultModel();
        
		try {
			initialContext = new InitialContext();
			refreshGlobalVariables();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	TimerConfig config = new TimerConfig();
		config.setPersistent(false);
        timerService.createIntervalTimer(0, 5000, config);
//		helper.setNewTimer(new NativeAPI());

    }

    private Model setDefaultModel() {
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource(resourceUri);
        resource.addProperty(Omn_resource.hasInterface, "/native/api/lodliveTEST");
        resource.addProperty(Omn_resource.hasInterface, "/native/api/resources");
        resource.addProperty(Omn_resource.hasInterface,
                "/native/api/resources/testbed");
        resource.addProperty(Omn_resource.hasInterface,
                "/native/api/resources/${resourceName}");
        resource.addProperty(Omn_resource.hasInterface,
                "/native/api/resources/${adapterName}/instances");
        defaultModel = model;

        return model;
    }

    @Timeout
    public void timerMethod(Timer timer) {
    	refreshGlobalVariables();

        
    	if(!rdfReady && allreadySearchingForTripletStore){
         	LOGGER.log(Level.SEVERE,"Someone is allready searching for Database - I'll drink a coffe");
    	}else if(rdfReady){
        	
    		
    		LOGGER.log(Level.SEVERE,"Someone found Database"); 
          	try{
                if (defaultModel == null) {
                    TripletStoreAccessor.addResource(setDefaultModel()
                            .getResource(resourceUri));
                    Iterator<Timer> timerIterator = timerService.getAllTimers().iterator();
                    while(timerIterator.hasNext()){
                    	timerIterator.next().cancel();
                    	}
                    }else {
                    TripletStoreAccessor.addResource(defaultModel
                            .getResource(resourceUri));
                    Iterator<Timer> timerIterator = timerService.getAllTimers().iterator();
                    while(timerIterator.hasNext()){
                    	timerIterator.next().cancel();
                    	}
                    }
            	
        	}catch (ResourceRepositoryException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
          	
    	
    	}else{
        	LOGGER.log(Level.SEVERE,"Database is not ready and noone is searching for it - Please check the deployment of FederationManager!"); 
        }
    }
    
    public void refreshGlobalVariables(){
    	try {
			rdfReady = (Boolean) initialContext.lookup("java:global/RDF-Database-Ready");
	     	allreadySearchingForTripletStore = (Boolean) initialContext.lookup("java:global/RDF-Database-Testing");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    

//	class NativeAPI implements Callable<Void> {
//		 
//				@Override
//				public Void call() throws ResourceRepositoryException {
//					if (defaultModel == null) {
//						TripletStoreAccessor.addResource(setDefaultModel().getResource(
//								"http://localhost/resource/Native"));
//					} else {
//						TripletStoreAccessor.addResource(defaultModel
//								.getResource("http://localhost/resource/Native"));
//					}
//					return null;
//				}
//	}


}
