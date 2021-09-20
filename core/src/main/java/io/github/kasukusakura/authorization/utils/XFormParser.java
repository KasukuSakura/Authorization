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

package io.github.kasukusakura.authorization.utils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("RegExpRedundantEscape")
public class XFormParser {
    private static final Pattern SPLITTER = Pattern.compile("\\&");

    public static Map<String, String> parse(String rawQuery) {
        String[] split = SPLITTER.split(rawQuery);
        HashMap<String, String> rsp = new HashMap<>(split.length);
        for (String rule : split) {
            int indexOf = rule.indexOf('=');
            if (indexOf == -1) {
                rsp.put(URLDecoder.decode(rule, StandardCharsets.UTF_8), "");
            } else {
                rsp.put(
                        URLDecoder.decode(rule.substring(0, indexOf), StandardCharsets.UTF_8),
                        URLDecoder.decode(rule.substring(indexOf + 1), StandardCharsets.UTF_8)
                );
            }
        }
        return rsp;
    }
}
