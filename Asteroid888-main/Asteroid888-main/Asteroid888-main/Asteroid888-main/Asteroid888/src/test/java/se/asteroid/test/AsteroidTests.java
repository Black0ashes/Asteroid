package se.asteroid.test;

import se.asteroid.model.Asteroid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class AsteroidTests {
    private Asteroid largeAsteroid;
    private Asteroid smallAsteroid;
    private static final double DELTA = 0.01;
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int LARGE_HITBOX = 100;  // Updated to match implementation
    private static final int SMALL_HITBOX = 45;   // Updated to match implementation

    @BeforeEach
    void setUp() {
        largeAsteroid = new Asteroid(400, 300, true);
        smallAsteroid = new Asteroid(400, 300, false);
    }

    @Test
    void testInitialization() {
        assertTrue(largeAsteroid.isLarge());
        assertEquals(100, largeAsteroid.getHealth());
        assertNotNull(largeAsteroid.getBounds());

        assertFalse(smallAsteroid.isLarge());
        assertEquals(50, smallAsteroid.getHealth());
        assertNotNull(smallAsteroid.getBounds());
    }

    @Test
    void testMovement() {
        double initialX = largeAsteroid.getX();
        double initialY = largeAsteroid.getY();

        largeAsteroid.update();

        assertNotEquals(initialX, largeAsteroid.getX(), "Asteroid should move in X direction");
        assertNotEquals(initialY, largeAsteroid.getY(), "Asteroid should move in Y direction");
    }

    @Test
    void testScreenBounce() {
        // Test right boundary bounce
        largeAsteroid = new Asteroid(SCREEN_WIDTH - 10, 300, true);
        double initialVelocityX = largeAsteroid.getVelocityX();
        largeAsteroid.update();

        // Only test bounce if moving toward boundary
        if (initialVelocityX > 0) {
            assertTrue(largeAsteroid.getVelocityX() < 0, "Should bounce off right wall");
        }

        // Test bottom boundary bounce
        largeAsteroid = new Asteroid(400, SCREEN_HEIGHT - 10, true);
        double initialVelocityY = largeAsteroid.getVelocityY();
        largeAsteroid.update();

        // Only test bounce if moving toward boundary
        if (initialVelocityY > 0) {
            assertTrue(largeAsteroid.getVelocityY() < 0, "Should bounce off bottom wall");
        }
    }

    @Test
    void testVelocityRanges() {
        // Test velocity ranges for large asteroids
        for (int i = 0; i < 100; i++) {
            Asteroid asteroid = new Asteroid(400, 300, true);
            assertTrue(Math.abs(asteroid.getVelocityX()) <= 0.5,
                    "Large asteroid X velocity should be within range");
            assertTrue(Math.abs(asteroid.getVelocityY()) <= 0.5,
                    "Large asteroid Y velocity should be within range");
        }

        // Test velocity ranges for small asteroids
        for (int i = 0; i < 100; i++) {
            Asteroid asteroid = new Asteroid(400, 300, false);
            assertTrue(Math.abs(asteroid.getVelocityX()) <= 1.0,
                    "Small asteroid X velocity should be within range");
            assertTrue(Math.abs(asteroid.getVelocityY()) <= 1.0,
                    "Small asteroid Y velocity should be within range");
        }
    }

    @Test
    void testRotation() {
        double initialRotation = largeAsteroid.rotationAngle;
        largeAsteroid.update();
        assertNotEquals(initialRotation, largeAsteroid.rotationAngle,
                "Asteroid should rotate");
    }

    @Test
    void testBoundaryContainment() {
        Asteroid asteroid = new Asteroid(0, 0, true);
        asteroid.update();
        Rectangle bounds = asteroid.getBounds();

        assertTrue(bounds.x >= 0, "Asteroid should not go beyond left boundary");
        assertTrue(bounds.y >= 0, "Asteroid should not go beyond top boundary");

        asteroid = new Asteroid(SCREEN_WIDTH, SCREEN_HEIGHT, true);
        asteroid.update();
        bounds = asteroid.getBounds();

        assertTrue(bounds.x + bounds.width <= SCREEN_WIDTH,
                "Asteroid should not go beyond right boundary");
        assertTrue(bounds.y + bounds.height <= SCREEN_HEIGHT,
                "Asteroid should not go beyond bottom boundary");
    }

    @Test
    void testHealthManagement() {
        assertEquals(100, largeAsteroid.getHealth(), "Large asteroid should start with full health");
        assertEquals(50, smallAsteroid.getHealth(), "Small asteroid should start with full health");

        largeAsteroid.hit();
        assertEquals(90, largeAsteroid.getHealth(), "Health should decrease by 10 when hit");

        for (int i = 0; i < 5; i++) {
            smallAsteroid.hit();
        }
        assertEquals(0, smallAsteroid.getHealth(), "Health should be 0 after sufficient hits");
        assertTrue(smallAsteroid.isDestroyed(), "Asteroid should be destroyed when health reaches 0");
    }
}