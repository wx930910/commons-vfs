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
package org.apache.commons.vfs2.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileFilter;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.Assert;
import org.junit.Test;

// CHECKSTYLE:OFF Test code
public class OrFileFilterTest extends BaseFilterTest {

	public static FileFilter mockFileFilter3() throws FileSystemException {
		FileFilter mockInstance = mock(FileFilter.class);
		return mockInstance;
	}

	public static FileFilter mockFileFilter2() throws FileSystemException {
		FileFilter mockInstance = mock(FileFilter.class);
		when(mockInstance.accept(any(FileSelectInfo.class))).thenReturn(true);
		return mockInstance;
	}

	public static FileFilter mockFileFilter1() throws FileSystemException {
		FileFilter mockInstance = mock(FileFilter.class);
		return mockInstance;
	}

	@Test
	public void testOrFileFilterFileFilter() throws FileSystemException, FileSystemException, FileSystemException {

		// PREPARE
		final FileFilter filter1 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter2 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter3 = OrFileFilterTest.mockFileFilter1();

		// TEST
		final OrFileFilter testee = new OrFileFilter(filter1, filter2, filter3);

		// VERIFY
		assertContainsOnly(testee.getFileFilters(), filter1, filter2, filter3);

	}

	@Test
	public void testOrFileFilterList() throws FileSystemException, FileSystemException, FileSystemException {

		// PREPARE
		final FileFilter filter1 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter2 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter3 = OrFileFilterTest.mockFileFilter1();
		final List<FileFilter> list = new ArrayList<>();
		list.add(filter1);
		list.add(filter2);
		list.add(filter3);

		// TEST
		final OrFileFilter testee = new OrFileFilter(list);

		// VERIFY
		assertContainsOnly(testee.getFileFilters(), filter1, filter2, filter3);

	}

	@Test
	public void testAddFileFilter() throws FileSystemException, FileSystemException, FileSystemException {

		// PREPARE
		final FileFilter filter1 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter2 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter3 = OrFileFilterTest.mockFileFilter1();

		// TEST
		final OrFileFilter testee = new OrFileFilter();
		testee.addFileFilter(filter1);
		testee.addFileFilter(filter2);
		testee.addFileFilter(filter3);

		// VERIFY
		assertContainsOnly(testee.getFileFilters(), filter1, filter2, filter3);

	}

	@Test
	public void testRemoveFileFilter() throws FileSystemException, FileSystemException, FileSystemException {

		// PREPARE
		final FileFilter filter1 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter2 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter3 = OrFileFilterTest.mockFileFilter1();
		final OrFileFilter testee = new OrFileFilter(filter1, filter2, filter3);

		// TEST
		testee.removeFileFilter(filter2);

		// VERIFY
		assertContainsOnly(testee.getFileFilters(), filter1, filter3);

	}

	@Test
	public void testSetFileFilters() throws FileSystemException, FileSystemException, FileSystemException {

		// PREPARE
		final FileFilter filter1 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter2 = OrFileFilterTest.mockFileFilter1();
		final FileFilter filter3 = OrFileFilterTest.mockFileFilter1();
		final List<FileFilter> list = new ArrayList<>();
		list.add(filter1);
		list.add(filter2);
		list.add(filter3);
		final OrFileFilter testee = new OrFileFilter();

		// TEST
		testee.setFileFilters(list);

		// VERIFY
		assertContainsOnly(testee.getFileFilters(), filter1, filter2, filter3);

	}

	@Test
	public void testAccept() throws FileSystemException, FileSystemException, FileSystemException, FileSystemException,
			FileSystemException, FileSystemException, FileSystemException, FileSystemException, FileSystemException,
			FileSystemException, FileSystemException {

		final FileSelectInfo any = createFileSelectInfo(new File("anyfile"));

		// Empty
		Assert.assertFalse(new OrFileFilter().accept(any));

		// True
		Assert.assertTrue(new OrFileFilter(OrFileFilterTest.mockFileFilter2()).accept(any));
		Assert.assertTrue(
				new OrFileFilter(OrFileFilterTest.mockFileFilter2(), OrFileFilterTest.mockFileFilter2()).accept(any));
		Assert.assertTrue(
				new OrFileFilter(OrFileFilterTest.mockFileFilter3(), OrFileFilterTest.mockFileFilter2()).accept(any));
		Assert.assertTrue(
				new OrFileFilter(OrFileFilterTest.mockFileFilter2(), OrFileFilterTest.mockFileFilter3()).accept(any));

		// False
		Assert.assertFalse(new OrFileFilter(OrFileFilterTest.mockFileFilter3()).accept(any));
		Assert.assertFalse(
				new OrFileFilter(OrFileFilterTest.mockFileFilter3(), OrFileFilterTest.mockFileFilter3()).accept(any));

	}

}
// CHECKSTYLE:ON
