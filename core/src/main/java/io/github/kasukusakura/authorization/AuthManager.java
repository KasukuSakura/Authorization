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

import io.github.kasukusakura.authorization.internal.AuthManagerImpl;

import java.util.Map;

/**
 * The manager that manage all 2FA services.
 */
public abstract class AuthManager {
    public abstract Map<String, IAuthorizationService> getAuthorizationServices();

    public abstract Map<String, IAuthorizationService.URIDeserializeService> getUriDeserializeServices();

    public abstract IAuthorizationService getAuthorizationService(String name);

    public abstract IAuthorizationService.URIDeserializeService getUriDeserializeService(String protocol);

    public abstract boolean registerAuthorizationService(IAuthorizationService service);

    public abstract Environment getEnvironment();

    public static AuthManager newInstance() {
        return new AuthManagerImpl();
    }

}
