package app.ui;

import app.Constants;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

public class StyledComboBoxUI extends BasicComboBoxUI {

    @Override
    protected JButton createArrowButton() {
        JButton arrowButton = new JButton();
        arrowButton.setBorder(BorderFactory.createEmptyBorder());
        arrowButton.setBackground(Constants.BUTTON_COLOR);
        arrowButton.setForeground(Color.WHITE);
        arrowButton.setFocusPainted(false);
        arrowButton.setPreferredSize(new Dimension(16, 16));
        arrowButton.setIcon(new ImageIcon("src/assets/icons/dropdown.png")); // Ensure the path is correct
        return arrowButton;
    }

    @Override
    public void configureArrowButton() {
        super.configureArrowButton();
        if (arrowButton != null) {
            arrowButton.setBorder(BorderFactory.createEmptyBorder());
            arrowButton.setBackground(Constants.BUTTON_COLOR);
            arrowButton.setPreferredSize(new Dimension(16, 16));
            arrowButton.setIcon(new ImageIcon("src/app/assets/icons/dropdown.png")); // Apply the icon explicitly
        }
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        c.setBackground(new Color(238, 238, 238));
        c.setForeground(Color.BLACK);
        c.setBorder(new RoundedBorder(Constants.BUTTON_COLOR, Constants.BORDER_RADIUS));
        c.setFont(Constants.DEFAULT_FONT);
        c.setPreferredSize(new Dimension(150, 30));
    }
}