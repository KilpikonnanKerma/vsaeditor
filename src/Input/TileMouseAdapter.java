package Input;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import Main.*;

public class TileMouseAdapter extends MouseAdapter {
    
        private final int y, x;
        private final LevelPanel levelPanel;

        public TileMouseAdapter(int y, int x, LevelPanel levelPanel) {
            this.levelPanel = levelPanel;
            this.y = y;
            this.x = x;
        }

        @Override
        public void mousePressed(MouseEvent e) {

            LevelEditor editor = levelPanel.editor;
            editor.activeLevelPanel = this.levelPanel;

            if (SwingUtilities.isRightMouseButton(e)) {
              
                String[] eventTypes = {"levelSwitch", "playerFall"};
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
                    editor.events.add(new LevelEditor.EventData(x, y, eventType, eventParam));
                    levelPanel.buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                }
                return;
            }

            if (editor.currentTool == LevelEditor.Tool.CURSOR) {
                editor.showInspector(levelPanel, y, x);
                editor.activeLevelPanel = levelPanel;
                return;
            }

            if (editor.currentTool == LevelEditor.Tool.ENTITY) {
                String[] types = {"Player", "Enemy", "Item", "Custom..."};
                JComboBox<String> typeBox = new JComboBox<>(types);
                JTextField nameField = new JTextField();
                JPanel panel = new JPanel(new java.awt.GridLayout(0, 1));
                panel.add(new JLabel("Entity type:"));
                panel.add(typeBox);
                panel.add(new JLabel("Name:"));
                panel.add(nameField);

                int result = JOptionPane.showConfirmDialog(
                    null, panel, "Add Entity", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
                );
                if (result == JOptionPane.OK_OPTION) {
                    String type = (String) typeBox.getSelectedItem();
                    if ("Custom...".equals(type)) {
                        type = JOptionPane.showInputDialog("Enter custom type:");
                        if (type == null || type.trim().isEmpty()) return;
                    }
                    String name = nameField.getText().trim();
                    LevelPanel.Entity entity = new LevelPanel.Entity(x, y, type);
                    entity.name = name;
                    levelPanel.entities.add(entity);
                    levelPanel.repaint();
                }
                return;
            }

            if (editor.settingSpawn) {
                editor.playerSpawnX = x;
                editor.playerSpawnY = y;
                editor.settingSpawn = false;

                levelPanel.buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
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
            int[][] levelData = levelPanel.levelData;

            int[][] copy = new int[levelData.length][levelData[0].length];
            for (int y = 0; y < levelData.length; y++)
                System.arraycopy(levelData[y], 0, copy[y], 0, levelData[0].length);
            levelPanel.undoStack.push(copy);
        }

        private void paintTile() {

            LevelEditor editor = levelPanel.editor;
            int[][] levelData = levelPanel.levelData;
            JButton[][] buttons = levelPanel.buttons;

            saveUndoData();
            if (editor.currentTool == LevelEditor.Tool.ERASE) {

                boolean eventRemoved = editor.events.removeIf(ev -> ev.x == x && ev.y == y);
                if (eventRemoved) {
                    buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                } else levelData[y][x] = -1;

            } else if (editor.currentTool == LevelEditor.Tool.BUCKET) {
                int target = levelData[y][x];
                if (target != editor.selectedSprite) {
                    floodFill(y, x, target, editor.selectedSprite);
                }
            } else if (editor.currentTool == LevelEditor.Tool.ENEMY) {
                editor.events.add(new LevelEditor.EventData(x, y, "enemy", ""));
                buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else if (editor.currentTool == LevelEditor.Tool.ITEM) {
                editor.events.add(new LevelEditor.EventData(x, y, "item", ""));
                buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
            } else if (editor.currentTool == LevelEditor.Tool.COLLISION) {
                levelPanel.collisionData[y][x] = !levelPanel.collisionData[y][x];
                buttons[y][x].setBorder(levelPanel.collisionData[y][x]
                    ? BorderFactory.createLineBorder(Color.BLACK, 2)
                    : BorderFactory.createLineBorder(Color.GRAY, 1));
                return;
            }else {
                levelData[y][x] = editor.selectedSprite;
            }
            levelPanel.UpdateButtonIcon(y, x);
        }

        private void floodFill(int y, int x, int target, int replacement) {
            int[][] levelData = levelPanel.levelData;

            if (y < 0 || y >= levelData.length || x < 0 || x >= levelData[0].length) return;
            if (levelData[y][x] != target || levelData[y][x] == replacement) return;
            levelData[y][x] = replacement;
            levelPanel.UpdateButtonIcon(y, x);
            floodFill(y + 1, x, target, replacement);
            floodFill(y - 1, x, target, replacement);
            floodFill(y, x + 1, target, replacement);
            floodFill(y, x - 1, target, replacement);
        }
    }