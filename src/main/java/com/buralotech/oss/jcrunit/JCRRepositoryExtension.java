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

import org.junit.jupiter.api.extension.*;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * JUnit 5 (Jupiter) extension that will start an embedded JCR repository before the test method execution and
 * stop the embedded JCR repository when the test method completes.
 *
 * @author <a href="mailto:bmatthews68@gmail.com">Brian Matthews</a>
 * @since 3.0
 */
public class JCRRepositoryExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    /**
     * The name of the property used to cache the reference to the JCR repository helper.
     */
    private static final String HELPER = "helper";

    /**
     * The name of the property used to cache the reference to the JCR repository.
     */
    private static final String REPOSITORY = "repository";

    /**
     * This callback is invoked before the test method is executed and is responsible for starting the embedded
     * JCR repository.
     *
     * @param context – the extension context for the Executable about to be invoked; never {@code null}.
     */
    @Override
    public void beforeEach(final ExtensionContext context) {
        final JCRRepositoryConfiguration annotation = getAnnotation(context);
        if (annotation != null) {
            try {
                final JCRRepositoryTester helper = JCRRepositoryTester.createHelper(annotation);
                final ExtensionContext.Store store = getStore(context);
                store.put(HELPER, helper);
                store.put(REPOSITORY, helper.getRepository());
            } catch (final IOException | RepositoryException e) {
                throw new AssertionError("Failed to launch embedded JCR repository", e);
            }
        }
    }

    /**
     * This callback is invoked after the test method is executed and is responsible for stopping the embedded
     * JCR repository.
     *
     * @param context – the extension context for the Executable about to be invoked; never {@code null}.
     */
    @Override
    public void afterEach(final ExtensionContext context) {
        final ExtensionContext.Store store = getStore(context);
        if (store != null) {
            store.remove(REPOSITORY);
            store.remove(HELPER);
        }
    }

    /**
     * Check the parameter type is {@link JCRRepositoryTester} or {@link Repository}.
     *
     * @param parameterContext The context for the parameter for which an argument should be resolved;
     *                         never {@code null}.
     * @param extensionContext The extension context for the Executable about to be invoked; never {@code null}.
     * @return {@code true} if the parameter type is {@link JCRRepositoryTester} or {@link Repository}.
     * Otherwise, {@code false}.
     */
    @Override
    public boolean supportsParameter(final ParameterContext parameterContext,
                                     final ExtensionContext extensionContext) throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();
        return JCRRepositoryTester.class.equals(parameterType) || Repository.class.equals(parameterType);
    }

    /**
     * Resolve {@link JCRRepositoryTester} parameters.
     *
     * @param parameterContext The context for the parameter for which an argument should be resolved;
     *                         never {@code null}.
     * @param extensionContext –The extension context for the Executable about to be invoked; never {@code null}.
     * @return The resolved parameter.
     * @throws ParameterResolutionException If a conneciton to the directory server could not be established.
     */
    @Override
    public Object resolveParameter(final ParameterContext parameterContext,
                                   final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();
        if (JCRRepositoryTester.class.equals(parameterType)) {
            return getStore(extensionContext).get(HELPER, JCRRepositoryTester.class);
        } else if (Repository.class.equals(parameterType)) {
            return getStore(extensionContext).get(REPOSITORY, Repository.class);
        } else {
            return null;
        }
    }

    /**
     * Get teh context storage for the method invocation.
     *
     * @param extensionContext – the extension context for the Executable about to be invoked; never {@code null}.
     * @return The context store.
     */
    private ExtensionContext.Store getStore(final ExtensionContext extensionContext) {
        final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(
                JCRRepositoryExtension.class,
                extensionContext.getRequiredTestMethod());
        return extensionContext.getStore(namespace);
    }

    /**
     * Locate the annotation that specifies the configuration for the content repository. The annotation is
     * sought on the test method declaration before falling back to check the test class.
     *
     * @param extensionContext – the extension context for the Executable about to be invoked; never {@code null}.
     * @return The {@code JCRRepositoryConfiguration} annotation if found. Otherwise, {@code null}.
     */
    private JCRRepositoryConfiguration getAnnotation(final ExtensionContext extensionContext) {
        final JCRRepositoryConfiguration annotation = extensionContext.getRequiredTestMethod()
                .getAnnotation(JCRRepositoryConfiguration.class);
        if (annotation == null) {
            return extensionContext.getRequiredTestClass().getAnnotation(JCRRepositoryConfiguration.class);
        } else {
            return annotation;
        }
    }
}
