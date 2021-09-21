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
import java.util.Collections;
import java.util.Map;

/**
 * An instance of a secret key
 */
public interface IAuthorizationKey {
    /**
     * Get the provider of this key
     */
    public IAuthorizationService getService();

    /**
     * Serialize this key as binary storage.
     *
     * @apiNote It was serialized as raw format. Not contains any protection.
     * Encrypt it if possible
     *
     * @see IAuthorizationService#deserialize(DataInput)
     */
    public void serialize(DataOutput output) throws IOException;

    /**
     * Check a given code can match this key or not
     *
     * @see #calcValidKey()
     */
    public boolean checkValid(String input);

    /**
     * Serialize this key as URI format
     *
     * @return null if not support
     */
    public default String serializeToUri() {
        return null;
    }

    /**
     * The human-readable name of this key
     */
    public String getKeyName();

    /**
     * Calculate a valid key code at current system time.
     * Or returning null if this key cannot calculate a code
     *
     * @see #checkValid(String)
     */
    public default String calcValidKey() {
        return null;
    }

    /**
     * The invalidation time of {@link #calcValidKey()}
     *
     * @return -1 if {@link #calcValidKey()} not supported
     */
    public default long keyNextInvalidatedTime() {
        return -1;
    }

    /**
     * Get details info of this key for desktop application verbose
     */
    public default Map<String, String> getDetailsInfo() {
        return Collections.emptyMap();
    }

    /**
     * Try to rename this key
     *
     * @return false if failed
     * @apiNote After rename successful. Should re-save this key
     */
    public default boolean rename(String name) {
        return false;
    }
}
