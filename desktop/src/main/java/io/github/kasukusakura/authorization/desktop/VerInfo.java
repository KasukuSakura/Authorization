/*
 * Copyright 2021 KasukuSakura
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

package io.github.kasukusakura.authorization.desktop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.Properties;

public class VerInfo {
    static final Properties PROPERTIES;
    public static final String version;

    static {
        Properties properties = new Properties();
        try (Reader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        VerInfo.class.getResourceAsStream("/authenticator.properties"),
                        "Properties not found"
                )
        ))) {
            properties.load(reader);
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
        PROPERTIES = properties;
        version = properties.getProperty("version");
    }
}
