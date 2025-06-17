package Main;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.*;

public class Main {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int width = 30, height = 30;

            try {
                javax.swing.UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (Exception ex) {
                System.err.println("Failed to initialize FlatLaf");
            }

            LevelEditor editor = new LevelEditor(width, height);
            editor.setVisible(true);
            editor.setExtendedState(LevelEditor.lastExtendedState);
        });
    }

}
