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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DataOutputUtil {
    public static byte[] readByteArray(DataInput input) throws IOException {
        byte[] src = new byte[input.readInt()];
        input.readFully(src);
        return src;
    }

    public static void writeByteArray(DataOutput output, byte[] data) throws IOException {
        output.writeInt(data.length);
        output.write(data);
    }

    public static String readOptionalString(DataInput input) throws IOException {
        if (input.readBoolean()) {
            return input.readUTF();
        }
        return null;
    }

    public static void writeOptionalString(DataOutput output, String str) throws IOException {
        if (str == null) {
            output.writeBoolean(false);
        } else {
            output.writeBoolean(true);
            output.writeUTF(str);
        }
    }
}
