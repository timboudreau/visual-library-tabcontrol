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
package com.mastfrog.colors;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 *
 * @author Tim Boudreau
 */
final class ImageDialog extends JDialog {

    @SuppressWarnings(value = "OverridableMethodCallInConstructor")
    ImageDialog(String msg, BufferedImage expect, BufferedImage got, BufferedImage diff) {
        setLayout(new BorderLayout());
        JPanel beforeAfter = new JPanel(new GridLayout(1, 2));
        beforeAfter.add(new ImageComponent(expect, "Expected", 6));
        beforeAfter.add(new ImageComponent(got, "Got", 6));
        add(beforeAfter, BorderLayout.NORTH);
        add(new ImageComponent(diff, "Difference", 6), BorderLayout.CENTER);
        setTitle(msg);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (onDone != null) {
            onDone.run();
            onDone = null;
        }
    }
    Runnable onDone;

    void display(Runnable onDone) {
        this.onDone = onDone;
        EventQueue.invokeLater(() -> {
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            pack();
            setLocation(new Point(40, 40));
            setVisible(true);
        });
    }
}
