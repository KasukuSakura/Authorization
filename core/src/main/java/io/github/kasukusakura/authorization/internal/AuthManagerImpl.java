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

package io.github.kasukusakura.authorization.internal;

import io.github.kasukusakura.authorization.AuthManager;
import io.github.kasukusakura.authorization.Environment;
import io.github.kasukusakura.authorization.IAuthorizationService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class AuthManagerImpl extends AuthManager {
    protected final Map<String, IAuthorizationService> serviceMap = new HashMap<>();
    protected final Map<String, IAuthorizationService.URIDeserializeService> protocolServices = new HashMap<>();
    protected final Environment environment = new SimpleEnvImpl();


    public AuthManagerImpl() {
        this(true);
    }

    public AuthManagerImpl(boolean loadServices) {
        if (loadServices) {
            registerAuthorizationService(new OtpAuth());

            ServiceLoader.load(IAuthorizationService.class)
                    .forEach(this::registerAuthorizationService);
        }
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public Map<String, IAuthorizationService> getAuthorizationServices() {
        return Collections.unmodifiableMap(serviceMap);
    }

    @Override
    public IAuthorizationService getAuthorizationService(String name) {
        return serviceMap.get(name);
    }

    @Override
    public boolean registerAuthorizationService(IAuthorizationService service) {
        if (serviceMap.putIfAbsent(service.getName(), service) == null) {
            service.useEnvironment(environment);
            IAuthorizationService.URIDeserializeService uriDeserializeService = service.getUriDeserializeService();
            if (uriDeserializeService != null) {
                this.protocolServices.put(uriDeserializeService.getProtocol(), uriDeserializeService);
            }
            return true;
        }
        return false;
    }

    @Override
    public Map<String, IAuthorizationService.URIDeserializeService> getUriDeserializeServices() {
        return Collections.unmodifiableMap(protocolServices);
    }

    @Override
    public IAuthorizationService.URIDeserializeService getUriDeserializeService(String protocol) {
        return protocolServices.get(protocol);
    }
}
