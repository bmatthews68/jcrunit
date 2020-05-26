package com.btmatthews.jcrunit;

import org.junit.Rule;
import org.junit.Test;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class TestJCRRepositoryRule {

    private static final String DATA_TXT = "data.txt";
    private static final String DATA_FOLDER = "/data";
    @Rule
    public JCRRepositoryRule repositoryRule = new JCRRepositoryRule();

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

    private byte[] generateBinaryData() {
        final Random random = new SecureRandom();
        final int size = 1024 + random.nextInt(4096);
        final byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }
}
