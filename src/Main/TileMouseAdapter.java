package Main;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import Main.LevelEditor.EventData;

public class TileMouseAdapter extends MouseAdapter {
    
        private final int y, x;
        private final LevelEditor editor;

        public TileMouseAdapter(int y, int x, LevelEditor editor) {
            this.editor = editor;
            this.y = y;
            this.x = x;
        }

        @Override
        public void mousePressed(MouseEvent e) {

            if (SwingUtilities.isRightMouseButton(e)) {

                String[] eventTypes = {"levelSwitch"};
                JComboBox<String> typeBox = new JComboBox<>(eventTypes);
                JTextField paramField = new JTextField();

                JPanel panel = new JPanel(new java.awt.GridLayout(0, 1));
                panel.add(new javax.swing.JLabel("Event type:"));
                panel.add(typeBox);
                panel.add(new javax.swing.JLabel("Parameter:"));
                panel.add(paramField);

                int result = JOptionPane.showConfirmDialog(
                    null, panel, "Add Event", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
                );
                if (result == JOptionPane.OK_OPTION) {
                    String eventType = (String) typeBox.getSelectedItem();
                    String eventParam = paramField.getText();
                    editor.events.add(new EventData(x, y, eventType, eventParam));
                    editor.buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                }
                return;
            }

            if (editor.settingSpawn) {
                editor.playerSpawnX = x;
                editor.playerSpawnY = y;
                editor.settingSpawn = false;

                editor.buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
            } else {
                paintTile();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
                paintTile();
            }
        }

        private void saveUndoData() {
            int[][] copy = new int[editor.levelData.length][editor.levelData[0].length];
            for (int y = 0; y < editor.levelData.length; y++)
                System.arraycopy(editor.levelData[y], 0, copy[y], 0, editor.levelData[0].length);
            editor.undoStack.push(copy);
        }

        private void paintTile() {
            saveUndoData();
            if (editor.currentTool == LevelEditor.Tool.ERASE) {
                editor.levelData[y][x] = -1; // or whatever your "empty" index is
            } else if (editor.currentTool == LevelEditor.Tool.BUCKET) {
                int target = editor.levelData[y][x];
                if (target != editor.selectedSprite) {
                    floodFill(y, x, target, editor.selectedSprite);
                }
            } else {
                editor.levelData[y][x] = editor.selectedSprite;
            }
            editor.updateButtonIcon(y, x);
        }

        private void floodFill(int y, int x, int target, int replacement) {
            if (y < 0 || y >= editor.levelData.length || x < 0 || x >= editor.levelData[0].length) return;
            if (editor.levelData[y][x] != target || editor.levelData[y][x] == replacement) return;
            editor.levelData[y][x] = replacement;
            editor.updateButtonIcon(y, x);
            floodFill(y + 1, x, target, replacement);
            floodFill(y - 1, x, target, replacement);
            floodFill(y, x + 1, target, replacement);
            floodFill(y, x - 1, target, replacement);
        }
    }