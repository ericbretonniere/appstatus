/*
 * Copyright 2010 Capgemini
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 */
package net.sf.appstatus.agent.batch;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import net.sf.appstatus.agent.batch.helpers.NOPJobProgressMonitorAgentFactory;
import net.sf.appstatus.agent.batch.impl.StaticJobProgressMonitorAgentFactoryBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job progress monitor agent factory.
 * 
 * @author Guillaume Mary
 * 
 */
public final class JobProgressMonitorAgentFactory {

	static final int UNINITIALIZED = 0;
	static final int ONGOING_INITILIZATION = 1;
	static final int FAILED_INITILIZATION = 2;
	static final int SUCCESSFUL_INITILIZATION = 3;
	static final int NOP_FALLBACK_INITILIZATION = 4;
	static int INITIALIZATION_STATE = UNINITIALIZED;

	static NOPJobProgressMonitorAgentFactory NOP_FALLBACK_FACTORY = new NOPJobProgressMonitorAgentFactory();
	static NOPJobProgressMonitorAgentFactory TEMP_FACTORY = new NOPJobProgressMonitorAgentFactory();

	private static String STATIC_JOB_PROGRESS_MONITOR_AGENT_FACTORY_BINDER_PATH = "net/sf/appstatus/agent/batch/impl/StaticJobProgressMonitorAgentFactoryBinder.class";

	static final String UNSUCCESSFUL_INIT_MSG = "net.sf.appstatus.agent.batch.JobProgressMonitorAgentFactory could not be successfully initialized.";

	private static Logger log = LoggerFactory
			.getLogger(JobProgressMonitorAgentFactory.class);

	/**
	 * Bind the service monitor factory.
	 */
	private final static void bind() {
		try {
			// the next line does the binding
			StaticJobProgressMonitorAgentFactoryBinder.getSingleton();
			INITIALIZATION_STATE = SUCCESSFUL_INITILIZATION;
		} catch (NoClassDefFoundError ncde) {
			String msg = ncde.getMessage();
			if (msg != null
					&& msg.indexOf("net/sf/appstatus/agent/batch/impl/StaticJobProgressMonitorAgentFactoryBinder") != -1) {
				INITIALIZATION_STATE = NOP_FALLBACK_INITILIZATION;
				log.error("Failed to load class \"net.sf.appstatus.agent.batch.impl.StaticJobProgressMonitorAgentFactoryBinder\".");
				log.error("Defaulting to no-operation (NOP) logger implementation");
			} else {
				failedBinding(ncde);
				throw ncde;
			}
		} catch (java.lang.NoSuchMethodError nsme) {
			String msg = nsme.getMessage();
			if (msg != null
					&& msg.indexOf("net.sf.appstatus.agent.batch.impl.StaticJobProgressMonitorAgentFactoryBinder.getSingleton()") != -1) {
				INITIALIZATION_STATE = FAILED_INITILIZATION;
				log.error("appstatus-core is incompatible with this binding.");
				log.error("Upgrade your binding");
			}
			throw nsme;
		} catch (Exception e) {
			failedBinding(e);
			throw new IllegalStateException(
					"Unexpected initialization failure", e);
		}
	}

	/**
	 * Binding fail.
	 * 
	 * @param t
	 *            cause
	 */
	static void failedBinding(Throwable t) {
		INITIALIZATION_STATE = FAILED_INITILIZATION;
		log.error("Failed to instantiate Job Progress Monitor Agent Factory", t);
	}

	/**
	 * Return a new {@link IJobProgressMonitorAgent} instance.
	 * 
	 * @return a new {@link IJobProgressMonitorAgent} instance
	 */
	public static IJobProgressMonitorAgent getAgent(String executionId) {
		IJobProgressMonitorAgentFactory monitorFactory = getIJobProgressMonitorAgentFactory();
		return monitorFactory.getAgent(executionId);
	}

	/**
	 * Return the {@link IJobProgressMonitorAgentFactory} instance in use.
	 * 
	 * <p>
	 * IJobProgressMonitorAgentFactory instance is bound with this class at
	 * compile time.
	 * 
	 * @return the IJobProgressMonitorAgentFactory instance in use
	 */
	public static IJobProgressMonitorAgentFactory getIJobProgressMonitorAgentFactory() {
		if (INITIALIZATION_STATE == UNINITIALIZED) {
			INITIALIZATION_STATE = ONGOING_INITILIZATION;
			performInitialization();

		}
		switch (INITIALIZATION_STATE) {
		case SUCCESSFUL_INITILIZATION:
			return StaticJobProgressMonitorAgentFactoryBinder.getSingleton()
					.getJobProgressMonitorAgentFactory();
		case NOP_FALLBACK_INITILIZATION:
			return NOP_FALLBACK_FACTORY;
		case FAILED_INITILIZATION:
			throw new IllegalStateException(UNSUCCESSFUL_INIT_MSG);
		case ONGOING_INITILIZATION:
			return TEMP_FACTORY;
		}
		throw new IllegalStateException("Unreachable code");
	}

	private final static void performInitialization() {
		singleImplementationSanityCheck();
		bind();
	}

	/**
	 * Check if the implementation is unique.
	 */
	private static void singleImplementationSanityCheck() {
		try {
			ClassLoader jobProgressMonitorAgentFactoryClassLoader = JobProgressMonitorAgentFactory.class
					.getClassLoader();
			Enumeration<URL> paths;
			if (jobProgressMonitorAgentFactoryClassLoader == null) {
				paths = ClassLoader
						.getSystemResources(STATIC_JOB_PROGRESS_MONITOR_AGENT_FACTORY_BINDER_PATH);
			} else {
				paths = jobProgressMonitorAgentFactoryClassLoader
						.getResources(STATIC_JOB_PROGRESS_MONITOR_AGENT_FACTORY_BINDER_PATH);
			}
			List<URL> implementationList = new ArrayList<URL>();
			while (paths.hasMoreElements()) {
				URL path = paths.nextElement();
				implementationList.add(path);
			}
			if (implementationList.size() > 1) {
				log.error("Class path contains multiple Job Progress Monitor Agent bindings.");
				for (int i = 0; i < implementationList.size(); i++) {
					log.error("Found binding in [" + implementationList.get(i)
							+ "]");
				}
			}
		} catch (IOException ioe) {
			log.error("Error getting resources from path", ioe);
		}
	}

	/**
	 * Default constructor.
	 */
	private JobProgressMonitorAgentFactory() {
		// prevent instantiation
	}
}
