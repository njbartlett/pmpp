/*******************************************************************************
 * Copyright (c) 2009 neil.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     neil - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.s3install;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.threerings.s3.client.S3Connection;

public class Activator implements BundleActivator {

	private static final Log LOG = LogFactory.getLog(Activator.class);

	private static final String PROP_CONFIG_FILE = "s3config";
	private static final String DEFAULT_CONFIG_FILE = "s3install.properties";

	private static final String PROP_ID = "id";
	private static final String PROP_KEY = "key";
	private static final String PROP_BUCKET = "bucket";
	private static final String DEFAULT_PREFIX = "bundle-";
	private static final String PROP_PREFIX = "prefix";
	private static final String PROP_PERIOD = "period";
	private static final int DEFAULT_PERIOD = 10000;
	private static final int MIN_PERIOD = 2000;

	private volatile PeriodicThread thread = null;

	public void start(BundleContext context) throws Exception {
		// Load properties
		String configFileProp = context.getProperty(PROP_CONFIG_FILE);
		if (configFileProp == null) {
			configFileProp = DEFAULT_CONFIG_FILE;
		}
		FileInputStream configFile = new FileInputStream(configFileProp);
		Properties s3props = new Properties();
		s3props.load(configFile);

		// Access properties
		String s3id = s3props.getProperty(PROP_ID);
		if (s3id == null) {
			LOG.warn("Property " + PROP_ID + " must be specified.");
			return;
		}

		String key = s3props.getProperty(PROP_KEY);
		if (key == null) {
			LOG.warn("Property " + PROP_KEY + " must be specified.");
			return;
		}

		String bucket = s3props.getProperty(PROP_BUCKET);
		if (bucket == null) {
			LOG.warn("Property " + PROP_BUCKET + " must be specified.");
			return;
		}

		String prefix = s3props.getProperty(PROP_PREFIX, DEFAULT_PREFIX);
		
		int period = DEFAULT_PERIOD;
		String periodStr = s3props.getProperty(PROP_PERIOD);
		try {
			if(periodStr != null) period = Integer.parseInt(periodStr);
		} catch (NumberFormatException e) {
			LOG.error("Invalid number for property '" + PROP_PERIOD + "'");
			return;
		}
		if(period < MIN_PERIOD) {
			LOG.warn("Specified period is less than the minimum. Setting to " + MIN_PERIOD + "ms.");
			period = MIN_PERIOD;
		}

		// Open S3 connection
		S3Connection s3 = new S3Connection(s3id, key);
		thread = new PeriodicThread(new S3Scanner(context, s3, bucket, prefix), period);
		thread.start();
	}

	public void stop(BundleContext context) throws Exception {
		if (thread != null)
			thread.interrupt();
	}

}
