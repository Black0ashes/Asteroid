package se.asteroid.model;

import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

public class Ship extends Character {
    private static final double DECELERATION = 0.98;
    private static final double ACCELERATION = 0.5;
    private boolean shooting;
    private List<Projectile> projectiles;
    private static final double MAX_VELOCITY = 5.0;
    private static final Logger logger = LogManager.getLogger(Ship.class);
    private boolean isMoving = false;
    // Sprite Ship fields
    private BufferedImage spriteSheet;
    private BufferedImage[][] sprites;
    private int currentRow = 3; // Default to horizontal row
    private int currentFrame = 0;
    private int animationDelay = 5;
    private int animationTick = 0;
    private static final int SPRITE_ROWS = 4;
    private static final int SPRITE_COLS = 4;
    private static final int SPRITE_WIDTH = 80;  // 320/4
    private static final int SPRITE_HEIGHT = 80; // 320/4
    private static final int HORIZONTAL_ROW = 3; // Row for A/D movement
    private static final int VERTICAL_ROW = 2;   // Row for W/S movement
    // Screen dimensions
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    // Sprite fields gunflash
    private static BufferedImage[] gunflashSprites;
    private static final int GUNFLASH_FRAMES = 4;
    private int currentGunflashFrame = -1; // -1 means no gunflash
    private static final int GUNFLASH_ANIMATION_DELAY = 4;
    private int gunflashTick = 0;
    private static final int GUNFLASH_WIDTH = 64;
    private static final int GUNFLASH_HEIGHT = 64;
    //Invincible bf spawn
    private boolean isInvincible = false;
    private int invincibleTicks = 0;
    private static final int INVINCIBLE_DURATION = 120;
    private static final float BLINK_RATE = 0.125f;
    private static final float SHIELD_ROTATION = 0.05f;
    private float shieldAngle = 0;
    private MovementDirection currentDirection = MovementDirection.NONE;

    private static final int SINGLE_SPRITE_WIDTH = 160;
    private static final int SINGLE_SPRITE_HEIGHT = 72;

    private enum MovementDirection {
        HORIZONTAL,
        VERTICAL,
        NONE
    }

    static {
        loadGunflashSprites();
    }

    private static void loadGunflashSprites() {
        try {
            // เปลี่ยนชื่อไฟล์ให้ตรงกับที่มีในโปรเจค
            BufferedImage spriteSheet = ImageIO.read(Ship.class.getResource("/assets/bullet_effect.png"));
            gunflashSprites = new BufferedImage[GUNFLASH_FRAMES];

            // ตรวจสอบว่าโหลด sprite สำเร็จ
            if (spriteSheet == null) {
                return;
            }

            int spriteWidth = spriteSheet.getWidth() / GUNFLASH_FRAMES;
            int spriteHeight = spriteSheet.getHeight();

            for (int i = 0; i < GUNFLASH_FRAMES; i++) {
                gunflashSprites[i] = spriteSheet.getSubimage(
                        i * spriteWidth,
                        0,
                        spriteWidth,
                        spriteHeight
                );
            }
        } catch (IOException e) {
        } catch (Exception e) {
        }
    }

    public Ship(double x, double y) {
        super(x, y, 0, 0, 0, 100);
        projectiles = new ArrayList<>();
        loadSpriteSheet();
        logger.info("PlayerShip initialized at position ({}, {})", x, y);
    }

    private void loadSpriteSheet() {
        try {
            spriteSheet = ImageIO.read(getClass().getResource("/assets/ship.png"));
            sprites = new BufferedImage[SPRITE_ROWS][SPRITE_COLS];

            for (int row = 0; row < SPRITE_ROWS; row++) {
                for (int col = 0; col < SPRITE_COLS; col++) {
                    sprites[row][col] = spriteSheet.getSubimage(
                            col * SPRITE_WIDTH,
                            row * SPRITE_HEIGHT,
                            SPRITE_WIDTH,
                            SPRITE_HEIGHT
                    );
                }
            }            logger.debug("Sprite sheet loaded successfully");

        } catch (IOException e) {
            logger.error("Failed to load sprite sheet", e);

        }
    }

    private void updateAnimation() {
        // อัพเดท animation ของตัวยาน
        if (isMoving) {
            animationTick++;
            if (animationTick >= animationDelay) {
                animationTick = 0;
                currentFrame--;
                if (currentFrame < 0) {
                    currentFrame = SPRITE_COLS - 1;
                }
            }
        } else {
            currentFrame = SPRITE_COLS - 1;
            animationTick = 0;
            currentDirection = MovementDirection.NONE;
        }

        // อัพเดท gunflash animation
        if (currentGunflashFrame >= 0) {
            gunflashTick++;
            if (gunflashTick >= GUNFLASH_ANIMATION_DELAY) {
                gunflashTick = 0;
                currentGunflashFrame++;
                if (currentGunflashFrame >= GUNFLASH_FRAMES) {
                    currentGunflashFrame = -1; // End gunflash animation
                }
            }
        }
    }

    @Override
    public void update() {
        //  invincibility
        if (isInvincible) {
            invincibleTicks--;
            if (invincibleTicks <= 0) {
                isInvincible = false;
                logger.debug("Invincibility expired");
            }
            shieldAngle += SHIELD_ROTATION;
        }

        move(); // This calls the parent's move() which includes screenWrap()

        velocityX *= DECELERATION;
        velocityY *= DECELERATION;
        isMoving = Math.abs(velocityX) > 0.01 || Math.abs(velocityY) > 0.01;
        // อัปเดต animation
        updateAnimation();

        // อัปเดตกระสุน
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update();
            if (projectile.isOffScreen(SCREEN_WIDTH, SCREEN_HEIGHT)) {
                projectiles.remove(i);
            }
        }

    }
    @Override
    public void draw(Graphics2D g) {
        // Draw shield effect when invincible
        if (isInvincible) {
            drawShieldEffect(g);
        }

        // Draw ship with blinking effect when invincible
        if (!isInvincible || (invincibleTicks * BLINK_RATE) % 1 > 0.5) {
            if (sprites != null && sprites[currentRow][currentFrame] != null) {
                AffineTransform old = g.getTransform();

                // เคลื่อนไปที่ตำแหน่งยาน
                g.translate(x, y);

                // หมุนตัวยาน
                g.rotate(Math.toRadians(angle));

                // วาดตัวยาน
                g.drawImage(sprites[currentRow][currentFrame],
                        -SPRITE_WIDTH/2,
                        -SPRITE_HEIGHT/2,
                        SPRITE_WIDTH,
                        SPRITE_HEIGHT,
                        null);

                // วาด gunflash ถ้ากำลังเล่น animation
                if (currentGunflashFrame >= 0 && gunflashSprites != null) {
                    // เก็บ transform ของตัวยานไว้
                    AffineTransform shipTransform = g.getTransform();

                    // หมุน gunflash 90 องศา
                    g.rotate(Math.toRadians(90));

                    // ย้าย gunflash ไปที่ส่วนบนของยาน
                    g.drawImage(gunflashSprites[currentGunflashFrame],
                            SPRITE_HEIGHT/2 - 115,  // ย้ายไปด้านบนของยาน
                            -GUNFLASH_WIDTH/2,  // กึ่งกลางตามแนวแกน x
                            GUNFLASH_WIDTH,
                            GUNFLASH_HEIGHT,
                            null
                    );

                    // กลับไปที่ transform ของตัวยาน
                    g.setTransform(shipTransform);
                }

                g.setTransform(old);
            }
        }

        // Draw all bullets
        for (Projectile projectile : projectiles) {
            projectile.draw(g);
        }
    }

    private void drawShieldEffect(Graphics2D g) {
        int shieldSize = 60;
        int numRings = 3;

        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(shieldAngle);

        for (int i = 0; i < numRings; i++) {
            float alpha = (float) (0.2 - (i * 0.05));
            Color gradientColor = new Color(0, 200, 255, (int) (alpha * 255));
            g.setColor(gradientColor);

            int size = shieldSize + (i * 12);
            g.drawOval(-size / 2, -size / 2, size, size);

            // Draw particles
            int particleCount = 6 + i * 2; // More particles on outer rings
            for (int j = 0; j < particleCount; j++) {
                double angle = Math.toRadians((360.0 / particleCount) * j + (shieldAngle * (i + 1)));
                int particleX = (int) (Math.cos(angle) * size / 2);
                int particleY = (int) (Math.sin(angle) * size / 2);
                int particleSize = 4 + i;  // Larger particles on outer rings
                g.fillOval(particleX - particleSize / 2, particleY - particleSize / 2, particleSize, particleSize);
            }
        }

        // Draw an inner pulsating glow
        int glowRadius = (int) (shieldSize * 0.75 + (Math.sin(shieldAngle) * 5));
        Color innerGlow = new Color(0, 255, 255, 120);
        g.setColor(innerGlow);
        g.fillOval(-glowRadius / 2, -glowRadius / 2, glowRadius, glowRadius);

        g.setTransform(old);
    }

    private void startGunflashAnimation() {
        currentGunflashFrame = 0;
        gunflashTick = 0;
    }

    public void shoot() {
        double radianAngle = Math.toRadians(angle - 90);
        double spawnDistance = SPRITE_HEIGHT / 2;
        double bulletX = x + spawnDistance * Math.cos(radianAngle);
        double bulletY = y + spawnDistance * Math.sin(radianAngle);
        projectiles.add(new Projectile(bulletX, bulletY, angle - 90));
        startGunflashAnimation();
        logger.debug("Shot fired at angle: {}", angle);
    }


    public void moveLeft() {
        velocityX -= ACCELERATION;
        limitVelocity();
        isMoving = true;
        currentDirection = MovementDirection.HORIZONTAL;
        currentRow = HORIZONTAL_ROW;
        logPosition();
    }

    public void moveRight() {
        velocityX += ACCELERATION;
        limitVelocity();
        isMoving = true;
        currentDirection = MovementDirection.HORIZONTAL;
        currentRow = HORIZONTAL_ROW;
        logPosition();
    }

    public void moveUp() {
        velocityY -= ACCELERATION;
        limitVelocity();
        isMoving = true;
        currentDirection = MovementDirection.VERTICAL;
        currentRow = VERTICAL_ROW;
        logPosition();
    }

    public void moveDown() {
        velocityY += ACCELERATION;
        limitVelocity();
        isMoving = true;
        currentDirection = MovementDirection.VERTICAL;
        currentRow = VERTICAL_ROW;
        logPosition();
    }


    public void setShooting(boolean shooting) {
        this.shooting = shooting;
        if (shooting) {
            shoot();
        }
    }

    private void limitVelocity() {
        double currentSpeed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (currentSpeed > MAX_VELOCITY) {
            double scale = MAX_VELOCITY / currentSpeed;
            velocityX *= scale;
            velocityY *= scale;
        }
    }

    public List<Projectile> getBullets() {
        return projectiles;
    }

    public void rotateLeft() {
        angle -= 10;
    }

    public void rotateRight() {
        angle += 10;
    }

    private void logPosition() {
        // Log current position and angle
        logger.debug("Ship position - X: {}, Y: {}, Angle: {}",
                String.format("%.2f", x),
                String.format("%.2f", y),
                String.format("%.2f", angle));
    }

    public double getX() {
        return x;
    }


    public double getY() {
        return y;
    }

    public Rectangle getBounds() {
        // กำหนดขนาด hitbox ให้เป็นสี่เหลี่ยมที่เหมาะสมกับรูปยาน
        int hitboxWidth = SPRITE_WIDTH / 4;    // ความกว้างประมาณครึ่งหนึ่งของ sprite
        int hitboxHeight = SPRITE_HEIGHT * 1/3; // ความสูงประมาณ 2/3 ของ sprite เพื่อให้ครอบคลุมส่วนตัวยาน

        return new Rectangle(
                (int)x - hitboxWidth/2,   // จุดเริ่มต้น x (กึ่งกลาง)
                (int)y - hitboxHeight/2,  // จุดเริ่มต้น y (กึ่งกลาง)
                hitboxWidth,              // ความกว้างของ hitbox
                hitboxHeight              // ความสูงของ hitbox
        );
    }
    public void setInvincible(boolean invincible) {
        this.isInvincible = invincible;
        this.invincibleTicks = invincible ? INVINCIBLE_DURATION : 0;
        logger.info("Invincibility {} - Duration: {} ticks",
                invincible ? "activated" : "deactivated",
                invincible ? INVINCIBLE_DURATION : 0);
    }

    public boolean isInvincible() {
        return isInvincible;
    }
    @Override
    protected void screenWrap() {
        boolean wrapped = false;
        double oldX = x;
        double oldY = y;

        if (x < -SPRITE_WIDTH/2) {
            x = SCREEN_WIDTH + SPRITE_WIDTH/2;
            wrapped = true;
        } else if (x > SCREEN_WIDTH + SPRITE_WIDTH/2) {
            x = -SPRITE_WIDTH/2;
            wrapped = true;
        }

        if (y < -SPRITE_HEIGHT/2) {
            y = SCREEN_HEIGHT + SPRITE_HEIGHT/2;
            wrapped = true;
        } else if (y > SCREEN_HEIGHT + SPRITE_HEIGHT/2) {
            y = -SPRITE_HEIGHT/2;
            wrapped = true;
        }

        if (wrapped) {
            logger.debug("Screen wrap from ({}, {}) to ({}, {})", oldX, oldY, x, y);
        }
    }




    public class BeamHitbox {
        private double startX, startY;
        private double endX, endY;
        private double width;

        public BeamHitbox(double startX, double startY, double endX, double endY, double width) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.width = width;
        }

        public boolean intersects(Rectangle target) {
            // Calculate beam direction vector
            double dx = endX - startX;
            double dy = endY - startY;
            double length = Math.sqrt(dx * dx + dy * dy);

            if (length == 0) return false;

            // Normalize direction vector
            dx /= length;
            dy /= length;

            // Calculate vector to target center
            double targetCenterX = target.getCenterX();
            double targetCenterY = target.getCenterY();

            // Vector from beam start to target center
            double vx = targetCenterX - startX;
            double vy = targetCenterY - startY;

            // Project target center onto beam line
            double projection = vx * dx + vy * dy;

            // Find closest point on beam line to target center
            double closestX = startX + Math.max(0, Math.min(length, projection)) * dx;
            double closestY = startY + Math.max(0, Math.min(length, projection)) * dy;

            // Use beam sprite width for collision detection
            return Math.sqrt(
                    Math.pow(closestX - targetCenterX, 2) +
                            Math.pow(closestY - targetCenterY, 2)
            ) < (width/2 + Math.max(target.width, target.height)/2);
        }
    }



    public void fireMissile(List<Asteroid> asteroids, List<RegularEnemy> regularEnemies, List<SecondTier> secondTier, Boss boss) {
        Character nearestEnemy = findNearestEnemy(asteroids, regularEnemies, secondTier, boss);
        if (nearestEnemy != null) {
            projectiles.add(new Missile(this.getX(), this.getY(), nearestEnemy));
        }
    }

    public void fireMissile(Boss boss) {
        Character nearestEnemy = findNearestEnemy(boss);
        if (nearestEnemy != null) {
            projectiles.add(new Missile(this.getX(), this.getY(), nearestEnemy));
        }
    }


    private Character findNearestEnemy(List<Asteroid> asteroids, List<RegularEnemy> regularEnemies, List<SecondTier> secondTierEnemies, Boss boss) {
        Character nearestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        for (Asteroid asteroid : asteroids) {
            double distance = getDistanceTo(asteroid);
            if (distance < minDistance) {
                minDistance = distance;
                nearestEnemy = asteroid;
            }
        }

        for (RegularEnemy enemy : regularEnemies) {
            double distance = getDistanceTo(enemy);
            if (distance < minDistance) {
                minDistance = distance;
                nearestEnemy = enemy;
            }
        }

        for (SecondTier enemy : secondTierEnemies) {
            double distance = getDistanceTo(enemy);
            if (distance < minDistance) {
                minDistance = distance;
                nearestEnemy = enemy;
            }
        }

        if (boss != null && boss.isAlive()) {
            double distance = getDistanceTo(boss);
            if (distance < minDistance) {
                nearestEnemy = boss;
            }
        }

        return nearestEnemy;
    }
    private Character findNearestEnemy(Boss boss) {
        Character nearestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        if (boss != null && boss.isAlive()) {
            double distance = getDistanceTo(boss);
            if (distance < minDistance) {
                nearestEnemy = boss;
            }
        }

        return nearestEnemy;
    }


    private double getDistanceTo(Character character) {
        double deltaX = character.getX() - this.getX();
        double deltaY = character.getY() - this.getY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}

