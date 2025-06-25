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

import org.json.*;

import Menu.*;
import Input.*;

public class LevelEditor extends JFrame {

    String[] commonEditors = {
        "code",          // VS Code
        "subl",          // Sublime Text
        "notepad++.exe", // Notepad++ (Windows)
        "notepad.exe",   // Notepad (Windows)
        "gedit",         // Gedit (Linux)
        "kate",          // Kate (Linux)
        "mousepad",      // Mousepad (Linux)
        "leafpad",       // Leafpad (Linux)
        "nano",          // Nano (terminal)
        "vim",           // Vim (terminal)
        "emacs"          // Emacs (terminal)
    };

    private static final String PREF_SPRITE_POS = "spriteSelectorPosition";
    private String spriteSelectorPosition = "TOP";

    private static final String PREF_SCRIPT_EDITOR = "scriptEditorPath";
    private String scriptEditorPath = "";

    private DefaultListModel<String> scriptListModel;
    private JList<String> scriptList;
    private JButton removeScriptBtn;

    public int selectedSprite = 1;

    public int playerSpawnX = 0;
    public int playerSpawnY = 0;
    public boolean settingSpawn = false;

    ImageIcon[] spriteIcons;
    private String[] spriteFileNames;

    Shortcuts shortcuts;

    private JPanel spritePanel;
    private JScrollPane spriteScroll;

    private JPanel inspectorPanel;
    private JTextField inspectorTag;
    private JLabel inspectorTitle, inspectorX, inspectorY, inspectorIndex, inspectorCollision;
    private JSplitPane splitPane;
    private JButton inspectorScriptBtn;
    private int inspectorScriptY = -1, inspectorScriptX = -1;
    private LevelPanel inspectorScriptLevel = null;

    private LevelPanel.Entity inspectorEntity = null;
    private JButton removeEntityBtn;
    private JTextField entityNameField, entityTypeField;

    private static final String PREF_SPRITE_FOLDER = "spriteFolder";
    private java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(LevelEditor.class);

    public boolean showTileIndex = false;

    public enum Tool { CURSOR, PAINT, BUCKET, ERASE, ENEMY, ITEM, COLLISION, ENTITY }
    public Tool currentTool = Tool.PAINT;

    public JPanel gridPanel;
    public int tileSize = 16;

    private JLabel statusBar;

    // Cache scaled icons
    private java.util.Map<Integer, ImageIcon[]> scaledIconCache = new java.util.HashMap<>();

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

    public List<LevelPanel> levels = new ArrayList<>();
    public JPanel canvasPanel; // This will hold all LevelPanels

    public LevelPanel draggingLevel = null;
    public LevelPanel activeLevelPanel = null;

    public Point dragOffset = null;

    String currentProjectName = "Untitled Project";

    public LevelEditor(int width, int height) {

        setTitle("VSA Editor - (" + currentProjectName + ")");
        setSize(850, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
      
        setLocationRelativeTo(null);

        // JMenuItem resizeLevel = new JMenuItem("Resize Level");
        // resizeLevel.addActionListener(e -> {
        //     String newWidthStr = JOptionPane.showInputDialog(this, "Enter new width:", levelPanel.levelData[0].length);
        //     String newHeightStr = JOptionPane.showInputDialog(this, "Enter new height:", levelData.length);
        //     if (newWidthStr == null || newHeightStr == null) return;
        //     try {
        //         int newWidth = Integer.parseInt(newWidthStr);
        //         int newHeight = Integer.parseInt(newHeightStr);
        //         if (newWidth < 1 || newHeight < 1) throw new NumberFormatException();
        //         resizeLevel(newWidth, newHeight);
        //     } catch (NumberFormatException ex) {
        //         JOptionPane.showMessageDialog(this, "Invalid size.");
        //     }
        // });

        
        // fileMenu.add(resizeLevel);

        setJMenuBar(new EditorMenuBar(this));

        canvasPanel = new JPanel(null); // null layout for absolute positioning
        canvasPanel.setPreferredSize(new Dimension(2000, 2000));
        JScrollPane scrollPane = new JScrollPane(canvasPanel);
        add(scrollPane, BorderLayout.CENTER);

        scrollPane.getViewport().addMouseWheelListener(e -> HandleZoom(e, scrollPane));

        scriptEditorPath = prefs.get(PREF_SCRIPT_EDITOR, "");

        inspectorPanel = new JPanel();
        inspectorPanel.setLayout(new BoxLayout(inspectorPanel, BoxLayout.Y_AXIS));
        inspectorPanel.setBorder(BorderFactory.createTitledBorder("Inspector"));

        JPanel transformPanel = new JPanel();
        transformPanel.setLayout(new BoxLayout(transformPanel, BoxLayout.Y_AXIS));
        transformPanel.setBorder(BorderFactory.createTitledBorder("Transform"));

        JPanel xyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        xyPanel.setPreferredSize(new Dimension(getPreferredSize().width, 5));
        inspectorX = new JLabel("X: -");
        inspectorY = new JLabel("Y: -");
        xyPanel.add(inspectorX);
        xyPanel.add(inspectorY);
        transformPanel.add(xyPanel);

        inspectorPanel.add(transformPanel);

        JPanel entityPanel = new JPanel();
        entityPanel.setLayout(new BoxLayout(entityPanel, BoxLayout.Y_AXIS));
        entityPanel.setBorder(BorderFactory.createTitledBorder("Entity"));

        JPanel entityPanelItems = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        entityPanelItems.setPreferredSize(new Dimension(getPreferredSize().width, 5));

        entityTypeField = new JTextField();
        entityTypeField.setEditable(false);
        entityPanelItems.add(new JLabel("Type:"));
        entityPanelItems.add(entityTypeField);

        entityNameField = new JTextField();
        entityPanelItems.add(new JLabel("Name:"));
        entityPanelItems.add(entityNameField);

        entityPanel.add(entityPanelItems);

        removeEntityBtn = new JButton("Remove Entity");
        removeEntityBtn.setEnabled(false);
        entityPanel.add(removeEntityBtn);

        inspectorPanel.add(entityPanel);

        removeEntityBtn.addActionListener(e -> {
            if (inspectorEntity != null && inspectorScriptLevel != null) {
                inspectorScriptLevel.entities.remove(inspectorEntity);
                inspectorEntity = null;
                removeEntityBtn.setEnabled(false);
                entityTypeField.setText("");
                entityNameField.setText("");
                inspectorScriptLevel.repaint();
            }
        });
        entityNameField.addActionListener(e -> {
            if (inspectorEntity != null) {
                inspectorEntity.name = entityNameField.getText();
            }
        });

        // --- Tile Section ---
        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
        tilePanel.setBorder(BorderFactory.createTitledBorder("Tile"));

        inspectorIndex = new JLabel("Index: -");
        inspectorIndex.setAlignmentX(Component.LEFT_ALIGNMENT);
        inspectorIndex.setHorizontalAlignment(SwingConstants.LEFT);
        tilePanel.add(inspectorIndex);

        inspectorCollision = new JLabel("Collision: -");
        inspectorCollision.setAlignmentX(Component.LEFT_ALIGNMENT);
        inspectorCollision.setHorizontalAlignment(SwingConstants.LEFT);
        tilePanel.add(inspectorCollision);

        // Tag row
        inspectorTag = new JTextField();
        JPanel tagPanel = new JPanel(new BorderLayout(8, 0));
        tagPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, inspectorTag.getPreferredSize().height));
        tagPanel.add(new JLabel("Tag:"), BorderLayout.WEST);
        tagPanel.add(inspectorTag, BorderLayout.CENTER);
        tagPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tilePanel.add(tagPanel);

        inspectorPanel.add(tilePanel);

        // --- Scripts Section ---
        JPanel scriptsPanel = new JPanel();
        scriptsPanel.setLayout(new BoxLayout(scriptsPanel, BoxLayout.Y_AXIS));
        scriptsPanel.setBorder(BorderFactory.createTitledBorder("Scripts"));

        scriptListModel = new DefaultListModel<>();
        scriptList = new JList<>(scriptListModel);
        scriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scriptListScroll = new JScrollPane(scriptList);
        scriptListScroll.setPreferredSize(new Dimension(180, 60));
        scriptListScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        scriptsPanel.add(scriptListScroll);

        JPanel removePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        removeScriptBtn = new JButton("Remove Script");
        removeScriptBtn.setEnabled(false);
        removeScriptBtn.addActionListener(e -> {
            int idx = scriptList.getSelectedIndex();
            if (idx >= 0 && inspectorScriptLevel != null && inspectorScriptY >= 0 && inspectorScriptX >= 0) {
                java.util.List<String> scripts = getTileScriptNames(inspectorScriptLevel, inspectorScriptY, inspectorScriptX);
                if (idx < scripts.size()) {
                    scripts.remove(idx);
                    updateScriptList(inspectorScriptLevel, inspectorScriptY, inspectorScriptX);
                }
            }
        });
        removePanel.add(removeScriptBtn);

        scriptsPanel.add(removePanel, BorderLayout.SOUTH);

        inspectorScriptBtn = new JButton("Attach Script");
        inspectorScriptBtn.setEnabled(false);
        JPanel scriptBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        scriptBtnPanel.add(inspectorScriptBtn);
        scriptsPanel.add(Box.createVerticalGlue());
        scriptsPanel.add(scriptBtnPanel);

        inspectorPanel.add(scriptsPanel);

        inspectorScriptBtn.addActionListener(e -> {
            if (inspectorScriptLevel != null && inspectorScriptY >= 0 && inspectorScriptX >= 0) {
                // Open script picker dialog
                String scriptName = JOptionPane.showInputDialog(this, "Script name (class):", "MyScript");
                if (scriptName != null && !scriptName.trim().isEmpty()) {
                    java.util.List<String> scripts = getTileScriptNames(inspectorScriptLevel, inspectorScriptY, inspectorScriptX);
                    if (!scripts.contains(scriptName)) {
                        scripts.add(scriptName);
                        updateScriptList(inspectorScriptLevel, inspectorScriptY, inspectorScriptX);
                    }
                    OpenScriptEditor(inspectorScriptLevel, inspectorScriptY, inspectorScriptX, scriptName);
                }
            }
        });

        // Split pane: left = scrollPane, right = inspector
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, inspectorPanel);
        splitPane.setResizeWeight(1.0); // Give most space to the editor
        splitPane.setDividerLocation(0.8); // Start with inspector small
        add(splitPane, BorderLayout.CENTER);

        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    boolean clickedOnLevel = false;
                    for (LevelPanel lp : levels) {
                        if (lp.getBounds().contains(e.getPoint())) {
                            clickedOnLevel = true;
                            break;
                        }
                    }
                    if (!clickedOnLevel) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem addLevel = new JMenuItem("Add Level");
                        addLevel.addActionListener(ev -> addNewLevelAt(e.getX(), e.getY()));
                        menu.add(addLevel);
                        menu.show(canvasPanel, e.getX(), e.getY());
                    }
                }
            }
        });

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

        add(spriteScroll, BorderLayout.NORTH);

        spriteSelectorPosition = prefs.get(PREF_SPRITE_POS, "TOP");
        placeSpritePanel();

        String theme = prefs.get("theme", "Light");
        try {
            if ("Dark".equals(theme)) {
                com.formdev.flatlaf.FlatDarkLaf.setup();
            } else {
                com.formdev.flatlaf.FlatLightLaf.setup();
            }
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {}

        showTileIndex = prefs.getBoolean("showTileIndex", false);

        // pack();

        shortcuts = new Shortcuts(this);

        // status bar stuff
        statusBar = new JLabel("Tile: -, -   Zoom: " + tileSize + "px" + "   Tool: " + currentTool);
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 2));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusBar, BorderLayout.WEST);

        add(statusPanel, BorderLayout.PAGE_END);
    }

    public void showInspector(LevelPanel level, int y, int x) {
        inspectorX.setText("X: " + x);
        inspectorY.setText("Y: " + y);

        inspectorIndex.setText("Index: " + level.levelData[y][x]);
        boolean hasCollision = false;
        if (level.collisionData != null && y < level.collisionData.length && x < level.collisionData[0].length) {
            hasCollision = level.collisionData[y][x];
        }
        inspectorCollision.setText("Collision: " + (hasCollision ? "Yes" : "No"));
        inspectorTag.setText(level.tileTags[y][x]);
        inspectorTag.setEditable(true);
        inspectorTag.addActionListener(ev -> {
            level.tileTags[y][x] = inspectorTag.getText();
        });
        inspectorScriptBtn.setEnabled(true);
        inspectorScriptY = y;
        inspectorScriptX = x;
        inspectorScriptLevel = level;
        updateScriptList(level, y, x);

        LevelPanel.Entity found = null;
        for (LevelPanel.Entity entity : level.entities) {
            if (entity.x == x && entity.y == y) {
                found = entity;
                break;
            }
        }
        if (found != null) {
            inspectorEntity = found;
            entityTypeField.setText(found.type);
            entityNameField.setText(found.name);
            removeEntityBtn.setEnabled(true);
        } else {
            inspectorEntity = null;
            entityTypeField.setText("");
            entityNameField.setText("");
            removeEntityBtn.setEnabled(false);
        }
    }

    public void showEntityInspector(LevelPanel level, LevelPanel.Entity entity) {
        showInspector(level, entity.y, entity.x);
    }

    public void clearInspector() {
        inspectorTitle.setText("No tile selected");
        inspectorX.setText("X: -");
        inspectorY.setText("Y: -");
        inspectorIndex.setText("Index: -");
        inspectorCollision.setText("Collision: -");
        inspectorScriptBtn.setEnabled(false);
        inspectorScriptY = inspectorScriptX = -1;
        inspectorScriptLevel = null;
    }

    public void showPreferencesDialog() {
        JDialog dialog = new JDialog(this, "Preferences", true);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setSize(500, 300);
        dialog.setLayout(new BorderLayout());

        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;

        settingsPanel.add(new JLabel("Sprite selector position:"), gbc);

        String[] positions = {"TOP", "LEFT", "RIGHT"};
        JComboBox<String> posBox = new JComboBox<>(positions);
        posBox.setSelectedItem(spriteSelectorPosition);
        gbc.gridx = 1;
        settingsPanel.add(posBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        settingsPanel.add(new JLabel("Show tile index:"), gbc);

        JCheckBox showIndexBox = new JCheckBox();
        showIndexBox.setSelected(showTileIndex);
        gbc.gridx = 1;
        settingsPanel.add(showIndexBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        settingsPanel.add(new JLabel("Theme:"), gbc);

        String[] themes = {"Light", "Dark"};
        JComboBox<String> themeBox = new JComboBox<>(themes);
        boolean isDark = UIManager.getLookAndFeel().getName().toLowerCase().contains("dark");
        themeBox.setSelectedItem(isDark ? "Dark" : "Light");
        gbc.gridx = 1;
        settingsPanel.add(themeBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        settingsPanel.add(new JLabel("External script editor:"), gbc);

        JTextField editorPathField = new JTextField(scriptEditorPath, 20);
        gbc.gridx = 1;
        // settingsPanel.add(editorPathField, gbc);

        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(ev -> {
            JFileChooser chooser = new JFileChooser(".");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = chooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                editorPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        gbc.gridx = 2;
        settingsPanel.add(browseBtn, gbc);

        JComboBox<String> detectedEditors = new JComboBox<>();
        detectedEditors.addItem(""); // Empty option

        for (String editor : commonEditors) {
            if (isEditorAvailable(editor)) {
                detectedEditors.addItem(editor);
            }
        }
        if (detectedEditors.getItemCount() > 1) {
            gbc.gridx = 1;
            settingsPanel.add(detectedEditors, gbc);

            detectedEditors.addActionListener(ev -> {
                String selected = (String) detectedEditors.getSelectedItem();
                if (selected != null && !selected.isEmpty()) {
                    editorPathField.setText(selected);
                }
            });
        }


        dialog.add(settingsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 16));
        JButton applyBtn = new JButton("Apply");
        JButton cancelBtn = new JButton("Cancel");
        buttonPanel.add(applyBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        applyBtn.addActionListener(e -> {
            spriteSelectorPosition = (String) posBox.getSelectedItem();
            prefs.put(PREF_SPRITE_POS, spriteSelectorPosition);
            placeSpritePanel();

            showTileIndex = showIndexBox.isSelected();
            prefs.putBoolean("showTileIndex", showTileIndex);
            for(LevelPanel level : levels) {
                for (int y = 0; y < level.buttons.length; y++)
                    for (int x = 0; x < level.buttons[y].length; x++)
                        level.buttons[y][x].repaint();
            }

            String selectedTheme = (String) themeBox.getSelectedItem();
            try {
                if ("Dark".equals(selectedTheme)) {
                    com.formdev.flatlaf.FlatDarkLaf.setup();
                } else {
                    com.formdev.flatlaf.FlatLightLaf.setup();
                }
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to set theme: " + ex.getMessage());
            }
            prefs.put("theme", selectedTheme);

            scriptEditorPath = editorPathField.getText().trim();
            prefs.put(PREF_SCRIPT_EDITOR, scriptEditorPath);

            dialog.dispose();
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void placeSpritePanel() {
        getContentPane().remove(spriteScroll);

        if (spriteSelectorPosition.equals("LEFT") || spriteSelectorPosition.equals("RIGHT")) {
            spritePanel.setLayout(new GridLayout(0, 1, 2, 2));
            spriteScroll.setPreferredSize(new Dimension(100, 600));
        } else {
            spritePanel.setLayout(new GridLayout(1, 0, 2, 2));
            spriteScroll.setPreferredSize(new Dimension(600, 100));
        }

        switch (spriteSelectorPosition) {
            case "LEFT":
                add(spriteScroll, BorderLayout.WEST);
                break;
            case "RIGHT":
                add(spriteScroll, BorderLayout.EAST);
                break;
            default:
                add(spriteScroll, BorderLayout.NORTH);
        }
        spritePanel.revalidate();
        spritePanel.repaint();
        revalidate();
        repaint();
    }

    public void newProject() {
        lastExtendedState = getExtendedState();
        LevelEditor newEditor = new LevelEditor(10,10);
        newEditor.setExtendedState(lastExtendedState);
        newEditor.setVisible(true);

        this.dispose();
    }

    private void addNewLevelAt(int x, int y) {
        int width = 20, height = 20;
        int tileSize = this.tileSize;
        LevelPanel newLevel = new LevelPanel(this, width, height, tileSize);
        newLevel.logicalX = x / tileSize;
        newLevel.logicalY = y / tileSize;
        newLevel.offsetX = x;
        newLevel.offsetY = y;
        newLevel.setBounds(x, y, width * tileSize, height * tileSize);
        levels.add(newLevel);
        canvasPanel.add(newLevel);
        canvasPanel.repaint();
    }

    public void loadProject() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Load Project");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            String json = sb.toString();
            // Use a JSON library like org.json or minimal manual parsing
            org.json.JSONObject obj = new org.json.JSONObject(json);

            tileSize = obj.getInt("tileSize");
            levels.clear();
            canvasPanel.removeAll();

            org.json.JSONArray levelsArr = obj.getJSONArray("levels");
            for (int i = 0; i < levelsArr.length(); i++) {
                org.json.JSONObject lvl = levelsArr.getJSONObject(i);
                int x = lvl.getInt("x");
                int y = lvl.getInt("y");
                int width = lvl.getInt("width");
                int height = lvl.getInt("height");
                
                LevelPanel lp = new LevelPanel(this, width, height, tileSize);
                lp.setLocation(x, y);

                lp.logicalX = lvl.optInt("logicalX", x / tileSize);
                lp.logicalY = lvl.optInt("logicalY", y / tileSize);
                lp.offsetX = (int)Math.round(lp.logicalX * tileSize);
                lp.offsetY = (int)Math.round(lp.logicalY * tileSize);
                lp.setBounds(lp.offsetX, lp.offsetY, width * tileSize, height * tileSize);

                // Load levelData
                org.json.JSONArray dataArr = lvl.getJSONArray("levelData");
                for (int yy = 0; yy < height; yy++) {
                    org.json.JSONArray row = dataArr.getJSONArray(yy);
                    for (int xx = 0; xx < width; xx++) {
                        lp.levelData[yy][xx] = row.getInt(xx);
                        lp.buttons[yy][xx].setIcon(spriteIcons[lp.levelData[yy][xx]]);
                    }
                }

                // Load collisionData
                // org.json.JSONArray collArr = lvl.getJSONArray("collisionData");
                // for (int yy = 0; yy < height; yy++) {
                //     org.json.JSONArray row = collArr.getJSONArray(yy);
                //     for (int xx = 0; xx < width; xx++) {
                //     }
                // }

                // Load events
                org.json.JSONArray eventsArr = lvl.getJSONArray("events");
                for (int j = 0; j < eventsArr.length(); j++) {
                    org.json.JSONObject ev = eventsArr.getJSONObject(j);
                    events.add(new EventData(ev.getInt("x"), ev.getInt("y"), ev.getString("type"), ev.getString("param")));
                }

                // Load spawn
                playerSpawnX = lvl.getInt("playerSpawnX");
                playerSpawnY = lvl.getInt("playerSpawnY");

                levels.add(lp);
                canvasPanel.add(lp);
            }
            canvasPanel.repaint();
            JOptionPane.showMessageDialog(this, "Project loaded!");
            setTitle("VSA Level Editor - (" + file + ")");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading project: " + e.getMessage());
        }
    }

    public void loadSprites() {
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

    public void undo() {
        if (activeLevelPanel == null) return;
        if (!activeLevelPanel.undoStack.isEmpty()) {
            int[][] prev = activeLevelPanel.undoStack.pop();
            if (prev.length == activeLevelPanel.levelData.length && prev[0].length == activeLevelPanel.levelData[0].length) {
                for (int y = 0; y < activeLevelPanel.levelData.length; y++)
                    System.arraycopy(prev[y], 0, activeLevelPanel.levelData[y], 0, activeLevelPanel.levelData[0].length);

                // Update buttons
                for (int y = 0; y < activeLevelPanel.buttons.length; y++)
                    for (int x = 0; x < activeLevelPanel.buttons[0].length; x++)
                        activeLevelPanel.UpdateButtonIcon(y, x);
            }
        }
    }

    private void HandleZoom(MouseWheelEvent e, JScrollPane scrollPane) {
        if (!e.isControlDown()) return;

        // Mouse position relative to canvasPanel
        Point mouse = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), canvasPanel);

        double relX = mouse.getX() / (double)canvasPanel.getWidth();
        double relY = mouse.getY() / (double)canvasPanel.getHeight();

        // int oldTileSize = tileSize;
        int zoomStep = 2;
        int newTileSize = tileSize + (e.getWheelRotation() < 0 ? zoomStep : -zoomStep);
        newTileSize = Math.max(8, newTileSize);

        if (newTileSize == tileSize) return;

        changeZoom(newTileSize);

        SwingUtilities.invokeLater(() -> {
            int newW = canvasPanel.getWidth();
            int newH = canvasPanel.getHeight();

            int viewW = scrollPane.getViewport().getWidth();
            int viewH = scrollPane.getViewport().getHeight();

            int newX = (int)(relX * newW - viewW / 2);
            int newY = (int)(relY * newH - viewH / 2);

            newX = Math.max(0, Math.min(newX, newW - viewW));
            newY = Math.max(0, Math.min(newY, newH - viewH));

            scrollPane.getHorizontalScrollBar().setValue(newX);
            scrollPane.getVerticalScrollBar().setValue(newY);
        });
    }

    public void changeZoom(int newTileSize) {
        // double scale = newTileSize / (double)tileSize;
        tileSize = newTileSize;
        ImageIcon[] scaledIcons = getScaledIcons(tileSize);

        int maxRight = 0, maxBottom = 0;
        for (LevelPanel lp : levels) {
            // Scale position based on logical coordinates
            lp.offsetX = (int)Math.round(lp.logicalX * tileSize);
            lp.offsetY = (int)Math.round(lp.logicalY * tileSize);

            for (int y = 0; y < lp.buttons.length; y++) {
                for (int x = 0; x < lp.buttons[y].length; x++) {
                    Dimension d = new Dimension(tileSize, tileSize);
                    lp.buttons[y][x].setPreferredSize(d);
                    lp.buttons[y][x].setMinimumSize(d);
                    lp.buttons[y][x].setMaximumSize(d);
                    lp.buttons[y][x].setMargin(new Insets(0, 0, 0, 0));
                    lp.buttons[y][x].setContentAreaFilled(false);
                    lp.buttons[y][x].setHorizontalAlignment(SwingConstants.CENTER);
                    lp.buttons[y][x].setVerticalAlignment(SwingConstants.CENTER);

                    if (lp.levelData[y][x] >= 0 && lp.levelData[y][x] < scaledIcons.length) {
                        lp.buttons[y][x].setIcon(scaledIcons[lp.levelData[y][x]]);
                    }

                    lp.buttons[y][x].setBounds(x * tileSize, y * tileSize + 24, tileSize, tileSize);
                }
            }
            int w = lp.levelData[0].length * tileSize;
            int h = lp.levelData.length * tileSize;
            lp.setBounds(lp.offsetX, lp.offsetY, w, h);
            maxRight = Math.max(maxRight, lp.offsetX + w);
            maxBottom = Math.max(maxBottom, lp.offsetY + h);
            lp.revalidate();
            lp.repaint();
        }
        canvasPanel.setPreferredSize(new Dimension(maxRight + 100, maxBottom + 100));
        canvasPanel.revalidate();

        statusBar.setText("Tile: -, -   Zoom: " + tileSize + "px" + "   Tool: " + currentTool);
    }


    // private void resizeLevel(int newWidth, int newHeight) {
    //     int[][] newLevelData = new int[newHeight][newWidth];
    //     boolean[][] newCollisionData = new boolean[newHeight][newWidth];
    //     JButton[][] newButtons = new JButton[newHeight][newWidth];

    //     for (int y = 0; y < newHeight; y++)
    //         for (int x = 0; x < newWidth; x++)
    //             newLevelData[y][x] = -1;

    //     // Copy old data
    //     for (int y = 0; y < Math.min(levelData.length, newHeight); y++)
    //         for (int x = 0; x < Math.min(levelData[0].length, newWidth); x++)
    //             newLevelData[y][x] = levelData[y][x];
            
    //     for (int y = 0; y < Math.min(collisionData.length, newHeight); y++)
    //         for (int x = 0; x < Math.min(collisionData[0].length, newWidth); x++)
    //             newCollisionData[y][x] = collisionData[y][x];
    //     collisionData = newCollisionData;

    //     JPanel newGridPanel = new JPanel(new GridLayout(newHeight, newWidth, 0, 0));
    //     for (int y = 0; y < newHeight; y++) {
    //         for (int x = 0; x < newWidth; x++) {

    //             final int fx = x;
    //             final int fy = y;

    //             newButtons[y][x] = new IndexedButton(this, y, x);
    //             Dimension d = new Dimension(tileSize, tileSize);
    //             newButtons[y][x].setPreferredSize(d);
    //             newButtons[y][x].setMinimumSize(d);
    //             newButtons[y][x].setMaximumSize(d);
    //             newButtons[y][x].setIcon(null);
    //             newButtons[y][x].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
    //             newButtons[y][x].setMargin(new Insets(0, 0, 0, 0));
    //             newButtons[y][x].setBackground(Color.LIGHT_GRAY);

    //             newButtons[y][x].addMouseListener(new TileMouseAdapter(y, x, this));
    //             newButtons[y][x].addMouseMotionListener(new TileMouseAdapter(y, x, this));

    //             newButtons[y][x].addMouseMotionListener(new MouseMotionAdapter() {
    //                 @Override
    //                 public void mouseMoved(MouseEvent e) {
    //                     statusBar.setText("Tile: " + fx + ", " + fy + "   Zoom: " + tileSize + "px" + "   Tool: " + currentTool);
    //                 }
    //             });
                
    //             newGridPanel.add(newButtons[y][x]);
    //         }
    //     }

    //     for (Component c : getContentPane().getComponents()) {
    //         if (c instanceof JScrollPane) {
    //             getContentPane().remove(c);
    //             break;
    //         }
    //     }

    //     JPanel gridWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    //     gridWrapper.add(newGridPanel);
    //     JScrollPane gridScroll = new JScrollPane(gridWrapper);
    //     add(gridScroll, BorderLayout.CENTER);

    //     gridScroll.getViewport().addMouseWheelListener(e -> HandleZoom(e, gridScroll));
    //     this.addMouseWheelListener(e -> HandleZoom(e, gridScroll));

    //     levelData = newLevelData;
    //     buttons = newButtons;
    //     gridPanel = newGridPanel;

    //     loadShortcuts(gridPanel);

    //     revalidate();
    //     repaint();
    // }

    public void saveProject() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Save Project");
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("{\n");
            writer.write("  \"tileSize\": " + tileSize + ",\n");
            writer.write("  \"levels\": [\n");
            for (int i = 0; i < levels.size(); i++) {
                LevelPanel lp = levels.get(i);
                writer.write("    {\n");
                writer.write("      \"x\": " + lp.getX() + ",\n");
                writer.write("      \"y\": " + lp.getY() + ",\n");
                writer.write("      \"logicalX\": " + lp.logicalX + ",\n");
                writer.write("      \"logicalY\": " + lp.logicalY + ",\n");
                writer.write("      \"width\": " + lp.levelData[0].length + ",\n");
                writer.write("      \"height\": " + lp.levelData.length + ",\n");

                // Save levelData
                writer.write("      \"levelData\": [\n");
                for (int y = 0; y < lp.levelData.length; y++) {
                    writer.write("        [");
                    for (int x = 0; x < lp.levelData[0].length; x++) {
                        writer.write(String.valueOf(lp.levelData[y][x]));
                        if (x < lp.levelData[0].length - 1) writer.write(", ");
                    }
                    writer.write("]");
                    if (y < lp.levelData.length - 1) writer.write(",");
                    writer.write("\n");
                }
                writer.write("      ],\n");

                // Save collisionData
                writer.write("      \"collisionData\": [\n");
                for (int y = 0; y < lp.levelData.length; y++) {
                    writer.write("        [");
                    for (int x = 0; x < lp.levelData[0].length; x++) {
                        writer.write(lp.collisionData != null && lp.collisionData[y][x] ? "true" : "false");
                        if (x < lp.levelData[0].length - 1) writer.write(", ");
                    }
                    writer.write("]");
                    if (y < lp.levelData.length - 1) writer.write(",");
                    writer.write("\n");
                }
                writer.write("      ],\n");

                // Save events
                writer.write("      \"events\": [\n");
                List<EventData> eventsForLevel = new ArrayList<>();
                for (EventData ev : events) {
                    if (ev.x >= 0 && ev.x < lp.levelData[0].length && ev.y >= 0 && ev.y < lp.levelData.length) {
                        eventsForLevel.add(ev);
                    }
                }
                for (int j = 0; j < eventsForLevel.size(); j++) {
                    EventData ev = eventsForLevel.get(j);
                    writer.write("        {\"x\": " + ev.x + ", \"y\": " + ev.y + ", \"type\": \"" + ev.type + "\", \"param\": \"" + ev.param + "\"}");
                    if (j < eventsForLevel.size() - 1) writer.write(",");
                    writer.write("\n");
                }
                writer.write("      ],\n");

                writer.write("      \"playerSpawnX\": " + lp.editor.playerSpawnX + ",\n");
                writer.write("      \"playerSpawnY\": " + lp.editor.playerSpawnY + "\n");

                writer.write("    }");
                if (i < levels.size() - 1) writer.write(",");
                writer.write("\n");

                writer.write("      \"tileScripts\": [\n");
                for (int y = 0; y < lp.tileScripts.length; y++) {
                    writer.write("        [");
                    for (int x = 0; x < lp.tileScripts[0].length; x++) {
                        java.util.List<String> scripts = lp.tileScripts[y][x];
                        writer.write("[");
                        for (int s = 0; s < scripts.size(); s++) {
                            writer.write(JSONObject.quote(scripts.get(s)));
                            if (s < scripts.size() - 1) writer.write(", ");
                        }
                        writer.write("]");
                        if (x < lp.tileScripts[0].length - 1) writer.write(", ");
                    }
                    writer.write("]");
                    if (y < lp.tileScripts.length - 1) writer.write(",");
                    writer.write("\n");
                }
                writer.write("      ],\n");
            }
            writer.write("  ]\n");
            writer.write("}\n");
            JOptionPane.showMessageDialog(this, "Project saved!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving project: " + e.getMessage());
        }
    }

    private void OpenScriptEditor(LevelPanel level, int y, int x, String scriptName) {
        String className = scriptName;
        File scriptsDir = new File("Scripts");
        if (!scriptsDir.exists()) scriptsDir.mkdirs();
        File scriptFile = new File(scriptsDir, className + ".java");

        String currentScript = "";
        if (scriptFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                currentScript = sb.toString();
            } catch (IOException ex) {}
        } else {
            currentScript = getDefaultScript(className);
        }

        if (scriptEditorPath != null && !scriptEditorPath.isEmpty()) {
            try {
                new ProcessBuilder(scriptEditorPath, scriptFile.getAbsolutePath()).start();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to launch external editor: " + ex.getMessage());
            }
            return;
        }

        JDialog dialog = new JDialog((Frame) null, "Script Editor: " + className, false);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JTextArea scriptArea = new JTextArea(20, 60);
        scriptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        scriptArea.setText(currentScript);

        JScrollPane scroll = new JScrollPane(scriptArea);

        JButton saveBtn = new JButton("Save Script");
        saveBtn.addActionListener(ev -> {
            String script = scriptArea.getText();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile))) {
                writer.write(script);
                JOptionPane.showMessageDialog(this, "Script saved to " + scriptFile.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to save script: " + ex.getMessage());
            }
            dialog.dispose();
        });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(ev -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                    scriptArea.setText(sb.toString());
                } catch (IOException ex) {
                    // Ignore
                }
            }
        });

        dialog.setVisible(true);
    }

    private String getDefaultScript(String className) {
        return "public class " + className + " {\n"
            + "    // Your script here\n"
            + "    public void run() {\n"
            + "        // ...\n"
            + "    }\n"
            + "}\n";
    }

    private boolean isEditorAvailable(String editor) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which",
                editor
            );
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private java.util.List<String> getTileScriptNames(LevelPanel level, int y, int x) {
        return level.tileScripts[y][x];
    }

    private void updateScriptList(LevelPanel level, int y, int x) {
        scriptListModel.clear();
        JPanel varsPanel = new JPanel();
        varsPanel.setLayout(new BoxLayout(varsPanel, BoxLayout.Y_AXIS));
        for (String name : getTileScriptNames(level, y, x)) {
            scriptListModel.addElement(name);
            // Try to read variables from the script file
            File scriptFile = new File("Scripts", name + ".java");
            if (scriptFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("public") && line.contains(";")) {
                            JLabel varLabel = new JLabel(line.replace("public", "").replace(";", "").trim());
                            varsPanel.add(varLabel);
                        }
                    }
                } catch (IOException ex) {}
            }
        }
        // Remove old vars panel if present (always just before the script button panel)
        int scriptBtnIdx = inspectorPanel.getComponentCount() - 1;
        if (scriptBtnIdx > 0 && inspectorPanel.getComponent(scriptBtnIdx - 1) instanceof JPanel) {
            inspectorPanel.remove(scriptBtnIdx - 1);
            scriptBtnIdx--;
        }
        inspectorPanel.add(varsPanel, scriptBtnIdx);
        inspectorPanel.revalidate();
        inspectorPanel.repaint();
        removeScriptBtn.setEnabled(scriptListModel.size() > 0);
    }
}