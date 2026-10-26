package tankops.client.state;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import tankops.common.model.common.Direction;

public class  InputHandler {
    private boolean up, down, left, right;
    private boolean isShooting;
    private int mouseX, mouseY;

    public InputHandler(JComponent component) {
        setupKeyBindings(component);
        setupMouseListeners(component);
    }

    private void setupKeyBindings(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        keyBinding(inputMap, actionMap, "W", "move_up", () -> up = true, () -> up = false);
        keyBinding(inputMap, actionMap, "S", "move_down", () -> down = true, () -> down = false);
        keyBinding(inputMap, actionMap, "A", "move_left", () -> left = true, () -> left = false);
        keyBinding(inputMap, actionMap, "D", "move_right", () -> right = true, () -> right = false);

        keyBinding(inputMap, actionMap, "UP", "move_up", () -> up = true, () -> up = false);
        keyBinding(inputMap, actionMap, "DOWN", "move_down", () -> down = true, () -> down = false);
        keyBinding(inputMap, actionMap, "LEFT", "move_left", () -> left = true, () -> left = false);
        keyBinding(inputMap, actionMap, "RIGHT", "move_right", () -> right = true, () -> right = false);

        keyBinding(inputMap, actionMap, "SPACE", "shoot", () -> isShooting = true, () -> isShooting = false);
    }

    private void keyBinding(InputMap im, ActionMap am, String key, String name, Runnable onPress, Runnable onRelease) {
        im.put(KeyStroke.getKeyStroke(key), name + "_press");
        am.put(name + "_press", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {onPress.run();}
        });

        im.put(KeyStroke.getKeyStroke("released " + key), name + "_release");
        am.put(name + "_release", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {onRelease.run();}
        });
    }

    private void setupMouseListeners(JComponent component) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) isShooting = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) isShooting = false;
            }
        };

        component.addMouseListener(mouseAdapter);
        component.addMouseMotionListener(mouseAdapter);
    }

    public Direction getMovementDirection() {
        if (up) return Direction.UP;
        if (down) return Direction.DOWN;
        if (left) return Direction.LEFT;
        if (right) return Direction.RIGHT;
        return null;
    }

    public boolean isUp() {return up;}
    public boolean isDown() {return down;}
    public boolean isLeft() {return left;}
    public boolean isRight() {return right;}
    public boolean isShooting() {return isShooting;}
    public int getMouseX() {return mouseX;}
    public int getMouseY() {return mouseY;}
}
