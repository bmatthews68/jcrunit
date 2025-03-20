package com.buralotech.oss.jcrunit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.RepositoryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TestJCRAssertions {

    @Mock
    private JCRRepositoryTester repositoryTester;

    @Test
    void pathExistsDelegatesToExistsMethod() throws RepositoryException {
        doReturn(new JCRAssertions(repositoryTester)).when(repositoryTester).assertThat();
        doReturn(true).when(repositoryTester).exists(anyString());
        assertThat(repositoryTester).pathExists("/a");
        verify(repositoryTester).exists("/a");
    }

    @Test
    void pathExistsDelegatesToExistsMethodAndFails() throws RepositoryException {
        doReturn(new JCRAssertions(repositoryTester)).when(repositoryTester).assertThat();
        doReturn(false).when(repositoryTester).exists(anyString());
        assertThatThrownBy(() -> assertThat(repositoryTester).pathExists("/a"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected /a to exist.");
        verify(repositoryTester).exists("/a");
    }


    @Test
    void pathDoesNotExistDelegatesToExistsMethod() throws RepositoryException {
        doReturn(new JCRAssertions(repositoryTester)).when(repositoryTester).assertThat();
        doReturn(false).when(repositoryTester).exists(anyString());
        assertThat(repositoryTester).pathDoesNotExist("/a");
        verify(repositoryTester).exists("/a");
    }

    @Test
    void pathDoesNotExistDelegatesToExistsMethodAndFails() throws RepositoryException {
        doReturn(new JCRAssertions(repositoryTester)).when(repositoryTester).assertThat();
        doReturn(true).when(repositoryTester).exists(anyString());
        assertThatThrownBy(() -> assertThat(repositoryTester).pathDoesNotExist("/a"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected /a to not exist.");
        verify(repositoryTester).exists("/a");
    }
}
