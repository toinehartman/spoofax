/**
 * 
 */
package org.strategoxt.imp.runtime;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.widgets.Display;

/**
 * A Reentrant lock implementation that ensures the SWT event loop runs
 * while acquiring a lock from the main thread, thus avoiding a certain
 * class of dead locks.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class SWTSafeLock extends ReentrantLock {

	private static final long serialVersionUID = 1448450448343689240L;

	private static final long EVENT_RATE = 50;
	
	public SWTSafeLock(boolean fair) {
		super(fair);
	}
	
	public SWTSafeLock() {
		// Default constructor (unfair)
	}

	@Override
	public void lock() {
		if (Environment.isMainThread()) {
			try {
				while (!tryLock(EVENT_RATE, TimeUnit.MILLISECONDS)) {
					while (Display.getCurrent().readAndDispatch());
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			super.lock();
		}
	}
}
