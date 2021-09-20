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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BFStream {
    public static InputStream dec(InputStream is, byte[] passwd)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            IOException,
            IllegalBlockSizeException,
            BadPaddingException {
        if (passwd == null) return is;
        SecretKeySpec KS = new SecretKeySpec(passwd, "Blowfish");
        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.DECRYPT_MODE, KS);

        CO co = new CO(cipher);
        is.transferTo(co);
        is.close();

        return new ByteArrayInputStream(cipher.doFinal(co.toByteArray()));
    }

    public static OutputStream enc(OutputStream os, byte[] passwd) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (passwd == null) return os;
        SecretKeySpec KS = new SecretKeySpec(passwd, "Blowfish");
        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.ENCRYPT_MODE, KS);

        CO co = new CO(cipher);
        co.transfer = os;
        return co;
    }

    private static class CO extends ByteArrayOutputStream {
        Cipher cipher;
        OutputStream transfer;

        public CO(Cipher cipher) {
            this.cipher = cipher;
        }


        @Override
        public void close() throws IOException {
            if (transfer != null) {
                try (OutputStream os = transfer) {
                    os.write(cipher.doFinal(toByteArray()));
                } catch (IllegalBlockSizeException | BadPaddingException e) {
                    throw new IOException(e);
                }
            }
        }
    }
}
