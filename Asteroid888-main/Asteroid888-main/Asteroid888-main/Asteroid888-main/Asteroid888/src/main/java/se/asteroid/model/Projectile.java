package se.asteroid.model;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Projectile {
    double x;
    double y;
    private double angle;
    double velocityX;
    double velocityY;
    private double speed = 4;

    // Static bullet sprite
    private static BufferedImage bulletSprite;
    private static final int SPRITE_WIDTH = 48;
    private static final int SPRITE_HEIGHT = 24;
    static final Logger logger = Logger.getLogger(Projectile.class.getName());

    public static void setBulletSprite(BufferedImage bulletSprite) {
        Projectile.bulletSprite = bulletSprite;
    }

    static {
        URL resourceUrl = Projectile.class.getResource("/assets/bullet.PNG");
        if (resourceUrl == null) {
            logger.log(Level.SEVERE, "Resource /assets/bullet.png not found!");
        } else {
            try {
                bulletSprite = ImageIO.read(resourceUrl);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to load bullet sprite", e);
            }
        }
    }

    public Projectile(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.velocityX = speed * Math.cos(Math.toRadians(angle));
        this.velocityY = speed * Math.sin(Math.toRadians(angle));
    }

    public void setVelocity(double velocityX, double velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    public void update() {
        x += velocityX;
        y += velocityY;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public boolean isOffScreen(int width, int height) {
        return x < 0 || x > width || y < 0 || y > height;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void draw(Graphics2D g) {
        if (bulletSprite == null) {
            //Fallback if no image loaded
            g.setColor(Color.BLUE);
            g.fillOval((int) x - 2, (int) y - 2, 4, 4);
            return;
        }

        AffineTransform oldTransform = g.getTransform();

        g.translate(x, y);
        g.rotate(Math.toRadians(angle));

        // Draw from current position
        g.drawImage(bulletSprite,
                -SPRITE_WIDTH/2,
                -SPRITE_HEIGHT/2,
                SPRITE_WIDTH,
                SPRITE_HEIGHT,
                null);

        g.setTransform(oldTransform);
    }

    public Rectangle getBounds() {
        int hitboxWidth = SPRITE_WIDTH - 8;
        int hitboxHeight = SPRITE_HEIGHT - 4;
        return new Rectangle(
                (int) x - hitboxWidth/2,
                (int) y - hitboxHeight/2,
                hitboxWidth,
                hitboxHeight
        );
    }
}