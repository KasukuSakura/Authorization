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

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.File;

@ConfigSerializable
public class Configuration {
    public boolean passwordProtected = false;
    public boolean firstUse = true;

    public static Configuration INSTANCE;

    private static final File confFile = new File(KeyStorage.STORAGE, "config.conf");
    public static final HoconConfigurationLoader LOADER = HoconConfigurationLoader.builder()
            .file(confFile)
            .prettyPrinting(true)
            .emitComments(true)
            .emitJsonCompatible(true)
            .defaultOptions(ConfigurationOptions.defaults()
                    .shouldCopyDefaults(true)
            )
            .build();

    public static void reload() {
        try {
            if (!confFile.isFile()) {
                INSTANCE = new Configuration();
                return;
            }
            CommentedConfigurationNode node = LOADER.load();
            CommentedConfigurationNode cloned = LOADER.load();

            INSTANCE = node.get(Configuration.class);

            if (INSTANCE == null) {
                node.set(INSTANCE = new Configuration());
            }
            if (!node.equals(cloned)) {
                LOADER.save(node);
            }
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new Configuration();
        }
    }

    public static void save() {
        KeyStorage.STORAGE.mkdirs();
        CommentedConfigurationNode node = LOADER.createNode();
        try {
            node.set(INSTANCE);
            LOADER.save(node);
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }
}
