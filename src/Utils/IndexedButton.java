package Utils;

import javax.swing.JButton;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import Main.*;

public class IndexedButton extends JButton {
        private final LevelPanel levelPanel;

        private final int y, x;

        public IndexedButton(LevelPanel levelPanel, int y, int x) {
            super();
            this.levelPanel = levelPanel;
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

            for (LevelPanel.Entity entity : levelPanel.entities) {
                if (entity.x == x && entity.y == y) {
                    int size = Math.min(getWidth(), getHeight());
                    Color color = switch (entity.type.toLowerCase()) {
                        case "player" -> Color.GREEN;
                        case "enemy" -> Color.RED;
                        case "item" -> Color.YELLOW;
                        default -> Color.CYAN;
                    };
                    g.setColor(color);
                    g.fillOval(size/4, size/4, size/2, size/2);

                    g.setColor(Color.BLACK);
                    g.setFont(g.getFont().deriveFont(Font.BOLD, size/2f));
                    String label = entity.type.length() > 0 ? entity.type.substring(0,1).toUpperCase() : "?";
                    g.drawString(label, size/2 - 6, size/2 + 6);
                }
            }

            if (levelPanel.editor.showTileIndex) {
                String text = String.valueOf(levelPanel.levelData[y][x]);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 9f));
                FontMetrics fm = g.getFontMetrics();
                int tx = getWidth() - fm.stringWidth(text) - 2;
                int ty = getHeight() - 2;
                g.setColor(Color.BLACK);
                g.drawString(text, tx, ty);
            }
        }
    }