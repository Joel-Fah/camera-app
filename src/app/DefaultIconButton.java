package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DefaultIconButton extends JButton {

    public DefaultIconButton(Icon icon, int borderRadius, int padding) {
        super(icon);
        setBackground(Constants.BACKGROUND_COLOR);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        setContentAreaFilled(false);
        setOpaque(true);

        // Hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(Constants.HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(Constants.BACKGROUND_COLOR);
            }
        });

        // Focus effect
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                setBackground(Constants.FOCUS_COLOR);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                setBackground(Constants.BACKGROUND_COLOR);
            }
        });

        // Border radius
        setBorder(BorderFactory.createLineBorder(Constants.BACKGROUND_COLOR, borderRadius));
    }
}