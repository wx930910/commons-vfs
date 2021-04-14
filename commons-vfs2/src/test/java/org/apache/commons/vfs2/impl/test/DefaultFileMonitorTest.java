/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2.impl.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.AbstractVfsTestCase;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.junit.Ignore;

/**
 * Test to verify DefaultFileMonitor
 */
public class DefaultFileMonitorTest extends AbstractVfsTestCase {

	public FileListener mockFileListener1() throws Exception {
		FileListener mockInstance = mock(FileListener.class);
		doAnswer((stubInvo) -> {
			changeStatus = 1;
			return null;
		}).when(mockInstance).fileChanged(any());
		doAnswer((stubInvo) -> {
			changeStatus = 2;
			return null;
		}).when(mockInstance).fileDeleted(any());
		doAnswer((stubInvo) -> {
			changeStatus = 3;
			return null;
		}).when(mockInstance).fileCreated(any());
		return mockInstance;
	}

	private FileSystemManager fsManager;
	private File testDir;
	private int changeStatus = 0;
	private File testFile;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fsManager = VFS.getManager();
		testDir = AbstractVfsTestCase.getTestDirectoryFile();
		changeStatus = 0;
		testFile = new File(testDir, "testReload.properties");

		if (testFile.exists()) {
			testFile.delete();
		}
	}

	@Override
	public void tearDown() throws Exception {
		if (testFile != null) {
			testFile.deleteOnExit();
		}
		super.tearDown();
	}

	public void testFileCreated() throws Exception, Exception {
		final FileObject fileObj = fsManager.resolveFile(testFile.toURI().toURL().toString());
		final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
		// TestFileListener manipulates changeStatus
		monitor.setDelay(100);
		monitor.addFile(fileObj);
		monitor.start();
		try {
			writeToFile(testFile);
			Thread.sleep(300);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event", 3, changeStatus);
		} finally {
			monitor.stop();
		}
	}

	public void testFileDeleted() throws Exception, Exception {
		writeToFile(testFile);
		final FileObject fileObj = fsManager.resolveFile(testFile.toURI().toString());
		final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
		// TestFileListener manipulates changeStatus
		monitor.setDelay(100);
		monitor.addFile(fileObj);
		monitor.start();
		try {
			testFile.delete();
			Thread.sleep(300);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event", 2, changeStatus);
		} finally {
			monitor.stop();
		}
	}

	public void testFileModified() throws Exception, Exception {
		writeToFile(testFile);
		final FileObject fileObj = fsManager.resolveFile(testFile.toURI().toURL().toString());
		final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
		// TestFileListener manipulates changeStatus
		monitor.setDelay(100);
		monitor.addFile(fileObj);
		monitor.start();
		try {
			// Need a long delay to insure the new timestamp doesn't truncate to be the same
			// as
			// the current timestammp. Java only guarantees the timestamp will be to 1
			// second.
			Thread.sleep(1000);
			final long value = System.currentTimeMillis();
			final boolean rc = testFile.setLastModified(value);
			assertTrue("setLastModified succeeded", rc);
			Thread.sleep(300);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event", 1, changeStatus);
		} finally {
			monitor.stop();
		}
	}

	public void testFileRecreated() throws Exception, Exception {
		final FileObject fileObj = fsManager.resolveFile(testFile.toURI().toURL().toString());
		final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
		// TestFileListener manipulates changeStatus
		monitor.setDelay(100);
		monitor.addFile(fileObj);
		monitor.start();
		try {
			writeToFile(testFile);
			Thread.sleep(300);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event " + changeStatus, 3, changeStatus);
			changeStatus = 0;
			testFile.delete();
			Thread.sleep(300);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event " + changeStatus, 2, changeStatus);
			changeStatus = 0;
			Thread.sleep(500);
			monitor.addFile(fileObj);
			writeToFile(testFile);
			Thread.sleep(300);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event " + changeStatus, 3, changeStatus);
		} finally {
			monitor.stop();
		}
	}

	public void testChildFileRecreated() throws Exception, Exception {
		writeToFile(testFile);
		final FileObject fileObj = fsManager.resolveFile(testDir.toURI().toURL().toString());
		final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
		monitor.setDelay(2000);
		monitor.setRecursive(true);
		monitor.addFile(fileObj);
		monitor.start();
		try {
			changeStatus = 0;
			Thread.sleep(300);
			testFile.delete();
			Thread.sleep(3000);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event " + changeStatus, 2, changeStatus);
			changeStatus = 0;
			Thread.sleep(300);
			writeToFile(testFile);
			Thread.sleep(3000);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event " + changeStatus, 3, changeStatus);
		} finally {
			monitor.stop();
		}
	}

	public void testChildFileDeletedWithoutRecursiveChecking() throws Exception, Exception {
		writeToFile(testFile);
		final FileObject fileObj = fsManager.resolveFile(testDir.toURI().toURL().toString());
		final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
		monitor.setDelay(2000);
		monitor.setRecursive(false);
		monitor.addFile(fileObj);
		monitor.start();
		try {
			changeStatus = 0;
			Thread.sleep(300);
			testFile.delete();
			Thread.sleep(3000);
			assertEquals("Event should not have occurred", 0, changeStatus);
		} finally {
			monitor.stop();
		}
	}

	public void testFileMonitorRestarted() throws Exception, Exception {
		final FileObject fileObj = fsManager.resolveFile(testFile.toURI().toString());
		final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
		// TestFileListener manipulates changeStatus
		monitor.setDelay(100);
		monitor.addFile(fileObj);

		monitor.start();
		writeToFile(testFile);
		Thread.sleep(300);
		monitor.stop();

		monitor.start();
		try {
			testFile.delete();
			Thread.sleep(300);
			assertTrue("No event occurred", changeStatus != 0);
			assertEquals("Incorrect event", 2, changeStatus);
		} finally {
			monitor.stop();
		}
	}

	/**
	 * VFS-299: Handlers are not removed. One instance is
	 * {@link DefaultFileMonitor#removeFile(FileObject)}.
	 *
	 * As a result, the file monitor will fire two created events.
	 */
	@Ignore("VFS-299")
	public void ignore_testAddRemove() throws Exception, Exception {
		final FileObject file = fsManager.resolveFile(testFile.toURI().toString());
		final FileListener listener = mock(FileListener.class);
		AtomicLong listenerCreated = new AtomicLong();
		doThrow(new UnsupportedOperationException()).when(listener).fileChanged(any(FileChangeEvent.class));
		doAnswer((stubInvo) -> {
			listenerCreated.incrementAndGet();
			return null;
		}).when(listener).fileCreated(any());
		doThrow(new UnsupportedOperationException()).when(listener).fileDeleted(any(FileChangeEvent.class));
		final DefaultFileMonitor monitor = new DefaultFileMonitor(listener);
		monitor.setDelay(100);

		try {
			monitor.addFile(file);
			monitor.removeFile(file);
			monitor.addFile(file);
			monitor.start();
			writeToFile(testFile);
			Thread.sleep(300);
			assertEquals("Created event is only fired once", 1, listenerCreated.get());
		} finally {
			monitor.stop();
		}
	}

	/**
	 * VFS-299: Handlers are not removed. There is no API for properly
	 * decommissioning a file monitor.
	 *
	 * As a result, listeners of stopped monitors still receive events.
	 */
	@Ignore("VFS-299")
	public void ignore_testStartStop() throws Exception, Exception, Exception {
		final FileObject file = fsManager.resolveFile(testFile.toURI().toString());

		final FileListener stoppedListener = mock(FileListener.class);
		AtomicLong stoppedListenerCreated = new AtomicLong();
		doThrow(new UnsupportedOperationException()).when(stoppedListener).fileChanged(any(FileChangeEvent.class));
		doAnswer((stubInvo) -> {
			stoppedListenerCreated.incrementAndGet();
			return null;
		}).when(stoppedListener).fileCreated(any());
		doThrow(new UnsupportedOperationException()).when(stoppedListener).fileDeleted(any(FileChangeEvent.class));
		final DefaultFileMonitor stoppedMonitor = new DefaultFileMonitor(stoppedListener);
		stoppedMonitor.start();
		stoppedMonitor.addFile(file);
		stoppedMonitor.stop();

		// Variant 1: it becomes documented behavior to manually remove all files after
		// stop() such that all listeners are removed
		// This currently does not work, see DefaultFileMonitorTests#testAddRemove
		// above.
		// stoppedMonitor.removeFile(file);

		// Variant 2: change behavior of stop(), which then removes all handlers.
		// This would remove the possibility to pause watching files. Resuming watching
		// for the same files via start(); stop(); start(); would not work.

		// Variant 3: introduce new method DefaultFileMonitor#close which definitely
		// removes all resources held by DefaultFileMonitor.

		final FileListener activeListener = mock(FileListener.class);
		AtomicLong activeListenerCreated = new AtomicLong();
		doThrow(new UnsupportedOperationException()).when(activeListener).fileChanged(any(FileChangeEvent.class));
		doAnswer((stubInvo) -> {
			activeListenerCreated.incrementAndGet();
			return null;
		}).when(activeListener).fileCreated(any());
		doThrow(new UnsupportedOperationException()).when(activeListener).fileDeleted(any(FileChangeEvent.class));
		final DefaultFileMonitor activeMonitor = new DefaultFileMonitor(activeListener);
		activeMonitor.setDelay(100);
		activeMonitor.addFile(file);
		activeMonitor.start();
		try {
			writeToFile(testFile);
			Thread.sleep(1000);

			assertEquals("The listener of the active monitor received one created event", 1,
					activeListenerCreated.get());
			assertEquals("The listener of the stopped monitor received no events", 0, stoppedListenerCreated.get());
		} finally {
			activeMonitor.stop();
		}
	}

	private void writeToFile(final File file) throws Exception {
		final FileWriter out = new FileWriter(file);
		out.write("string=value1");
		out.close();
	}

}
