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

import java.security.SecureRandom;
import java.util.Random;

public abstract class AbstractJCRRepositoryTest {

    private final Random random = new SecureRandom();

    protected byte[] generateBinaryData() {
        final int size = 1024 + random.nextInt(4096);
        final byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }
}
