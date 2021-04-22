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
package com.jcraft.jsch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory;

/**
 * Simple JSch identity repository factory (that just returns the default
 * factory).
 *
 * This class is packaged in {@code com.jcraft.jsch} because
 * {@code com.jcraft.jsch.LocalIdentityRepository} is declared with default
 * scope.
 *
 */
public class TestIdentityRepositoryFactory {
	public static IdentityRepositoryFactory mockIdentityRepositoryFactory1() {
		IdentityRepositoryFactory mockInstance = mock(IdentityRepositoryFactory.class);
		when(mockInstance.create(any(JSch.class))).thenAnswer((stubInvo) -> {
			JSch jsch = stubInvo.getArgument(0);
			return new LocalIdentityRepository(jsch);
		});
		return mockInstance;
	}
}
