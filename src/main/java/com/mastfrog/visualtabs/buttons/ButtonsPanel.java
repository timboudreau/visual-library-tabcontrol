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
package com.mastfrog.visualtabs.buttons;

import com.mastfrog.visualtabs.AWTEventTriggers;
import com.mastfrog.visualtabs.TabFeatures;
import com.mastfrog.visualtabs.TabsAppearance;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public final class ButtonsPanel extends Widget {

    private final RightButtonWidget left, right, down, maximize;

    public ButtonsPanel(Scene scene, TabsAppearance appearance, ButtonListener listener) {
        this(scene, appearance.buttonPainter(), appearance, listener);
    }

    public ButtonsPanel(Scene scene, ButtonPainter painter, TabsAppearance appearance, ButtonListener listener) {
        super(scene);
        left = new RightButtonWidget(scene, new ButtonIcon(ButtonAction.LEFT, painter));
        right = new RightButtonWidget(scene, new ButtonIcon(ButtonAction.RIGHT, painter));
        down = new RightButtonWidget(scene, new ButtonIcon(ButtonAction.POPUP, painter));
        maximize = new RightButtonWidget(scene, new ButtonIcon(ButtonAction.MAXIMIZE, painter));
        left.getActions().addAction(scene.createWidgetHoverAction());
        right.getActions().addAction(scene.createWidgetHoverAction());
        down.getActions().addAction(scene.createWidgetHoverAction());
        maximize.getActions().addAction(scene.createWidgetHoverAction());
        
//        setLayout(LayoutFactory.createHorizontalFlowLayout(SerialAlignment.CENTER, 0));
        setLayout(new ZoomFriendlyFlowLayout());
        ActionAndDragReleaseListener l = new ActionAndDragReleaseListener(listener);
        addChild(left);
        addChild(right);
        addChild(down);
        addChild(maximize);
        left.getActions().addAction(l);
        right.getActions().addAction(l);
        down.getActions().addAction(l);
        maximize.getActions().addAction(l);
        setBackground(appearance.getBackground());
        setOpaque(true);
        setFont(appearance.tabFont());
    }

    public Widget left() {
        return left;
    }

    public Widget right() {
        return right;
    }


    public void updateFeatures(Set<TabFeatures> features) {
        if (!TabFeatures.isAvailable(ButtonAction.MAXIMIZE, features)) {
            maximize.setVisible(false);
        }
        if (!TabFeatures.isAvailable(ButtonAction.POPUP, features)) {
            down.setVisible(false);
        }
    }

    public static void main(String[] args) {
        ButtonListener bl = (type, pressed) -> {
            System.out.println(type + " " + (pressed ? "press" : "release"));
        };
        EventQueue.invokeLater(() -> {
            Scene scene = new Scene();
            scene.setZoomFactor(4);
            ButtonsPanel pnl = new ButtonsPanel(scene, new TabsAppearance(), bl);
            scene.addChild(pnl);
            JFrame jf = new JFrame();
            jf.setDefaultCloseOperation(EXIT_ON_CLOSE);
            jf.setContentPane(scene.createView());
            jf.pack();
            jf.setBounds(200, 200, 300, 300);
            jf.setVisible(true);
        });
    }

    /**
     * If you click a button and drag beyond the scene's view component's
     * bounds, you will never get a mouse released event, which will wreak havoc
     * with popups. This class takes care of forwarding clicks to the listener,
     * and ensures that mouse released events do the right thing by using a
     * temporary AWTEventListener to receive them.
     */
    private static class ActionAndDragReleaseListener extends WidgetAction.Adapter {

        private final ButtonListener listener;
        private ButtonAction last;
        private Widget lastWidget;

        public ActionAndDragReleaseListener(ButtonListener listener) {
            this.listener = listener;
        }

        @Override
        public State mouseReleased(Widget widget, WidgetMouseEvent event) {
            if (last == null) {
                return WidgetAction.State.REJECTED;
            }
            listener.onButtonAction(((RightButtonWidget) widget).actionType(), false);
            last = null;
            widget.setState(widget.getState().deriveSelected(false));
            return WidgetAction.State.createLocked(widget, this);
        }

        @Override
        public State mouseDragged(Widget widget, WidgetMouseEvent event) {
            last = ((RightButtonWidget) widget).actionType();
            widget.setState(widget.getState().deriveSelected(false).deriveWidgetHovered(false));
            lastWidget = widget;
            return WidgetAction.State.createLocked(widget, this);
        }

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            ButtonAction action = ((RightButtonWidget) widget).actionType();
            last = action;
            listener.onButtonAction(action, true);
            widget.setState(widget.getState().deriveSelected(true));
            lastWidget = widget;
            AWTEventTriggers.detectMouseReleased(new Releaser(action));
            return WidgetAction.State.createLocked(widget, this);
        }

        class Releaser implements Runnable {

            private final ButtonAction type;

            public Releaser(ButtonAction type) {
                assert type != null;
                this.type = type;
            }

            @Override
            public void run() {
                if (last == type) {
                    listener.onButtonAction(type, false);
                    if (lastWidget != null) {
                        lastWidget.setState(lastWidget.getState().deriveSelected(false));
                    }
                    last = null;
                    lastWidget = null;
                }
            }
        }
    }
}
