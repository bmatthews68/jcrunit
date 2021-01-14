/*
 * Copyright 2015-2021 Brian Thomas Matthews
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

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
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
 * @since 3.0
 */
public final class JCRRepositoryTester {

    /**
     * The JCR repository.
     */
    private final Repository repository;

    /**
     * The credentials used to authenticate when connecting to the repository.
     */
    private final Credentials credentials;

    /**
     * Initialise the helper state with the repository and credentials.
     *
     * @param credentials The credentials.
     */
    public JCRRepositoryTester(final Repository repository,
                               final Credentials credentials) {
        this.repository = repository;
        this.credentials = credentials;
    }

    /**
     * Create the {@link JCRRepositoryTester} using the username, password and XML files.
     *
     * @param username   The username.
     * @param password   The user's password.
     * @param importXMLs Paths of XML files used to populate the repository.
     * @return A {@link JCRRepositoryTester}.
     * @throws IOException         If there was a problem reading from an XML file.
     * @throws RepositoryException If there was a problem creating repository entries.
     */
    public static JCRRepositoryTester createHelper(final String username,
                                                   final char[] password,
                                                   final String[] importXMLs)
            throws IOException, RepositoryException {
        final Credentials credentials = new SimpleCredentials(username, password);
        final Repository repository = new Jcr(new Oak()).createRepository();
        final JCRRepositoryTester helper = new JCRRepositoryTester(repository, credentials);
        for (final String path : importXMLs) {
            helper.importFromXML(path);
        }
        return helper;
    }

    /**
     * Create the {@link JCRRepositoryTester} using the details from the {@link JCRRepositoryConfiguration} annotation.
     *
     * @param annotation Annotation specifying the username, password and XML files.
     * @return A {@link JCRRepositoryTester}.
     * @throws IOException         If there was a problem reading from an XML file.
     * @throws RepositoryException If there was a problem creating repository entries.
     */
    public static JCRRepositoryTester createHelper(final JCRRepositoryConfiguration annotation)
            throws IOException, RepositoryException {
        return createHelper(annotation.username(), annotation.password().toCharArray(), annotation.importXMLs());
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
    public JCRRepositoryTester createRootFolder(final String name)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final Node parent = session.getRootNode();
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
    public JCRRepositoryTester createFolder(final String path,
                                            final String name)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final Node parent = session.getNode(path);
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
    public JCRRepositoryTester createFile(final String path,
                                          final String name,
                                          final String type,
                                          final String encoding,
                                          final byte[] data)
            throws IOException, RepositoryException {
        try (final InputStream inputStream = new ByteArrayInputStream(data)) {
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
    public JCRRepositoryTester createFile(final String path,
                                          final String name,
                                          final String type,
                                          final String encoding,
                                          final String data)
            throws IOException, RepositoryException {
        try (final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
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
    public JCRRepositoryTester createFile(final String path,
                                          final String name,
                                          final String type,
                                          final String encoding,
                                          final InputStream inputStream)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final ValueFactory valueFactory = session.getValueFactory();
            final Node parent = session.getNode(path);
            final Node file = parent.addNode(name, NT_FILE);
            final Node resource = file.addNode(JCR_CONTENT, NT_RESOURCE);
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
    public JCRRepositoryTester assertFolderExists(final String path) {
        try {
            final Session session = repository.login(credentials);
            try {
                final Node node = session.getNode(path);
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
    public JCRRepositoryTester assertFileExists(final String path) {
        try {
            final Session session = repository.login(credentials);
            try {
                final Node node = session.getNode(path);
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
    public JCRRepositoryTester importFromXML(final String path) throws IOException, RepositoryException {
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
    public JCRRepositoryTester importFromXML(final InputStream inputStream) throws IOException, RepositoryException {
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
    public JCRRepositoryTester importFromXML(final Credentials credentials, final InputStream inputStream)
            throws IOException, RepositoryException {
        final Session session = repository.login(credentials);
        try {
            session.importXML("/", inputStream, IMPORT_UUID_COLLISION_THROW);
            session.save();
        } finally {
            session.logout();
        }
        return this;
    }
}
