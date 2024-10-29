package se.asteroid.model;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class RegularEnemy extends Character {
    private static final int SHOOT_COOLDOWN = 120;
    private int currentCooldown = 0;
    private List<Projectile> projectiles;
    private Ship target;
    private int maxHealth;
    private static final double BULLET_SPEED = 1.0;
    private BufferedImage enemyImage;

    private static final int SHIP_WIDTH = 70;
    private static final int SHIP_HEIGHT = 70;
    private static final int HITBOX_WIDTH = 45;
    private static final int HITBOX_HEIGHT = 35;

    public RegularEnemy(double x, double y, double velocityX, double velocityY, double angle, int health) {
        super(x, y, velocityX, velocityY, angle, health);
        this.projectiles = new ArrayList<>();
        this.maxHealth = health;
        this.currentCooldown = (int)(Math.random() * SHOOT_COOLDOWN);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/assets/regular_enemy.PNG"));
            Image originalImage = icon.getImage();

            enemyImage = new BufferedImage(SHIP_WIDTH, SHIP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = enemyImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, SHIP_WIDTH, SHIP_HEIGHT, null);
            g2d.dispose();
        } catch (Exception e) {
            System.err.println("Error loading enemy image: " + e.getMessage());
        }
    }

    @Override
    public void update() {
        x += velocityX;
        y += velocityY;

        if (x - SHIP_WIDTH/2 <= 0) {
            x = SHIP_WIDTH/2;
            velocityX = Math.abs(velocityX);
        } else if (x + SHIP_WIDTH/2 >= 800) {
            x = 800 - SHIP_WIDTH/2;
            velocityX = -Math.abs(velocityX);
        }

        if (y - SHIP_HEIGHT/2 <= 0) {
            y = SHIP_HEIGHT/2;
            velocityY = Math.abs(velocityY);
        } else if (y + SHIP_HEIGHT/2 >= 600) {
            y = 600 - SHIP_HEIGHT/2;
            velocityY = -Math.abs(velocityY);
        }

        if (currentCooldown > 0) {
            currentCooldown--;
        }

        if (target != null && currentCooldown <= 0) {
            shoot();
            currentCooldown = SHOOT_COOLDOWN;
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update();
            if (projectile.isOffScreen(800, 600)) {
                projectiles.remove(i);
            }
        }

        if (target != null) {
            double dx = target.getX() - x;
            double dy = target.getY() - y;
            angle = Math.toDegrees(Math.atan2(dy, dx));
        }
    }

    @Override
    public void draw(Graphics2D g) {
        if (enemyImage != null) {
            AffineTransform transform = new AffineTransform();
            transform.translate(x - SHIP_WIDTH/2, y - SHIP_HEIGHT/2);
            transform.rotate(Math.toRadians(angle), SHIP_WIDTH/2, SHIP_HEIGHT/2);
            g.drawImage(enemyImage, transform, null);

        }

        drawHealthBar(g);

        for (Projectile projectile : projectiles) {
            projectile.draw(g);
        }
    }

    private void drawHealthBar(Graphics2D g) {
        int healthBarWidth = 100;
        int healthBarHeight = 6;
        int currentHealthWidth = (int)((health / (double)maxHealth) * healthBarWidth);

        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect((int)x - healthBarWidth/2, (int)y - SHIP_HEIGHT/2 - 15,
                healthBarWidth, healthBarHeight);

        g.setColor(new Color(0, 255, 0, 192));
        g.fillRect((int)x - healthBarWidth/2, (int)y - SHIP_HEIGHT/2 - 15,
                currentHealthWidth, healthBarHeight);
    }

    private void shoot() {
        if (target != null) {
            double dx = target.getX() - x;
            double dy = target.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            double accuracy = 0.95;
            if (Math.random() > accuracy) {
                dx += (Math.random() - 0.5) * 20;
                dy += (Math.random() - 0.5) * 20;
                distance = Math.sqrt(dx * dx + dy * dy);
            }

            dx = (dx / distance) * BULLET_SPEED;
            dy = (dy / distance) * BULLET_SPEED;

            Projectile projectile = new Projectile(x, y, angle);
            projectile.setVelocity(dx, dy);
            projectiles.add(projectile);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(
                (int)(x - HITBOX_WIDTH/2),
                (int)(y - HITBOX_HEIGHT/2),
                HITBOX_WIDTH,
                HITBOX_HEIGHT
        );
    }

    public void hit() {
        health -= 25;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public List<Projectile> getBullets() {
        return projectiles;
    }
    public void setTarget(Ship target) {
        this.target = target;
    }
}