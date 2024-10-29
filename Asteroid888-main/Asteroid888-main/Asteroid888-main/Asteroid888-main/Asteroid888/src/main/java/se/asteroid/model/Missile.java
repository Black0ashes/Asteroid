package se.asteroid.model;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;

public class Missile extends Projectile {
    private Character target; // The target enemy the missile will track.
    private static BufferedImage missileSprite;

    // Define the size specifically for Missile
    private static final int SPRITE_WIDTH = 48;
    private static final int SPRITE_HEIGHT = 48;

    static {
        try {
            missileSprite = ImageIO.read(Missile.class.getResource("/assets/missile.PNG"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load missile sprite", e);
        }
    }

    public Missile(double x, double y, Character target) {
        super(x, y, 10);
        this.target = target;
    }

    @Override
    public void update() {
        if (target == null || !target.isAlive()) {
            // If the target is null or destroyed, move straight.
            moveStraight();
            return;
        }

        // Calculate direction towards the target
        double deltaX = target.getX() - this.getX();
        double deltaY = target.getY() - this.getY();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // Normalize the direction
        double speed = 5; // Set missile speed
        this.velocityX = (deltaX / distance) * speed;
        this.velocityY = (deltaY / distance) * speed;

        // Move missile
        this.x += velocityX;
        this.y += velocityY;
    }

    private void moveStraight() {
        this.x += velocityX;
        this.y += velocityY;
    }

    @Override
    public void draw(Graphics2D g) {
        if (missileSprite == null) {
            super.draw(g);
            return;
        }

        AffineTransform oldTransform = g.getTransform();

        g.translate(x, y);
        g.rotate(Math.toRadians(0)); // Adjust angle if needed

        g.drawImage(missileSprite,
                -SPRITE_WIDTH / 2,
                -SPRITE_HEIGHT / 2,
                SPRITE_WIDTH,
                SPRITE_HEIGHT,
                null);

        g.setTransform(oldTransform);
    }
}
