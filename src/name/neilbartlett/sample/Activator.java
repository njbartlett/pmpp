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
package name.neilbartlett.sample;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Neil Bartlett
 *
 */
public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		context.registerService(CommandProvider.class.getName(), new HelloCommand(), null);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
