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

import io.github.kasukusakura.authorization.IAuthorizationKey;
import io.github.kasukusakura.authorization.IAuthorizationService;
import io.github.kasukusakura.authorization.KeyRule;
import io.github.kasukusakura.authorization.utils.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/*
 * https://en.wikipedia.org/wiki/Time-based_One-Time_Password
 */
public class OtpAuth
        extends AuthorizationServiceBase
        implements
        IAuthorizationService,
        IAuthorizationService.URIDeserializeService {

    private static final ThreadLocal<byte[]> TMD8 = ThreadLocal.withInitial(
            () -> new byte[8]
    );

    public class TotpAuthKey implements IAuthorizationKey {
        String keyName;
        byte[] src;
        String algorithm; // SHA1, SHA256, SHA512
        String issuer;
        int digits = 6;
        long period = 30; // s

        transient SecretKeySpec keySpec;
        transient long digits_0;
        transient long period_ms;

        void initialize() {
            if (algorithm == null) {
                algorithm = "SHA1";
            }
            algorithm = algorithm.toUpperCase(Locale.ROOT);
            keySpec = new SecretKeySpec(src, "Hmac" + algorithm);
            if (keyName == null || keyName.isEmpty()) {
                keyName = "RandomKey " + UUID.randomUUID();
            }
            digits_0 = (long) Math.pow(10, digits);
            period_ms = TimeUnit.SECONDS.toMillis(period);
            if (issuer != null && issuer.isEmpty()) {
                issuer = null;
            }
        }

        @Override
        public void serialize(DataOutput output) throws IOException {
            output.writeUTF(keyName);
            output.writeUTF(algorithm);
            DataOutputUtil.writeByteArray(output, src);
            DataOutputUtil.writeOptionalString(output, issuer);
            output.writeInt(digits);
            output.writeLong(period);
        }

        public long currentFrame() {
            return System.currentTimeMillis() / period_ms;
        }

        public int code(long frame) {
            byte[] data = TMD8.get();
            for (int i = 8; i-- > 0; frame >>>= 8) {
                data[i] = (byte) frame;
            }
            try {
                Mac mac = Mac.getInstance("Hmac" + algorithm);
                mac.init(keySpec);
                byte[] hash = mac.doFinal(data);
                int offset = hash[hash.length - 1] & 0xF;

                int truncatedHash = 0;
                for (int i = 0; i < 4; ++i) {
                    truncatedHash <<= 8;

                    // Java bytes are signed, but we need an unsigned integer:
                    // cleaning off all but the LSB.
                    truncatedHash |= (hash[offset + i] & 0xFF);
                }

                // Clean bits higher than the 32nd (inclusive) and calculate the
                // module with the maximum validation code value.
                truncatedHash &= 0x7FFFFFFF;
                truncatedHash %= digits_0;

                // Returning the validation code to the caller.
                return truncatedHash;
            } catch (Exception anyError) {
                //noinspection ThrowablePrintedToSystemOut
                System.err.println(anyError);
                return -1;
            }
        }

        @Override
        public long keyNextInvalidatedTime() {
            return (currentFrame() + 1) * period_ms;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean checkValid(String input) {
            long v;
            try {
                v = Long.parseLong(input);
                if (v < 0) return false;
            } catch (NumberFormatException ignored) {
                return false;
            }

            long now = currentFrame();

            if (code(now) == v) return true;
            if (code(now + 1) == v) return true;
            if (code(now - 1) == v) return true;

            return false;
        }

        @Override
        public String getKeyName() {
            return keyName;
        }

        @Override
        public IAuthorizationService getService() {
            return OtpAuth.this;
        }

        @Override
        public String calcValidKey() {
            String rsp = String.valueOf(code(currentFrame()));
            if (rsp.length() < digits) {
                int c = digits - rsp.length();
                StringBuilder sb = new StringBuilder(digits);

                while (c-- > 0) sb.append('0');
                sb.append(rsp);

                return sb.toString();
            }
            return rsp;
        }

        @Override
        public String serializeToUri() {
            StringBuilder sb = new StringBuilder("otpauth://totp/");
            URLEncoder.encode(keyName, StandardCharsets.UTF_8, sb);
            sb.append("?secret=");
            Base32.encode(src, sb);
            sb.append("&algorithm=").append(algorithm);
            sb.append("&digits=").append(digits);
            sb.append("&period=").append(period);
            if (issuer != null) {
                sb.append("&issuer=");
                URLEncoder.encode(issuer, StandardCharsets.UTF_8, sb);
            }
            return sb.toString();
        }

        @Override
        public Map<String, String> getDetailsInfo() {
            HashMap<String, String> details = new HashMap<>();
            details.put("name", keyName);
            details.put("secret", Base32.encode(src));
            details.put("algorithm", algorithm);
            details.put("digits", String.valueOf(digits));
            details.put("period (s)", String.valueOf(period));
            return details;
        }

        @Override
        public boolean rename(String name) {
            if (name == null || name.isEmpty()) return false;
            this.keyName = name;
            return true;
        }
    }

    @Override
    public String getName() {
        return "otpauth";
    }

    @Override
    public IAuthorizationKey deserialize(DataInput input) throws IOException, InvalidKeyException {
        TotpAuthKey key = new TotpAuthKey();
        key.keyName = input.readUTF();
        key.algorithm = input.readUTF();
        key.src = DataOutputUtil.readByteArray(input);
        key.issuer = DataOutputUtil.readOptionalString(input);
        key.digits = input.readInt();
        key.period = input.readLong();
        key.initialize();
        return key;
    }

    @Override
    public IAuthorizationKey newRandomAuthorizationKey(Random random, String keyName) {
        byte[] src = new byte[20];
        random.nextBytes(src);
        TotpAuthKey key = new TotpAuthKey();
        key.keyName = keyName;
        key.src = src;
        key.initialize();
        return key;
    }

    @Override
    public IAuthorizationKey generateNewKey(Random random, Map<String, String> values) {
        TotpAuthKey totpAuthKey = new TotpAuthKey();
        totpAuthKey.keyName = values.get("name");
        totpAuthKey.issuer = values.get("issuer");
        totpAuthKey.algorithm = values.get("algorithm");
        totpAuthKey.digits = Integer.parseInt(values.getOrDefault("digits", "6"));
        totpAuthKey.period = Integer.parseInt(values.getOrDefault("period", "30"));

        String secret = values.get("secret");
        if (secret != null && !secret.isEmpty()) {
            totpAuthKey.src = Base32.decode(secret);
        } else {
            int len = Integer.parseInt(values.getOrDefault("secret-len", "20"));
            byte[] src = new byte[len];
            random.nextBytes(src);
            totpAuthKey.src = src;
        }
        totpAuthKey.initialize();
        return totpAuthKey;
    }

    @Override
    public Map<String, KeyRule> getKeyRules() {
        KeyRule.KeyDetailsGenerator generator = KeyRule.generator();
        generator.rule("name")
                .description("The name of key")
        ;
        generator.rule("issuer")
                .description("The issuer of this key")
        ;
        generator.rule("algorithm").type(KeyRule.Type.COMBO_LIST)
                .description("The algorithm")
                .options("SHA1", "SHA256", "SHA512")
                .value("SHA1")
        ;
        generator.rule("digits")
                .description("The digits")
                .value(6)
        ;
        generator.rule("period")
                .description("The period of this key")
                .value("30")
        ;
        generator.rule("secret")
                .description("The secret of this key");
        generator.rule("secret-len")
                .description("Key length")
                .value(20);
        return generator.rules;
    }

    @Override
    public URIDeserializeService getUriDeserializeService() {
        return this;
    }

    @Override
    public String getProtocol() {
        return "otpauth";
    }

    @Override
    public IAuthorizationKey deserialize(URI uri) throws IOException, InvalidKeyException {
        if (!"totp".equals(uri.getHost())) {
            throw new InvalidKeyException("Only support totp protocol, but got " + uri.getHost());
        }
        String name = uri.getPath().substring(1);
        if (name.isEmpty()) throw new InvalidKeyException("Empty key name");
        if (uri.getRawQuery() == null) throw new InvalidKeyException("No parameters");
        Map<String, String> args = XFormParser.parse(uri.getRawQuery());
        String secret = args.get("secret");
        if (secret == null) {
            throw new InvalidKeyException("No secrets found in " + uri);
        }
        TotpAuthKey authKey = new TotpAuthKey();
        authKey.keyName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        authKey.src = Base32.decode(secret);
        authKey.algorithm = args.get("algorithm");
        authKey.digits = Integer.parseInt(args.getOrDefault("digits", "6"));
        authKey.period = Long.parseLong(args.getOrDefault("period", "30"));
        authKey.issuer = args.get("issuer");
        authKey.initialize();
        return authKey;
    }

    @Override
    public IAuthorizationService getService() {
        return this;
    }
}
