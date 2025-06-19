package Main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import Input.TileMouseAdapter;

import Utils.*;

public class LevelPanel extends JPanel {
    public final LevelEditor editor;

    public int offsetX, offsetY; // Position on the canvas
    public int[][] levelData;
    public JButton[][] buttons;
    public boolean[][] collisionData;

    public java.util.Deque<int[][]> undoStack = new java.util.ArrayDeque<>();

    public int logicalX = 0;
    public int logicalY = 0;

    public String levelName = "Level";
    private JLabel nameLabel;

    public String[][] tileTags;
    public List<String>[][] tileScripts;

    public static class Entity {
        public int x, y;
        public String type;
        public String name = "";
        public Entity(int x, int y, String type) {
            this.x = x; this.y = y; this.type = type;
        }
    }
    public List<Entity> entities = new ArrayList<>();
    
    public LevelPanel(LevelEditor editor, int width, int height, int tileSize) {
        this.editor = editor;
        setLayout(new GridLayout(height, width, 0, 0));

        setLayout(null);

        nameLabel = new JLabel();
        nameLabel.setOpaque(true);
        nameLabel.setBackground(new Color(0,0,0,128));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(getFont().deriveFont(Font.BOLD, 14f));
        nameLabel.setBounds(0, 0, width * tileSize, 24);
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        nameLabel.setVerticalAlignment(SwingConstants.CENTER);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        add(nameLabel);
        UpdateNameLabel();

        setOpaque(true);
        setBackground(Color.GRAY);
        setBounds(offsetX, offsetY, width * tileSize, height * tileSize);

        levelData = new int[height][width];
        buttons = new JButton[height][width];
        collisionData = new boolean[height][width];

        tileTags = new String[height][width];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tileTags[y][x] = "";
        
        tileScripts = new ArrayList[height][width];
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    tileScripts[y][x] = new java.util.ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buttons[y][x] = new IndexedButton(this, y, x);
                Dimension d = new Dimension(tileSize, tileSize);
                buttons[y][x].setPreferredSize(d);
                buttons[y][x].setMinimumSize(d);
                buttons[y][x].setMaximumSize(d);
                buttons[y][x].setBackground(Color.LIGHT_GRAY);
                buttons[y][x].addMouseListener(new TileMouseAdapter(y, x, this));
                buttons[y][x].addMouseMotionListener(new TileMouseAdapter(y, x, this));

                buttons[y][x].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) {
                            LevelPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, LevelPanel.this));
                        }
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) {
                            LevelPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, LevelPanel.this));
                        }
                    }
                });
                buttons[y][x].addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) {
                            LevelPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, LevelPanel.this));
                        }
                    }
                });

                buttons[y][x].setBounds(x * tileSize, y * tileSize + 24, tileSize, tileSize);
                add(buttons[y][x]);
            }
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) {
                    editor.dragOffset = e.getPoint();
                    editor.draggingLevel = LevelPanel.this;
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                editor.dragOffset = null;
                editor.draggingLevel = null;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (editor.dragOffset != null && editor.draggingLevel == LevelPanel.this) {
                    int newX = getX() + e.getX() - editor.dragOffset.x;
                    int newY = getY() + e.getY() - editor.dragOffset.y;
                    setLocation(newX, newY);
                    offsetX = newX;
                    offsetY = newY;
                    // Update logical position
                    logicalX = (int)Math.round(offsetX / (double)editor.tileSize);
                    logicalY = (int)Math.round(offsetY / (double)editor.tileSize);
                    editor.canvasPanel.repaint();
                }
            }
        });

        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    String newName = JOptionPane.showInputDialog(LevelPanel.this, "Enter level name:", levelName);
                    if (newName != null && !newName.trim().isEmpty()) {
                        levelName = newName.trim();
                        UpdateNameLabel();
                    }
                }
            }
        });
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        if (nameLabel != null) {
            nameLabel.setBounds(0, 0, w, 24);
        }
    }

    public void UpdateButtonIcon(int y, int x) {
        if (levelData[y][x] >= 0 && levelData[y][x] < editor.spriteIcons.length) {
            buttons[y][x].setIcon(editor.spriteIcons[levelData[y][x]]);
            buttons[y][x].setBackground(null);
        } else {
            buttons[y][x].setIcon(null);
            buttons[y][x].setBackground(Color.LIGHT_GRAY);
        }
    }

    public void UpdateNameLabel() {
        int idx = editor.levels.indexOf(this);
        nameLabel.setText("[" + idx + "] " + levelName);
    }
}