package Menu;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import Main.LevelEditor;

public class EditorMenuBar extends JMenuBar{
    public EditorMenuBar(LevelEditor editor) {
        JMenu fileMenu = new JMenu("File");

        JMenuItem newFile = new JMenuItem("New project (Ctrl + N)");
        newFile.addActionListener(e -> {
            editor.newProject();
        });

        JMenuItem loadFile = new JMenuItem("Load project");
        loadFile.addActionListener(e -> {
            editor.loadProject();
        });
        fileMenu.add(loadFile);

        JMenuItem saveFile = new JMenuItem("Save project (Ctrl + S)");
        saveFile.addActionListener(e -> {
            editor.saveProject();
        });

        JMenuItem loadSprites = new JMenuItem("Load sprites");
        loadSprites.addActionListener(e -> editor.loadSprites());
        fileMenu.add(loadSprites);

        fileMenu.add(newFile);
        fileMenu.add(loadFile);
        fileMenu.add(saveFile);
        fileMenu.addSeparator();
        fileMenu.add(loadSprites);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem undo = new JMenuItem("Undo (Ctrl + Z)");
        undo.addActionListener(e -> editor.undo());
        
        JMenuItem zoomIn = new JMenuItem("Zoom In");
        zoomIn.addActionListener(e -> editor.changeZoom(editor.tileSize + 8));

        JMenuItem zoomOut = new JMenuItem("Zoom Out");
        zoomOut.addActionListener(e -> editor.changeZoom(Math.max(8, editor.tileSize - 8)));

        JMenuItem preferences = new JMenuItem("Preferences...");
        preferences.addActionListener(e -> editor.showPreferencesDialog());
        editMenu.add(undo);
        editMenu.addSeparator();
        editMenu.add(zoomIn);
        editMenu.add(zoomOut);
        editMenu.addSeparator();
        editMenu.add(preferences);

        JMenu toolMenu = new JMenu("Tools");

        JMenuItem cursorTool = new JMenuItem("Cursor tool (C)");
        cursorTool.addActionListener(e -> {
            editor.currentTool = LevelEditor.Tool.CURSOR;
            editor.settingSpawn = false;
        });

        JMenuItem penTool = new JMenuItem("Pen tool (P)");
        penTool.addActionListener(e -> {
            editor.currentTool = LevelEditor.Tool.PAINT;
            editor.settingSpawn = false;
        });

        JMenuItem bucketTool = new JMenuItem("Bucket tool (B)");
        bucketTool.addActionListener(e -> {
            editor.currentTool = LevelEditor.Tool.BUCKET;
        });

        JMenuItem eraseTool = new JMenuItem("Eraser tool (E)");
        bucketTool.addActionListener(e -> {
            editor.currentTool = LevelEditor.Tool.ERASE;
        });

        JMenuItem spawnTool = new JMenuItem("Set spawn (S)");
        bucketTool.addActionListener(e -> {
            editor.currentTool = LevelEditor.Tool.PAINT;
            editor.settingSpawn = true;
        });

        JMenuItem entityTool = new JMenuItem("Place Entity");
        entityTool.addActionListener(e -> editor.currentTool = LevelEditor.Tool.ENTITY);
        
        JMenuItem itemTool = new JMenuItem("Place Item");
        itemTool.addActionListener(e -> editor.currentTool = LevelEditor.Tool.ITEM);

        JMenuItem collisionTool = new JMenuItem("Toggle Collision");
        collisionTool.addActionListener(e -> editor.currentTool = LevelEditor.Tool.COLLISION);

        toolMenu.add(cursorTool);
        toolMenu.add(penTool);
        toolMenu.add(bucketTool);
        toolMenu.add(eraseTool);
        toolMenu.add(spawnTool);
        toolMenu.addSeparator();
        toolMenu.add(entityTool);
        toolMenu.add(itemTool);
        toolMenu.add(collisionTool);

        add(fileMenu);
        add(editMenu);
        add(toolMenu);
    }
}