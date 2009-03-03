/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.s3install;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.threerings.s3.client.S3Connection;

/**
 * @author Neil Bartlett
 *
 */
public class S3InstallMSF implements ManagedServiceFactory {
	
	static final String MSF_NAME = "name.neilbartlett.s3install";

	private static final String PROP_ID = "s3id";
	private static final String PROP_KEY = "s3key";
	private static final String PROP_BUCKET = "bucket";
	private static final String DEFAULT_PREFIX = "install-";
	private static final String PROP_PREFIX = "prefix";
	private static final String PROP_PERIOD = "period";
	private static final int DEFAULT_PERIOD = 10000;
	private static final int MIN_PERIOD = 2000;
	
	private final Logger log = LoggerFactory.getLogger(S3InstallMSF.class);

	private final BundleContext context;
	private final Map<String, Thread> threads = new ConcurrentHashMap<String, Thread>();

	public S3InstallMSF(BundleContext context) {
		this.context = context;
	}
	
	public String getName() {
		return MSF_NAME;
	}

	public void updated(String pid, @SuppressWarnings("unchecked") Dictionary props)
			throws ConfigurationException {
		@SuppressWarnings("unchecked")
		Dictionary<String, Object> dict = (Dictionary<String, Object>) props;
		
		// Access Properties
		String s3id = getMandatoryString(PROP_ID, dict);
		String s3key = getMandatoryString(PROP_KEY, dict);
		String bucket = getMandatoryString(PROP_BUCKET, dict);
		
		String prefix = (String) dict.get(PROP_PREFIX);
		if(prefix == null) prefix = DEFAULT_PREFIX;
		
		int period = DEFAULT_PERIOD;
		String periodStr = (String) dict.get(PROP_PERIOD);
		try {
			if(periodStr != null) period = Integer.parseInt(periodStr);
		} catch (NumberFormatException e) {
			throw new ConfigurationException(PROP_PERIOD, "Invalid numeric value", e);
		}
		if(period < MIN_PERIOD) {
			period = MIN_PERIOD;
			log.warn("Specified period is less than the minimum. Setting to " + MIN_PERIOD + "ms.");
		}
		
		// Open an S3Connection
		S3Connection s3 = new S3Connection(s3id, s3key);
		Thread thread = new PeriodicActionThread(new S3Scanner(context, s3, bucket, prefix), period);
		
		// Put in the map and remove any previous binding
		Thread priorThread = threads.put(pid, thread);
		if(priorThread != null) priorThread.interrupt();
		
		thread.start();
	}

	public void deleted(String pid) {
		Thread priorThread = threads.remove(pid);
		if(priorThread != null) priorThread.interrupt();
	}
	
	private static String getMandatoryString(String name, Dictionary<String,Object> props) throws ConfigurationException {
		String value = (String) props.get(name);
		if(value == null) throw new ConfigurationException(name, "Missing mandatory property");
		return value;
	}
	
	public void interruptAll() {
		for (Thread thread : threads.values()) {
			thread.interrupt();
		}
	}
}
