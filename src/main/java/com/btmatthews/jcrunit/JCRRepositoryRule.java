/*
 * Copyright 2015-2020 Brian Thomas Matthews
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

package com.btmatthews.jcrunit;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.rules.ExternalResource;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
import static javax.jcr.Node.JCR_CONTENT;
import static javax.jcr.Property.JCR_DATA;
import static javax.jcr.Property.JCR_ENCODING;
import static javax.jcr.Property.JCR_MIMETYPE;
import static javax.jcr.nodetype.NodeType.NT_FILE;
import static javax.jcr.nodetype.NodeType.NT_FOLDER;
import static javax.jcr.nodetype.NodeType.NT_RESOURCE;
import static org.junit.Assert.assertTrue;

/**
 * A JUnit Rule to help test applications that use the Java Content Repository API. This rule creates an in-memory
 * content repository using <a href="https://jackrabbit.apache.org/oak/">Jackrabbit Oak</a>.
 *
 * @author <a href="mailto:brian.matthews@buralo.com">Brian Matthews</a>
 * @since 1.0
 */
public final class JCRRepositoryRule extends ExternalResource {

    /**
     * The JCR repository.
     */
    private Repository repository;

    /**
     * The credentials used to authenticate when connecting to the repository.
     */
    private final Credentials credentials;

    /**
     * Initialise the repository with default credentials.
     *
     * @deprecated Use {@link #withDefaultCredentials()} instead.
     */
    @Deprecated
    public JCRRepositoryRule() {
        this("admin", "admin");
    }

    /**
     * Initialise the repository with username and password credentials.
     *
     * @param username The username.
     * @param password The password.
     * @deprecated Use {@link #withCredentials(String, String)} instead.
     */
    @Deprecated
    public JCRRepositoryRule(final String username,
                             final String password) {
        this(username, password.toCharArray());

    }

    /**
     * Initialise the repository with username and password credentials.
     *
     * @param username The username.
     * @param password The password.
     * @deprecated Use {@link #withCredentials(String, char[])} instead.
     */
    @Deprecated
    public JCRRepositoryRule(final String username,
                             final char[] password) {
        this(new SimpleCredentials(username, password));
    }

    /**
     * Private constructor to initialise the rule state with the credentials.
     *
     * @param credentials The credentials.
     */
    private JCRRepositoryRule(final Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Invoked by JUnit before the test case is run and is responsible for instantiating the in-memory JCR
     * repository.
     */
    @Override
    public void before() {
        repository = new Jcr(new Oak()).createRepository();
    }

    /**
     * Invoked by JUnit after test case has completed and is responsible for resetting the rule allowing the
     * in-memory JCR repository to be garbage collected.
     */
    @Override
    public void after() {
        repository = null;
    }

    /**
     * Return the JCR repository.
     *
     * @return The JCR repository.
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Create a top-level folder in the repository.
     *
     * @param name The name of the new top-level folder to be created.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryRule createRootFolder(final String name)
            throws RepositoryException {
        final var session = repository.login(credentials);
        try {
            final var parent = session.getRootNode();
            parent.addNode(name, NT_FOLDER);
            session.save();
        } finally {
            session.logout();
        }
        return this;
    }

    /**
     * Create a sub-folder in the repository.
     *
     * @param path The fully qualified path of the parent folder.
     * @param name The name of the new sub-folder to be created.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryRule createFolder(final String path,
                                          final String name)
            throws RepositoryException {
        final var session = repository.login(credentials);
        try {
            final var parent = session.getNode(path);
            parent.addNode(name, NT_FOLDER);
            session.save();
        } finally {
            session.logout();
        }
        return this;
    }

    /**
     * Create a file in the repository.
     *
     * @param path     The fully qualified path of the parent folder.
     * @param name     The name of the file to be created.
     * @param type     The content type of the file.
     * @param encoding The content encoding of the file.
     * @param data     The binary content of the file.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryRule createFile(final String path,
                                        final String name,
                                        final String type,
                                        final String encoding,
                                        final byte[] data)
            throws IOException, RepositoryException {
        try (var inputStream = new ByteArrayInputStream(data)) {
            return createFile(path, name, type, encoding, inputStream);
        }
    }

    /**
     * Create a file in the repository.
     *
     * @param path     The fully qualified path of the parent folder.
     * @param name     The name of the file to be created.
     * @param type     The content type of the file.
     * @param encoding The content encoding of the file.
     * @param data     The string content of the file.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryRule createFile(final String path,
                                        final String name,
                                        final String type,
                                        final String encoding,
                                        final String data)
            throws IOException, RepositoryException {
        try (var inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            return createFile(path, name, type, encoding, inputStream);
        }
    }

    /**
     * Create a file in the repository.
     *
     * @param path        The fully qualified path of the parent folder.
     * @param name        The name of the file to be created.
     * @param type        The content type of the file.
     * @param encoding    The content encoding of the file.
     * @param inputStream The input stream that provides the binary content of the file.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryRule createFile(final String path,
                                        final String name,
                                        final String type,
                                        final String encoding,
                                        final InputStream inputStream)
            throws RepositoryException {
        final var session = repository.login(credentials);
        try {
            final var valueFactory = session.getValueFactory();
            final var parent = session.getNode(path);
            final var file = parent.addNode(name, NT_FILE);
            final var resource = file.addNode(JCR_CONTENT, NT_RESOURCE);
            resource.setProperty(JCR_MIMETYPE, type);
            resource.setProperty(JCR_ENCODING, encoding);
            resource.setProperty(JCR_DATA, valueFactory.createBinary(inputStream));
            session.save();
        } finally {
            session.logout();
        }
        return this;
    }

    /**
     * Assert that a folder exists in the repository.
     *
     * @param path The fully qualified path of the folder.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryRule assertFolderExists(final String path) {
        try {
            final var session = repository.login(credentials);
            try {
                final var node = session.getNode(path);
                assertTrue(path + " is not a folder", node.isNodeType(NT_FOLDER));
            } finally {
                session.logout();
            }
        } catch (final RepositoryException e) {
            throw new AssertionError(path + " does not exist", e);
        }
        return this;
    }

    /**
     * Assert that a file exists in the repository.
     *
     * @param path The fully qualified path of the file.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryRule assertFileExists(final String path) {
        try {
            final var session = repository.login(credentials);
            try {
                final var node = session.getNode(path);
                assertTrue(path + " is not a file", node.isNodeType(NT_FILE));
            } finally {
                session.logout();
            }
        } catch (final RepositoryException e) {
            throw new AssertionError(path + " does not exist", e);
        }
        return this;
    }

    /**
     * Import file and folder nodes into the repository with from an XML resource on the class path.
     *
     * @param path The path of the XML resource on the class path.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     * @throws IOException         If there was a problem reading the XML resource.
     * @throws RepositoryException If there was a problem importing the XML resource.
     */
    public JCRRepositoryRule importFromXML(final String path) throws IOException, RepositoryException {
        return importFromXML(Thread.currentThread().getContextClassLoader().getResourceAsStream(path));
    }

    /**
     * Import file and folder nodes into the repository with from an XML resource loaded from an input stream.
     *
     * @param inputStream The input stream from which the XML resource can be loaded.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     * @throws IOException         If there was a problem reading the XML resource.
     * @throws RepositoryException If there was a problem importing the XML resource.
     */
    public JCRRepositoryRule importFromXML(final InputStream inputStream) throws IOException, RepositoryException {
        return importFromXML(credentials, inputStream);
    }

    /**
     * Import file and folder nodes into the repository with from an XML resource loaded from an input stream using
     * the specified credentials.
     *
     * @param credentials The credentials to use for the operation.
     * @param inputStream The input stream from which the XML resource can be loaded.
     * @return A reference to <code>this</code> to allow fluent-style chaining of invocations.
     * @throws IOException         If there was a problem reading the XML resource.
     * @throws RepositoryException If there was a problem importing the XML resource.
     */
    public JCRRepositoryRule importFromXML(final Credentials credentials, final InputStream inputStream)
            throws IOException, RepositoryException {
        final var session = repository.login(credentials);
        try {
            session.importXML("/", inputStream, IMPORT_UUID_COLLISION_THROW);
            session.save();
        } finally {
            session.logout();
        }
        return this;
    }

    /**
     * Factory method to create a {@link JCRRepositoryRule} with default credentials.
     *
     * @return The {@link JCRRepositoryRule}.
     */
    public static JCRRepositoryRule withDefaultCredentials() {
        return withCredentials("admin", "admin");
    }

    /**
     * Factory method to create a {@link JCRRepositoryRule} with username and password credentials.
     *
     * @param username The username.
     * @param password The password.
     * @return The {@link JCRRepositoryRule}.
     */
    public static JCRRepositoryRule withCredentials(final String username,
                                                    final String password) {
        return withCredentials(username, password.toCharArray());
    }

    /**
     * Factory method to create a {@link JCRRepositoryRule} with username and password credentials.
     *
     * @param username The username.
     * @param password The password.
     * @return The {@link JCRRepositoryRule}.
     */
    public static JCRRepositoryRule withCredentials(final String username,
                                                    final char[] password) {
        return withCredentials(new SimpleCredentials(username, password));
    }

    /**
     * Factory method to create a {@link JCRRepositoryRule} with custom credentials.
     *
     * @param credentials The custom credentials.
     * @return The {@link JCRRepositoryRule}.
     */
    public static JCRRepositoryRule withCredentials(final Credentials credentials) {
        return new JCRRepositoryRule(credentials);
    }
}
