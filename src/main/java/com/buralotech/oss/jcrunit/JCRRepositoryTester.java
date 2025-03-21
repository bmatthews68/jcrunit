/*
 * Copyright 2015-2025 Brian Thomas Matthews
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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.assertj.core.api.AssertProvider;

import javax.jcr.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
import static javax.jcr.Node.JCR_CONTENT;
import static javax.jcr.Property.*;
import static javax.jcr.nodetype.NodeType.*;
import static org.junit.Assert.assertTrue;

/**
 * A JUnit Rule to help test applications that use the Java Content Repository API. This rule creates an in-memory
 * content repository using <a href="https://jackrabbit.apache.org/oak/">Jackrabbit Oak</a>.
 *
 * @author <a href="mailto:brian.matthews@buralo.com">Brian Matthews</a>
 * @since 3.0
 */
public final class JCRRepositoryTester implements AssertProvider<JCRAssertions> {

    @FunctionalInterface
    public interface CreationCallback {
        void accept(Node node) throws RepositoryException;
    }

    /**
     * The default username and password.
     */
    private static final String ADMIN = "admin";

    /**
     * The default credentials.
     */
    private static final Credentials ADMIN_CREDENTIALS = new SimpleCredentials(ADMIN, ADMIN.toCharArray());

    /**
     * The JCR repository.
     */
    private final Repository repository;

    /**
     * The credentials used to authenticate when connecting to the repository.
     */
    private final Credentials credentials;

    /**
     * Indicates if the created nodes should be referenceable.
     */
    private final boolean referenceable;

    /**
     * Initialise the helper state with the repository and credentials.
     *
     * @param credentials The credentials.
     */
    public JCRRepositoryTester(final Repository repository,
                               final Credentials credentials,
                               final boolean referenceable) {
        this.repository = repository;
        this.credentials = credentials;
        this.referenceable = referenceable;
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
                                                   final String password,
                                                   final boolean referenceable,
                                                   final String[] importXMLs)
            throws IOException, RepositoryException {
        final Repository repository = new Jcr(new Oak()).createRepository();
        if (!ADMIN.equals(username)) {
            final Session session = repository.login(ADMIN_CREDENTIALS);
            try {
                ((JackrabbitSession) session).getUserManager().createUser(username, password);
                session.save();
            } finally {
                session.logout();
            }
        }
        final JCRRepositoryTester helper = new JCRRepositoryTester(repository, new SimpleCredentials(username, password.toCharArray()), referenceable);
        for (final String path : importXMLs) {
            helper.importFromXML(ADMIN_CREDENTIALS, path);
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
        return createHelper(annotation.username(), annotation.password(), annotation.referenceable(), annotation.importXMLs());
    }

    /**
     * Return the JCR repository.
     *
     * @return The JCR repository.
     */
    public Repository getRepository() {
        return repository;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Create a top-level folder in the repository.
     *
     * @param name The name of the new top-level folder to be created.
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryTester createRootFolder(final String name,
                                                final CreationCallback... callbacks)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final var parent = session.getRootNode();
            final var node = parent.addNode(name, NT_FOLDER);
            if (referenceable) {
                node.addMixin(MIX_REFERENCEABLE);
            }
            session.save();
            for (var callback : callbacks) {
                callback.accept(node);
            }
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
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryTester createFolder(final String path,
                                            final String name,
                                            final CreationCallback... callbacks)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final var parent = session.getNode(path);
            final var node = parent.addNode(name, NT_FOLDER);
            if (referenceable) {
                node.addMixin(MIX_REFERENCEABLE);
            }
            session.save();
            for (var callback : callbacks) {
                callback.accept(node);
            }
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
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryTester createFile(final String path,
                                          final String name,
                                          final String type,
                                          final String encoding,
                                          final byte[] data,
                                          final CreationCallback... callbacks)
            throws IOException, RepositoryException {
        try (final InputStream inputStream = new ByteArrayInputStream(data)) {
            return createFile(path, name, type, encoding, inputStream, callbacks);
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
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryTester createFile(final String path,
                                          final String name,
                                          final String type,
                                          final String encoding,
                                          final String data,
                                          final CreationCallback... callbacks)
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
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     */
    public JCRRepositoryTester createFile(final String path,
                                          final String name,
                                          final String type,
                                          final String encoding,
                                          final InputStream inputStream,
                                          final CreationCallback... callbacks)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final ValueFactory valueFactory = session.getValueFactory();
            final Node parent = session.getNode(path);
            final Node file = parent.addNode(name, NT_FILE);
            if (referenceable) {
                file.addMixin(MIX_REFERENCEABLE);
            }
            final Node resource = file.addNode(JCR_CONTENT, NT_RESOURCE);
            resource.setProperty(JCR_MIMETYPE, type);
            resource.setProperty(JCR_ENCODING, encoding);
            resource.setProperty(JCR_DATA, valueFactory.createBinary(inputStream));
            session.save();
            for (var callback : callbacks) {
                callback.accept(file);
            }
        } finally {
            session.logout();
        }
        return this;
    }

    /**
     * Assert that a folder exists in the repository.
     *
     * @param path The fully qualified path of the folder.
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
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
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
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
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     * @throws IOException         If there was a problem reading the XML resource.
     * @throws RepositoryException If there was a problem importing the XML resource.
     */
    public JCRRepositoryTester importFromXML(final String path) throws IOException, RepositoryException {
        return importFromXML(Thread.currentThread().getContextClassLoader().getResourceAsStream(path));
    }

    /**
     * Import file and folder nodes into the repository with from an XML resource on the class path using
     * the specified credentials.
     *
     * @param credentials The credentials to use for the operation.
     * @param path        The path of the XML resource on the class path.
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     * @throws IOException         If there was a problem reading the XML resource.
     * @throws RepositoryException If there was a problem importing the XML resource.
     */
    public JCRRepositoryTester importFromXML(final Credentials credentials,
                                             final String path)
            throws IOException, RepositoryException {
        return importFromXML(credentials, Thread.currentThread().getContextClassLoader().getResourceAsStream(path));
    }

    /**
     * Import file and folder nodes into the repository with from an XML resource loaded from an input stream.
     *
     * @param inputStream The input stream from which the XML resource can be loaded.
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
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
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
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

    /**
     * Verify that a node exists.
     *
     * @param path The absolute path of the node.
     * @return {@code true} if the node exists. Otherwise, {@code false}.
     * @throws RepositoryException If there was a problem verifying that the node exists.
     */
    public boolean exists(final String path) throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            return session.nodeExists(path);
        } finally {
            session.logout();
        }
    }

    /**
     * Verify that the node at the specified path is the expected type.
     *
     * @param path     The path.
     * @param nodeType The expected type.
     * @return {@code true} if the node has the expected type. Otherwise, {@code false}.
     * @throws RepositoryException If there was a problem verifying that the node type.
     */
    public boolean isType(final String path,
                          final String nodeType)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final var node = session.getNode(path);
            return node.isNodeType(nodeType);
        } finally {
            session.logout();
        }
    }

    /**
     * Verify that the node at the specified path has the named property.
     *
     * @param path         The path.
     * @param propertyName The property name.
     * @return {@code true} if the node has the named property. Otherwise, {@code false}.
     * @throws RepositoryException If there was a problem verifying that the node has the named property.
     */
    public boolean propertyExists(final String path,
                                  final String propertyName)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final var node = session.getNode(path);
            return node.hasProperty(propertyName);
        } finally {
            session.logout();
        }
    }

    /**
     * Get the named property for the node at the specified path.
     *
     * @param path         The path.
     * @param propertyName The property name.
     * @return The property value.
     * @throws RepositoryException If there was a problem getting the property.
     */
    public <T> Optional<Property> property(final String path,
                                           final String propertyName)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final var node = session.getNode(path);
            if (node.hasProperty(propertyName)) {
                return Optional.of(node.getProperty(propertyName));
            } else {
                return Optional.empty();
            }
        } finally {
            session.logout();
        }
    }

    /**
     * Purge all the files and folders added to the repository.
     *
     * @return A reference to {@code this} to allow fluent-style chaining of invocations.
     * @throws RepositoryException If there was a problem purging the files and folders added to the repository.
     */
    public JCRRepositoryTester purge() throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final var root = session.getRootNode();
            final var nodes = root.getNodes();
            while (nodes.hasNext()) {
                var node = nodes.nextNode();
                if (node.isNodeType(NT_FOLDER) || node.isNodeType(NT_FILE)) {
                    node.remove();
                }
            }
            session.save();
        } finally {
            session.logout();
        }
        return this;
    }

    /**
     * Provide the assertions object.
     *
     * @return The assertions object.
     */
    @Override
    public JCRAssertions assertThat() {
        return new JCRAssertions(this);
    }
}
