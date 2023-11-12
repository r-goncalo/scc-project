package scc.srv;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.ws.rs.core.Application;

public class MainApplication extends Application
{
	private Set<Object> singletons = new HashSet<Object>();
	private Set<Class<?>> resources = new HashSet<Class<?>>();

	public MainApplication() {
		Locale.setDefault(Locale.US);//todo check if this is needed

		resources.add(ControlResource.class);
		resources.add(LogResource.class);

		singletons.add( new MediaResource());
		singletons.add( new UserResource());
		singletons.add( new HouseResource());
		singletons.add( new RentalResource());
		singletons.add( new QuestionResource());

	}

	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
