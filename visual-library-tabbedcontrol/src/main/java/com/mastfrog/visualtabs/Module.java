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
import java.beans.PropertyChangeListener;
import javax.swing.UIManager;
import static org.netbeans.swing.tabcontrol.TabDisplayer.EDITOR_TAB_DISPLAYER_UI_CLASS_ID;
import org.openide.modules.ModuleInstall;

/**
 * Initializes the look and feel constants.
 *
 * @author Tim Boudreau
 */
public class Module extends ModuleInstall {

    static final String VALUE = "com.mastfrog.visualtabls.VisualTabDisplayerUI";
    static final PropertyChangeListener PCL = evt -> {
        if (evt != null && EDITOR_TAB_DISPLAYER_UI_CLASS_ID.equals(evt.getPropertyName()))  {
            if (!VALUE.equals(evt.getNewValue())) {
                init();
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
        UIManager.put(EDITOR_TAB_DISPLAYER_UI_CLASS_ID, VALUE);
    }
}
