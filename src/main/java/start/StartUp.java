package start;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn_resource;

import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.fiteagle.api.core.TimerHelper;
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
	public void addNativeApi() {
		setDefaultModel();
		TimerHelper timer = new TimerHelper(new NativeAPI());
	}

//	@PreDestroy
//	public void deleteNativeApi() {
//		TimerHelper timer = new TimerHelper(new DeleteNativeAPI());
//	}

	private Model setDefaultModel() {
		Model model = ModelFactory.createDefaultModel();
		Resource resource = model
				.createResource("http://localhost/resource/Native");
		resource.addProperty(Omn_resource.hasInterface, "/native/api/lodlive");
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

	class DeleteNativeAPI implements Callable<Void> {

		@Override
		public Void call() throws ResourceRepositoryException,
				InvalidModelException {
			if (defaultModel == null) {
				TripletStoreAccessor.deleteModel(setDefaultModel());
			} else {
				TripletStoreAccessor.deleteModel(defaultModel);
			}
			return null;
		}
	}

	class NativeAPI implements Callable<Void> {

		@Override
		public Void call() throws ResourceRepositoryException {
			if (defaultModel == null) {
				TripletStoreAccessor.addResource(setDefaultModel().getResource(
						"http://localhost/resource/Native"));
			} else {
				TripletStoreAccessor.addResource(defaultModel
						.getResource("http://localhost/resource/Native"));
			}
			return null;
		}
	}
}
