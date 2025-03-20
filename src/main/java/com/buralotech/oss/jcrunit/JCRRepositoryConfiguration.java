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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to specify the parameters used to configure the JCR repository. The
 * annotation can be applied to the test class or method. If the annotation has been applied to both
 * the test class and the test method the annotation on the test method is used.
 *
 * @author <a href="mailto:bmatthews68@gmail.com">Brian Matthews</a>
 * @since 3.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JCRRepositoryConfiguration {

    /**
     * The user name.
     */
    String username() default "admin";

    /**
     * The password.
     */
    String password() default "admin";

    /**
     * Indicates whether nodes created in the repository should be referenceable.
     */
    boolean referenceable() default false;

    /**
     * Paths to XML files used to import content into a repository.
     */
    String[] importXMLs() default {};
}
