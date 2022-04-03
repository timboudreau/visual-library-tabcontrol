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

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import static org.netbeans.swing.tabcontrol.TabDisplayer.EDITOR_TAB_DISPLAYER_UI_CLASS_ID;
import org.openide.modules.ModuleInstall;

/**
 * Initializes the look and feel constants.
 *
 * @author Tim Boudreau
 */
public class Module extends ModuleInstall {

    private static final Logger LOG = Logger.getLogger(Module.class.getName());

    static final String VALUE = "com.mastfrog.visualtabs.VisualTabDisplayerUI";

    static final PropertyChangeListener PCL = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt != null && EDITOR_TAB_DISPLAYER_UI_CLASS_ID.equals(evt.getPropertyName())) {
                LOG.log(Level.INFO, "VisualTabs-Renit for prop change {0} -> {1}", new Object[]{evt.getOldValue(), evt.getNewValue()});
                if (!VALUE.equals(evt.getNewValue())) {
                    EventQueue.invokeLater(Module::init);
                }
            } else if (evt != null && "lookAndFeel".equals(evt.getPropertyName())) {
                EventQueue.invokeLater(() -> {
                    PropertyChangeListener[] old = UIManager.getDefaults().getPropertyChangeListeners();
                    if (old == null || old.length == 0 || !Arrays.asList(old).contains(this)) {
                        UIManager.getDefaults().addPropertyChangeListener(this);
                    }
                });
            }
        }
    };

    @Override
    public void restored() {
        super.restored();
        init();
        EventQueue.invokeLater(Module::init);
    }

    static {
        init();
        UIManager.addPropertyChangeListener(PCL);
        UIManager.getDefaults().addPropertyChangeListener(PCL);
    }

    static void init() {
        LOG.log(Level.INFO, "VisualTabs-Init");
        UIManager.put(EDITOR_TAB_DISPLAYER_UI_CLASS_ID, VALUE);
    }
}
