package Main;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LevelEditor extends JFrame {

    int[][] levelData;
    JButton[][] buttons;
    int selectedSprite = 1;

    int playerSpawnX = 0;
    int playerSpawnY = 0;
    boolean settingSpawn = false;

    private ImageIcon[] spriteIcons;
    private String[] spriteFileNames;

    private JPanel spritePanel;
    private JScrollPane spriteScroll;

    private static final String PREF_SPRITE_FOLDER = "spriteFolder";
    private java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(LevelEditor.class);

    private boolean showTileIndex = false;

    public enum Tool { PAINT, BUCKET, ERASE }
    public Tool currentTool = Tool.PAINT;

    public JPanel gridPanel;
    public int tileSize = 16;

    private JLabel statusBar;

    // Cache scaled icons
    private java.util.Map<Integer, ImageIcon[]> scaledIconCache = new java.util.HashMap<>();

    // UNDOOO
    java.util.Deque<int[][]> undoStack = new java.util.ArrayDeque<>();

    static int lastExtendedState = JFrame.NORMAL;

    public static class EventData {
        public int x, y;
        public String type;
        public String param;
        public EventData(int x, int y, String type, String param) {
            this.x = x; this.y = y; this.type = type; this.param = param;
        }
    }
    public List<EventData> events = new ArrayList<>();

    public LevelEditor(int width, int height) {

        levelData = new int[height][width];
        buttons = new JButton[height][width];

        setTitle("Level Editor");
        setSize(850, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
      
        setLocationRelativeTo(null);

        // setResizable(false);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newFile = new JMenuItem("New level (Ctrl + N)");
        newFile.addActionListener(e -> {
            newLevel();
        });

        JMenuItem loadFile = new JMenuItem("Load level");
        loadFile.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                loadLevel(file.getAbsolutePath());
            }
        });
        fileMenu.add(loadFile);

        JMenuItem saveFile = new JMenuItem("Save level (Ctrl + S)");
        saveFile.addActionListener(e -> {
            saveLevel();
        });

        JMenuItem loadSprites = new JMenuItem("Load sprites");
        loadSprites.addActionListener(e -> loadSprites());
        fileMenu.add(loadSprites);

        JMenuItem resizeLevel = new JMenuItem("Resize Level");
        resizeLevel.addActionListener(e -> {
            String newWidthStr = JOptionPane.showInputDialog(this, "Enter new width:", levelData[0].length);
            String newHeightStr = JOptionPane.showInputDialog(this, "Enter new height:", levelData.length);
            if (newWidthStr == null || newHeightStr == null) return;
            try {
                int newWidth = Integer.parseInt(newWidthStr);
                int newHeight = Integer.parseInt(newHeightStr);
                if (newWidth < 1 || newHeight < 1) throw new NumberFormatException();
                resizeLevel(newWidth, newHeight);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid size.");
            }
        });

        menuBar.add(fileMenu);
        fileMenu.add(newFile);
        fileMenu.add(loadFile);
        fileMenu.add(saveFile);
        fileMenu.addSeparator();
        fileMenu.add(loadSprites);
        fileMenu.add(resizeLevel);

        JMenu editMenu = new JMenu("Edit");
        JCheckBoxMenuItem toggleIndex = new JCheckBoxMenuItem("Show Tile Index", showTileIndex);
        toggleIndex.addActionListener(e -> {
            showTileIndex = toggleIndex.isSelected();

            // Repaint all buttons to update index visibility
            for (int y = 0; y < buttons.length; y++)
                for (int x = 0; x < buttons[y].length; x++)
                    buttons[y][x].repaint();
        });

        JMenuItem undo = new JMenuItem("Undo (Ctrl + Z)");
        undo.addActionListener(e -> undo());
        
        JMenuItem zoomIn = new JMenuItem("Zoom In");
        zoomIn.addActionListener(e -> changeZoom(tileSize + 8));

        JMenuItem zoomOut = new JMenuItem("Zoom Out");
        zoomOut.addActionListener(e -> changeZoom(Math.max(8, tileSize - 8)));


        editMenu.add(undo);
        editMenu.addSeparator();

        editMenu.add(zoomIn);
        editMenu.add(zoomOut);
        editMenu.addSeparator();
        editMenu.add(toggleIndex);
        menuBar.add(editMenu);


        JMenu toolMenu = new JMenu("Tools");
        JMenuItem penTool = new JMenuItem("Pen tool (P)");
        penTool.addActionListener(e -> {
            currentTool = Tool.PAINT;
            settingSpawn = false;
        });

        JMenuItem bucketTool = new JMenuItem("Bucket tool (B)");
        bucketTool.addActionListener(e -> {
            currentTool = Tool.BUCKET;
        });

        JMenuItem eraseTool = new JMenuItem("Eraser tool (E)");
        bucketTool.addActionListener(e -> {
            currentTool = Tool.ERASE;
        });

        JMenuItem spawnTool = new JMenuItem("Set spawn (S)");
        bucketTool.addActionListener(e -> {
            currentTool = Tool.PAINT;
            settingSpawn = true;
        });

        toolMenu.add(penTool);
        toolMenu.add(bucketTool);
        toolMenu.add(eraseTool);
        toolMenu.add(spawnTool);
        menuBar.add(toolMenu);

        setJMenuBar(menuBar);

        gridPanel = new JPanel(new GridLayout(height, width, 0, 0));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int fx = x;
                final int fy = y;

                buttons[y][x] = new IndexedButton(y, x);
                Dimension d = new Dimension(tileSize, tileSize);
                buttons[y][x].setPreferredSize(d);
                buttons[y][x].setMinimumSize(d);
                buttons[y][x].setMaximumSize(d);
                buttons[y][x].setIcon(null);
                buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                buttons[y][x].setMargin(new Insets(0, 0, 0, 0));
                buttons[y][x].setBackground(Color.LIGHT_GRAY);
                
                buttons[y][x].addMouseListener(new TileMouseAdapter(y, x, this));
                buttons[y][x].addMouseMotionListener(new TileMouseAdapter(y, x, this));

                buttons[y][x].addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        statusBar.setText("Tile: " + fx + ", " + fy + "   Zoom: " + tileSize + "px" + "   Tool: " + currentTool);
                    }
                });

                gridPanel.add(buttons[y][x]);
            }
        }

        String lastSpriteFolder = prefs.get(PREF_SPRITE_FOLDER, null);
        boolean loaded = false;
        spritePanel = new JPanel();

        if (lastSpriteFolder != null) {
            File spriteDir = new File(lastSpriteFolder);
            File[] spriteFiles = spriteDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (spriteDir.isDirectory() && spriteFiles != null && spriteFiles.length > 0) {
                loadSpritesFromDir(spriteDir);
                loaded = true;
            }
        }
        if (!loaded) {
            spritePanel.setLayout(new GridBagLayout());
            JButton loadSpritesButton = new JButton("Load sprites");
            loadSpritesButton.addActionListener(e -> loadSprites());
            spritePanel.add(loadSpritesButton, new GridBagConstraints());
        }
        spriteScroll = new JScrollPane(spritePanel);
        spriteScroll.setPreferredSize(new Dimension(600, 100));
        add(spriteScroll, BorderLayout.NORTH);

        JPanel gridWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        gridWrapper.add(gridPanel);
        JScrollPane gridScroll = new JScrollPane(gridWrapper);
        add(gridScroll, BorderLayout.CENTER);

        add(spriteScroll, BorderLayout.NORTH);

        // pack();

        loadShortcuts(gridPanel);

        // status bar stuff
        statusBar = new JLabel("Tile: -, -   Zoom: " + tileSize + "px" + "   Tool: " + currentTool);
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 2));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusBar, BorderLayout.WEST);

        add(statusPanel, BorderLayout.PAGE_END);
    }

    private void loadShortcuts(JPanel panel) {

        KeyStroke undoKeyStroke = KeyStroke.getKeyStroke("control Z");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(undoKeyStroke, "undo");
        getRootPane().getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke("control S");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(saveKeyStroke, "saveLevel");
        getRootPane().getActionMap().put("saveLevel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveLevel();
            }
        });

        KeyStroke newLevelKeyStroke = KeyStroke.getKeyStroke("control N");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(newLevelKeyStroke, "newLevel");
        getRootPane().getActionMap().put("newLevel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newLevel();
            }
        });

        int zoomStep = 2;
        panel.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) {
                    changeZoom(tileSize + zoomStep);
                } else if (e.getWheelRotation() > 0) {
                    changeZoom(Math.max(8, tileSize - zoomStep));
                }
            }
        });

        KeyStroke paintKeyStroke = KeyStroke.getKeyStroke("P");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(paintKeyStroke, "paintTool");
        getRootPane().getActionMap().put("paintTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = Tool.PAINT;
                settingSpawn = false;
            }
        });

        KeyStroke bucketKeyStroke = KeyStroke.getKeyStroke("B");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(bucketKeyStroke, "bucketTool");
        getRootPane().getActionMap().put("bucketTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = Tool.BUCKET;
            }
        });

        KeyStroke eraserKeyStroke = KeyStroke.getKeyStroke("E");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(eraserKeyStroke, "eraseTool");
        getRootPane().getActionMap().put("eraseTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = Tool.ERASE;
            }
        });


        KeyStroke spawnKeyStroke = KeyStroke.getKeyStroke("S");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(spawnKeyStroke, "spawnTool");
        getRootPane().getActionMap().put("spawnTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = Tool.PAINT;
                settingSpawn = true;
            }
        });
    }

    private void newLevel() {
        String widthStr = JOptionPane.showInputDialog(this, "Enter level width:");
        String heightStr = JOptionPane.showInputDialog(this, "Enter level height:");
        int width2 = 30;
        int height2 = 30;
        
        try {
            width2 = Integer.parseInt(widthStr);
            height2 = Integer.parseInt(heightStr);
        } catch (Exception ignored) {}

        lastExtendedState = getExtendedState();
        LevelEditor newEditor = new LevelEditor(width2, height2);
        newEditor.setVisible(true);
        newEditor.setExtendedState(lastExtendedState);

        this.dispose();
    }

    private void loadLevel(String filename) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filename));
            int width = -1, height = -1;
            int spawnX = 0, spawnY = 0;

            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i).trim();
                if (line.startsWith("SPAWN")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        spawnX = Integer.parseInt(parts[1]);
                        spawnY = Integer.parseInt(parts[2]);
                    }
                    lines.remove(i);
                } else if (line.startsWith("SIZE")) {
                    String[] parts = line.split("\\s+");
                    width = Integer.parseInt(parts[1]);
                    height = Integer.parseInt(parts[2]);
                    lines.remove(i);
                }
            }

            for (String line : lines) {
                if (line.startsWith("EVENT")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 5) {
                        int ex = Integer.parseInt(parts[1]);
                        int ey = Integer.parseInt(parts[2]);
                        String type = parts[3];
                        String param = parts[4];
                        events.add(new EventData(ex, ey, type, param));
                        buttons[ey][ex].setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    }
                }
            }

            // If size is different, recreate the editor
            if (width != levelData[0].length || height != levelData.length) {

                lastExtendedState = getExtendedState();
                LevelEditor newEditor = new LevelEditor(width, height);
                newEditor.setExtendedState(lastExtendedState);
                newEditor.levelName = filename;
                newEditor.playerSpawnX = spawnX;
                newEditor.playerSpawnY = spawnY;
                newEditor.setVisible(true);
                this.dispose();
                newEditor.loadLevel(filename); // load into new editor
                return;
            }

            // Otherwise, fill the grid
            java.util.Scanner scanner = new java.util.Scanner(String.join("\n", lines));
            for (int y = 0; y < levelData.length; y++) {
                for (int x = 0; x < levelData[0].length; x++) {
                    if (scanner.hasNextInt()) {
                        levelData[y][x] = scanner.nextInt();
                        updateButtonIcon(y, x);
                    }
                }
            }

            // Make the green marker
            playerSpawnX = spawnX;
            playerSpawnY = spawnY;
            buttons[playerSpawnY][playerSpawnX].setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));

            JOptionPane.showMessageDialog(this, "Level loaded!");

            scanner.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading level: " + e.getMessage());
        }
    }

    private void loadSprites() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File spriteDir = chooser.getSelectedFile();
            prefs.put(PREF_SPRITE_FOLDER, spriteDir.getAbsolutePath());

            updateSpriteJson(spriteDir);
            loadSpritesFromDir(spriteDir);
        }
    }

    private void loadSpritesFromDir(File spriteDir) {
        File jsonFile = new File(spriteDir, "sprites.json");
        if (!jsonFile.exists()) {
            JOptionPane.showMessageDialog(this, "sprites.json not found in this folder!");
            return;
        }

        spriteFileNames = readSpriteJson(jsonFile);
        spriteIcons = new ImageIcon[spriteFileNames.length];

        spritePanel.removeAll();
        spritePanel.setLayout(new GridLayout(0, 20, 2, 2));

        for (int i = 0; i < spriteFileNames.length; i++) {
            File imgFile = new File(spriteDir, spriteFileNames[i]);
            if (!imgFile.exists()) {
                System.err.println("Missing sprite: " + spriteFileNames[i]);
                continue;
            }

            Image img = new ImageIcon(imgFile.getPath()).getImage();
            Image scaled = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            spriteIcons[i] = new ImageIcon(scaled);

            JButton spriteButton = new JButton(spriteIcons[i]);
            final int spriteIndex = i;
            spriteButton.addActionListener(e -> selectedSprite = spriteIndex);
            spritePanel.add(spriteButton);
        }

        spritePanel.revalidate();
        spritePanel.repaint();
    }

    private String[] readSpriteJson(File jsonFile) {
        String[] result = new String[0];

        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line.trim());
            }

            String content = sb.toString();

            int start = content.indexOf("[");
            int end = content.indexOf("]", start);
            if (start != -1 && end != -1 && end > start) {
                String arrayContent = content.substring(start + 1, end).trim();
                if (!arrayContent.isEmpty()) {
                    String[] parts = arrayContent.split(",");
                    result = new String[parts.length];

                    for (int i = 0; i < parts.length; i++) {
                        String name = parts[i].trim().replaceAll("^\"|\"$", ""); // Remove quotes
                        result[i] = name;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void writeSpriteJson(File jsonFile, String[] spriteNames) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile))) {
            writer.write("{\n");
            writer.write("  \"generatedBy\": \"VSA Level Editor\",\n");
            writer.write("  \"sprites\": [\n");
            for (int i = 0; i < spriteNames.length; i++) {
                writer.write("    \"" + spriteNames[i] + "\"");
                if (i < spriteNames.length - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("  ]\n");
            writer.write("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSpriteJson(File spriteDir) {
        File jsonFile = new File(spriteDir, "sprites.json");
        String[] existing = new String[0];
        if (jsonFile.exists()) {
            existing = readSpriteJson(jsonFile);
        }

        File[] pngFiles = spriteDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (pngFiles == null) return;

        // Check for new sprites
        int count = 0;
        for (File f : pngFiles) {
            boolean found = false;
            for (String e : existing) {
                if (e.equals(f.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) count++;
        }

        if (count == 0) return;

        String[] updated = new String[existing.length + count];
        System.arraycopy(existing, 0, updated, 0, existing.length);

        int index = existing.length;
        for (File f : pngFiles) {
            boolean found = false;
            for (String e : existing) {
                if (e.equals(f.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                updated[index++] = f.getName();
            }
        }

        writeSpriteJson(jsonFile, updated);
    }

    ImageIcon[] getScaledIcons(int size) {
        if (scaledIconCache.containsKey(size)) {
            return scaledIconCache.get(size);
        }
        ImageIcon[] scaled = new ImageIcon[spriteIcons.length];
        for (int i = 0; i < spriteIcons.length; i++) {
            Image img = spriteIcons[i].getImage();
            Image scaledImg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            scaled[i] = new ImageIcon(scaledImg);
        }
        scaledIconCache.put(size, scaled);
        return scaled;
    }

    void updateButtonIcon(int y, int x) {
        if (levelData[y][x] >= 0 && levelData[y][x] < spriteIcons.length) {
            buttons[y][x].setIcon(spriteIcons[levelData[y][x]]);
            buttons[y][x].setBackground(null);
        } else {
            buttons[y][x].setIcon(null);
            buttons[y][x].setBackground(Color.LIGHT_GRAY);
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            int[][] prev = undoStack.pop();

            // Don't undo if level sizes don't match
            if (prev.length == levelData.length && prev[0].length == levelData[0].length) {
                for (int y = 0; y < levelData.length; y++)
                    System.arraycopy(prev[y], 0, levelData[y], 0, levelData[0].length);

                // Update buttons
                for (int y = 0; y < buttons.length; y++)
                    for (int x = 0; x < buttons[0].length; x++)
                        updateButtonIcon(y, x);
            } else {
                // Optionally: show a message or ignore if size changed
            }
        }
    }

    private void changeZoom(int newTileSize) {
        tileSize = newTileSize;
        ImageIcon[] scaledIcons = getScaledIcons(tileSize);

        for (int y = 0; y < buttons.length; y++) {
            for (int x = 0; x < buttons[y].length; x++) {
                Dimension d = new Dimension(tileSize, tileSize);
                buttons[y][x].setPreferredSize(d);
                buttons[y][x].setMinimumSize(d);
                buttons[y][x].setMaximumSize(d);
                buttons[y][x].setMargin(new Insets(0, 0, 0, 0));
                buttons[y][x].setContentAreaFilled(false);
                buttons[y][x].setHorizontalAlignment(SwingConstants.CENTER);
                buttons[y][x].setVerticalAlignment(SwingConstants.CENTER);

                // Use cached scaled icon
                if (levelData[y][x] >= 0 && levelData[y][x] < scaledIcons.length) {
                    buttons[y][x].setIcon(scaledIcons[levelData[y][x]]);
                }
            }
        }

        gridPanel.setPreferredSize(new Dimension(
            buttons[0].length * tileSize,
            buttons.length * tileSize
        ));

        gridPanel.revalidate();
        gridPanel.repaint();

        statusBar.setText("Tile: -, -   Zoom: " + tileSize + "px" + "   Tool: " + currentTool);
    }


    private void resizeLevel(int newWidth, int newHeight) {
        int[][] newLevelData = new int[newHeight][newWidth];
        JButton[][] newButtons = new JButton[newHeight][newWidth];

        for (int y = 0; y < newHeight; y++)
            for (int x = 0; x < newWidth; x++)
                newLevelData[y][x] = -1;

        // Copy old data
        for (int y = 0; y < Math.min(levelData.length, newHeight); y++)
            for (int x = 0; x < Math.min(levelData[0].length, newWidth); x++)
                newLevelData[y][x] = levelData[y][x];

        JPanel newGridPanel = new JPanel(new GridLayout(newHeight, newWidth, 0, 0));
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {

                final int fx = x;
                final int fy = y;

                newButtons[y][x] = new IndexedButton(y, x);
                Dimension d = new Dimension(tileSize, tileSize);
                newButtons[y][x].setPreferredSize(d);
                newButtons[y][x].setMinimumSize(d);
                newButtons[y][x].setMaximumSize(d);
                newButtons[y][x].setIcon(null);
                newButtons[y][x].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                newButtons[y][x].setMargin(new Insets(0, 0, 0, 0));
                newButtons[y][x].setBackground(Color.LIGHT_GRAY);

                newButtons[y][x].addMouseListener(new TileMouseAdapter(y, x, this));
                newButtons[y][x].addMouseMotionListener(new TileMouseAdapter(y, x, this));

                newButtons[y][x].addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        statusBar.setText("Tile: " + fx + ", " + fy + "   Zoom: " + tileSize + "px" + "   Tool: " + currentTool);
                    }
                });
                
                newGridPanel.add(newButtons[y][x]);
            }
        }

        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JScrollPane) {
                getContentPane().remove(c);
                break;
            }
        }

        JPanel gridWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        gridWrapper.add(newGridPanel);
        JScrollPane gridScroll = new JScrollPane(gridWrapper);
        add(gridScroll, BorderLayout.CENTER);

        levelData = newLevelData;
        buttons = newButtons;
        gridPanel = newGridPanel;

        loadShortcuts(gridPanel);

        revalidate();
        repaint();
    }

    private String levelName;
    private void saveLevel() {
        String name = JOptionPane.showInputDialog(this, "Enter level name:", levelName);
        if (name == null || name.isBlank()) return;

        levelName = name.endsWith(".txt") ? name : name + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(levelName))) {
            for (int[] row : levelData) {
                for (int cell : row) {
                    writer.write(cell + " ");
                }
                writer.newLine();
            }

            // Write size identifier at the end

            writer.write("SIZE " + levelData[0].length + " " + levelData.length);
            writer.newLine();
            writer.write("SPAWN " + playerSpawnX + " " + playerSpawnY);
            writer.newLine();

            for (EventData ev : events) {
                writer.write(String.format("EVENT %d %d %s %s", ev.x, ev.y, ev.type, ev.param));
                writer.newLine();
            }

            JOptionPane.showMessageDialog(this, "Level saved successfully as " + levelName + "!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving level: " + e.getMessage());
        }
    }


    // Helper classes

    private class IndexedButton extends JButton {
        private final int y, x;

        public IndexedButton(int y, int x) {
            super();
            this.y = y;
            this.x = x;
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setContentAreaFilled(true);
            setFocusPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (showTileIndex) {
                String text = String.valueOf(levelData[y][x]);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 9f));
                FontMetrics fm = g.getFontMetrics();
                int tx = getWidth() - fm.stringWidth(text) - 2;
                int ty = getHeight() - 2;
                g.setColor(Color.BLACK);
                g.drawString(text, tx, ty);
            }
        }
    }
}