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

package io.github.kasukusakura.authorization;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

/**
 * The service for serialize/deserialize/generate a key.
 */
public interface IAuthorizationService {
    public String getName();

    /**
     * Read a key from input
     *
     * @see IAuthorizationKey#serialize(DataOutput)
     */
    public IAuthorizationKey deserialize(DataInput input) throws IOException, InvalidKeyException;

    public IAuthorizationKey newRandomAuthorizationKey(Random random, String keyName);

    /**
     * Generate a new key with given rules
     *
     * @see #getKeyRules()
     */
    public IAuthorizationKey generateNewKey(Random random, Map<String, String> values);

    public default Map<String, KeyRule> getKeyRules() {
        return Collections.emptyMap();
    }

    public void useEnvironment(Environment environment);

    /**
     * Get the uri parser for this service.
     *
     * @return null if not support parsing uri
     */
    default URIDeserializeService getUriDeserializeService() {
        return null;
    }

    public interface URIDeserializeService {
        /**
         * The scheme of uri
         */
        String getProtocol();

        IAuthorizationKey deserialize(URI uri) throws IOException, InvalidKeyException;

        /**
         * The declared service
         */
        IAuthorizationService getService();
    }
}
