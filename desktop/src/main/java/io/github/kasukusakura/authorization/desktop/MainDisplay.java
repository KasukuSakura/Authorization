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

import com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.github.kasukusakura.authorization.AuthManager;
import io.github.kasukusakura.authorization.IAuthorizationKey;
import io.github.kasukusakura.authorization.IAuthorizationService;
import io.github.kasukusakura.authorization.KeyRule;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.LC;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;

import javax.accessibility.AccessibleContext;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class MainDisplay {
    static {
        FlatDarkFlatIJTheme.setup();
    }

    public static final JFrame MAIN_FRAME = new JFrame("Kasuku Sakura - Authenticator Desktop") {
        @Override
        protected void processWindowEvent(WindowEvent e) {
            super.processWindowEvent(e);
            if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                systemShutdown();
            }
        }
    };

    public static final JPanel STATUS_BAR = new JPanel();
    public static final JPanel MAIN_CONTENT = new JPanel();
    public static final JLabel BOTTOM_MSG_BAR = new JLabel("Authenticator " + VerInfo.version);
    public static final ConcurrentLinkedDeque<RenderedLabel> KEYS = new ConcurrentLinkedDeque<>();
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(5, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Service #" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });
    public static final UnitValue UNIT_VALUE_ZERO = new UnitValue(0);
    public static final MouseListener COPY_ON_CLICK_LISTENER = new MouseAdapter() {
        //region
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            Component component = e.getComponent();
            if (component instanceof Label) {
                copyText(((Label) component).getText());
            }
            if (component instanceof JLabel) {
                copyText(((JLabel) component).getText());
            }
        }
        //endregion
    };

    public static class KeyDetailsSwitcher extends MouseAdapter {
        public RenderedLabel declaredLabel;

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Component component = e.getComponent();
                if (component == declaredLabel
                        || component == declaredLabel.declaredPanel
                        || component == declaredLabel.counter
                ) handle();
            }
            if (e.getButton() == MouseEvent.BUTTON3) {
                declaredLabel.onRightClick(e);
            }
        }

        private void handle() {
            declaredLabel.setDetailsVisitable(!declaredLabel.isDetailsVisitable());
            MAIN_FRAME.validate();
            MAIN_FRAME.repaint();
        }
    }

    public static final AuthManager AUTH_MANAGER = AuthManager.newInstance();

    public static class BottomMsgUpdater {
        public static String BOTTOM_MSG = "Authorization";
        public static volatile String nextDisplayMsg;
        public static long invalidateTime;
    }

    public static class SvdProgressBar extends JProgressBar {
        public long maxV, minV, currentV;

        @Override
        public void setMaximum(int n) {
            this.maxV = n;
            this.model.setMaximum(n);
        }

        @Override
        public void setMinimum(int n) {
            this.minV = n;
            this.model.setMinimum(n);
        }

        @Override
        public int getMaximum() {
            return (int) maxV;
        }

        @Override
        public int getMinimum() {
            return (int) this.minV;
        }

        @Override
        public double getPercentComplete() {
            long range = maxV - minV;
            long crt = currentV - minV;

            return ((double) crt) / ((double) range);
        }

        @Override
        public void setValue(int n) {
            setValue((long) n);
        }

        public void setValue(long value) {
            long oldV = currentV;
            this.currentV = value;
            AccessibleContext accessibleContext = this.accessibleContext;
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
                        (int) oldV,
                        (int) value
                );
            }
            validate();
            repaint();
        }
    }


    public static class RenderedLabel extends JLabel {
        public JPanel declaredPanel;
        public IAuthorizationKey declaredKey;
        public SvdProgressBar counter = new SvdProgressBar();
        public long startTime, nextTime;
        public JLabel codeLabel;
        public JPanel details;
        public JLabel codePlaceholder;

        public RenderedLabel(String text) {
            super(text);
            counter.setMaximum(1000);
            counter.setMinimum(0);
        }

        public boolean isDetailsVisitable() {
            return details.getParent() != null;
        }

        public void setDetailsVisitable(boolean visitable) {
            boolean isV = isDetailsVisitable();
            if (isV == visitable) return;
            if (visitable) {
                declaredPanel.add(details, "cell 0 2 3 1, growx");
            } else {
                declaredPanel.remove(details);
            }
        }

        public void onRightClick(MouseEvent event) {
            JPopupMenu popupMenu = new JPopupMenu();

            popupMenu.add(new JMenuItem(declaredKey.getKeyName())).setEnabled(false);
            popupMenu.addSeparator();

            popupMenu.add(new JMenuItem("Rename")
                    .handle(() -> {
                        Object newName = JOptionPane.showInputDialog(
                                MAIN_FRAME,
                                "New name",
                                "Rename key " + declaredKey.getKeyName(),
                                JOptionPane.PLAIN_MESSAGE,
                                null, null,
                                declaredKey.getKeyName()
                        );
                        if (newName != null) {
                            if (declaredKey.rename(newName.toString())) {
                                BottomMsgUpdater.nextDisplayMsg = "Rename successful";
                                Border border = declaredPanel.getBorder();
                                if (border instanceof TitledBorder) {
                                    ((TitledBorder) border).setTitle(declaredKey.getKeyName());
                                    declaredPanel.validate();
                                    declaredPanel.repaint();
                                }
                                KeyStorage.saveKey(declaredKey); // override
                            } else {
                                BottomMsgUpdater.nextDisplayMsg = "Rename failed";
                            }
                        } else {
                            BottomMsgUpdater.nextDisplayMsg = "Cancelled";
                        }
                    })
            );
            popupMenu.add(new JMenuItem("Delete")
                    .handle(() -> {
                        KeyStorage.deleteKey(declaredKey);
                        JPanel declaredPanel = this.declaredPanel;
                        if (declaredPanel.getParent() != null) {
                            declaredPanel.getParent().remove(declaredPanel);
                            MAIN_FRAME.validate();
                            MAIN_FRAME.repaint();
                        }
                    })
            );
            popupMenu.addSeparator();

            popupMenu.add(new JMenuItem("Hidden details")
                    .handle(() -> {
                        setDetailsVisitable(false);
                        MAIN_FRAME.validate();
                        MAIN_FRAME.repaint();
                    })
            );
            popupMenu.add(new JMenuItem("Show details")
                    .handle(() -> {
                        setDetailsVisitable(true);
                        MAIN_FRAME.validate();
                        MAIN_FRAME.repaint();
                    })
            );
            popupMenu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    public static void initialize() {
        MAIN_FRAME.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        {
            MigLayout layout = new MigLayout("insets 5");
            LC lc = ConstraintParser.parseLayoutConstraint((String) layout.getLayoutConstraints());
            UnitValue[] insets = lc.getInsets();
            lc.setInsets(new UnitValue[]{
                    new UnitValue(0, UnitValue.PIXEL, null),
                    insets[1], insets[2], insets[3]
            });
            layout.setLayoutConstraints(lc);
            STATUS_BAR.setLayout(layout);
            STATUS_BAR.add(new JSeparator(), "cell 0 0 5 1");
            STATUS_BAR.add(BOTTOM_MSG_BAR, "cell 0 1 0 0");
            // STATUS_BAR.setBackground(Color.RED);
            // BOTTOM_MSG_BAR.setBackground(Color.BLACK);
        }
        {
            JScrollPane pane = new JScrollPane(MAIN_CONTENT);
            pane.getVerticalScrollBar().setUnitIncrement(16);
            MAIN_CONTENT.setLayout(new MigLayout("fillx"));
            MAIN_FRAME.add(pane, BorderLayout.CENTER);
        }
        MAIN_FRAME.add(STATUS_BAR, BorderLayout.SOUTH);

        {
            JMenuBar menuBar = new JMenuBar();
            {
                JMenu file = new JMenu("File");

                file.add(new JMenuItem("New random key")
                        .handle(() -> {
                            JOptionPane optionPane = new JOptionPane();

                            JPanel panel = new JPanel();
                            optionPane.setMessage(panel);
                            panel.setLayout(new MigLayout("", "[][fill,grow]"));

                            JComboBox<String> serviceType = new JComboBox<>();
                            JTextField keyName = new JTextField("Key " + UUID.randomUUID());

                            Map<String, IAuthorizationService> services = AUTH_MANAGER.getAuthorizationServices();
                            for (String sType : services.keySet()) {
                                serviceType.addItem(sType);
                            }
                            panel.add(new JLabel("Service type"));
                            panel.add(serviceType, "wrap");
                            panel.add(new JLabel("Key name"));
                            panel.add(keyName);


                            JDialog dialog = optionPane.createDialog(MAIN_FRAME, "Random key generator");
                            dialog.setResizable(true);
                            dialog.setVisible(true);

                            IAuthorizationService service = services.get(String.valueOf(serviceType.getSelectedItem()));
                            if (service == null) {
                                new Exception("Internal error: No service: " + serviceType.getSelectedItem()).printStackTrace();
                                return;
                            }
                            IAuthorizationKey key = service.newRandomAuthorizationKey(new Random(), keyName.getText());
                            KeyStorage.saveKey(key);
                            displayKey(key);
                        })
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_N,
                                MouseEvent.CTRL_DOWN_MASK |
                                        MouseEvent.SHIFT_DOWN_MASK
                        ))
                );
                file.add(new JMenuItem("Generate new key")
                        .handle(() -> {
                            Map<String, IAuthorizationService> services = AUTH_MANAGER.getAuthorizationServices();
                            JTabbedPane top = new JTabbedPane();
                            List<Map<String, JComponent>> options = new ArrayList<>(services.size());
                            List<IAuthorizationService> serviceList = new ArrayList<>(services.size());

                            for (Map.Entry<String, IAuthorizationService> sv : services.entrySet()) {
                                JPanel panel = new JPanel();
                                panel.setLayout(new MigLayout("", "[fill][fill,grow]"));
                                top.addTab(sv.getKey(), panel);

                                IAuthorizationService authorizationService = sv.getValue();
                                Map<String, KeyRule> keyRules = authorizationService.getKeyRules();
                                Map<String, JComponent> cps = new HashMap<>();

                                options.add(cps);
                                serviceList.add(authorizationService);

                                int row = 0;
                                for (Map.Entry<String, KeyRule> keyRuleEntry : keyRules.entrySet()) {
                                    JLabel label = new JLabel(keyRuleEntry.getKey());
                                    panel.add(label, "cell 0 " + row);

                                    KeyRule keyRule = keyRuleEntry.getValue();
                                    JComponent cp;
                                    switch (keyRule.type) {
                                        case TEXT: {
                                            JTextField txt = new JTextField();
                                            cp = txt;
                                            if (keyRule.value != null) {
                                                txt.setText(keyRule.value.toString());
                                            }
                                            break;
                                        }
                                        case BOOLEAN: {
                                            JCheckBox checkBox = new JCheckBox();
                                            cp = checkBox;
                                            if (keyRule.value != null) {
                                                checkBox.setSelected((Boolean) keyRule.value);
                                            }
                                            break;
                                        }
                                        case COMBO_LIST: {
                                            JComboBox<String> comboBox = new JComboBox<>();
                                            cp = comboBox;
                                            for (String option : keyRule.options) {
                                                comboBox.addItem(option);
                                            }
                                            break;
                                        }
                                        default:
                                            throw new AssertionError();
                                    }
                                    panel.add(cp, "cell 1 " + row);
                                    label.setLabelFor(cp);

                                    cps.put(keyRuleEntry.getKey(), cp);

                                    row++;
                                    if (keyRule.description != null) {
                                        JLabel jb = new JLabel(keyRule.description);
                                        jb.setForeground(new Color(157, 157, 157));
                                        panel.add(jb, "cell 1 " + row);
                                        jb.setToolTipText(keyRule.description);
                                        label.setToolTipText(keyRule.description);
                                        cp.setToolTipText(keyRule.description);

                                        row++;
                                    }
                                }
                                top.setSelectedIndex(0);
                            }

                            JOptionPane optionPane = new JOptionPane();
                            optionPane.setMessage(top);
                            optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
                            JDialog dialog = optionPane.createDialog(MAIN_FRAME, "Generate new key");
                            dialog.setResizable(true);
                            dialog.setVisible(true);

                            Map<String, JComponent> selectedOptions = options.get(top.getSelectedIndex());
                            IAuthorizationService selectedService = serviceList.get(top.getSelectedIndex());

                            Map<String, String> keys = new HashMap<>();
                            for (Map.Entry<String, JComponent> cps : selectedOptions.entrySet()) {
                                String v;
                                JComponent p = cps.getValue();
                                if (p instanceof JTextComponent) {
                                    v = ((JTextComponent) p).getText();
                                } else if (p instanceof JCheckBox) {
                                    v = String.valueOf(((JCheckBox) p).isSelected());
                                } else if (p instanceof JComboBox) {
                                    v = String.valueOf(((JComboBox<?>) p).getSelectedItem());
                                } else {
                                    v = p.toString();
                                }
                                keys.put(cps.getKey(), v);
                            }
                            IAuthorizationKey authorizationKey = selectedService.generateNewKey(new Random(), keys);
                            KeyStorage.saveKey(authorizationKey);
                            displayKey(authorizationKey);
                        })
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_G,
                                MouseEvent.CTRL_DOWN_MASK |
                                        MouseEvent.SHIFT_DOWN_MASK
                        ))
                );

                file.addSeparator();
                class QRScanTask implements Runnable {
                    protected BufferedImage read() throws Exception {
                        FileChooser chooser = new FileChooser();
                        File selected = chooser.showOpenDialog(null);
                        if (selected == null) return null;
                        ImageIO.setUseCache(false);
                        return ImageIO.read(selected);
                    }

                    @Override
                    public void run() {
                        try {
                            BufferedImage image = read();
                            if (image == null) return;

                            LuminanceSource source = new BufferedImageLuminanceSource(image);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                            Result result = new MultiFormatReader().decode(bitmap);

                            String uri = result.getText();
                            loadFromURI(uri);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(
                                    MAIN_FRAME,
                                    e.toString(),
                                    "Scan image failed",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                }
                file.add(new JMenuItem("Import key")
                        .handleIn(Platform::runLater, () -> {
                            FileChooser chooser = new FileChooser();
                            File selected = chooser.showOpenDialog(null);
                            if (selected == null) return;
                            try {
                                KeyStorage.loadKey(selected, true);
                            } catch (Exception e) {
                                e.printStackTrace();
                                JOptionPane.showMessageDialog(
                                        MAIN_FRAME,
                                        e.toString(),
                                        "Error when reading " + selected,
                                        JOptionPane.ERROR_MESSAGE
                                );
                            }
                        })
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_I,
                                MouseEvent.CTRL_DOWN_MASK |
                                        MouseEvent.SHIFT_DOWN_MASK
                        ))
                );
                file.add(new JMenuItem("Scan from file")
                        .handleIn(Platform::runLater, new QRScanTask())
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_F,
                                MouseEvent.CTRL_DOWN_MASK |
                                        MouseEvent.SHIFT_DOWN_MASK
                        ))
                );
                file.add(new JMenuItem("Scan from clipboard")
                        .handleAsync(new QRScanTask() {
                            //region
                            @SuppressWarnings("unchecked")
                            @Override
                            protected BufferedImage read() throws Exception {
                                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                                Transferable contents = clipboard.getContents(null);
                                if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                                    return (BufferedImage) contents.getTransferData(DataFlavor.imageFlavor);
                                }
                                if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                    List<File> list = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
                                    return ImageIO.read(list.get(0));
                                }
                                return (BufferedImage) clipboard.getData(DataFlavor.imageFlavor);
                            }
                            //endregion
                        })
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_C,
                                MouseEvent.CTRL_DOWN_MASK |
                                        MouseEvent.SHIFT_DOWN_MASK
                        ))
                );

                file.addSeparator();
                file.add(new JMenuItem("Reload")
                        .handle(KeyStorage::reloadKeys)
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_R,
                                MouseEvent.CTRL_DOWN_MASK |
                                        MouseEvent.SHIFT_DOWN_MASK
                        ))
                );
                file.add(new JMenuItem("Open storage folder")
                        .handle(KeyStorage::openExplorer)
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_O,
                                MouseEvent.CTRL_DOWN_MASK |
                                        MouseEvent.SHIFT_DOWN_MASK
                        ))
                );

                file.addSeparator();
                file.add(new JMenuItem("Exit")
                        .handle(MainDisplay::systemShutdown)
                );

                menuBar.add(file);
            }
            {
                JMenu view = new JMenu("View");

                view.add(new JMenuItem("Expand all")
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_ADD,
                                InputEvent.CTRL_DOWN_MASK |
                                        InputEvent.SHIFT_DOWN_MASK
                        ))
                        .handle(() -> {
                            for (RenderedLabel key : KEYS)
                                key.setDetailsVisitable(true);
                            MAIN_FRAME.validate();
                            MAIN_FRAME.repaint();
                        })
                );
                view.add(new JMenuItem("Collapse all")
                        .accelerator(KeyStroke.getKeyStroke(
                                KeyEvent.VK_SUBTRACT,
                                InputEvent.CTRL_DOWN_MASK |
                                        InputEvent.SHIFT_DOWN_MASK
                        ))
                        .handle(() -> {
                            for (RenderedLabel key : KEYS)
                                key.setDetailsVisitable(false);
                            MAIN_FRAME.validate();
                            MAIN_FRAME.repaint();
                        })
                );

                menuBar.add(view);
            }
            {
                JMenu help = new JMenu("Help");

                help.add(new JMenuItem("Help")
                        .handle(() -> {
                            BottomMsgUpdater.nextDisplayMsg = "No help found.";
                        })
                );
                help.add(new JMenuItem("About")
                        .handle(MainDisplay::showAboutDialog)
                );

                menuBar.add(help);
            }


            MAIN_FRAME.setJMenuBar(menuBar);
        }
    }

    public static void displayKey(IAuthorizationKey key) {
        if (key == null) return;

        KeyDetailsSwitcher keyDetailsSwitcher = new KeyDetailsSwitcher();

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[fill][grow][button]", ""));
        panel.setBorder(new TitledBorder(String.valueOf(key.getKeyName())));
        RenderedLabel rl = new RenderedLabel("");
        keyDetailsSwitcher.declaredLabel = rl;

        panel.add(rl.codeLabel = new JLabel(key.getService().getName()), "cell 0 0");
        panel.add(rl.codePlaceholder = new JLabel(""), "cell 1 0");
        panel.add(rl, "cell 2 0, al right");
        rl.setText(key.getService().getName());
        rl.declaredPanel = panel;
        rl.declaredKey = key;
        panel.add(rl.counter, "cell 0 1 3 1");
        // rl.counter.setIndeterminate(true);

        MAIN_CONTENT.add(panel, "wrap, growx");
        KEYS.add(rl);
        rl.codeLabel.addMouseListener(COPY_ON_CLICK_LISTENER);
        panel.addMouseListener(keyDetailsSwitcher);
        for (Component subC : panel.getComponents()) {
            subC.addMouseListener(keyDetailsSwitcher);
        }

        JPanel details = new JPanel();
        details.setLayout(new MigLayout());
        rl.details = details;

        {
            Map<String, String> detailsInfo = key.getDetailsInfo();
            if (detailsInfo == null || detailsInfo.isEmpty()) {
                details.add(new JLabel("No details"));
            } else {
                int counter = 0;
                for (Map.Entry<String, String> entry : detailsInfo.entrySet()) {
                    putDetails(details, entry.getKey(), entry.getValue(), counter);
                    counter++;
                }
                try {
                    String uri = key.serializeToUri();
                    if (uri != null) {
                        putDetails(details, "uri", uri, counter);
                        details.add(new JLabel(new ImageIcon(
                                ZXing.renderQRCode(uri, 200, 200)
                        )), "cell 1 " + (counter + 1));
                    }
                } catch (UnsupportedOperationException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        for (Component subC : details.getComponents()) {
            subC.addMouseListener(keyDetailsSwitcher);
        }
    }

    private static void putDetails(Container container, String key, String value, int counter) {
        container.add(new JLabel(key), "cell 0 " + counter);
        JLabel label = new JLabel(value);
        container.add(label, "cell 1 " + counter);
        label.addMouseListener(COPY_ON_CLICK_LISTENER);
    }

    private static void startScheduler() {
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            {
                String display = BottomMsgUpdater.nextDisplayMsg;
                if (display != null) {
                    BottomMsgUpdater.nextDisplayMsg = null;
                    BOTTOM_MSG_BAR.setText(display);
                    BottomMsgUpdater.invalidateTime = now + TimeUnit.SECONDS.toMillis(5);
                } else {
                    long invalidateTime = BottomMsgUpdater.invalidateTime;
                    if (invalidateTime != 0 && now > invalidateTime) {
                        BottomMsgUpdater.invalidateTime = 0;
                        BOTTOM_MSG_BAR.setText(BottomMsgUpdater.BOTTOM_MSG);
                    }
                }
            }
            for (RenderedLabel key : KEYS) {
                if (now > key.nextTime) {
                    String n = key.declaredKey.calcValidKey();
                    key.startTime = now;
                    key.nextTime = now + TimeUnit.SECONDS.toMillis(10);
                    if (n == null) {
                        key.codeLabel.setText("<Calc valid key failed>");
                    } else {
                        key.nextTime = Math.max(key.nextTime, key.declaredKey.keyNextInvalidatedTime());
                        key.codeLabel.setText(n);
                    }
                } else {
                    key.counter.setValue(
                            ((key.nextTime - now) * 1000 / (key.nextTime - key.startTime))
                    );
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    public static void copyText(String text) {
        if (text == null) return;
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        systemClipboard.setContents(new StringSelection(text), null);
        BottomMsgUpdater.nextDisplayMsg = "Copied " + text;
    }

    public static void loadFromURI(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null) {
                throw new IllegalArgumentException("Missing protocol(" + value + ")");
            }
            System.out.println("Parsing from uri " + uri);
            IAuthorizationService.URIDeserializeService service = AUTH_MANAGER.getUriDeserializeService(uri.getScheme());
            if (service == null) {
                throw new UnsupportedOperationException("No service can load " + value);
            }
            IAuthorizationKey key = service.deserialize(uri);
            KeyStorage.saveKey(key);
            displayKey(key);
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

    public static void systemShutdown() {
        MAIN_FRAME.dispose();
        SCHEDULED_EXECUTOR_SERVICE.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        Platform.startup(() -> {
        });
        //System.setProperty("org.lwjgl.util.Debug", "true");
        //System.setProperty("org.lwjgl.util.DebugLoader", "true");
        initialize();
        MAIN_FRAME.setSize(500, 500);
        MAIN_FRAME.setLocationRelativeTo(null);
        MAIN_FRAME.setVisible(true);
        startScheduler();

        KeyStorage.reloadKeys();
    }

    private static void showAboutDialog() {
        DialogFrame dialogFrame = new DialogFrame(MAIN_FRAME);

        dialogFrame.setLayout(new MigLayout("insets dialog, nogrid"));

        dialogFrame.add(new HttpLinkLabel("https://github.com/KasukuSakura/Authorization", "Kasuku Sakura Authenticator"), "wrap");

        dialogFrame.add(new JSeparator(), "wrap");

        dialogFrame.add(new JLabel("Version: " + VerInfo.version), "wrap");

        dialogFrame.add(new JSeparator(), "wrap");

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        dialogFrame.add(new JLabel("Runtime: " +
                runtimeMXBean.getSpecName() + " " +
                runtimeMXBean.getSpecVersion()
        ), "wrap");

        dialogFrame.add(new JLabel("VM: " +
                runtimeMXBean.getVmName() + " " +
                runtimeMXBean.getVmVersion() + " by " +
                runtimeMXBean.getVmVendor()
        ), "wrap");

        dialogFrame.add(new JSeparator(), "wrap");

        dialogFrame.add(new JLabel("Storage location:"));
        dialogFrame.add(new HttpLinkLabel(null, "") {
            @Override
            public String getText() {
                return KeyStorage.STORAGE.getPath();
            }

            @Override
            protected void onClicked() {
                KeyStorage.openExplorer();
            }
        });

        dialogFrame.add(new JSeparator(), "wrap");

        dialogFrame.add(new JLabel("Powered by"));
        dialogFrame.add(new HttpLinkLabel(null, "open-source software") {
            @Override
            protected void onClicked() {
                JPanel panel = new JPanel();
                panel.setLayout(new MigLayout("fill"));

                panel.add(new JLabel("Software"));
                panel.add(new JLabel("License"), "wrap");
                panel.add(new JSeparator(), "wrap");

                String[] libraries = {
                        "zxing", "https://github.com/zxing/zxing", "Apache-2.0 License", "https://github.com/zxing/zxing/blob/master/LICENSE",
                        "JavaFX", "https://github.com/openjdk/jfx", null, null,
                        "FlatLaf", "https://github.com/JFormDesigner/FlatLaf/", "Apache-2.0 License", "https://github.com/JFormDesigner/FlatLaf/blob/main/LICENSE",
                        "MigLayout", "https://github.com/mikaelgrev/miglayout", null, null,
                };

                int lined = libraries.length;
                for (int proj = 0; proj < lined; proj += 4) {
                    HttpLinkLabel projName = new HttpLinkLabel(libraries[proj + 1], libraries[proj]);
                    panel.add(projName);

                    if (libraries[proj + 2] != null) {
                        HttpLinkLabel license = new HttpLinkLabel(libraries[proj + 3], libraries[proj + 2]);
                        panel.add(license, "wrap");
                    } else {
                        panel.add(new JLabel("-"), "wrap");
                    }
                }

                JOptionPane.showMessageDialog(this, panel, "3rd software libraries used by Kasuku Kasura Authorization", JOptionPane.PLAIN_MESSAGE);
            }
        }, "wrap");

        dialogFrame.add(new JLabel("Copyright 2021 KasukuSakura"), "wrap");


        dialogFrame.pack();
        dialogFrame.showUp();
    }

    static class JMenuItem extends javax.swing.JMenuItem {
        public JMenuItem() {
        }

        public JMenuItem(Icon icon) {
            super(icon);
        }

        public JMenuItem(String text) {
            super(text);
        }

        public JMenuItem(Action a) {
            super(a);
        }

        public JMenuItem(String text, Icon icon) {
            super(text, icon);
        }

        public JMenuItem(String text, int mnemonic) {
            super(text, mnemonic);
        }

        JMenuItem accelerator(KeyStroke value) {
            setAccelerator(value);
            return this;
        }

        JMenuItem handleAsync(Runnable task) {
            addActionListener(e -> SCHEDULED_EXECUTOR_SERVICE.execute(() -> {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }));
            return this;
        }

        JMenuItem handle(Runnable task) {
            addActionListener(e -> task.run());
            return this;
        }

        JMenuItem handleIn(Consumer<Runnable> executor, Runnable task) {
            addActionListener(e -> executor.accept(task));
            return this;
        }
    }

}
