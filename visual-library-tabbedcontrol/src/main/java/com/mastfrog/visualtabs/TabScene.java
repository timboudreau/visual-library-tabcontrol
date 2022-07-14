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

import com.mastfrog.visualtabs.buttons.ButtonsPanel;
import com.mastfrog.visualtabs.PanTray.DragScrollTimer;
import com.mastfrog.visualtabs.buttons.ButtonAction;
import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.HoverProvider;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.MoveStrategy;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.action.TwoStateHoverProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.animator.AnimatorEvent;
import org.netbeans.api.visual.animator.AnimatorListener;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.swing.tabcontrol.DefaultTabDataModel;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDataModel;
import org.netbeans.swing.tabcontrol.TabDisplayer;
import org.netbeans.swing.tabcontrol.TabDisplayerUI;
import org.netbeans.swing.tabcontrol.TabListPopupAction;
import org.netbeans.swing.tabcontrol.customtabs.Tabbed;
import org.netbeans.swing.tabcontrol.event.TabActionEvent;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tim Boudreau
 */
public class TabScene extends Scene {

    private static final Logger LOG = Logger.getLogger(TabScene.class.getName());
    final TabsAppearance appearance;

    private final TabDataModel model;
    private final SingleSelectionModel selection;

    private final LayerWidget tabsLayer = new LayerWidget(this);
    private final PanTray panTray;
    private final Widget tabs = new Widget(this);
    private final LayerWidget glowLayer = new LayerWidget(this);
    private final GlowWidget glow;
    private final LayerWidget dragLayer = new LayerWidget(this);
    private final Widget tabsContainer = new Widget(this);
    private final Widget raggedEdges = new Widget(this);
    private final Widget raggedEdgesLayer = new LayerWidget(this);
    private final ButtonsPanel buttons;
    private final RequestProcessor.Task task;

    private final TabWidgetMapper mapper;

    TabScene(TabDataModel model, SingleSelectionModel selection) {
        this(new TabsAppearance().forceFontSize(18).setTabsInnerSpacing(5), model, selection);
    }

    TabScene(TabsAppearance appearance, TabDataModel model, SingleSelectionModel selection) {
        this.appearance = appearance;
        this.model = model;
        this.selection = selection;

        panTray = new PanTray(this, appearance.panTrayLeftInset(),
                appearance.panTrayRightInset(),
                this::updateHoverForPan, this::updatePartiallyVisibleTabWidget);
        // Used for computing how far off screen should hide the
        // close button
        panTray.setFont(appearance.tabFont());
        mapper = new TabWidgetMapper(this::createTabWidget, model, selection, tabs::removeChild, this::fullValidate);

        addChild(tabsContainer);

        tabsContainer.addChild(tabsLayer);
        tabsLayer.addChild(panTray);
        int glowWidth = appearance.glowWidth();
        tabsLayer.setPreferredLocation(new Point(0, glowWidth));
        panTray.addChild(tabs);
        task = RequestProcessor.getDefault().create(() -> {
            EventQueue.invokeLater(this::reallyEnsureSomethingVisible);
        });
        tabs.setLayout(new SortedFlowLayout(model, false, LayoutFactory.SerialAlignment.LEFT_TOP, 0, appearance.panTrayLeftInset()));

        tabsContainer.addChild(glowLayer);
        glow = new GlowWidget(this, appearance);
        glowLayer.addChild(glow);
        glowLayer.setLayout(new GlowLayerLayout(glowWidth, this::getSelectedWidget));

        tabsContainer.addChild(dragLayer);
        addChild(raggedEdgesLayer);
        raggedEdgesLayer.addChild(raggedEdges);
        raggedEdges.setBorder(new RaggedBorder(appearance.panTrayLeftInset(), appearance.panTrayRightInset(),
                appearance, this::selectionDirection));

        tabsContainer.addDependency(() -> {
            Rectangle tabsBounds = tabsContainer.getBounds();
            if (tabsBounds != null) {
                tabsBounds.y += appearance.glowWidth();
                tabsBounds.height -= appearance.glowWidth() * 2;
                if (!tabsBounds.equals(raggedEdges.getPreferredBounds())) {
                    raggedEdges.setPreferredBounds(tabsBounds);
                }
            }
        });

        buttons = new ButtonsPanel(this, appearance, this::onButtonAction);
        addChild(buttons);
        setLayout(new TabsAndButtonsLayout());
        tabs.addDependency(buttons::revalidate);

        // We don't do anything with hover here, it just ensures the hover of other
        // things gets cancelled when the mouse leaves the tabs
        buttons.getActions().addAction(new WidgetAction.Adapter() {
            @Override
            public WidgetAction.State mouseEntered(Widget widget, WidgetMouseEvent event) {
                hp.onMouseExited();
                return WidgetAction.State.REJECTED;
            }
        });

        getSceneAnimator().getPreferredBoundsAnimator().addAnimatorListener(al);
        getSceneAnimator().getPreferredLocationAnimator().addAnimatorListener(al);

        panTray.addDependency(() -> {
            EventQueue.invokeLater(() -> {
                buttons.left().setEnabled(panTray.canScrollLeft());
                buttons.right().setEnabled(panTray.canScrollRight());
            });
        });
    }

    void reallyEnsureSomethingVisible() {
        Widget w = getSelectedWidget();
        if (w != null && changesSinceLastEnsure > 2) {
            panTray.ensureChildVisible(w);
        } else {
            panTray.ensureSomethingIsVisible();
        }
    }

    private int changesSinceLastEnsure;

    void ensureSomethingVisible() {
        changesSinceLastEnsure++;
        task.schedule(750);
    }

    void fullValidate() {
        ensureSelectedWidget(getSelectedWidget());
        ensureSomethingVisible();
        validate();
        repaint();
    }

    void hideCloseButton(TabWidget w) {
        w.closeButton.setEnabled(false);
        w.revalidate();
        w.closeButton.repaint();
    }

    void showCloseButton(TabWidget w) {
        w.closeButton.setEnabled(true);
        w.revalidate();
        w.closeButton.repaint();
    }

    void updatePartiallyVisibleTabWidget(Widget prevWidget, Widget widget, boolean veryPartial) {
        // Hides the close button on partially scrolled off to the left widgets, so
        // clicking to select does not inadvertantly hit the close button when there
        // is not much showing to click on (the original tab control does the same)
        if (prevWidget != null && prevWidget != widget) {
            showCloseButton((TabWidget) prevWidget);
        }
        if (widget != null) {
            if (veryPartial) {
                hideCloseButton((TabWidget) widget);
            } else {
                showCloseButton((TabWidget) widget);
            }
        }
    }

    static final class DndHack extends JComponent implements Tabbed.Accessor {

        private JComponent view;

        DndHack(JComponent view) {
            this.view = view;
            add(view);
        }

        @Override
        public Dimension getPreferredSize() {
            return view.getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return view.getMinimumSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return view.getMaximumSize();
        }

        @Override
        public void doLayout() {
            view.setBounds(0, 0, getWidth(), getHeight());
        }

        @Override
        public Tabbed getTabbed() {
            // This allows us to defeat the window system's drag and drop support
            return null;
        }
    }

    AL al = new AL();

    class AL implements AnimatorListener {

        @Override
        public void animatorStarted(AnimatorEvent event) {
        }

        @Override
        public void animatorReset(AnimatorEvent event) {
        }

        @Override
        public void animatorFinished(AnimatorEvent event) {
            glow.revalidate();
            glowLayer.revalidate(false);
        }

        @Override
        public void animatorPreTick(AnimatorEvent event) {
        }

        @Override
        public void animatorPostTick(AnimatorEvent event) {
            if (glow.isVisible() && glowLayer.isVisible()) {
                glow.revalidate();
                glowLayer.revalidate();
            }
        }
    }

    int selectionDirection() {
        TabWidget w = getSelectedWidget();
        if (w == null) {
            return 0;
        }
        return panTray.directionOf(w);
    }

    ButtonAction currentPressed;

    void onButtonAction(ButtonAction type, boolean pressed) {
        if (pressed) {
            currentPressed = type;
        } else {
            currentPressed = null;
        }
        switch (type) {
            case LEFT:
                panTray.scrollLeft();
                if (pressed) {
                    leftRightButtonTimer.start();
                }
                break;
            case RIGHT:
                panTray.scrollRight();
                if (pressed) {
                    leftRightButtonTimer.start();
                }
                break;
            case POPUP:
                if (pressed) {
                    TabDisplayer td = displayer();
                    if (td != null) {
                        TabListPopupAction tlpa = new TabListPopupAction(td);
                        ActionEvent fake = new ActionEvent(getView(), ActionEvent.ACTION_PERFORMED, "pressed");
                        tlpa.actionPerformed(fake);
                    }
                }
                break;
            case MAXIMIZE:
                if (!pressed) {
                    TabDisplayer td = displayer();
                    if (td != null) {
                        TabActionEvent max = new TabActionEvent(getView(), TabDisplayer.COMMAND_MAXIMIZE, selection.getSelectedIndex());
                        td.getUI().postTabAction(max);
                    }
                }
        }
        if (!pressed) {
            leftRightButtonTimer.stop();
        }
    }

    public void onLeftRightButtonTimer(ActionEvent ae) {
        if (currentPressed != null) {
            switch (currentPressed) {
                case LEFT:
                    panTray.scrollLeft();
                    break;
                case RIGHT:
                    panTray.scrollRight();
                    break;
            }
        }
    }

    private Timer leftRightButtonTimer = new Timer(375, this::onLeftRightButtonTimer);

    void updateHoverForPan(long eventTime, Widget target, Point screenPoint) {
        hp.hoverMayBeChanged(eventTime, target, screenPoint);
    }

    class TabsAndButtonsLayout implements Layout {

        @Override
        public void layout(Widget widget) {
            Rectangle dummy = new Rectangle(0, 0, 24, 24);
            panTray.resolveBounds(null, dummy);
            raggedEdges.resolveBounds(null, dummy);
            tabsContainer.resolveBounds(null, dummy);
            buttons.resolveBounds(null, dummy);
//            buttons.getLayout().layout(buttons);
        }

        @Override
        public boolean requiresJustification(Widget widget) {
            return true;
        }

        @Override
        public void justify(Widget widget) {
            if (getView() == null) {
                return;
            }
            Dimension max = getViewSize();
            Rectangle pref = tabsContainer.getBounds();
            if (pref == null || pref.width < 1) {
                return;
            }
            Rectangle r = buttons.getPreferredBounds();
            int h = Math.max(r.height, pref.height) - 1;
            h += appearance.glowWidth();
            r.x = max.width - r.width;
            r.height = h;
            if (r.height < pref.height) {
                r.y += (pref.height - r.height) / 2;
            }
            int gw = appearance.glowWidth();
            int btop = -r.y + (gw / 2);
            Rectangle buttonBounds = new Rectangle(r.x, r.y, r.width,
                    Math.max(r.height, pref.height));

            buttonBounds.height = r.height;

            Point buttonLocation = new Point(0, btop - (gw / 2));
            buttons.resolveBounds(buttonLocation, buttonBounds);

            Point pt = tabsContainer.getPreferredLocation();
            if (pt == null) {
                pt = new Point(0, 0);
            }
            pt.y -= gw / 2;
            tabsContainer.resolveBounds(pt, new Rectangle(0, 0, r.x,
                    h));
            panTray.resolveBounds(null, new Rectangle(0, 0, r.x,
                    h));
            raggedEdges.resolveBounds(new Point(0, gw / 2), new Rectangle(0, 0, r.x,
                    h - gw));
        }
    }

    public Dimension getViewSize() {
        JComponent view = getView();
        if (view == null) {
            return new Dimension(0, 0);
        }
        Dimension result = view.getSize();
        double scale = getZoomFactor();
        if (scale != 1D) {
            result.width *= 1D / scale;
            result.height *= 1D / scale;
        }
        return result;
    }

    private boolean active = true;

    @Override
    protected void paintWidget() {
        TabDisplayer d = displayer();
        if (d != null) {
            active = d.isActive();
            glowLayer.setVisible(active && selection.getSelectedIndex() >= 0);
        }
        super.paintWidget();
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return true;
    }

    public void ensureTabVisible(int index) {
        if (index >= 0 && index < model.size()) {
            TabData dta = model.getTab(index);
            TabWidget widget = widgetFor(dta);
            if (widget != null) {
                panTray.ensureChildVisible(widget);
            }
        }
    }

    public Image tabImage(int index) {
        TabWidget wid = mapper.widget(index);
        Rectangle bds = wid.getBounds();
        if (bds == null) {
            bds = wid.getPreferredBounds();
        }
        if (bds == null) {
            Dimension size = wid.getPreferredSize();
            if (size != null) {
                bds = new Rectangle(new Point(0, 0), size);
            }
        }
        BufferedImage img = new BufferedImage(bds.width, bds.height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = img.createGraphics();
        try {
            wid.withTemporaryGraphics(g, () -> {
                wid.paint();
            });
        } finally {
            g.dispose();
        }
        return img;
    }

    public int tabForCoordinate(Point p) {
        for (TabWidget w : mapper) {
            Point test = w.convertSceneToLocal(p);
            if (w.isHitAt(test)) {
                return mapper.indexOf(w);
            }
        }
        return -1;
    }

    protected void setAttentionHighlight(int tab, boolean highlight) {
        // do nothing for now
    }

    public Rectangle getTabRect(int index, Rectangle destination) {
        if (destination == null) {
            destination = new Rectangle();
        }
        if (index >= 0 && index < model.size()) {
            TabWidget w = mapper.widget(index);
            if (w != null) {
                Rectangle r = w.getBounds();
                if (r != null) {
                    r = w.convertLocalToScene(r);
                    destination.setBounds(r);
                }
            }
        }
        return destination;
    }

    public int dropIndexOfPoint(Point p) {
        Rectangle tmp = new Rectangle();
        for (int i = 0; i < model.size(); i++) {
            getTabRect(i, tmp);
            if (tmp.contains(p)) {
                if (p.x < (tmp.x + tmp.width) / 2) {
                    return i;
                } else {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private TabWidget getSelectedWidget() {
        int sel = selection.getSelectedIndex();
        if (sel < 0) {
            return null;
        }
        if (sel >= model.size()) {
            return null;
        }
        TabWidget result = mapper.widget(sel);
        ensureSelectedWidget(result);
        return result;
    }

    void ensureSelectedWidget(TabWidget w) {
        if (w == null) {
            for (TabWidget tw : mapper) {
                if (tw.getState().isSelected() && tw != w) {
                    tw.removeDependency(glowDependency);
                    tw.setState(tw.getState().deriveSelected(false));
                }
            }
        } else {
            if (!w.getState().isSelected()) {
                w.setState(w.getState().deriveSelected(true));
                w.addDependency(glowDependency);
            }
            for (TabWidget tw : mapper) {
                if (tw != w && tw.getState().isSelected()) {
                    tw.setState(tw.getState().deriveSelected(false));
                    tw.removeDependency(glowDependency);
                }
            }
        }
    }

    void init() {
        selectionListener.init();
        selection.addChangeListener(selectionListener);
        model.addChangeListener(ce -> {
            // Text changes can result in a tab bounds moving while the
            // glow box remains around the tab's previous position
            if (getView() != null && getView().isShowing()) {
                glow.revalidate();
                glowLayer.revalidate();
                glowLayer.getLayout().layout(glowLayer);
                validate();
                repaint();
            }
        });
        sync();
        ensureSelectedWidget(getSelectedWidget());
        validate();
    }

    enum SelectionUpdateResult {
        CHANGED_INDEX,
        INDEX_OK,
        NOT_FOUND,
        NO_PREVIOUS_SELECTION
    }

    private final SelectionChangeListener selectionListener = new SelectionChangeListener();

    private static final long STARTUP = System.currentTimeMillis();

    private static long elapsedSinceStartup() {
        return System.currentTimeMillis() - STARTUP;
    }

    class SelectionChangeListener implements ChangeListener, Runnable {

        private int lastIndex = -1;
        private TabData lastSelectedData;
        private long lastChanged = System.currentTimeMillis();
        private RequestProcessor.Task delayedRefresh = RequestProcessor.getDefault().create(this);
        private int changes;

        void init() {
            lastIndex = selection.getSelectedIndex();
            if (lastIndex > 0 && lastIndex < model.size()) {
                lastSelectedData = model.getTab(lastIndex);
            } else {
                lastSelectedData = null;
            }
        }

        long elapsed() {
            long now = System.currentTimeMillis();
            long lc = lastChanged;
            lastChanged = now;
            return now - lc;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            int change = changes++;
            if (change == 0 & elapsedSinceStartup() < 45000) {
                LOG.log(Level.FINE, "Defer initial tab change 2 seconds");
                task.schedule(2000);
            } else if (elapsed() < 200) {
                LOG.log(Level.FINER, "Many tab changes, reschedule in 500ms");
                task.schedule(500);
            } else {
                LOG.log(Level.FINEST, "Run tab state change immediately");
                run();
            }
        }

        @Override
        public void run() {
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(this);
                return;
            }
            boolean changed = false;
            int index = selection.getSelectedIndex();
            long ela = elapsed();
            LOG.log(Level.FINE, "{0}: SEL CHANGE -> index {1}", new Object[]{ela, index});
            if (index < 0) {
                TabData old = lastSelectedData;
                LOG.log(Level.FINE, "Negative index from {0}", old);
                lastSelectedData = null;
                lastIndex = -1;
                if (old != null) {
                    TabWidget previouslySelected = widgetFor(old);
                    if (previouslySelected != null && previouslySelected.getState().isSelected()) {
                        previouslySelected.setState(previouslySelected.getState().deriveSelected(false));
                        changed = true;
                    }
                }
            } else {
                TabData old = lastSelectedData;
                TabData data = model.getTab(index);

                LOG.log(Level.FINE, "{0}: select from {1} to {2}",
                        new Object[]{ela, old == null ? -1 : model.indexOf(old), index});

                lastSelectedData = data;
                if (old != null) {
                    TabWidget previouslySelected = widgetFor(old);
                    if (previouslySelected != null && previouslySelected.getState().isSelected()) {
                        LOG.log(Level.FINEST, "{0}:  remove dep from prev", ela);
                        previouslySelected.removeDependency(glowDependency);
                        previouslySelected.setState(previouslySelected.getState().deriveSelected(false));
                        previouslySelected.revalidate();
                        changed = true;
                    }
                }
                TabWidget newlySelected = widgetFor(data);
                LOG.log(Level.FINEST, "{0}: new sel {1}", new Object[]{ela, data});
                if (newlySelected != null) {
                    newlySelected.addDependency(glowDependency);
                    newlySelected.setState(newlySelected.getState().deriveSelected(true));
                    newlySelected.bringToFront();
                    newlySelected.revalidate();
                    panTray.ensureChildVisible(newlySelected);
                    ensureSelectedWidget(newlySelected);
                    changed = true;
                    if (data != old) {
                        Component comp = data.getComponent();
                        if (comp != null) {
                            EventQueue.invokeLater(() -> {
                                if (comp.isDisplayable()) {
                                    LOG.log(Level.FINEST, "{0}:  send focus to it", ela);
                                    comp.requestFocusInWindow();
                                } else {
                                    LOG.log(Level.FINEST, ela + ":  not displayable, can't send focus");
                                }
                            });
                        }
                    }
                } else {
                    LOG.log(Level.WARNING, "don''t have a widget for {0} with {1}",
                            new Object[]{data, index});
                }
            }
            lastIndex = index;
            if (changed) {
                glowLayer.revalidate();
                glow.revalidate();
                validate();
            }
        }
    }

    final Dependency glowDependency = new Dependency() {
        @Override
        public void revalidateDependency() {
            glowLayer.revalidate(false);
            glow.revalidate();
        }
    };

    private TabWidget widgetFor(TabData data) {
        return mapper.get(data);
    }

    public TabsAppearance appearance() {
        return appearance;
    }

    private void updatePreferredBounds() {
        JComponent view = getView();
        if (view != null) {
            Rectangle r = view.getVisibleRect();
            r.height = Math.max(47, r.height); // XXX huh?
            double zoom = getZoomFactor();
            if (zoom != 1D) {
                r.x *= zoom;
                r.y *= zoom;
                r.width *= zoom;
                r.height *= zoom;
            }

            setPreferredBounds(r);
            setPreferredSize(r.getSize());
            setMaximumSize(r.getSize());
            setMaximumBounds(r);
        }
    }

    class ViewListener extends MouseAdapter implements HierarchyBoundsListener, HierarchyListener, ComponentListener {

        @Override
        public void mouseExited(MouseEvent e) {
            JComponent v = (JComponent) e.getSource();
            Widget w = hp.onMouseExited();
            if (w != null) {
                Rectangle bds = w.convertLocalToScene(w.getBounds());
                v.repaint(bds.x, bds.y, bds.width, bds.height);
            }
            if (w != null) {
                Rectangle bds = w.convertLocalToScene(w.getBounds());
                v.repaint(bds.x, bds.y, bds.width, bds.height);
            }
        }

        @Override
        public void ancestorMoved(HierarchyEvent e) {
            // do nothing
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
            updatePreferredBounds();
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            TabDisplayer disp = displayer();
            setTabFeatures(TabFeatures.fromTabDisplayer(disp));
        }

        @Override
        public void componentResized(ComponentEvent e) {
            updatePreferredBounds();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            // do nothing
        }

        @Override
        public void componentShown(ComponentEvent e) {
            TabDisplayer disp = displayer();
            setTabFeatures(TabFeatures.fromTabDisplayer(disp));
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            // do nothing
        }
    }

    @Override
    public JComponent createView() {
        init();
        JComponent v = super.createView();
        ViewListener vl = new ViewListener();
        v.addMouseListener(vl);
        v.addHierarchyBoundsListener(vl);
        v.addHierarchyListener(vl);
        v.addComponentListener(vl);
        JComponent result = v;
        if (appearance.isInternalDragDropSupportEnabled()) {
            result = new DndHack(result);
        }
        return result;
    }

    void setTabFeatures(Set<TabFeatures> features) {
        this.features = features;
        buttons.updateFeatures(features);
        boolean close = features.contains(TabFeatures.CLOSE_BUTTONS);
        for (TabWidget w : mapper) {
            if (!close) {
                w.closeButton.setVisible(false);
            }
        }
    }

    Set<TabFeatures> features = EnumSet.allOf(TabFeatures.class);
    TwoStateHoverProvider cbhp = new TwoStateHoverProvider() {

        Widget lastHovered;

        @Override
        public void unsetHovering(Widget widget) {
            if (widget == null) {
                widget = lastHovered;
            }
            if (widget == null) {
                return;
            }
            widget.setState(widget.getState().deriveWidgetHovered(false));
            widget.repaint();
        }

        @Override
        public void setHovering(Widget widget) {
            lastHovered = widget;
            widget.setState(widget.getState().deriveWidgetHovered(true));
            widget.repaint();
        }
    };

    void sync() {
        mapper.sync();
    }

    private TabDisplayer displayer() {
        JComponent view = getView();
        if (view != null) {
            return (TabDisplayer) SwingUtilities.getAncestorOfClass(TabDisplayer.class, view);
        }
        return null;
    }

    private TabDisplayerUI ui() {
        TabDisplayer disp = displayer();
        return disp == null ? null : disp.getUI();
    }

    private WidgetAction.State showPopupForTab(Widget widget, WidgetAction.WidgetMouseEvent event) {
        TabDisplayerUI ui = ui();
        if (ui != null) {
            AWTEvent eo = EventQueue.getCurrentEvent();
            TabActionEvent ae = new TabActionEvent(getView(), TabDisplayer.COMMAND_POPUP_REQUEST,
                    mapper.indexOf(widget), eo instanceof MouseEvent ? (MouseEvent) eo : null);
            ui.postTabAction(ae);
            return ae.isConsumed() ? WidgetAction.State.CONSUMED : WidgetAction.State.REJECTED;
        } else {
            showDefaultPopup(widget, event);
            return WidgetAction.State.CHAIN_ONLY;
        }
    }

    @Messages({
        "# {0} - tab text",
        "closeTab=Close {0}",
        "# {0} - tab text",
        "closeAllBut=Close All But {0}",
        "closeRight=Close Tabs to the Right"
    })
    private void showDefaultPopup(Widget widget, WidgetMouseEvent evt) {
        TabWidget tw = (TabWidget) widget;
        JPopupMenu popup = new JPopupMenu();
        StringBuilder sb = new StringBuilder();
        deHtml(tw.get().getText(), sb);
        popup.add(new AbstractAction(Bundle.closeTab(sb)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                TabData dt = tw.get();
                if (dt != null) {
                    int ix = model.indexOf(dt);
                    boolean wasSelected = selection.getSelectedIndex() == ix;
                    if (ix >= 0) {
                        model.removeTab(ix);
                        if (wasSelected) {
                            if (model.size() > 0) {
                                selection.setSelectedIndex(model.size() - 1);
                            } else {
                                selection.clearSelection();
                            }
                        }
                        validate();
                    }
                }
            }
        });
        popup.add(new AbstractAction(Bundle.closeAllBut(sb)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                TabData dt = tw.get();
                model.setTabs(new TabData[]{dt});
                selection.setSelectedIndex(0);
                validate();
            }
        });
        if (!mapper.isLast(tw)) {
            popup.add(new AbstractAction(Bundle.closeRight()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    List<TabData> all = new ArrayList<>(model.getTabs());
                    int selIdx = selection.getSelectedIndex();
                    TabData selected = selIdx >= 0 && selIdx < model.size()
                            ? all.get(selIdx) : null;
                    Rectangle bounds = widget.getBounds();
                    if (bounds == null) {
                        TabScene.this.validate();
                        bounds = widget.getBounds();
                        if (bounds == null) {
                            // ???
                            return;
                        }
                    }
                    bounds = tw.convertLocalToScene(widget.getBounds());
                    boolean selectedWasRemoved = false;
                    for (TabWidget w : mapper) {
                        if (w == tw) {
                            continue;
                        }
                        Rectangle test = w.convertLocalToScene(w.getBounds());
                        if (test.x >= bounds.x + bounds.width) {
                            TabData dt = w.get();
                            selectedWasRemoved |= selected == dt;
                            if (dt != null) {
                                all.remove(dt);
                            }
                        }
                    }
                    if (all.size() < model.size()) {
                        model.setTabs(all.toArray(new TabData[0]));
                        if (selectedWasRemoved) {
                            selection.setSelectedIndex(all.size() - 1);
                        }
                        validate();
                    }
                }
            });
        }
        Point point = widget.convertLocalToScene(evt.getPoint());
        JComponent view = getView();
        if (view != null) { // unlikely but possible
            popup.show(view, point.x, point.y);
        }
    }

    class PopupAction extends WidgetAction.Adapter {

        @Override
        public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
            // XXX, why is isPopupTrigger always false?
            boolean popup = event.isPopupTrigger() || (event.getClickCount() == 1 && event.getButton() == MouseEvent.BUTTON3);
            if (popup) {
                return showPopupForTab(widget, event);
            } else {
                return WidgetAction.State.CHAIN_ONLY;
            }
        }
    }
    PopupAction popupAction = new PopupAction();

    TabWidget createTabWidget(TabData data, Function<TabWidget, TabData> func) {
        TabWidget w = new TabWidget(this, data, appearance, this::kind, this::requestClose, func);
//        w.closeButton.getActions().addAction(ActionFactory.createHoverAction(cbhp));
        w.closeButton.getActions().addAction(createWidgetHoverAction());
        w.getActions().addAction(0, popupAction);
        w.getActions().addAction(ActionFactory.createHoverAction(hp));
        w.getActions().addAction(0, ActionFactory.createSelectAction(sp));
        if (appearance.isInternalDragDropSupportEnabled()) {
            w.getActions().addAction(0, ActionFactory.createMoveAction(dmp, dmp));
        }
        w.getActions().addAction(0, new WidgetAction.Adapter() {
            @Override
            public WidgetAction.State mouseClicked(Widget widget, WidgetMouseEvent event) {
                if (!event.isPopupTrigger() && event.getClickCount() == 2) {
                    TabDisplayerUI ui = ui();
                    if (ui != null) {
                        TabActionEvent evt = new TabActionEvent(getView(), TabDisplayer.COMMAND_MAXIMIZE,
                                mapper.indexOf(widget));
                        ui.postTabAction(evt);
                    }
                    return WidgetAction.State.CONSUMED;
                }
                return WidgetAction.State.REJECTED;
            }

        });
        tabs.addChild(w);
        w.sync();
        validate();
        return w;
    }

    final HP hp = new HP();

    class HP implements HoverProvider, HoverNotifier {

        long lastHover;
        Widget lastHovered;

        Widget onMouseExited() {
            Widget result = lastHovered;
            widgetHovered(null);
            return result;
        }

        public void widgetHovered(Widget widget) {
            lastHover = System.currentTimeMillis();
            if (widget == null) {
                if (lastHovered != null) {
                    lastHovered.setState(lastHovered.getState().deriveWidgetHovered(false));
                    final Widget lh = lastHovered;
                    lastHovered = null;
                    lh.repaint();
                }
                return;
            }
            if (lastHovered != null && lastHovered != widget) {
                lastHovered.setState(lastHovered.getState().deriveWidgetHovered(false));
                lastHovered.repaint();
            }
            if (widget != lastHovered) {
                widget.setState(widget.getState().deriveWidgetHovered(true));
                widget.repaint();
            }
            lastHovered = widget;
        }

        @Override
        public void hoverMayBeChanged(long eventTime, Widget targetWidget, Point scenePoint) {
            if (eventTime > lastHover) {
                widgetHovered(targetWidget);
            }
        }

//        @Override
        public void unsetHovering(Widget widget) {
            widgetHovered(null);
        }

//        @Override
        public void setHovering(Widget widget) {
            widgetHovered(widget);
        }
    }

    TabKind kind(TabWidget data) {
        if (data == mp.moving) {
            return TabKind.DRAG_PROXY;
        }
        int size = model.size();
        int index = model.indexOf(data.get());
        if (index < 0) {
            return TabKind.DEFUNCT;
        }
        if (size == 1) {
            return TabKind.SINGLE;
        }
        if (index == 0) {
            return TabKind.LEFT_EDGE;
        }
        if (index == size - 1) {
            return TabKind.RIGHT_EDGE;
        }
        return TabKind.BETWEEN;
    }

    void requestClose(TabData data, WidgetMouseEvent wme) {
        int ix = model.indexOf(data);
        String command = wme.isShiftDown()
                ? TabDisplayer.COMMAND_CLOSE_ALL_BUT_THIS : TabDisplayer.COMMAND_CLOSE;
        if (ix >= 0) {
            TabDisplayer disp = displayer();
            if (disp != null) {
                AWTEvent evt = EventQueue.getCurrentEvent();
                TabActionEvent tae;
                if (evt instanceof MouseEvent) {
                    tae = new TabActionEvent(getView(), command, ix, (MouseEvent) evt);
                } else {
                    tae = new TabActionEvent(getView(), command, ix);
                }
                disp.getUI().postTabAction(tae);
            } else {
                switch (command) {
                    case TabDisplayer.COMMAND_CLOSE:
                        model.removeTab(ix);
                        break;
                    case TabDisplayer.COMMAND_CLOSE_ALL_BUT_THIS:
                        int[] indices = new int[model.size() - 1];
                        int curr = 0;
                        for (int i = 0; i < model.size(); i++) {
                            if (i == ix) {
                                continue;
                            }
                            indices[curr++] = i;
                        }
                        model.removeTabs(indices);
                        break;
                }
            }
            mapper.sync();
        }
    }

    private final SP sp = new SP();

    class SP implements SelectProvider {

        @Override
        public boolean isAimingAllowed(Widget widget, Point localLocation, boolean invertSelection) {
            return true;
        }

        @Override
        public boolean isSelectionAllowed(Widget widget, Point localLocation, boolean invertSelection) {
            TabWidget tab = (TabWidget) widget;
            boolean isCloseClick = tab.isInCloseButtonBounds(localLocation);
            if (isCloseClick) {
                return false;
            }
            return true;
        }

        @Override
        public void select(Widget widget, Point localLocation, boolean invertSelection) {
            if (widget == null) {
                selection.clearSelection();
                return;
            }
            TabWidget tab = (TabWidget) widget;

            int oldIndex = selection.getSelectedIndex();
            int newIndex = model.indexOf(tab.get());

            if (newIndex != oldIndex) {
                selection.setSelectedIndex(newIndex);
                TabData data = TabScene.this.model.getTab(newIndex);
                if (data.getComponent() != null) {
                    data.getComponent().requestFocus();
                }
            }
        }
    }

    private final MP mp = new MP();
    private final DelayedMoveProvider dmp = new DelayedMoveProvider<>(mp, 200,
            this::isThresholdDistance, this::ensureWidgetSelected);

    private boolean isThresholdDistance(Point a, Point b) {
        int dist = Math.abs(a.x - b.x);
        return dist > appearance.dragThresholdDistance();
    }

    private void ensureWidgetSelected(Widget clicked) {
        TabData data = ((TabWidget) clicked).get();
        int ix = TabScene.this.model.indexOf(data);
        if (ix >= 0) {
            int currSelection = TabScene.this.selection.getSelectedIndex();
            if (currSelection == ix) {
                // ensure the selected state
                clicked.setState(clicked.getState().deriveSelected(true));
            } else {
                TabScene.this.selection.setSelectedIndex(ix);
            }
            panTray.ensureChildVisible(clicked);
        }
    }

    final class MP implements MoveProvider, MoveStrategy, Dependency {

        TabWidget moving;
        Point orig;
        TabWidget origWidget;
        Point origDragPoint;
        Rectangle pb;
        Point lastLoc;
        DragScrollTimer timer;

        Widget dropIndicator;

        void done() {
            if (moving != null) {
                moving.removeDependency(this);
            }
            moving = null;
            orig = null;
            origWidget = null;
            origDragPoint = null;
            pb = null;
            lastLoc = null;
            if (timer != null) {
                timer.stop();
            }
            if (dropIndicator != null) {
                dragLayer.removeChild(dropIndicator);
            }
            timer = null;
        }

        @Override
        public Point locationSuggested(Widget widget, Point originalLocation, Point suggestedLocation) {
            if (origDragPoint == null) {
                return originalLocation;
            }
            Point xlated = new Point(origDragPoint);
            int distance = (originalLocation.x - suggestedLocation.x);
            xlated.x -= distance;
            xlated.y = origDragPoint.y;

            return xlated;
        }

        private void updateLayersForDraggingState() {
            glowLayer.setVisible(false);
            dragLayer.bringToFront();
        }

        private Widget dropIndicator() {
            Widget result = new Widget(TabScene.this);
            result.setBackground(new Color(90, 10, 10, 128));
            result.setOpaque(true);
            dragLayer.addChild(result);
            return result;
        }

        @Override
        public void movementStarted(Widget widget) {
            updateLayersForDraggingState();

            pb = widget.getPreferredBounds();
            TabScene.this.getSceneAnimator().animatePreferredBounds(
                    widget, new Rectangle(pb.getLocation(),
                            new Dimension(0, pb.height)));
            TabWidget tw = (TabWidget) widget;
            origWidget = tw;
            TabData data = tw.get();
            moving = new TabWidget(TabScene.this,
                    data, appearance, TabScene.this::kind, (ignored, ign2) -> {
                    }, ignored -> data);

            moving.addDependency(this);

            dropIndicator = dropIndicator();

            Point pt = widget.getParentWidget().convertLocalToScene(widget.getLocation());

            origDragPoint = dragLayer.convertSceneToLocal(pt);

            orig = widget.getLocation();
            moving.setPreferredLocation(origDragPoint);

            dragLayer.addChild(moving);
            moving.bringToFront();
            moving.getLayout().layout(moving);

            moving.revalidate();
            dragLayer.revalidate();
            // Ensure the text is right and the composite is set
            moving.sync();
            timer = panTray.dragTimer(moving);
            timer.start();
        }

        @Override
        public Point getOriginalLocation(Widget widget) {
            if (moving == widget) {
                return orig;
            }
            return null;
        }

        @Override
        public void setNewLocation(Widget widget, Point location) {
            if (moving == null) {
                return;
            }
            lastLoc = location;
            moving.setPreferredLocation(location);
            updateDropIndicator(location);
            moving.repaint();
            dragLayer.revalidate();
        }

        void updateDropIndicator(Point location) {
            if (dropIndicator != null) {
                int target = findTargetWidget();
                if (target < 0) {
                    return;
                }
                TabWidget targ = mapper.widget(target);
                if (targ == null) {
                    return;
                }
                Rectangle targetBounds = targ.getBounds();
                if (targetBounds == null) {
                    TabScene.this.validate();
                    targetBounds = targ.getBounds();
                    if (targetBounds == null) {
                        return;
                    }
                }
                targetBounds = targ.convertLocalToScene(targetBounds);
                Rectangle dragBounds = dragLayer.convertSceneToLocal(targetBounds);
                dragBounds.width /= 2;
                if (target > 0) {
                    TabWidget prev = mapper.widget(target - 1);
                    Rectangle prevBounds = prev.getBounds();
                    dragBounds.width += prevBounds.width / 2;
                    dragBounds.x -= prevBounds.width / 2;
                }
                dropIndicator.setPreferredBounds(dragBounds);
            }
        }

        @Override
        public void movementFinished(Widget widget) {
            int dist = distance();
            if (moving != null) {
                dragLayer.removeChild(moving);
                dragLayer.revalidate();
            }
            boolean changed = false;
            if (dist > THRESHOLD) {
                int index = findTargetWidget();
                if (index >= 0) {
                    TabData toMove = moving.get();
                    changed = moveTabToNewIndex(toMove, index);
                }
            }
            widget.setPreferredBounds(null);
            if (widget.getParentWidget() != null) {
                Rectangle r = widget.getPreferredBounds();
                r.width = pb.width;
                TabScene.this.getSceneAnimator().animatePreferredBounds(widget, r);
            }
            if (changed) {
                mapper.sync();
            }
            done();
            glowLayer.setVisible(true);
            glowLayer.revalidate();
            glow.revalidate();
            glow.repaint();
        }

        private int findTargetWidget() {
            Rectangle movingBounds = moving.getPreferredBounds();
            if (movingBounds == null || lastLoc == null) {
                return -1;
            }
            movingBounds.setLocation(lastLoc);

            int maxScore = Integer.MIN_VALUE;
            TabWidget target = null;

            boolean after = true;
            boolean end = true;
            boolean begin = true;
            for (TabWidget w : mapper) {
                Rectangle bds = w.getBounds();
                if (bds == null || bds.width == 0) {
                    continue;
                }
                bds = w.convertLocalToScene(bds);
                end &= movingBounds.x >= bds.x + bds.width;
                begin &= movingBounds.x < bds.x;
                if (bds.x <= movingBounds.x && bds.x + bds.width >= movingBounds.x + movingBounds.width) {
                    // Fully contains the target
                    maxScore = movingBounds.width;
                    target = w;
                    after = false;
                    end = false;
                    break;
                } else if (bds.intersects(movingBounds)) {
                    int score = (int) bds.createIntersection(movingBounds).getWidth();
                    if (score > maxScore) {
                        after = movingBounds.x > ((bds.x + bds.width) / 2);
                        maxScore = score;
                        target = w;
                    }
                }
            }
            int targetIndex = -1;
            if (target != null) {
                targetIndex = model.indexOf(target.get());
                if (after) {
                    targetIndex++;
                }
            } else if (begin) {
                targetIndex = 0;
            } else if (end) {
                targetIndex = model.size();
            }
            return targetIndex;
        }

        final int THRESHOLD = 15; // XXX should be based on screen/font size

        int distance() {
            return lastLoc == null ? 0 : Math.abs(origDragPoint.x - lastLoc.x);
        }

        private boolean moveTabToNewIndex(TabData toMove, int index) {
            boolean changed = false;
            int origIndex = model.indexOf(toMove);
            if (origIndex != index) {
                List<TabData> all = new ArrayList<>(model.getTabs());
                all.remove(origIndex);
                if (index > origIndex) {
                    index--;
                }
                if (index < all.size()) {
                    all.add(index, toMove);
                } else {
                    all.add(toMove);
                }
                model.setTabs(all.toArray(new TabData[all.size()]));
                changed = true;
            }
            return changed;
        }

        @Override
        public void revalidateDependency() {
            buttons.revalidate();
            if (dropIndicator != null) {
                dropIndicator.revalidate();
            }
        }
    }

    static String deHtml(String txt) {
        StringBuilder sb = new StringBuilder();
        deHtml(txt, sb);
        return sb.toString();
    }

    static void deHtml(String txt, StringBuilder into) {
        boolean inTag = false;
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            if (!inTag) {
                inTag = c == '<';
                if (inTag) {
                    continue;
                }
            } else if (inTag && c == '>') {
                inTag = false;
                continue;
            } else if (inTag) {
                continue;
            }
            into.append(c);
        }
    }

    final String m2s() {
        StringBuilder sb = new StringBuilder().append(model.size()).append(":");
        for (int i = 0; i < model.size(); i++) {
            deHtml(model.getTab(i).getText(), sb);
            Object uo = model.getTab(i).getUserObject();
            sb.append("=").append(uo == null ? "null" : uo.getClass().getName() + "@" + System.identityHashCode(uo) + ":" + uo);
            if (i != model.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    public boolean isActive() {
        return active;
    }
    
    static class TestIcon implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.orange);
            g.fillRect(x, y, 11, 11);
            g.setColor(Color.black);
            g.drawRect(x, y, 11, 11);
            g.setColor(Color.blue);
            g.fillRoundRect(x + 3, y + 3, 3, 3, 4, 4);
        }

        @Override
        public int getIconWidth() {
            return 11;
        }

        @Override
        public int getIconHeight() {
            return 11;
        }
    }

    public static void main(String[] args) throws Throwable {
        System.setProperty("swing.aatext", "true");
        System.setProperty("hidpi", "true");
        System.setProperty("sun.java2d.dpiaware", "true");
        System.setProperty("awt.useSystemAAFontSettings", "true");
        System.setProperty("jdk.gtk.version", "3");

//        UIManager.setLookAndFeel(new GTKLookAndFeel());
//        UIManager.setLookAndFeel(new DarkNimbusLookAndFeel());
        UIManager.setLookAndFeel(new NimbusLookAndFeel());
//        UIManager.setLookAndFeel(new MetalLookAndFeel());
//        UIManager.setLookAndFeel(new MotifLookAndFeel());

        DefaultTabDataModel mdl = new DefaultTabDataModel();

        SingleSelectionModel ssm = new DefaultSingleSelectionModel();

        TabsAppearance app = new TabsAppearance().forceFontSize(18);

        TabScene scene = new TabScene(app, mdl, ssm);
        Icon icon = new TestIcon();

        mdl.addTab(0, new TabData("a", icon, "Whatever", "Whatevs..."));
        mdl.addTab(1, new TabData("b", icon, "Foo.java", "Heya..."));
        mdl.addTab(2, new TabData("c", icon, "Bar.java", "Hooya..."));
        mdl.addTab(3, new TabData("d", icon, "YaddaYadda.java", "Whaya..."));
        mdl.addTab(3, new TabData("e", icon, "Wumble <i>Squmfurt</i>", "Whaya..."));
        mdl.addTab(3, new TabData("f", icon, "<b>Floog</b>.java", "Whaya..."));
        mdl.addTab(3, new TabData("g", icon, "WumbleFumerSqub", "Whaya..."));
        mdl.addTab(3, new TabData("h", icon, "Gok.java", "Whaya..."));
        mdl.addTab(3, new TabData("i", icon, "Gark.java", "Whaya..."));

        EventQueue.invokeLater(() -> {

            ssm.setSelectedIndex(0);

            JFrame jf = new JFrame();
            jf.setLayout(new BorderLayout());

            JPanel jp = new JPanel();
            JButton jb = new JButton("Add Another");
            jp.add(jb);
            char[] c = new char[]{'j'};
            jb.addActionListener((ae) -> {
                char cc = c[0]++;
                mdl.addTab(0, new TabData(cc + "", icon, "Tab " + cc, "Woo hoo " + cc));
            });

            JButton b2 = new JButton("Change text of 2");
            String base = "Modified-";
            int[] ix = new int[]{0};
            b2.addActionListener(ae -> {
                String newText = base + (++ix[0]) + (ix[0] % 2 == 0 ? "-hey" : "");
                mdl.setText(1, newText);
            });
            jp.add(b2);

            JButton b3 = new JButton("Ensure 2 visible");
            b3.addActionListener(ae -> {
                if (mdl.size() >= 2) {
                    scene.ensureTabVisible(2);
                }
            });
            jp.add(b3);
            JButton b4 = new JButton("Ensure last visible");
            b4.addActionListener(ae -> {
                if (mdl.size() > 0) {
                    scene.ensureTabVisible(mdl.size() - 1);
                }
            });
            jp.add(b4);

            JComponent comp = scene.createView();
            jf.add(comp, BorderLayout.NORTH);
            jf.add(jp, BorderLayout.CENTER);

            jf.setBounds(200, 200, 1100, 500);

            jf.setDefaultCloseOperation(EXIT_ON_CLOSE);

            jf.setVisible(true);

//            Colors.listUIManager();
        });
    }

    static class TestIcon2 implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ((Graphics2D) g).setStroke(new BasicStroke(0.5F));
            g.setColor(Color.orange);
            g.fillRect(x + 1, y, 11, 14);
            g.setColor(Color.black);
            g.drawRect(x + 1, y, 11, 14);
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    public static class DarkNimbusLookAndFeel extends NimbusLookAndFeel {

        @Override
        public String getName() {
            return NbBundle.getMessage(DarkNimbusLookAndFeel.class, "LBL_DARK_NIMBUS");
        }

        @Override
        public UIDefaults getDefaults() {
            UIDefaults res = super.getDefaults();
            res.put("nb.dark.theme", Boolean.TRUE); //NOI18N
            res.put("nb.preferred.color.profile", "Norway Today"); //NOI18N
            return res;
        }

        @Override
        public Color getDerivedColor(String uiDefaultParentName, float hOffset, float sOffset, float bOffset, int aOffset, boolean uiResource) {
            float brightness = bOffset;
            if ((bOffset == -0.34509805f) && "nimbusBlueGrey".equals(uiDefaultParentName)) { //NOI18N
                //Match only for TreeHandle Color in Nimbus, workaround for #231953
                brightness = -bOffset;
            }
            return super.getDerivedColor(uiDefaultParentName, hOffset, sOffset, brightness, aOffset, uiResource);
        }

        @Override
        public void initialize() {
            super.initialize();
            DarkNimbusTheme.install(this);
        }

        public static class DarkNimbusTheme {

            public static void install(LookAndFeel laf) {

                Color caretForeground = new Color(230, 230, 230);
                Color selectionBackground = new Color(104, 93, 156);
                Color selectedText = new Color(255, 255, 255);

                UIManager.put("nb.dark.theme", Boolean.TRUE);
                UIManager.put("control", new Color(128, 128, 128));
                UIManager.put("info", new Color(128, 128, 128));
                UIManager.put("nimbusBase", new Color(18, 30, 49));
                UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
                UIManager.put("nimbusDisabledText", new Color(196, 196, 196));
                UIManager.put("nimbusFocus", new Color(115, 164, 209));
                UIManager.put("nimbusGreen", new Color(176, 179, 50));
                UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
                UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
                UIManager.put("nimbusOrange", new Color(191, 98, 4));
                UIManager.put("nimbusRed", new Color(169, 46, 34));
                UIManager.put("nimbusSelectedText", selectedText);
                UIManager.put("nimbusSelectionBackground", selectionBackground);
                UIManager.put("text", new Color(230, 230, 230));
//        UIManager.put( "nb.imageicon.filter", new DarkIconFilter() );
                UIManager.put("nb.errorForeground", new Color(127, 0, 0)); //NOI18N
                UIManager.put("nb.warningForeground", new Color(255, 216, 0)); //NOI18N

                UIManager.put("nb.heapview.foreground", new Color(230, 230, 230)); //NOI18N
                UIManager.put("nb.heapview.background", new Color(18, 30, 49)); //NOI18N

                UIManager.put("PropSheet.setBackground", new Color(112, 112, 112)); //NOI18N
                UIManager.put("PropSheet.selectedSetBackground", new Color(100, 100, 100)); //NOI18N

                UIManager.put("nb.bugtracking.comment.background", new Color(112, 112, 112)); //NOI18N
                UIManager.put("nb.bugtracking.comment.foreground", new Color(230, 230, 230)); //NOI18N
                UIManager.put("nb.bugtracking.label.highlight", new Color(160, 160, 160)); //NOI18N
                UIManager.put("nb.bugtracking.table.background", new Color(18, 30, 49)); //NOI18N
                UIManager.put("nb.bugtracking.table.background.alternate", new Color(13, 22, 36)); //NOI18N
                UIManager.put("nb.bugtracking.new.color", new Color(0, 224, 0)); //NOI18N
                UIManager.put("nb.bugtracking.modified.color", new Color(81, 182, 255)); //NOI18N
                UIManager.put("nb.bugtracking.obsolete.color", new Color(153, 153, 153)); //NOI18N
                UIManager.put("nb.bugtracking.conflict.color", new Color(255, 51, 51)); //NOI18N

                UIManager.put("nb.html.link.foreground", new Color(164, 164, 255)); //NOI18N
                UIManager.put("nb.html.link.foreground.hover", new Color(255, 216, 0)); //NOI18N
                UIManager.put("nb.html.link.foreground.visited", new Color(0, 200, 0)); //NOI18N
                UIManager.put("nb.html.link.foreground.focus", new Color(255, 216, 0)); //NOI18N

                UIManager.put("nb.startpage.defaultbackground", Boolean.TRUE);
                UIManager.put("nb.startpage.defaultbuttonborder", Boolean.TRUE);
                UIManager.put("nb.startpage.bottombar.background", new Color(64, 64, 64));
                UIManager.put("nb.startpage.topbar.background", new Color(64, 64, 64));
                UIManager.put("nb.startpage.border.color", new Color(18, 30, 49));
                UIManager.put("nb.startpage.tab.border1.color", new Color(64, 64, 64));
                UIManager.put("nb.startpage.tab.border2.color", new Color(64, 64, 64));
                UIManager.put("nb.startpage.rss.details.color", new Color(230, 230, 230));
                UIManager.put("nb.startpage.rss.header.color", new Color(128, 128, 255));
                UIManager.put("nb.startpage.contentheader.color1", new Color(12, 33, 61)); //NOI18N
                UIManager.put("nb.startpage.contentheader.color2", new Color(16, 24, 42)); //NOI18N

                UIManager.put("nb.popupswitcher.background", new Color(18, 30, 49)); //NOI18N

                UIManager.put("TextField.selectionForeground", selectedText); //NOI18N
                UIManager.put("TextField.selectionBackground", selectionBackground); //NOI18N
                UIManager.put("TextField.caretForeground", caretForeground); //NOI18N
                UIManager.put("nb.editor.errorstripe.caret.color", caretForeground); //NOI18N

                UIManager.put("nb.wizard.hideimage", Boolean.TRUE); //NOI18N

                //diff & diff sidebar
                UIManager.put("nb.diff.added.color", new Color(36, 52, 36)); //NOI18N
                UIManager.put("nb.diff.changed.color", new Color(36, 47, 101)); //NOI18N
                UIManager.put("nb.diff.deleted.color", new Color(56, 30, 30)); //NOI18N
                UIManager.put("nb.diff.applied.color", new Color(36, 52, 36)); //NOI18N
                UIManager.put("nb.diff.notapplied.color", new Color(36, 47, 101)); //NOI18N
                UIManager.put("nb.diff.unresolved.color", new Color(56, 30, 30)); //NOI18N

                UIManager.put("nb.diff.sidebar.changed.color", new Color(18, 30, 74)); //NOI18N
                UIManager.put("nb.diff.sidebar.deleted.color", new Color(66, 30, 49)); //NOI18N

                UIManager.put("nb.versioning.tooltip.background.color", new Color(18, 30, 74)); //NOI18N

                //form designer
                UIManager.put("nb.formdesigner.gap.fixed.color", new Color(112, 112, 112)); //NOI18N
                UIManager.put("nb.formdesigner.gap.resizing.color", new Color(116, 116, 116)); //NOI18N
                UIManager.put("nb.formdesigner.gap.min.color", new Color(104, 104, 104)); //NOI18N

                UIManager.put("nbProgressBar.Foreground", new Color(230, 230, 230));
                UIManager.put("nbProgressBar.popupDynaText.foreground", new Color(191, 186, 172));

                // debugger
                UIManager.put("nb.debugger.debugging.currentThread", new Color(30, 80, 28)); //NOI18N
                UIManager.put("nb.debugger.debugging.highlightColor", new Color(40, 60, 38)); //NOI18N
                UIManager.put("nb.debugger.debugging.BPHits", new Color(65, 65, 0)); //NOI18N
                UIManager.put("nb.debugger.debugging.bars.BPHits", new Color(120, 120, 25)); //NOI18N
                UIManager.put("nb.debugger.debugging.bars.currentThread", new Color(40, 100, 35)); //NOI18N

                //versioning
                UIManager.put("nb.versioning.added.color", new Color(0, 224, 0)); //NOI18N
                UIManager.put("nb.versioning.modified.color", new Color(81, 182, 255)); //NOI18N
                UIManager.put("nb.versioning.deleted.color", new Color(153, 153, 153)); //NOI18N
                UIManager.put("nb.versioning.conflicted.color", new Color(255, 51, 51)); //NOI18N
                UIManager.put("nb.versioning.ignored.color", new Color(153, 153, 153)); //NOI18N
                UIManager.put("nb.versioning.remotemodification.color", new Color(230, 230, 230)); //NOI18N

                // autoupdate
                UIManager.put("nb.autoupdate.search.highlight", new Color(255, 75, 0));

                UIManager.put("selection.highlight", new Color(202, 152, 0));
                UIManager.put("textArea.background", new Color(128, 128, 128));

                UIManager.put("nb.close.tab.icon.enabled.name", "org/openide/awt/resources/vista_close_enabled.png");
                UIManager.put("nb.close.tab.icon.pressed.name", "org/openide/awt/resources/vista_close_pressed.png");
                UIManager.put("nb.close.tab.icon.rollover.name", "org/openide/awt/resources/vista_close_rollover.png");
                UIManager.put("nb.bigclose.tab.icon.enabled.name", "org/openide/awt/resources/vista_bigclose_rollover.png");
                UIManager.put("nb.bigclose.tab.icon.pressed.name", "org/openide/awt/resources/vista_bigclose_rollover.png");
                UIManager.put("nb.bigclose.tab.icon.rollover.name", "org/openide/awt/resources/vista_bigclose_rollover.png");

                //browser picker
                UIManager.put("Nb.browser.picker.background.light", new Color(116, 116, 116));
                UIManager.put("Nb.browser.picker.foreground.light", new Color(192, 192, 192));
                //#233622
                UIManager.put("List[Selected].textForeground", UIManager.getColor("nimbusSelectedText"));

                UIManager.put("nb.explorer.noFocusSelectionBackground", UIManager.get("nimbusSelectionBackground"));

                //search in projects
                UIManager.put("nb.search.sandbox.highlight", selectionBackground);
                UIManager.put("nb.search.sandbox.regexp.wrong", new Color(255, 71, 71));
            }
        }
    }
}
