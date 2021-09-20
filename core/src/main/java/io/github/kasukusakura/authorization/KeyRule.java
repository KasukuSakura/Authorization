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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyRule {
    public enum Type {
        TEXT, COMBO_LIST, BOOLEAN,
    }

    public Type type = Type.TEXT;
    public Object value;
    public List<String> options;
    public String description;

    //region
    public KeyRule type(Type type) {
        this.type = type;
        return this;
    }

    public KeyRule value(Object value) {
        this.value = value;
        return this;
    }

    public KeyRule options(List<String> options) {
        this.options = options;
        return this;
    }
    public KeyRule options(String... options) {
        return options(Arrays.asList(options));
    }

    public KeyRule description(String description) {
        this.description = description;
        return this;
    }
    //endregion

    public static KeyDetailsGenerator generator() {
        return new KeyDetailsGenerator();
    }

    public static class KeyDetailsGenerator {
        public Map<String, KeyRule> rules = new HashMap<>();

        public KeyRule rule(String key) {
            KeyRule kr = new KeyRule();
            rules.put(key, kr);
            return kr;
        }
    }
}
