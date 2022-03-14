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

import java.awt.Component;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SingleSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDataModel;
import org.netbeans.swing.tabcontrol.event.ArrayDiff;
import org.netbeans.swing.tabcontrol.event.ComplexListDataEvent;
import org.netbeans.swing.tabcontrol.event.ComplexListDataListener;
import org.netbeans.swing.tabcontrol.event.VeryComplexListDataEvent;
import org.openide.windows.TopComponent;

/**
 *
 * @author Tim Boudreau
 */
final class TabWidgetMapper implements ComplexListDataListener, Iterable<TabWidget>, ChangeListener {

    private static final Logger LOG = Logger.getLogger(TabWidgetMapper.class.getName());
    private final BiFunction<TabData, Function<TabWidget, TabData>, TabWidget> factory;
    private final TabDataModel model;
    private final AL widgets;
    private final SingleSelectionModel sel;
    private List<TabData> modelSnapshot = new ArrayList<>();
    private final Consumer<TabWidget> remover;
    private final Runnable onChange;

    @SuppressWarnings("LeakingThisInConstructor")
    public TabWidgetMapper(BiFunction<TabData, Function<TabWidget, TabData>, TabWidget> factory,
            TabDataModel model, SingleSelectionModel sel, Consumer<TabWidget> remover, Runnable onChange) {
        this.factory = factory;
        this.model = model;
        this.sel = sel;
        this.remover = remover;
        model.addComplexListDataListener(this);
        widgets = new AL(model.size());
        this.onChange = onChange;
    }

    public Iterator<TabWidget> iterator() {
        return new ArrayList<>(widgets).iterator();
    }

    public int indexOf(Widget widget) {
        return widgets.indexOf(widget);
    }

    public TabWidget get(TabData data) {
        int ix = modelSnapshot.indexOf(data);
        if (ix >= 0 && ix < widgets.size()) {
            return widgets.get(ix);
        }
        return null;
    }

    public TabWidget widget(int index) {
        if (index < widgets.size() && index >= 0) {
            return widgets.get(index);
        }
        return null;
    }

    public int size() {
        return modelSnapshot.size();
    }

    public void forEachWidget(Consumer<? super TabWidget> c) {
        widgets.forEach(c);
    }

    public int widgetCount() {
        return widgets.size();
    }

    public TabData get(TabWidget widget) {
        int ix = widgets.indexOf(widget);
        if (ix < 0 || ix >= model.size()) {
            return new TabData(this, null, "defunct", "defunct");
        }
        return model.getTab(ix);
    }

    public boolean isLast(TabWidget widget) {
        return widgets.size() > 0 && widgets.get(widgets.size() - 1) == widget;
    }

    public boolean isFirst(TabWidget widget) {
        return widgets.size() > 0 && widgets.get(0) == widget;
    }

    public boolean isOnly(TabWidget widget) {
        return widgets.size() == 1 && widgets.get(0) == widget;
    }

    volatile boolean enqueued = false;

    void sync() {
        // We have had some races that result in every tab getting the
        // same name, so try to untangle that and double-plus ensure we
        // only update on the event thread.  This should get us out of the
        // way of flurries of model updates and coalesce those down to one
        // sync pass.
        if (!enqueued) {
            synchronized (this) {
                if (!enqueued) {
                    LOG.log(Level.FINEST, "enqueue sync");
                    enqueued = true;
                    EventQueue.invokeLater(this::_sync);
                }
            }
        }
    }

    void _sync() {
        enqueued = false;
        boolean wasEmpty = modelSnapshot.isEmpty();
        int oldSelection = sel.getSelectedIndex();
        TabWidget oldSelectedWidget = oldSelection >= 0 && oldSelection < widgets.size()
                ? widgets.get(oldSelection) : null;
        TabData oldData = oldSelectedWidget == null ? null : modelSnapshot.get(oldSelection);
        int sz = model.size();
        boolean modelSizeChanged = sz < widgets.size();
        List<TabData> newSnapshot = new ArrayList<>(model.getTabs());
        LOG.log(Level.INFO, "Sync model:tabs {0} size {1} old sel {2}",
                new Object[]{modelSizeChanged ? "size-change" : "no-size-change",
                    sz, oldSelection});

        if (modelSizeChanged) {
            for (int i = sz; i < widgets.size(); i++) {
                remover.accept(widgets.get(i));
            }
            LOG.log(Level.INFO, "remove range {0} - {1}", new Object[]{sz, widgets.size()});
            widgets.removeRange(sz, widgets.size());
        } else if (sz > widgets.size()) {
            modelSizeChanged = true;
            for (int i = widgets.size(); i < sz; i++) {
                TabWidget widge = factory.apply(model.getTab(i), this::get);
                widgets.add(widge);
                List<TabData> oldSnapshot = modelSnapshot;
                // Let the widget sync to new data
                modelSnapshot = newSnapshot;
                widge.sync();
                if (widge.get() == oldData) {
                    widge.setState(widge.getState().deriveSelected(true));
                } else if (wasEmpty && i == oldSelection) {
                    widge.setState(widge.getState().deriveSelected(true));
                }
                modelSnapshot = oldSnapshot;
                syncOne(widgets.size() - 1);
            }
        }
        if (wasEmpty && oldSelection == -1 && !newSnapshot.isEmpty()) {
            sel.setSelectedIndex(0);
            widgets.get(0).setState(widgets.get(0).getState().deriveSelected(true));
        } else if (oldSelectedWidget != null) {
            int newSelIndex = model.indexOf(oldData);
            if (newSelIndex < 0) {
                if (oldSelection >= 0 && oldSelection < model.size()) {
                    newSelIndex = oldSelection;
                } else if (model.size() > 0) {
                    newSelIndex = model.size() - 1;
                }
            }
            LOG.log(Level.INFO, "New selected index {0}", newSelIndex);
            if (newSelIndex >= 0) {
                if (newSelIndex != oldSelection) {
                    sel.setSelectedIndex(newSelIndex);
                    oldSelectedWidget.setState(oldSelectedWidget.getState().deriveSelected(false));
                    TabWidget newSelected = widgets.get(newSelIndex);
                    newSelected.setState(newSelected.getState().deriveSelected(true));
                    TabData td = model.getTab(newSelIndex);
                    if (td != null) {
                        Component c = td.getComponent();
                        if (c != null) {
                            String name = c.getName() == null
                                    ? c.toString() : c.getName();
//                            String name = c instanceof TopComponent
//                                    ? ((TopComponent) c).getDisplayName()
//                                    : c.getName() == null
//                                    ? c.toString() : c.getName();
                            LOG.log(Level.INFO, "Send focus to {0}", name);
                            c.requestFocusInWindow();
                        }
                    }
                } else {
                    if (newSelIndex < model.size()) {
                        TabData data = newSnapshot.get(newSelIndex);
                        if (data != oldData) {
                            sel.clearSelection();
                            sel.setSelectedIndex(newSelIndex);
                        }
                    }
                }
            } else {
                LOG.warning("No available tab to switch to.");
            }
        }

        modelSnapshot = newSnapshot;
        syncRange(0, widgets.size());
//        for (TabWidget w : this) {
//            w.sync();
//        }
        if (oldSelectedWidget == null && newSnapshot.size() > 0) {
            int last = model.size() - 1;
            LOG.log(Level.INFO, "  move selection to {0}", last);
            if (last >= 0) { // this can race
                TabWidget toSelect = widgets.get(last);
                sel.setSelectedIndex(last);
                toSelect.setState(toSelect.getState().deriveSelected(true));
            }
        }
        if (onChange != null) { // Null if called in constructor
            onChange.run();
        }
//        return modelSizeChanged;
    }

    private void syncOne(int index) {
        rangeSync.add(index);
    }

    private void syncRange(int first, int last) {
        rangeSync.add(first, last);
    }

    private final RangeSync rangeSync = new RangeSync();

    private class RangeSync implements Runnable {

        private boolean enqueued;

        private int first = Integer.MAX_VALUE;
        private int last = Integer.MIN_VALUE;

        void enqueue() {
            if (!enqueued) {
                enqueued = true;
                EventQueue.invokeLater(this);
            }
        }

        void reset() {
            enqueued = false;
            first = Integer.MAX_VALUE;
            last = Integer.MIN_VALUE;
        }

        void add(int index) {
            first = Math.min(index, first);
            last = Math.max(index, last);
            enqueue();
        }

        void add(int first, int last) {
            int a = Math.max(0, Math.min(first, last));
            int b = Math.max(0, Math.max(first, last));
            this.first = Math.min(a, first);
            this.last = Math.max(b, first);
            enqueue();
        }

        @Override
        public void run() {
            int from = this.first;
            int thru = this.last;
            reset();
            boolean anyChanged = false;
            for (int i = Math.max(0, from); i < Math.min(widgets.size(), thru + 1); i++) {
                TabWidget widget = widgets.get(i);
                anyChanged |= widget.sync();
            }
            if (anyChanged) {
                onChange.run();
            }
        }
    }

    @Override
    public void indicesAdded(ComplexListDataEvent e) {
        sync();
        int[] affected = e.getIndices();
        int first = widgets.size();
        int last = 0;
        if (affected != null && affected.length > 0) {
            for (int i = 0; i < affected.length; i++) {
                first = Math.min(first, affected[i]);
                last = Math.max(last, affected[i]);
            }
        } else {
            first = e.getIndex0();
            last = e.getIndex1();
        }
        syncRange(first, last);
    }

    @Override
    public void indicesRemoved(ComplexListDataEvent e) {
        sync();
        int[] affected = e.getIndices();
        int first = widgets.size();
        int last = 0;
        if (affected != null && affected.length > 0) {
            for (int i = 0; i < affected.length; i++) {
                first = Math.min(first, affected[i]);
                last = Math.max(last, affected[i]);
            }
        } else {
            first = e.getIndex0();
            last = e.getIndex1();
        }
        syncRange(first, last);
    }

    private void sync(Set<Integer> ints) {
        for (Integer i : ints) {
            syncOne(i);
        }
    }

    @Override
    public void indicesChanged(ComplexListDataEvent e) {
        if (e instanceof VeryComplexListDataEvent) {
            VeryComplexListDataEvent vclde = (VeryComplexListDataEvent) e;
            ArrayDiff diff = vclde.getDiff();
            sync(diff.getAddedIndices());
            sync(diff.getChangedIndices());
            sync(diff.getDeletedIndices());
            sync(diff.getMovedIndices());
            if (diff.getOldData().length != diff.getNewData().length) {
                sync();
            }
        } else {
            sync();
            int[] affected = e.getIndices();
            int first;
            int last;
            if (affected != null && affected.length > 0) {
                for (int i = 0; i < affected.length; i++) {
                    if (affected[i] < widgets.size()) {
                        widgets.get(affected[i]).sync();
                    }
                }
                return;
            } else {
                first = e.getIndex0();
                last = e.getIndex1();
            }
            syncRange(first, last);
        }
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        sync();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        sync();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        sync();
        syncRange(e.getIndex0(), e.getIndex1());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
//        sync();
    }

    static final class AL extends ArrayList<TabWidget> {

        AL(int size) {
            super(size);
        }

        @Override
        public void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }

    }
}
