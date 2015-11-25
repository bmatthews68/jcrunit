/*
 * Copyright 2015 Brian Thomas Matthews
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

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.rules.ExternalResource;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public final class JCRRepositoryRule extends ExternalResource {

    private Repository repository;

    private Credentials credentials;

    public JCRRepositoryRule() {
        this("admin", "admin");
    }

    public JCRRepositoryRule(final String username,
                             final String password) {
        this(username, password.toCharArray());
    }

    public JCRRepositoryRule(final String username,
                             final char[] password) {
        credentials = new SimpleCredentials(username, password);
    }

    @Override
    public void before() {
        repository = new Jcr(new Oak()).createRepository();
    }

    @Override
    public void after() {
        repository = null;
    }

    public Repository getRepository() {
        return repository;
    }

    public JCRRepositoryRule createRootFolder(final String name)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final Node parent = session.getRootNode();
            parent.addNode(name, NodeType.NT_FOLDER);
            session.save();
            return this;
        } finally {
            session.logout();
        }
    }

    public JCRRepositoryRule createFolder(final String path,
                                          final String name)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final Node parent = session.getNode(path);
            parent.addNode(name, NodeType.NT_FOLDER);
            session.save();
            return this;
        } finally {
            session.logout();
        }
    }

    public JCRRepositoryRule createFile(final String path,
                                        final String name,
                                        final String type,
                                        final String encoding,
                                        final byte[] data)
            throws IOException, RepositoryException {
        try (final InputStream inputStream = new ByteArrayInputStream(data)) {
            return createFile(path, name, type, encoding, inputStream);
        }
    }

    public JCRRepositoryRule createFile(final String path,
                                        final String name,
                                        final String type,
                                        final String encoding,
                                        final String data)
            throws IOException, RepositoryException {
        try (final InputStream inputStream = new ReaderInputStream(new StringReader(data))) {
            return createFile(path, name, type, encoding, inputStream);
        }
    }

    public JCRRepositoryRule createFile(final String path,
                                        final String name,
                                        final String type,
                                        final String encoding,
                                        final InputStream inputStream)
            throws RepositoryException {
        final Session session = repository.login(credentials);
        try {
            final ValueFactory valueFactory = session.getValueFactory();
            final Node parent = session.getNode(path);
            final Node file = parent.addNode(name, NodeType.NT_FILE);
            final Node resource = file.addNode(Node.JCR_CONTENT, NodeType.NT_RESOURCE);
            resource.setProperty(Property.JCR_MIMETYPE, type);
            resource.setProperty(Property.JCR_ENCODING, encoding);
            resource.setProperty(Property.JCR_DATA, valueFactory.createBinary(inputStream));
            session.save();
            return this;
        } finally {
            session.logout();
        }
    }

    public JCRRepositoryRule assertFolderExists(final String path) {
        try {
            final Session session = repository.login(credentials);
            try {
                final Node node = session.getNode(path);
                if (!node.isNodeType(NodeType.NT_FOLDER)) {
                    throw new AssertionError(path + " is not a folder");
                }
                return this;
            } finally {
                session.logout();
            }
        } catch (final RepositoryException e) {
            throw new AssertionError(path + " does not exist", e);
        }
    }

    public JCRRepositoryRule assertFileExists(final String path) {
        try {
            final Session session = repository.login(credentials);
            try {
                final Node node = session.getNode(path);
                if (!node.isNodeType(NodeType.NT_FILE)) {
                    throw new AssertionError(path + " is not a file");
                }
                return this;
            } finally {
                session.logout();
            }
        } catch (final RepositoryException e) {
            throw new AssertionError(path + " does not exist", e);
        }
    }
}
