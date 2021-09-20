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

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static io.github.kasukusakura.authorization.desktop.MainDisplay.MAIN_FRAME;

public class HttpLinkLabel extends JLabel {
    public String url;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public HttpLinkLabel(String url, String text) {
        super(text);
        this.url = url;
        setToolTipText(Objects.requireNonNullElse(url, ""));

        Font font = getFont();
        Map<TextAttribute, Object> attributes = (Map) font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        setFont(font.deriveFont(attributes));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    protected void onClicked() {
        System.out.println(url);
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Throwable err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(
                    MAIN_FRAME,
                    err.toString(),
                    "Scan image failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);

        if (e.getID() != MouseEvent.MOUSE_CLICKED) return;
        onClicked();
    }
}
