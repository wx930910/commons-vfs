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
package org.apache.commons.vfs2.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.AbstractVfsTestCase;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test to verify DefaultFileMonitor
 */
public class DefaultFileMonitorTest {

	public FileListener mockFileListener1() throws Exception {
		FileListener mockInstance = mock(FileListener.class);
		doAnswer((stubInvo) -> {
			changeStatus = Status.CHANGED;
			return null;
		}).when(mockInstance).fileChanged(any(FileChangeEvent.class));
		doAnswer((stubInvo) -> {
			changeStatus = Status.CREATED;
			return null;
		}).when(mockInstance).fileCreated(any(FileChangeEvent.class));
		doAnswer((stubInvo) -> {
			changeStatus = Status.DELETED;
			return null;
		}).when(mockInstance).fileDeleted(any(FileChangeEvent.class));
		return mockInstance;
	}

	private static final int DELAY_MILLIS = 100;
	private FileSystemManager fsManager;
	private File testDir;
	private volatile Status changeStatus;
	private File testFile;

	@BeforeClass
	public static void beforeClass() {
		// Fails randomly on Windows.
		assumeFalse(SystemUtils.IS_OS_WINDOWS);
	}

	@Before
	public void setUp() throws Exception {
		fsManager = VFS.getManager();
		testDir = AbstractVfsTestCase.getTestDirectoryFile();
		changeStatus = null;
		testFile = new File(testDir, "testReload.properties");

		if (testFile.exists()) {
			testFile.delete();
		}
	}

	@After
	public void tearDown() throws Exception {
		if (testFile != null) {
			if (!testFile.delete()) {
				testFile.deleteOnExit();
			}
		}
	}

	@Test
	public void testFileCreated() throws Exception, Exception {
		try (final FileObject fileObject = fsManager.resolveFile(testFile.toURI().toURL().toString())) {
			final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
			// TestFileListener manipulates changeStatus
			monitor.setDelay(DELAY_MILLIS);
			monitor.addFile(fileObject);
			monitor.start();
			try {
				writeToFile(testFile);
				Thread.sleep(DELAY_MILLIS * 5);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event", Status.CREATED, changeStatus);
			} finally {
				monitor.stop();
			}
		}
	}

	@Test
	public void testFileDeleted() throws Exception, Exception {
		writeToFile(testFile);
		try (final FileObject fileObject = fsManager.resolveFile(testFile.toURI().toString())) {
			final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
			// TestFileListener manipulates changeStatus
			monitor.setDelay(DELAY_MILLIS);
			monitor.addFile(fileObject);
			monitor.start();
			try {
				testFile.delete();
				Thread.sleep(500);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event", Status.DELETED, changeStatus);
			} finally {
				monitor.stop();
			}
		}
	}

	@Test
	public void testFileModified() throws Exception, Exception {
		writeToFile(testFile);
		try (final FileObject fileObject = fsManager.resolveFile(testFile.toURI().toURL().toString())) {
			final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
			// TestFileListener manipulates changeStatus
			monitor.setDelay(DELAY_MILLIS);
			monitor.addFile(fileObject);
			monitor.start();
			try {
				// Need a long delay to insure the new timestamp doesn't truncate to be the same
				// as
				// the current timestammp. Java only guarantees the timestamp will be to 1
				// second.
				Thread.sleep(DELAY_MILLIS * 10);
				final long valueMillis = System.currentTimeMillis();
				final boolean rcMillis = testFile.setLastModified(valueMillis);
				assertTrue("setLastModified succeeded", rcMillis);
				Thread.sleep(DELAY_MILLIS * 5);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event", Status.CHANGED, changeStatus);
			} finally {
				monitor.stop();
			}
		}
	}

	@Test
	public void testFileRecreated() throws Exception, Exception {
		try (final FileObject fileObject = fsManager.resolveFile(testFile.toURI().toURL().toString())) {
			final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
			// TestFileListener manipulates changeStatus
			monitor.setDelay(DELAY_MILLIS);
			monitor.addFile(fileObject);
			monitor.start();
			try {
				writeToFile(testFile);
				Thread.sleep(DELAY_MILLIS * 5);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event " + changeStatus, Status.CREATED, changeStatus);
				changeStatus = null;
				testFile.delete();
				Thread.sleep(DELAY_MILLIS * 5);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event " + changeStatus, Status.DELETED, changeStatus);
				changeStatus = null;
				Thread.sleep(DELAY_MILLIS * 5);
				monitor.addFile(fileObject);
				writeToFile(testFile);
				Thread.sleep(DELAY_MILLIS * 10);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event " + changeStatus, Status.CREATED, changeStatus);
			} finally {
				monitor.stop();
			}
		}
	}

	@Test
	public void testChildFileRecreated() throws Exception, Exception {
		writeToFile(testFile);
		try (final FileObject fileObj = fsManager.resolveFile(testDir.toURI().toURL().toString())) {
			final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
			monitor.setDelay(2000);
			monitor.setRecursive(true);
			monitor.addFile(fileObj);
			monitor.start();
			try {
				changeStatus = null;
				Thread.sleep(DELAY_MILLIS * 5);
				testFile.delete();
				Thread.sleep(DELAY_MILLIS * 30);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event " + changeStatus, Status.DELETED, changeStatus);
				changeStatus = null;
				Thread.sleep(DELAY_MILLIS * 5);
				writeToFile(testFile);
				Thread.sleep(DELAY_MILLIS * 30);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event " + changeStatus, Status.CREATED, changeStatus);
			} finally {
				monitor.stop();
			}
		}
	}

	@Test
	public void testChildFileDeletedWithoutRecursiveChecking() throws Exception, Exception {
		writeToFile(testFile);
		try (final FileObject fileObject = fsManager.resolveFile(testDir.toURI().toURL().toString())) {
			final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
			monitor.setDelay(2000);
			monitor.setRecursive(false);
			monitor.addFile(fileObject);
			monitor.start();
			try {
				changeStatus = null;
				Thread.sleep(DELAY_MILLIS * 5);
				testFile.delete();
				Thread.sleep(DELAY_MILLIS * 30);
				assertEquals("Event should not have occurred", null, changeStatus);
			} finally {
				monitor.stop();
			}
		}
	}

	@Test
	public void testFileMonitorRestarted() throws Exception, Exception {
		try (final FileObject fileObject = fsManager.resolveFile(testFile.toURI().toString())) {
			final DefaultFileMonitor monitor = new DefaultFileMonitor(mockFileListener1());
			// TestFileListener manipulates changeStatus
			monitor.setDelay(DELAY_MILLIS);
			monitor.addFile(fileObject);

			monitor.start();
			try {
				writeToFile(testFile);
				Thread.sleep(DELAY_MILLIS * 5);
			} finally {
				monitor.stop();
			}

			monitor.start();
			try {
				testFile.delete();
				Thread.sleep(DELAY_MILLIS * 5);
				assertTrue("No event occurred", changeStatus != null);
				assertEquals("Incorrect event", Status.DELETED, changeStatus);
			} finally {
				monitor.stop();
			}
		}
	}

	/**
	 * VFS-299: Handlers are not removed. One instance is
	 * {@link DefaultFileMonitor#removeFile(FileObject)}.
	 *
	 * As a result, the file monitor will fire two created events.
	 */
	@Ignore("VFS-299")
	@Test
	public void ignore_testAddRemove() throws Exception, Exception {
		try (final FileObject fileObject = fsManager.resolveFile(testFile.toURI().toString())) {
			final FileListener listener = mock(FileListener.class);
			AtomicLong listenerCreated = new AtomicLong();
			doAnswer((stubInvo) -> {
				listenerCreated.incrementAndGet();
				return null;
			}).when(listener).fileCreated(any(FileChangeEvent.class));
			doThrow(new UnsupportedOperationException()).when(listener).fileDeleted(any(FileChangeEvent.class));
			doThrow(new UnsupportedOperationException()).when(listener).fileChanged(any(FileChangeEvent.class));
			final DefaultFileMonitor monitor = new DefaultFileMonitor(listener);
			monitor.setDelay(DELAY_MILLIS);
			try {
				monitor.addFile(fileObject);
				monitor.removeFile(fileObject);
				monitor.addFile(fileObject);
				monitor.start();
				writeToFile(testFile);
				Thread.sleep(DELAY_MILLIS * 3);
				assertEquals("Created event is only fired once", 1, listenerCreated.get());
			} finally {
				monitor.stop();
			}
		}
	}

	/**
	 * VFS-299: Handlers are not removed. There is no API for properly
	 * decommissioning a file monitor.
	 *
	 * As a result, listeners of stopped monitors still receive events.
	 */
	@Ignore("VFS-299")
	@Test
	public void ignore_testStartStop() throws Exception, Exception, Exception {
		try (final FileObject fileObject = fsManager.resolveFile(testFile.toURI().toString())) {
			final FileListener stoppedListener = mock(FileListener.class);
			AtomicLong stoppedListenerCreated = new AtomicLong();
			doAnswer((stubInvo) -> {
				stoppedListenerCreated.incrementAndGet();
				return null;
			}).when(stoppedListener).fileCreated(any(FileChangeEvent.class));
			doThrow(new UnsupportedOperationException()).when(stoppedListener).fileDeleted(any(FileChangeEvent.class));
			doThrow(new UnsupportedOperationException()).when(stoppedListener).fileChanged(any(FileChangeEvent.class));
			final DefaultFileMonitor stoppedMonitor = new DefaultFileMonitor(stoppedListener);
			stoppedMonitor.start();
			try {
				stoppedMonitor.addFile(fileObject);
			} finally {
				stoppedMonitor.stop();
			}

			// Variant 1: it becomes documented behavior to manually remove all files after
			// stop() such that all
			// listeners
			// are removed
			// This currently does not work, see DefaultFileMonitorTests#testAddRemove
			// above.
			// stoppedMonitor.removeFile(file);

			// Variant 2: change behavior of stop(), which then removes all handlers.
			// This would remove the possibility to pause watching files. Resuming watching
			// for the same files via
			// start();
			// stop(); start(); would not work.

			// Variant 3: introduce new method DefaultFileMonitor#close which definitely
			// removes all resources held by
			// DefaultFileMonitor.

			final FileListener activeListener = mock(FileListener.class);
			AtomicLong activeListenerCreated = new AtomicLong();
			doAnswer((stubInvo) -> {
				activeListenerCreated.incrementAndGet();
				return null;
			}).when(activeListener).fileCreated(any(FileChangeEvent.class));
			doThrow(new UnsupportedOperationException()).when(activeListener).fileDeleted(any(FileChangeEvent.class));
			doThrow(new UnsupportedOperationException()).when(activeListener).fileChanged(any(FileChangeEvent.class));
			final DefaultFileMonitor activeMonitor = new DefaultFileMonitor(activeListener);
			activeMonitor.setDelay(DELAY_MILLIS);
			activeMonitor.addFile(fileObject);
			activeMonitor.start();
			try {
				writeToFile(testFile);
				Thread.sleep(DELAY_MILLIS * 10);

				assertEquals("The listener of the active monitor received one created event", 1,
						activeListenerCreated.get());
				assertEquals("The listener of the stopped monitor received no events", 0, stoppedListenerCreated.get());
			} finally {
				activeMonitor.stop();
			}
		}
	}

	private void writeToFile(final File file) throws IOException {
		// assertTrue(file.delete());
		try (final FileWriter out = new FileWriter(file)) {
			out.write("string=value1");
		}
	}

	private enum Status {
		CHANGED, DELETED, CREATED
	}

}
