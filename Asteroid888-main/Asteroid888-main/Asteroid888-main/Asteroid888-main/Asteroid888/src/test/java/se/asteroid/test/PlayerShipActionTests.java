package se.asteroid.test;
import se.asteroid.model.Boss;
import se.asteroid.model.Projectile;
import se.asteroid.model.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerShipActionTests {
    private Ship ship;
    private static final double DELTA = 0.01; // Delta for floating point comparisons
    private static final int SPRITE_HEIGHT = 80; // From PlayerShip class
    private static final int BEAM_SPAWN_DISTANCE = SPRITE_HEIGHT/2 + 46; // From PlayerShip class

    @BeforeEach
    void setUp() {
        ship = new Ship(400, 300); // Initialize ship at center of screen
    }

    @Test
    void testNormalShoot() {
        // Test single bullet shooting
        ship.shoot();
        assertEquals(1, ship.getBullets().size(), "Should create exactly one bullet");

        // Test bullet initial position using the correct spawn distance (SPRITE_HEIGHT/2)
        Projectile bullet = ship.getBullets().get(0);
        double spawnDistance = SPRITE_HEIGHT / 2;
        double angleInRadians = Math.toRadians(ship.getAngle() - 90);
        double expectedX = ship.getX() + spawnDistance * Math.cos(angleInRadians);
        double expectedY = ship.getY() + spawnDistance * Math.sin(angleInRadians);

        assertEquals(expectedX, bullet.getX(), DELTA, "Bullet X position should match expected spawn point");
        assertEquals(expectedY, bullet.getY(), DELTA, "Bullet Y position should match expected spawn point");
    }

    @Test
    void testUltimateShoot() {
        // Test ultimate shot (spreads multiple bullets)
        Ship ship = new Ship(400, 300);
        Boss boss = new Boss(100,100,ship);
        ship.fireMissile(boss);
        assertEquals(1, ship.getBullets().size(), "Missile shot should create 1 bullets");

        // Verify bullets are spread in a cone pattern
        double previousAngle = Double.NEGATIVE_INFINITY;
        for (Projectile bullet : ship.getBullets()) {
            double currentAngle = Math.toDegrees(Math.atan2(bullet.getVelocityY(), bullet.getVelocityX()));
            assertTrue(currentAngle > previousAngle, "Bullets should be spread in increasing angles");
            previousAngle = currentAngle;
        }
    }

    @Test
    void testInvincibility() {
        // Test initial state
        assertFalse(ship.isInvincible(), "Ship should not be invincible initially");

        // Activate invincibility
        ship.setInvincible(true);
        assertTrue(ship.isInvincible(), "Ship should be invincible after activation");

        // Test invincibility expiration
        for (int i = 0; i < 120; i++) { // INVINCIBLE_DURATION = 120
            ship.update();
        }
        assertFalse(ship.isInvincible(), "Invincibility should expire after duration");
    }

    @Test
    void testShootingWhileMoving() {
        // Move ship diagonally
        ship.moveRight();
        ship.moveUp();
        ship.update();

        // Shoot while moving
        ship.shoot();

        // Verify bullet trajectory matches ship's angle
        Projectile bullet = ship.getBullets().get(0);
        double bulletAngle = Math.toDegrees(Math.atan2(bullet.getVelocityY(), bullet.getVelocityX()));
        assertEquals(ship.getAngle() - 90, bulletAngle, DELTA, "Bullet angle should match ship's angle");
    }

    @Test
    void testBulletCleanup() {
        // Shoot multiple bullets
        for (int i = 0; i < 5; i++) {
            ship.shoot();
            ship.update();
        }

        int initialBulletCount = ship.getBullets().size();

        // Move bullets until they go off screen
        for (int i = 0; i < 100; i++) {
            ship.update();
        }

        assertTrue(ship.getBullets().size() < initialBulletCount,
                "Bullets should be removed when they go off screen");
    }

    @Test
    void testRapidFiring() {
        // Test rapid firing behavior
        for (int i = 0; i < 10; i++) {
            ship.setShooting(true);
            ship.update();
        }

        assertTrue(ship.getBullets().size() >= 10,
                "Should be able to fire multiple bullets in succession");

        // Verify each bullet has unique position
        for (int i = 0; i < ship.getBullets().size() - 1; i++) {
            Projectile bullet1 = ship.getBullets().get(i);
            Projectile bullet2 = ship.getBullets().get(i + 1);
            assertFalse(
                    bullet1.getX() == bullet2.getX() && bullet1.getY() == bullet2.getY(),
                    "Each bullet should have a unique position"
            );
        }
    }
}