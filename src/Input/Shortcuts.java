package Input;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import java.awt.event.*;

import Main.*;

public class Shortcuts {

    public Shortcuts(LevelEditor editor) {
        loadShortcuts(editor);
    }

    private void loadShortcuts(LevelEditor editor) {

        KeyStroke undoKeyStroke = KeyStroke.getKeyStroke("control Z");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(undoKeyStroke, "undo");
        editor.getRootPane().getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.undo();
            }
        });

        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke("control S");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(saveKeyStroke, "saveLevel");
        editor.getRootPane().getActionMap().put("saveLevel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.saveProject();
            }
        });

        KeyStroke newLevelKeyStroke = KeyStroke.getKeyStroke("control N");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(newLevelKeyStroke, "newLevel");
        editor.getRootPane().getActionMap().put("newLevel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.newProject();
            }
        });

        // int zoomStep = 2;
        // panel.addMouseWheelListener(e -> {
        //     if (e.isControlDown()) {
        //         if (e.getWheelRotation() < 0) {
        //             changeZoom(tileSize + zoomStep);
        //         } else if (e.getWheelRotation() > 0) {
        //             changeZoom(Math.max(8, tileSize - zoomStep));
        //         }
        //     }
        // });

        KeyStroke cursorKeyStroke = KeyStroke.getKeyStroke("C");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(cursorKeyStroke, "cursorTool");
        editor.getRootPane().getActionMap().put("cursorTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.currentTool = LevelEditor.Tool.CURSOR;
                editor.settingSpawn = false;
            }
        });

        KeyStroke paintKeyStroke = KeyStroke.getKeyStroke("P");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(paintKeyStroke, "paintTool");
        editor.getRootPane().getActionMap().put("paintTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.currentTool = LevelEditor.Tool.PAINT;
                editor.settingSpawn = false;
            }
        });

        KeyStroke bucketKeyStroke = KeyStroke.getKeyStroke("B");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(bucketKeyStroke, "bucketTool");
        editor.getRootPane().getActionMap().put("bucketTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.currentTool = LevelEditor.Tool.BUCKET;
            }
        });

        KeyStroke eraserKeyStroke = KeyStroke.getKeyStroke("E");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(eraserKeyStroke, "eraseTool");
        editor.getRootPane().getActionMap().put("eraseTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.currentTool = LevelEditor.Tool.ERASE;
            }
        });


        KeyStroke spawnKeyStroke = KeyStroke.getKeyStroke("S");
        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(spawnKeyStroke, "spawnTool");
        editor.getRootPane().getActionMap().put("spawnTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.currentTool = LevelEditor.Tool.PAINT;
                editor.settingSpawn = true;
            }
        });
    }
}
