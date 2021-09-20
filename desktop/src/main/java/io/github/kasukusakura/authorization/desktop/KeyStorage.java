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

import io.github.kasukusakura.authorization.IAuthorizationKey;
import io.github.kasukusakura.authorization.IAuthorizationService;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.github.kasukusakura.authorization.desktop.MainDisplay.MAIN_FRAME;

public class KeyStorage {
    static byte[] passwd;
    private static final Pattern PATTERN_NORMAL_NAME = Pattern.compile(
            "[^A-Za-z0-9_\\-]", Pattern.CASE_INSENSITIVE
    );

    public static final File STORAGE = new File(
            System.getProperty("user.home"),
            ".config/kasuku-sakura-authenticator"
    );
    public static final File KEYS_STORAGE = new File(
            STORAGE, "keys"
    );

    public static Map<IAuthorizationKey, File> KEYS = new HashMap<>();

    public static void loadKey(File keyFile, boolean external) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(
                BFStream.dec(new FileInputStream(keyFile), external ? null : passwd)
        ))) {
            String type = dis.readUTF();
            IAuthorizationService service = MainDisplay.AUTH_MANAGER.getAuthorizationService(type);
            if (service == null) {
                throw new NoSuchAlgorithmException("Service not found: " + type);
            }
            IAuthorizationKey key = service.deserialize(dis);

            if (external) {
                saveKey(key);
            } else {
                KEYS.put(key, keyFile);
            }

            MainDisplay.displayKey(key);
        } catch (Exception e) {
            RuntimeException re = new RuntimeException("Exception in reading key: " + keyFile, e);
            if (external) throw re;
            re.printStackTrace();
            MainDisplay.BottomMsgUpdater.nextDisplayMsg = e.toString();
        }
    }

    private static void readPasswd() {
        JPasswordField pwd = new JPasswordField();
        JOptionPane.showMessageDialog(
                MainDisplay.MAIN_FRAME,
                pwd,
                "Please input your password",
                JOptionPane.PLAIN_MESSAGE
        );
        ByteBuffer bb = StandardCharsets.UTF_8.encode(
                CharBuffer.wrap(pwd.getPassword())
        );
        byte[] rd = new byte[bb.remaining()];
        bb.get(rd);
        passwd = rd;
    }

    public static void reloadKeys() {
        MainDisplay.KEYS.removeIf(it -> {
            JPanel panel = it.declaredPanel;
            Container panelParent = panel.getParent();
            if (panelParent != null) {
                panelParent.remove(panel);
            }
            return true;
        });
        KEYS.clear();

        if (Configuration.INSTANCE.passwordProtected && passwd == null) {
            readPasswd();
        }

        File[] listFiles = KEYS_STORAGE.listFiles();
        if (listFiles == null) return;
        for (File keyFile : listFiles) {
            loadKey(keyFile, false);
        }
    }

    public static void saveKey(IAuthorizationKey key) {
        if (key == null) return;

        File file = KEYS.get(key);
        if (file == null) {
            KEYS.put(key, file = sokName(key));
        }
        save(key, file, false);
    }

    public static void dumpKey(IAuthorizationKey key, File target) {
        save(key, target, true);
    }

    public static void deleteKey(IAuthorizationKey key) {
        File file = KEYS.remove(key);
        if (file != null) file.delete();
    }

    public static String dropIllegalCharacters(String n) {
        return PATTERN_NORMAL_NAME.matcher(n).replaceAll("_");
    }

    private static File sokName(IAuthorizationKey key) {
        String n = dropIllegalCharacters(key.getKeyName());
        int counter = 0;
        File sf = new File(KEYS_STORAGE, n + ".key");
        do {
            if (!sf.exists()) return sf;

            counter++;
            sf = new File(KEYS_STORAGE, n + "_" + counter + ".key");
        } while (true);
    }

    private static void save(IAuthorizationKey key, File file, boolean ext) {
        KEYS_STORAGE.mkdirs();
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
                BFStream.enc(new FileOutputStream(file), ext ? null : passwd)
        ))) {
            dos.writeUTF(key.getService().getName());
            key.serialize(dos);
        } catch (Exception e) {
            new RuntimeException("Exception in writing " + file + "(" + key.getKeyName() + ")", e).printStackTrace();
        }
    }

    public static void openExplorer() {
        STORAGE.mkdirs();
        try {
            System.out.println(STORAGE);
            Desktop.getDesktop().open(STORAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    MAIN_FRAME,
                    e.toString(),
                    "Error when opening storage folder",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
