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
package org.apache.commons.vfs2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

/**
 * File system test that do some delete operations.
 */
public class ProviderDeleteTests extends AbstractProviderTestCase {

	public FileSelector mockFileSelector1(final String basename) throws Exception {
		String mockFieldVariableBasename;
		FileSelector mockInstance = mock(FileSelector.class);
		mockFieldVariableBasename = basename;
		when(mockInstance.includeFile(any(FileSelectInfo.class))).thenAnswer((stubInvo) -> {
			FileSelectInfo fileInfo = stubInvo.getArgument(0);
			return mockFieldVariableBasename.equals(fileInfo.getFile().getName().getBaseName());
		});
		when(mockInstance.traverseDescendents(any(FileSelectInfo.class))).thenReturn(true);
		return mockInstance;
	}

	/**
	 * Returns the capabilities required by the tests of this test case.
	 */
	@Override
	protected Capability[] getRequiredCaps() {
		return new Capability[] { Capability.CREATE, Capability.DELETE, Capability.GET_TYPE,
				Capability.LIST_CHILDREN, };
	}

	/**
	 * Sets up a scratch folder for the test to use.
	 */
	protected FileObject createScratchFolder() throws Exception {
		final FileObject scratchFolder = getWriteFolder();

		// Make sure the test folder is empty
		scratchFolder.delete(Selectors.EXCLUDE_SELF);
		scratchFolder.createFolder();

		final FileObject dir1 = scratchFolder.resolveFile("dir1");
		dir1.createFolder();
		final FileObject dir1file1 = dir1.resolveFile("a.txt");
		dir1file1.createFile();
		final FileObject dir2 = scratchFolder.resolveFile("dir2");
		dir2.createFolder();
		final FileObject dir2file1 = dir2.resolveFile("b.txt");
		dir2file1.createFile();

		return scratchFolder;
	}

	/**
	 * deletes the complete structure
	 */
	@Test
	public void testDeleteFiles() throws Exception {
		final FileObject scratchFolder = createScratchFolder();

		assertEquals(4, scratchFolder.delete(Selectors.EXCLUDE_SELF));
	}

	/**
	 * deletes a single file
	 */
	@Test
	public void testDeleteFile() throws Exception {
		final FileObject scratchFolder = createScratchFolder();

		final FileObject file = scratchFolder.resolveFile("dir1/a.txt");

		assertTrue(file.delete());
	}

	/**
	 * Deletes a non existent file
	 */
	@Test
	public void testDeleteNonExistantFile() throws Exception {
		final FileObject scratchFolder = createScratchFolder();

		final FileObject file = scratchFolder.resolveFile("dir1/aa.txt");

		assertFalse(file.delete());
	}

	/**
	 * deletes files
	 */
	@Test
	public void testDeleteAllFiles() throws Exception {
		final FileObject scratchFolder = createScratchFolder();

		assertEquals(2, scratchFolder.delete(new FileTypeSelector(FileType.FILE)));
	}

	/**
	 * deletes a.txt
	 */
	@Test
	public void testDeleteOneFiles() throws Exception, Exception {
		final FileObject scratchFolder = createScratchFolder();

		assertEquals(1, scratchFolder.delete(mockFileSelector1("a.txt")));
	}
}
