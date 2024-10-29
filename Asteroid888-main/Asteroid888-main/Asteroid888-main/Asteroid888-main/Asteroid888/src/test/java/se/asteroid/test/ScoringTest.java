package se.asteroid.test;

import se.asteroid.controller.GameController;
import org.junit.jupiter.api.*;
import se.asteroid.model.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScoringTest {
    private GameController gamePanel;
    private Field scoreField;
    private Field playerField;
    private Field asteroidsField;
    private Field regularEnemiesField;
    private Field secondTierEnemiesField;
    private Field bossField;
    private Method checkBulletCollisions;

    @BeforeEach
    void setUp() throws Exception {
        gamePanel = new GameController();

        // Access private fields
        scoreField = GameController.class.getDeclaredField("score");
        playerField = GameController.class.getDeclaredField("player");
        asteroidsField = GameController.class.getDeclaredField("asteroids");
        regularEnemiesField = GameController.class.getDeclaredField("regularEnemies");
        secondTierEnemiesField = GameController.class.getDeclaredField("secondTierEnemies");
        bossField = GameController.class.getDeclaredField("boss");
        checkBulletCollisions = GameController.class.getDeclaredMethod("checkBulletCollisions", Projectile.class);

        // Make them accessible
        scoreField.setAccessible(true);
        playerField.setAccessible(true);
        asteroidsField.setAccessible(true);
        regularEnemiesField.setAccessible(true);
        secondTierEnemiesField.setAccessible(true);
        bossField.setAccessible(true);
        checkBulletCollisions.setAccessible(true);

        // Reset score
        scoreField.set(gamePanel, 0);
    }

    private int getScore() throws Exception {
        return (int) scoreField.get(gamePanel);
    }

    @Test
    @Order(1)
    @DisplayName("Test Small Asteroid Scoring")
    void testSmallAsteroidScore() throws Exception {
        // Get the asteroids list
        @SuppressWarnings("unchecked")
        List<Asteroid> asteroids = (List<Asteroid>) asteroidsField.get(gamePanel);

        // Create a small asteroid at a specific position
        Asteroid smallAsteroid = new Asteroid(100, 100, false);
        asteroids.add(smallAsteroid);

        // Create a bullet that will hit the asteroid
        Projectile bullet = new Projectile(100, 100, 0);

        // Simulate multiple hits until destroyed
        while (!smallAsteroid.isDestroyed()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        Assertions.assertEquals(1, getScore(), "Small asteroid should give 1 point");
    }

    @Test
    @Order(2)
    @DisplayName("Test Large Asteroid Scoring")
    void testLargeAsteroidScore() throws Exception {
        // Get the asteroids list
        @SuppressWarnings("unchecked")
        List<Asteroid> asteroids = (List<Asteroid>) asteroidsField.get(gamePanel);

        // Create a large asteroid
        Asteroid largeAsteroid = new Asteroid(100, 100, true);
        asteroids.add(largeAsteroid);

        // Create a bullet that will hit the asteroid
        Projectile bullet = new Projectile(100, 100, 0);

        // Simulate multiple hits until destroyed
        while (!largeAsteroid.isDestroyed()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        Assertions.assertEquals(2, getScore(), "Large asteroid should give 2 points");
    }

    @Test
    @Order(3)
    @DisplayName("Test Regular Enemy Scoring")
    void testRegularEnemyScore() throws Exception {
        // Get the regular enemies list
        @SuppressWarnings("unchecked")
        List<RegularEnemy> regularEnemies = (List<RegularEnemy>) regularEnemiesField.get(gamePanel);

        // Create a regular enemy
        RegularEnemy enemy = new RegularEnemy(100, 100, 0, 0, 0, 50);
        regularEnemies.add(enemy);

        // Create a bullet that will hit the enemy
        Projectile bullet = new Projectile(100, 100, 0);

        // Simulate multiple hits until destroyed
        while (!enemy.isDestroyed()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        Assertions.assertEquals(1, getScore(), "Regular enemy should give 1 point");
    }

    @Test
    @Order(4)
    @DisplayName("Test Second Tier Enemy Scoring")
    void testSecondTierEnemyScore() throws Exception {
        // Get the second tier enemies list
        @SuppressWarnings("unchecked")
        List<SecondTier> secondTierEnemies = (List<SecondTier>) secondTierEnemiesField.get(gamePanel);

        // Create a second tier enemy
        SecondTier enemy = new SecondTier(100, 100, 0, 0, 0, 75);
        secondTierEnemies.add(enemy);

        // Create a bullet that will hit the enemy
        Projectile bullet = new Projectile(100, 100, 0);

        // Simulate multiple hits until destroyed
        while (!enemy.isDestroyed()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        Assertions.assertEquals(2, getScore(), "Second tier enemy should give 2 points");
    }

    @Test
    @Order(5)
    @DisplayName("Test Boss Scoring")
    void testBossScore() throws Exception {
        // Create and set boss
        Ship player = new Ship(400, 300);
        Boss boss = new Boss(100, 100, player);
        bossField.set(gamePanel, boss);

        // Create a bullet that will hit the boss
        Projectile bullet = new Projectile(100, 100, 0);

        // Simulate multiple hits until destroyed
        while (boss.isAlive()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        Assertions.assertEquals(50, getScore(), "Boss should give 50 points");
    }

    @Test
    @Order(6)
    @DisplayName("Test Multiple Enemy Types Scoring")
    void testMultipleEnemiesScore() throws Exception {
        // Get all enemy lists
        @SuppressWarnings("unchecked")
        List<Asteroid> asteroids = (List<Asteroid>) asteroidsField.get(gamePanel);
        @SuppressWarnings("unchecked")
        List<RegularEnemy> regularEnemies = (List<RegularEnemy>) regularEnemiesField.get(gamePanel);
        @SuppressWarnings("unchecked")
        List<SecondTier> secondTierEnemies = (List<SecondTier>) secondTierEnemiesField.get(gamePanel);

        // Add one of each type
        Asteroid smallAsteroid = new Asteroid(100, 100, false);
        RegularEnemy regularEnemy = new RegularEnemy(200, 200, 0, 0, 0, 50);
        SecondTier secondTierEnemy = new SecondTier(300, 300, 0, 0, 0, 75);

        asteroids.add(smallAsteroid);
        regularEnemies.add(regularEnemy);
        secondTierEnemies.add(secondTierEnemy);

        // Create a bullet
        Projectile bullet = new Projectile(100, 100, 0);

        // Destroy small asteroid
        while (!smallAsteroid.isDestroyed()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        // Destroy regular enemy
        bullet = new Projectile(200, 200, 0);
        while (!regularEnemy.isDestroyed()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        // Destroy second tier enemy
        bullet = new Projectile(300, 300, 0);
        while (!secondTierEnemy.isDestroyed()) {
            checkBulletCollisions.invoke(gamePanel, bullet);
        }

        // Expected total: 1 + 1 + 2 = 4 points
        Assertions.assertEquals(4, getScore(), "Total score should be sum of all destroyed enemies");
    }
}