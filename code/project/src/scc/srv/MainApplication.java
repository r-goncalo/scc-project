package scc.srv;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;

public class MainApplication extends Application
{
	private Set<Object> singletons = new HashSet<Object>();
	private Set<Class<?>> resources = new HashSet<Class<?>>();

	public MainApplication() {

		LogResource.writeLine("Starting app...");
		LogResource.writeLine("    App startup: region: " + System.getenv("REGION_NAME"));

		resources.add(ControlResource.class);
		resources.add(LogResource.class);

		singletons.add( new MediaResource());
		singletons.add( new UserResource());
		singletons.add( new HouseResource());

		LogResource.writeLine("    App startup: resources added");



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
