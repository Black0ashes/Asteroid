package se.asteroid;
import se.asteroid.controller.GameController;
import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Asteroid Game");
        GameController gamePanel = new GameController();
        frame.add(gamePanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}