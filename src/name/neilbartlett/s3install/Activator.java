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

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;

public class Activator implements BundleActivator {

	private S3InstallMSF msf;

	public void start(BundleContext context) throws Exception {
		msf = new S3InstallMSF(context);
		
		Properties props = new Properties();
		props.put(Constants.SERVICE_PID, S3InstallMSF.MSF_NAME);
		context.registerService(ManagedServiceFactory.class.getName(), msf, props);
	}

	public void stop(BundleContext context) throws Exception {
		msf.interruptAll();
	}

}
