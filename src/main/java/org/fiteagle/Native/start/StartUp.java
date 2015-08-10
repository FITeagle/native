package org.fiteagle.Native.start;

import info.openmultinet.ontology.vocabulary.Omn_resource;

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
	
//	@Inject
//	private TimerHelper helper;

	Model defaultModel;
	private int failureCounter = 0;
	private static String resourceUri = "http://localhost/resource/Native";

	@PostConstruct
	public void addNativeApi() {
		setDefaultModel();
		timerService.createIntervalTimer(0, 5000, new TimerConfig());
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
		if (failureCounter < 10) {
			try {
				if (defaultModel == null) {
					TripletStoreAccessor.addResource(setDefaultModel()
							.getResource(resourceUri));
					timer.cancel();
				} else {
					TripletStoreAccessor.addResource(defaultModel
							.getResource(resourceUri));
					timer.cancel();
				}
			} catch (ResourceRepositoryException e) {
				LOGGER.log(Level.INFO,
						 "Errored while adding something to Database - will try again");
				failureCounter++;
			} catch (HttpException e) {
				LOGGER.log(Level.INFO,
						 "Couldn't find RDF Database - will try again");
				failureCounter++;
			}
		} else {
			LOGGER.log(
					Level.SEVERE,
					"Tried to add something to Database several times, but failed. Please check the OpenRDF-Database");
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
