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

public class PeriodicThread extends Thread {

	private final PeriodicAction action;
	private final long period;
	
	public PeriodicThread(PeriodicAction action, long period) {
		this.action = action;
		this.period = period;
	}
	
	public PeriodicThread(final Runnable action, long period) {
		this(new PeriodicAction() {
			public void run() {
				action.run();
			}
		
			public void shutdown() {
				// No action
			}
		}, period);
	}

	@Override
	public void run() {
		try {
			while(!Thread.interrupted()) {
				action.run();
				Thread.sleep(period);
			}
		} catch (InterruptedException e) {
			// Exit quietly
		} finally {
			action.shutdown();
		}
	}
}
