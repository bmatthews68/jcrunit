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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Objects;

import static javax.jcr.nodetype.NodeType.NT_FILE;
import static javax.jcr.nodetype.NodeType.NT_FOLDER;

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
     * @return Self.
     */
    public JCRAssertions pathExists(final String path) {
        try {
            if (!actual.exists(path)) {
                throw failure("Expected [%s] to exist.", path);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: [%s]", path);
        }
        return this;
    }

    /**
     * Assert that a node does not exist at the specified path.
     *
     * @param path The path.
     * @return Self.
     */
    public JCRAssertions pathDoesNotExist(final String path) {
        try {
            if (actual.exists(path)) {
                throw failure("Expected [%s] to not exist.", path);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: [%s]", path);
        }
        return this;
    }

    /**
     * Assert that the node at the specified path is a file.
     *
     * @param path The path.
     * @return Self.
     */
    public JCRAssertions isFile(final String path) {
        try {
            if (!actual.isType(path, NT_FILE)) {
                throw failure("[%s] is not a file.", path);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: [%s]", path);
        }
        return this;
    }

    /**
     * Assert that the node at the specified path is a folder.
     *
     * @param path The path.
     * @return Self.
     */
    public JCRAssertions isFolder(final String path) {
        try {
            if (!actual.isType(path, NT_FOLDER)) {
                throw failure("[%s] is not a folder", path);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: [%s]", path);
        }
        return this;
    }

    /**
     * Assert that the node at the specified path has the specified property.
     *
     * @param path         The path.
     * @param propertyName The property name.
     * @return Self.
     */
    public JCRAssertions hasProperty(final String path,
                                     final String propertyName) {
        try {
            if (!actual.propertyExists(path, propertyName)) {
                throw failure("[%s] does not have property [%s]", path, propertyName);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: [%s] or property: [%s]", path, propertyName);
        }
        return this;
    }

    /**
     * Assert that the node at the specified path does not have the specified property.
     *
     * @param path         The path.
     * @param propertyName The property name.
     * @return Self.
     */
    public JCRAssertions hasNoProperty(final String path,
                                       final String propertyName) {
        try {
            if (actual.propertyExists(path, propertyName)) {
                throw failure("[%s] has property [%s]", path, propertyName);
            }
        } catch (final RepositoryException e) {
            throw failure("Invalid path: [%s] or property: [%s]", path, propertyName);
        }
        return this;
    }

    /**
     * Assert that the node at the specified path has the specified property with the expected value.
     * <p>
     * Only single value Boolean, Decimal, Double, Long and String types are supported.
     *
     * @param path          The path.
     * @param propertyName  The property name.
     * @param propertyValue The expected property value.
     * @return Self.
     */
    public <T> JCRAssertions hasPropertyValue(final String path,
                                              final String propertyName,
                                              final T propertyValue) {
        try {
            actual.property(path, propertyName)
                    .ifPresentOrElse(
                            property -> {
                                try {
                                    switch (property.getType()) {
                                        case PropertyType.BOOLEAN ->
                                                check(path, propertyName, propertyValue, property::getBoolean);
                                        case PropertyType.DECIMAL ->
                                                check(path, propertyName, propertyValue, property::getDecimal);
                                        case PropertyType.DOUBLE ->
                                                check(path, propertyName, propertyValue, property::getDouble);
                                        case PropertyType.LONG ->
                                                check(path, propertyName, propertyValue, property::getLong);
                                        case PropertyType.STRING ->
                                                check(path, propertyName, propertyValue, property::getString);
                                    }
                                } catch (final RepositoryException e) {
                                    throw failure("Invalid path: [%s] or property: [%s]", path, propertyName);
                                }
                            },
                            () -> {
                                if (propertyValue != null) {
                                    throw failure("[%s] does not have property [%s]", path, propertyName, propertyValue);
                                }
                            });
        } catch (final RepositoryException e) {
            throw failure("Invalid path: [%s] or property: [%s]", path, propertyName);
        }
        return this;
    }

    /**
     * Helper method to compare a property value.
     *
     * @param path         The  path.
     * @param propertyName The property name.
     * @param expectValue  The expected value.
     * @param supplier     Supplies the actual value.
     * @param <T>          The property type.
     * @throws RepositoryException If there was an exception accessing the value.
     */
    private <T> void check(final String path,
                           final String propertyName,
                           final T expectValue,
                           final ValueAccessor<T> supplier)
            throws RepositoryException {
        final T actualValue = supplier.get();
        if (!Objects.equals(expectValue, actualValue)) {
            throw failure("Expected [%s] to have property [%s] equal to [%s] but was [%s]", path,
                    propertyName, expectValue, actualValue);
        }
    }

    /**
     * Defines the contract for property accessors.
     *
     * @param <T> The property type.
     */
    @FunctionalInterface
    interface ValueAccessor<T> {
        /**
         * Access the property value.
         *
         * @return The property value.
         * @throws RepositoryException If there was an exception accessing the value.
         */
        T get() throws RepositoryException;
    }
}
