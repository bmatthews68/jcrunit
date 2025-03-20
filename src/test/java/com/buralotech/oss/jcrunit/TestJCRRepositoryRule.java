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

import org.junit.Rule;
import org.junit.Test;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class TestJCRRepositoryRule extends AbstractJCRRepositoryTest {

    private static final String DATA_TXT = "data.txt";
    private static final String DATA_FOLDER = "/data";
    @Rule
    public JCRRepositoryRule repositoryRule = JCRRepositoryRule.withDefaultCredentials();

    @Test
    public void repositoryHasBeenCreated() {
        assertNotNull(repositoryRule.getRepository());
    }

    @Test
    public void createFolderAtRoot() throws RepositoryException {
        repositoryRule
                .createRootFolder("top")
                .assertFolderExists("/top");
    }

    @Test
    public void cannotCreateDuplicateRootFolder() {
        assertThrows(
                ItemExistsException.class,
                () -> repositoryRule
                        .createRootFolder("top")
                        .assertFolderExists("/top")
                        .createRootFolder("top"));
    }

    @Test
    public void cannotCreateSubFolderIfRootDoesNotExist() {
        assertThrows(
                PathNotFoundException.class,
                () -> repositoryRule.createFolder("/top", "sub"));
    }


    @Test
    public void canCreateSubFolderWhenRootExists() throws RepositoryException {
        repositoryRule
                .createRootFolder("top")
                .assertFolderExists("/top")
                .createFolder("/top", "sub")
                .assertFolderExists("/top/sub");
    }

    @Test
    public void canCreateBinaryFile() throws IOException, RepositoryException {
        final byte[] data = generateBinaryData();
        repositoryRule
                .createRootFolder("bin")
                .assertFolderExists("/bin")
                .createFile("/bin", "data.bin", "application/octet", null, data)
                .assertFileExists("/bin/data.bin");
    }

    @Test
    public void canCreateTextFile() throws IOException, RepositoryException {
        repositoryRule
                .createRootFolder("txt")
                .assertFolderExists("/txt")
                .createFile("/txt", DATA_TXT, "text/plain", "UTF-8", "Hello world")
                .assertFileExists("/txt/data.txt");
    }

    @Test
    public void assertFolderExistsFailsWhenFolderDoesNotExist() {
        assertThrows(AssertionError.class, () -> repositoryRule.assertFolderExists(DATA_FOLDER));
    }

    @Test
    public void assertFolderExistsFailsBecauseItemIsNotAFolder() {
        assertThrows(
                AssertionError.class,
                () -> repositoryRule
                        .createRootFolder("data")
                        .createFile(DATA_FOLDER, DATA_TXT, "text/plain", "UTF-8", "Hello world!")
                        .assertFolderExists("/data/data.txt"));
    }

    @Test
    public void assertFileExistsFailsWhenFileDoesNotExist() {
        assertThrows(AssertionError.class, () -> repositoryRule.assertFileExists(DATA_TXT));
    }

    @Test
    public void assertFileExistsFailsBecauseItemIsNotAFile() {
        assertThrows(AssertionError.class, () -> repositoryRule.createRootFolder("data").assertFileExists(DATA_FOLDER));
    }

    @Test
    public void importFromXML() throws RepositoryException, IOException {
        repositoryRule
                .importFromXML("data.xml")
                .assertFolderExists("/a")
                .assertFolderExists("/a/b")
                .assertFileExists("/a/b/c")
                .assertFolderExists("/a/d");
    }

    @Test
    public void purge() throws RepositoryException {
        assertThat(repositoryRule.purge())
                .pathDoesNotExist("/a")
                .pathDoesNotExist("/a/b")
                .pathDoesNotExist("/a/b/c")
                .pathDoesNotExist("/a/d");
    }
}
