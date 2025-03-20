/*
 * Copyright 2025 Brian Thomas Matthews
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.buralotech.oss.jcrunit;

import org.assertj.core.api.AbstractAssert;

import javax.jcr.RepositoryException;

/**
 * Assertions for testing existence of content in repository.
 */
public class JCRAssertions extends AbstractAssert<JCRAssertions, JCRRepositoryTester> {

    /**
     * Initialise the assertions object.
     *
     * @param tester The repository helper.
     */
    JCRAssertions(final JCRRepositoryTester tester) {
        super(tester, JCRAssertions.class);
    }

    /**
     * Assert that a node exists at the specified path.
     *
     * @param path The path.
     * @return {@code true} if a node exists. Otherwise, {@code false}.
     */
    public JCRAssertions pathExists(final String path) {
        try {
            if (!actual.exists(path)) {
                throw failure("Expected %s to exist.", path);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: %s", path);
        }
        return this;
    }

    /**
     * Assert that a node does not exist at the specified path.
     *
     * @param path The path.
     * @return {@code true} if a node does not exist. Otherwise, {@code false}.
     */
    public JCRAssertions pathDoesNotExist(final String path) {
        try {
            if (actual.exists(path)) {
                throw failure("Expected %s to not exist.", path);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: %s", path);
        }
        return this;
    }
}
