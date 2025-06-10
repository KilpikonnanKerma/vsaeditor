package Main;

import javax.swing.SwingUtilities;

public class Main {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int width = 30, height = 30;
            LevelEditor editor = new LevelEditor(width, height);
            editor.setVisible(true);
            editor.setExtendedState(LevelEditor.lastExtendedState);
        });
    }

}
