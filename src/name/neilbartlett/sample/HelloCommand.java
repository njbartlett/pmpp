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

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * @author Neil Bartlett
 *
 */
public class HelloCommand implements CommandProvider {
	
	public String getHelp() {
		return "\tsayHello - Say hello"; 
	}
	
	public void _sayHello(CommandInterpreter ci) {
		ci.println("Hello World!");
	}

}
