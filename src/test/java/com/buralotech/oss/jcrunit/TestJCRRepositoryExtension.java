/*
 * Copyright 2021-2025 Brian Thomas Matthews
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jcr.*;
import java.io.IOException;
import java.util.function.Consumer;

import static javax.jcr.nodetype.NodeType.NT_FILE;
import static javax.jcr.nodetype.NodeType.NT_FOLDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JCRRepositoryConfiguration
@ExtendWith(JCRRepositoryExtension.class)
class TestJCRRepositoryExtension extends AbstractJCRRepositoryTest {

    private static final String USERNAME = "root";

    private static final String PASSWORD = "secret";

    private static final String DATA_TXT = "data.txt";

    private static final String DATA_FOLDER = "/data";

    @Test
    void repositoryHasBeenCreated(final JCRRepositoryTester helper) {
        assertNotNull(helper.getRepository());
    }

    @Test
    void repositoryHasBeenCreated(final Repository repository) {
        assertNotNull(repository);
    }

    @Test
    void createFolderAtRoot(final JCRRepositoryTester helper) throws RepositoryException {
        helper
                .createRootFolder("top")
                .assertFolderExists("/top");
    }

    @Test
    void cannotCreateDuplicateRootFolder(final JCRRepositoryTester helper) {
        assertThrows(
                ItemExistsException.class,
                () -> helper
                        .createRootFolder("top")
                        .assertFolderExists("/top")
                        .createRootFolder("top"));
    }

    @Test
    void cannotCreateSubFolderIfRootDoesNotExist(final JCRRepositoryTester helper) {
        assertThrows(
                PathNotFoundException.class,
                () -> helper.createFolder("/top", "sub"));
    }


    @Test
    void canCreateSubFolderWhenRootExists(final JCRRepositoryTester helper) throws RepositoryException {
        helper
                .createRootFolder("top")
                .assertFolderExists("/top")
                .createFolder("/top", "sub")
                .assertFolderExists("/top/sub");
    }

    @Test
    void canCreateBinaryFile(final JCRRepositoryTester helper) throws IOException, RepositoryException {
        final byte[] data = generateBinaryData();
        helper
                .createRootFolder("bin")
                .assertFolderExists("/bin")
                .createFile("/bin", "data.bin", "application/octet", null, data)
                .assertFileExists("/bin/data.bin");
    }

    @Test
    void canCreateTextFile(final JCRRepositoryTester helper) throws IOException, RepositoryException {
        helper
                .createRootFolder("txt")
                .assertFolderExists("/txt")
                .createFile("/txt", DATA_TXT, "text/plain", "UTF-8", "Hello world")
                .assertFileExists("/txt/data.txt");
    }

    @Test
    void assertFolderExistsFailsWhenFolderDoesNotExist(final JCRRepositoryTester helper) {
        assertThrows(AssertionError.class, () -> helper.assertFolderExists(DATA_FOLDER));
    }

    @Test
    void assertFolderExistsFailsBecauseItemIsNotAFolder(final JCRRepositoryTester helper) {
        assertThrows(
                AssertionError.class,
                () -> helper
                        .createRootFolder("data")
                        .createFile(DATA_FOLDER, DATA_TXT, "text/plain", "UTF-8", "Hello world!")
                        .assertFolderExists("/data/data.txt"));
    }

    @Test
    void assertFileExistsFailsWhenFileDoesNotExist(final JCRRepositoryTester helper) {
        assertThrows(AssertionError.class, () -> helper.assertFileExists(DATA_TXT));
    }

    @Test
    void assertFileExistsFailsBecauseItemIsNotAFile(final JCRRepositoryTester helper) {
        assertThrows(AssertionError.class, () -> helper.createRootFolder("data").assertFileExists(DATA_FOLDER));
    }

    @Test
    void importFromXML(final JCRRepositoryTester helper) throws RepositoryException, IOException {
        helper
                .importFromXML("data.xml")
                .assertFolderExists("/a")
                .assertFolderExists("/a/b")
                .assertFileExists("/a/b/c")
                .assertFolderExists("/a/d");
    }

    @Test
    @JCRRepositoryConfiguration(importXMLs = "data.xml")
    void importFromXMLViaAnnotation(final JCRRepositoryTester helper) {
        helper
                .assertFolderExists("/a")
                .assertFolderExists("/a/b")
                .assertFileExists("/a/b/c")
                .assertFolderExists("/a/d");
    }

    @Test
    @JCRRepositoryConfiguration(username = USERNAME, password = PASSWORD)
    void overrideUsernameAndPassword(final Repository repository) throws RepositoryException {
        final Session session = repository.login(new SimpleCredentials(USERNAME, PASSWORD.toCharArray()));
        assertNotNull(session);
        session.logout();
    }

    @Test
    @JCRRepositoryConfiguration(importXMLs = "data.xml")
    void purge(final JCRRepositoryTester helper) throws RepositoryException {
        assertThat(helper.purge())
                .pathDoesNotExist("/a")
                .pathDoesNotExist("/a/b")
                .pathDoesNotExist("/a/b/c")
                .pathDoesNotExist("/a/d");
    }

    @Test
    @JCRRepositoryConfiguration(importXMLs = "data.xml")
    void verifyIsType(final JCRRepositoryTester helper) throws RepositoryException {
        assertThat(helper.isType("/a/b", NT_FILE)).isFalse();
        assertThat(helper.isType("/a/b", NT_FOLDER)).isTrue();
        assertThat(helper.isType("/a/b/c", NT_FILE)).isTrue();
        assertThat(helper.isType("/a/b/c", NT_FOLDER)).isFalse();
        assertThat(helper.isType("/a/d", NT_FILE)).isFalse();
        assertThat(helper.isType("/a/d", NT_FOLDER)).isTrue();
    }

    @Test
    @JCRRepositoryConfiguration(importXMLs = "data.xml")
    void verifyPropertyExists(final JCRRepositoryTester helper) throws RepositoryException {
        assertThat(helper.propertyExists("/a/b", Property.JCR_CREATED_BY)).isTrue();
        assertThat(helper.propertyExists("/a/b/c", Property.JCR_CREATED_BY)).isTrue();
        assertThat(helper.propertyExists("/a/d", Property.JCR_CREATED_BY)).isTrue();
    }

    @Test
    @JCRRepositoryConfiguration(importXMLs = "data.xml")
    void verifyPropertyEquals(final JCRRepositoryTester helper) throws RepositoryException {
        assertThat(helper.property("/a/b", Property.JCR_CREATED_BY)).hasValueSatisfying(property -> isEquals("admin", property::getString));
        assertThat(helper.property("/a/b/c", Property.JCR_CREATED_BY)).hasValueSatisfying(property -> isEquals("admin", property::getString));
        assertThat(helper.property("/a/d", Property.JCR_CREATED_BY)).hasValueSatisfying(property -> isEquals("admin", property::getString));
    }

    private  <T> Consumer<T> isEquals(final T expectedValue,
                                    final JCRAssertions.ValueAccessor<T> accessor) {
        return (actual) -> {
            try {
                assertThat(accessor.get()).isEqualTo(expectedValue);
            } catch (final RepositoryException e) {
                fail(e);
            }
        };
    }
}