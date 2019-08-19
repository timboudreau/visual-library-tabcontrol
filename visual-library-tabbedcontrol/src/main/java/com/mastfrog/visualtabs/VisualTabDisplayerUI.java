/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.mastfrog.visualtabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.AWTEventListenerProxy;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ComponentUI;
import org.netbeans.swing.tabcontrol.DefaultTabDataModel;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDisplayer;
import static org.netbeans.swing.tabcontrol.TabDisplayer.EDITOR_TAB_DISPLAYER_UI_CLASS_ID;
import org.netbeans.swing.tabcontrol.TabDisplayerUI;
import org.netbeans.swing.tabcontrol.TabbedContainer;
import org.netbeans.swing.tabcontrol.event.TabActionEvent;

/**
 *
 * @author Tim Boudreau
 */
public class VisualTabDisplayerUI extends TabDisplayerUI {

    private TabScene scene;
    private TabsAppearance appearance;

    public VisualTabDisplayerUI(TabDisplayer displayer) {
        super(displayer);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scene.getView().getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(0, 0);
    }

    public static ComponentUI createUI(JComponent comp) {
        return new VisualTabDisplayerUI((TabDisplayer) comp);
    }

    protected TabsAppearance createAppearance() {
        return new TabsAppearance().disableInternalDragDropSupport();
    }

    private TabsAppearance appearance() {
        if (appearance == null) {
            appearance = createAppearance();
        }
        return appearance;
    }

    void ensureSomethingVisible() {
        scene.ensureSomethingVisible();
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.setLayout(null);
        c.removeAll();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        TabDisplayer disp = (TabDisplayer) c;
        scene = new TabScene(appearance(), disp.getModel(), disp.getSelectionModel());
//        scene.setZoomFactor(2);

        disp.setLayout(new BorderLayout());
        disp.add(scene.createView(), BorderLayout.CENTER);
        disp.setBorder(BorderFactory.createEmptyBorder());
        disp.setBackground(Color.BLUE);
        scene.sync();
//        MouseListenerRemover mlr = new MouseListenerRemover();
//        mlr.attach(disp);
    }

    class MouseListenerRemover extends ComponentAdapter implements HierarchyListener, HierarchyBoundsListener {

        void attach(TabDisplayer disp) {
            disp.addComponentListener(this);
            disp.addHierarchyListener(this);
            disp.addHierarchyBoundsListener(this);
        }

        void detach(TabDisplayer disp) {
            disp.removeComponentListener(this);
            disp.removeHierarchyListener(this);
            disp.removeHierarchyBoundsListener(this);
        }

        boolean removeIt(EventObject obj) {
            if (!(obj.getSource() instanceof TabDisplayer)) {
                return false;
            }
            TabDisplayer disp = (TabDisplayer) obj.getSource();
            MouseListener target = null;
            for (MouseListener l : disp.getMouseListeners()) {
                if ("ForwardingMouseListener".equals(l.getClass().getSimpleName())) {
                    target = l;
                    break;
                }
            }
            if (target != null) {
                System.out.println("REMOVED " + target);
                disp.removeMouseListener(target);
                disp.putClientProperty("forwardingMouseListener", target);
                detach(disp);
                System.out.println("SUCCESS FOR " + obj.getClass().getName());
                return true;
            }
            return false;
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            removeIt(e);
        }

        @Override
        public void ancestorMoved(HierarchyEvent e) {
            removeIt(e);
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
            removeIt(e);
        }

        @Override
        public void componentShown(ComponentEvent e) {
            removeIt(e);
        }
    }

    static void dumpMouseListeners(JComponent comp) {
        TabDisplayer td = comp instanceof TabDisplayer ? (TabDisplayer) comp : null;
        if (td == null) {
            td = (TabDisplayer) SwingUtilities.getAncestorOfClass(TabDisplayer.class, comp);
        }
        if (td != null) {
            System.out.println("MOUSE LISTENERS:");
            for (MouseListener ml : td.getMouseListeners()) {
                System.out.println("  -" + ml);
            }
            System.out.println("MOUSE MOTION LISTENERS: ");
            for (MouseMotionListener mml : td.getMouseMotionListeners()) {
                System.out.println("  -" + mml);
            }
            TabbedContainer tc = (TabbedContainer) SwingUtilities.getAncestorOfClass(TabbedContainer.class, td);
            if (tc != null) {
                System.out.println("TC MOUSE LISTENERS:");
                for (MouseListener ml : tc.getMouseListeners()) {
                    System.out.println("  -" + ml);
                }
                System.out.println("TC MOUSE MOTION LISTENERS: ");
                for (MouseMotionListener mml : tc.getMouseMotionListeners()) {
                    System.out.println("  -" + mml);
                }
            }
            System.out.println("AWT EVENT LISTENERS: ");
            for (AWTEventListener l : Toolkit.getDefaultToolkit().getAWTEventListeners()) {
                if (l instanceof AWTEventListenerProxy) {
                    AWTEventListenerProxy px = (AWTEventListenerProxy) l;
                    l = px.getListener();
                }
                System.out.println("  -" + l);
            }
        }
    }

    @Override
    public void autoscroll(Point location) {
        // do nothing
//        System.out.println("AUTOSCROLL " + location);
    }

    @Override
    public void postTabAction(TabActionEvent e) {
        boolean consumed = shouldPerformAction(e);
    }

    @Override
    public void makeTabVisible(int index) {
        scene.ensureTabVisible(index);
    }

    @Override
    public Image createImageOfTab(int index) {
        return scene.tabImage(index);
    }

    @Override
    public Polygon getExactTabIndication(int index) {
        return rectToPoly(getTabRect(index, null));
    }

    @Override
    public Polygon getInsertTabIndication(int index) {
        Rectangle r = getTabRect(index, null);
        r.x -= r.width / 2;
        return rectToPoly(r);
    }

    private static Polygon rectToPoly(Rectangle r) {
        return new Polygon(new int[]{
            r.x,
            r.x,
            r.x + r.width,
            r.x + r.width,
            r.x
        }, new int[]{
            r.y + r.height,
            r.y,
            r.y,
            r.y + r.height,
            r.y + r.height}, 5);
    }

    @Override
    protected void setAttentionHighlight(int tab, boolean highlight) {
        scene.setAttentionHighlight(tab, highlight);
    }

    @Override
    public int tabForCoordinate(Point p) {
        return scene.tabForCoordinate(p);
    }

    @Override
    public Rectangle getTabRect(int index, Rectangle destination) {
        return scene.getTabRect(index, destination);
    }

    @Override
    public int dropIndexOfPoint(Point p) {
        return scene.dropIndexOfPoint(p);
    }

    @Override
    protected SingleSelectionModel createSelectionModel() {
        return new DefaultSingleSelectionModel();
    }

    @Override
    public void registerShortcuts(JComponent comp) {
    }

    @Override
    public void unregisterShortcuts(JComponent comp) {
    }

    @Override
    protected void requestAttention(int tab) {
        setAttentionHighlight(tab, true);
    }

    @Override
    protected void cancelRequestAttention(int tab) {
        setAttentionHighlight(tab, false);
    }

    @Override
    protected Font getTxtFont() {
        return appearance.tabFont();
    }

    private static JLabel lbl(String s) {
        JLabel result = new JLabel(s);
        result.setOpaque(true);
        result.setBackground(Color.GRAY);
        return result;
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException {

//        System.setProperty("nb.forceui", "Aqua");
//        org.netbeans.swing.plaf.Startup.run(com.bulenkov.darcula.DarculaLaf.class, 18, null);
//        org.netbeans.swing.plaf.Startup.run(com.sun.java.swing.plaf.gtk.GTKLookAndFeel.class, 18, null);
//        org.netbeans.swing.plaf.Startup.run(javax.swing.plaf.nimbus.NimbusLookAndFeel.class, 21, null);
//        org.netbeans.swing.plaf.Startup.run(com.sun.java.swing.plaf.motif.MotifLookAndFeel.class, 18, null);
//        org.netbeans.swing.plaf.Startup.run(javax.swing.plaf.metal.MetalLookAndFeel.class, 18, null);
        UIManager.put(EDITOR_TAB_DISPLAYER_UI_CLASS_ID, VisualTabDisplayerUI.class.getName());
        EventQueue.invokeLater(() -> {

            DefaultTabDataModel mdl = new DefaultTabDataModel();
            Icon icon = new TabScene.TestIcon();
            mdl.addTab(0, new TabData(lbl("a"), icon, "Hello", "Woo hoo"));
            mdl.addTab(0, new TabData(lbl("b"), icon, "Goodbye", "Wugglegog"));
            mdl.addTab(0, new TabData(lbl("c"), icon, "Snorks", "Snorks"));
            mdl.addTab(1, new TabData(lbl("d"), icon, "Gog.java", "Snorks"));
            mdl.addTab(1, new TabData(lbl("e"), icon, "Hoodlebort.java", "Snorks"));
            mdl.addTab(1, new TabData(lbl("f"), icon, "Squixt.java", "Snorks"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Skoog.java", "Skoog"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Slixt.java", "Slixt"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Wybble.java", "Wybble"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Glonk.java", "Glonk"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Fafs.java", "Fafs"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Slixt.java", "Sqksk"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Wybble.java", "Wybble"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Glaggagoop.java", "Glaggagoop"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Snikters.java", "Snikters"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Gukker.java", "Gukker"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Quixt.java", "Quixt"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Glorg.java", "Glorg"));
            mdl.addTab(1, new TabData(lbl("t"), icon, "Snazzle.java", "Snazzle"));

            TabbedContainer disp = new TabbedContainer(mdl, TabDisplayer.TYPE_EDITOR);

            disp.setActive(true);

            disp.getSelectionModel().setSelectedIndex(0);

            JFrame jf = new JFrame();
            jf.setLayout(new BorderLayout());
            JPanel jp = new JPanel();

            jf.add(disp, BorderLayout.NORTH);
            jf.add(jp, BorderLayout.CENTER);

            JButton b = new JButton("Delete some stuff");
            b.addActionListener(ae -> {
                int total = Math.min(5, mdl.size() / 2);
                int[] tabs = new int[total];
                for (int i = 0; i < total; i++) {
                    tabs[i] = i;
                }
                mdl.removeTabs(tabs);
                TabDisplayer td = findDisplayer(disp);
                VisualTabDisplayerUI ui = (VisualTabDisplayerUI) td.getUI();
                System.out.println("UI " + ui);
                ui.ensureSomethingVisible();
            });
            jp.add(b);
            b = new JButton("Request att");
            b.addActionListener(ae -> {
                disp.requestAttention(0);
            });
            jp.add(b);

            jf.setDefaultCloseOperation(EXIT_ON_CLOSE);

            jf.setBounds(200, 200, 500, 300);
            jf.setVisible(true);
        });
    }

    private static TabDisplayer findDisplayer(Container c) {
        if (c instanceof TabDisplayer) {
            return (TabDisplayer) c;
        }
        for (Component c1 : c.getComponents()) {
            if (c1 instanceof Container) {
                TabDisplayer result = findDisplayer((Container) c1);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
