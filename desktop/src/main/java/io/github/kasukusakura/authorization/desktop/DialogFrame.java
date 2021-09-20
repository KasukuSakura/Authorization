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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class DialogFrame extends JDialog {
    //region
    public DialogFrame() {
        setup();
    }

    public DialogFrame(Frame owner) {
        super(owner);
        setup();
    }

    public DialogFrame(Frame owner, boolean modal) {
        super(owner, modal);
        setup();
    }

    public DialogFrame(Frame owner, String title) {
        super(owner, title);
        setup();
    }

    public DialogFrame(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        setup();
    }

    public DialogFrame(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        setup();
    }

    public DialogFrame(Dialog owner) {
        super(owner);
        setup();
    }

    public DialogFrame(Dialog owner, boolean modal) {
        super(owner, modal);
        setup();
    }

    public DialogFrame(Dialog owner, String title) {
        super(owner, title);
        setup();
    }

    public DialogFrame(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
        setup();
    }

    public DialogFrame(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        setup();
    }

    public DialogFrame(Window owner) {
        super(owner);
        setup();
    }

    public DialogFrame(Window owner, ModalityType modalityType) {
        super(owner, modalityType);
        setup();
    }

    public DialogFrame(Window owner, String title) {
        super(owner, title);
        setup();
    }

    public DialogFrame(Window owner, String title, ModalityType modalityType) {
        super(owner, title, modalityType);
        setup();
    }

    public DialogFrame(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
        setup();
    }
    //endregion

    private void setup() {
        setUndecorated(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void showUp() {
        setLocationRelativeTo(getOwner());
        setVisible(true);
        requestFocus();
        addFocusListener(new FocusAdapter() {
            private boolean isOwned(Component component) {
                do {
                    if (component == DialogFrame.this) return true;
                    if (component == null) return false;
                    component = component.getParent();
                } while (true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                Component eOppositeComponent = e.getOppositeComponent();
                if (isOwned(eOppositeComponent)) return;

                SwingUtilities.invokeLater(DialogFrame.this::dispose);
            }
        });
    }
}
