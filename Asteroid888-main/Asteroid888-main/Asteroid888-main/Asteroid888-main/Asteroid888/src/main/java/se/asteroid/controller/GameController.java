package se.asteroid.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.asteroid.model.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GameController extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    //Start screen
    private boolean gameStarted = false;
    private boolean showStartMenu = true;
    private Timer timer;
    private Ship player;
    private List<Asteroid> asteroids;
    private List<RegularEnemy> regularEnemies;
    private List<SecondTier> secondTierEnemies;
    private Boss boss;
    private Image backgroundImage;
    private Set<Integer> activeKeys;
    private List<Explosion> explosions;
    // Game state variables
    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;
    private boolean gameSucceeded = false;
    private String gameOverMessage = "";
    private String gameSucceedMessage = "YOU WIN! Final Score: ";
    private boolean isExploding = false;
    private int explosionTicks = 0;
    private static final int EXPLOSION_DURATION = 60;

    // Boss phase variables
    private boolean bossPhaseStarted = false;
    private boolean bossDefeated = false;
    //On win screen
    private float endingAlpha = 0f;
    private Timer endingTimer;

    private static final Logger logger = LogManager.getLogger(GameController.class);

    public GameController() {
        setDoubleBuffered(true);
        this.setPreferredSize(new Dimension(800, 600));
        setupInitialState();
        try {
            backgroundImage = ImageIO.read(getClass().getResource("/assets/background.gif"));
            if (backgroundImage == null) {
                System.out.println("Failed to load background image.");
            }
        } catch (IOException e) {
            System.out.println("Error loading background image: " + e.getMessage());
            e.printStackTrace();
        }
        setDoubleBuffered(true); // Enable double buffering to prevent flickering
        timer = new Timer(16, this); // 60 FPS
        timer.start();

        // Add input listeners
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        addKeyListener(this);


        // Initialize ending timer
        endingTimer = new Timer(16, e -> {
            if (gameSucceeded) {
                endingAlpha = Math.min(1f, endingAlpha + 0.02f);
                repaint();
            }
        });
        endingTimer.start();

        // Mouse controls for rotation
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!gameOver && gameStarted) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        player.rotateLeft();
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        player.rotateRight();
                    }
                }
            }
        });
    }
    private void setupInitialState() {
        // Initialize empty collections
        asteroids = new ArrayList<>();
        regularEnemies = new ArrayList<>();
        secondTierEnemies = new ArrayList<>();
        explosions = new ArrayList<>();
        activeKeys = new HashSet<>();

        // Set initial game state
        gameStarted = false;
        showStartMenu = true;
        gameOver = false;
        bossPhaseStarted = false;
        bossDefeated = false;
        isExploding = false;
    }
    private void initializeGame() {
        score = 0;
        logger.info("Game started. Score : 0");
        player = new Ship(400, 300);
        player.setInvincible(true);
        asteroids = new ArrayList<>();
        regularEnemies = new ArrayList<>();
        secondTierEnemies = new ArrayList<>();
        explosions = new ArrayList<>();
        boss = null;
        activeKeys = new HashSet<>();
        score = 0;
        lives = 3;
        gameOver = false;
        bossPhaseStarted = false;
        bossDefeated = false;
        isExploding = false;
        gameStarted = true;
        showStartMenu = false;

        // Spawn initial enemies
        spawnAsteroids();
        spawnRegularEnemies();
        spawnSecondTierEnemies();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (showStartMenu) {
                repaint();
                return;
            }

            if (gameOver || !gameStarted) return;

            if (isExploding) {
                handleExplosion();
                return;
            }

            handlePlayerMovement();
            updateGameObjects();
            checkCollisions();
            checkBossSpawning();

            repaint();
        } catch (Exception ex) {
            System.out.println("An error occurred during the game loop: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handlePlayerMovement() {
        if (activeKeys.contains(KeyEvent.VK_W)) player.moveUp();
        if (activeKeys.contains(KeyEvent.VK_S)) player.moveDown();
        if (activeKeys.contains(KeyEvent.VK_A)) player.moveLeft();
        if (activeKeys.contains(KeyEvent.VK_D)) player.moveRight();
    }

    private void updateGameObjects() {
        try {
            if (player != null) {
                player.update();
            } else {
                throw new NullPointerException("Player object is null.");
            }

            explosions.removeIf(explosion -> {
                if (explosion != null) {
                    explosion.update();
                    return explosion.isFinished();
                } else {
                    return true; // Remove null entries
                }
            });

            // Update regular enemies and their bullets
            for (RegularEnemy enemy : regularEnemies) {
                if (enemy != null) {
                    enemy.setTarget(player);
                    enemy.update();
                    updateEnemyBullets(enemy.getBullets());
                }
            }

            // Update second tier enemies
            for (SecondTier enemy : secondTierEnemies) {
                if (enemy != null) {
                    enemy.setTarget(player);
                    enemy.update();
                    updateEnemyBullets(enemy.getBullets());
                }
            }

            // Update asteroids
            for (Asteroid asteroid : asteroids) {
                if (asteroid != null) {
                    asteroid.update();
                }
            }

            // Update boss if present
            if (boss != null && boss.isAlive()) {
                boss.update();
            }

        } catch (NullPointerException e) {
            System.err.println("A required game object is missing: " + e.getMessage());
        } catch (ConcurrentModificationException e) {
            System.err.println("List was modified during iteration: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }


    private void updateEnemyBullets(List<Projectile> projectiles) {
        projectiles.removeIf(bullet -> bullet.isOffScreen(getWidth(), getHeight()));
        for (Projectile projectile : projectiles) {
            projectile.update();
        }
    }

    private void checkBossSpawning() {
        if (!bossPhaseStarted && asteroids.isEmpty() && regularEnemies.isEmpty() &&
                secondTierEnemies.isEmpty()) {
            startBossPhase();
        }
    }

    private void startBossPhase() {
        try {
            if (player == null) {
                throw new NullPointerException("Player is null. Boss cannot be created without a player.");
            }

            bossPhaseStarted = true;
            boss = new Boss(400, 300, player);

        } catch (NullPointerException e) {
            System.err.println("Failed to start boss phase: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred in startBossPhase: " + e.getMessage());
        }
    }


    private void checkCollisions() {
        if (isExploding) return;

        Rectangle playerBounds = player.getBounds();

        // Check player bullets with enemies and boss
        for (int i = player.getBullets().size() - 1; i >= 0; i--) {
            Projectile projectile = player.getBullets().get(i);
            boolean bulletHit = checkBulletCollisions(projectile);
            if (bulletHit) {
                player.getBullets().remove(i);
            }
        }

        if (!player.isInvincible()) {
            // Check enemy bullets with player
            checkEnemyCollisionsWithPlayer(playerBounds);

            // Check boss bullets with player
            if (boss != null && boss.isAlive()) {
                for (Projectile projectile : boss.getBullets()) {
                    if (projectile.getBounds().intersects(playerBounds)) {
                        startExplosion();
                        return;
                    }
                }
            }
        }if (isExploding) return;

    }

    private boolean checkBulletCollisions(Projectile projectile) {
        // Check asteroid collisions
        for (int j = asteroids.size() - 1; j >= 0; j--) {
            Asteroid asteroid = asteroids.get(j);
            if (projectile.getBounds().intersects(asteroid.getBounds())) {
                explosions.add(new Explosion(projectile.getX(), projectile.getY()));
                asteroid.hit();
                if (asteroid.isDestroyed()) {
                    int points = asteroid.isLarge() ? 2 : 1;
                    score += asteroid.isLarge() ? 2 : 1;
                    logger.info("Score increased by {} points - Asteroid destroyed. Current score: {}",
                            points,score);
                    asteroids.remove(j);
                }
                return true;
            }
        }

        // Check regular enemy collisions
        for (int j = regularEnemies.size() - 1; j >= 0; j--) {
            RegularEnemy enemy = regularEnemies.get(j);
            if (projectile.getBounds().intersects(enemy.getBounds())) {
                explosions.add(new Explosion(projectile.getX(), projectile.getY()));
                enemy.hit();
                if (enemy.isDestroyed()) {
                    score += 1;
                    logger.info("Score increased by 1 point - Regular enemy destroyed. Current score: {}",
                            score);
                    regularEnemies.remove(j);
                }
                return true;
            }
        }

        // Check second tier enemy collisions
        for (int j = secondTierEnemies.size() - 1; j >= 0; j--) {
            SecondTier enemy = secondTierEnemies.get(j);
            if (projectile.getBounds().intersects(enemy.getBounds())) {
                explosions.add(new Explosion(projectile.getX(), projectile.getY()));
                enemy.hit();
                if (enemy.isDestroyed()) {
                    score += 2;
                    logger.info("Score increased by 2 points - Second tier enemy destroyed. Current score: {}",
                            score);
                    secondTierEnemies.remove(j);
                }
                return true;
            }
        }

        // Check boss collision
        if (boss != null && boss.isAlive() && projectile.getBounds().intersects(boss.getBounds())) {
            explosions.add(new Explosion(projectile.getX(), projectile.getY()));
            boss.hit(10);
            if (!boss.isAlive()) {
                score += 50;
                logger.info("Score increased by 50 points - Boss defeated! Final score: {}",
                        score);
                bossDefeated = true;
                gameSucceeded = true;
                gameOver=true;
                gameSucceedMessage += score;
            }
            return true;
        }

        return false;
    }

    private void checkEnemyCollisionsWithPlayer(Rectangle playerBounds) {
        // ถ้าผู้เล่นกำลัง invincible ให้ข้ามการเช็ค
        if (player.isInvincible()) return;

        // Check regular enemy bullets
        for (RegularEnemy enemy : regularEnemies) {
            if (checkEnemyBulletsWithPlayer(enemy.getBullets(), playerBounds) ||
                    playerBounds.intersects(enemy.getBounds())) {
                startExplosion();
                return;
            }
        }

        // Check second tier enemy bullets
        for (SecondTier enemy : secondTierEnemies) {
            if (checkEnemyBulletsWithPlayer(enemy.getBullets(), playerBounds) ||
                    playerBounds.intersects(enemy.getBounds())) {
                startExplosion();
                return;
            }
        }

        // Check asteroid collisions
        for (Asteroid asteroid : asteroids) {
            if (playerBounds.intersects(asteroid.getBounds())) {
                startExplosion();
                return;
            }
        }
    }

    private boolean checkEnemyBulletsWithPlayer(List<Projectile> projectiles, Rectangle playerBounds) {
        for (Projectile projectile : projectiles) {
            if (projectile.getBounds().intersects(playerBounds)) {
                return true;
            }
        }
        return false;
    }

    private void startExplosion() {
        isExploding = true;
        explosionTicks = 0;
    }

    private void handleExplosion() {
        explosionTicks++;
        if (explosionTicks >= EXPLOSION_DURATION) {
            isExploding = false;
            explosionTicks = 0;
            lives--;

            if (lives <= 0) {
                gameOver = true;
                repaint();
                logger.info("Game Over. Final score: {}", score);
                gameOverMessage = "GAME OVER - Final Score: " + score;

            } else {
                logger.info("Player lost a life. Lives remaining: {}", lives);
                player = new Ship(400, 300);
                player.setInvincible(true);
                if (boss != null) {
                    boss.setTarget(player);
                }
            }
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g); // Directly call paint without clearing the background
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Clear the previous frame
        Graphics2D g2d = (Graphics2D) g;

        // Draw background only once per frame
        g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

        if (showStartMenu) {
            drawStartMenu(g2d);
            return;
        }

        if (!gameOver && gameStarted) {
            if (!isExploding) {
                player.draw(g2d);
            } else {
                drawExplosion(g2d);
            }

            for (Asteroid asteroid : asteroids) {
                asteroid.draw(g2d);
            }
            for (RegularEnemy enemy : regularEnemies) {
                enemy.draw(g2d);
            }
            for (SecondTier enemy : secondTierEnemies) {
                enemy.draw(g2d);
            }

            if (boss != null && boss.isAlive()) {
                boss.draw(g2d);
            }

            for (Explosion explosion : explosions) {
                explosion.draw(g2d);
            }

            drawHUD(g2d);

        } else if (gameOver) {
            if (bossDefeated) {
                drawGameSucceeded(g2d);
            } else {
                drawGameOver(g2d);
            }
        }
    }

    private void drawStartMenu(Graphics2D g2d) {
        // Draw the original background image
        g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "ASTEROID888";
        FontMetrics titleMetrics = g2d.getFontMetrics();
        int titleX = (getWidth() - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, getHeight() / 4);

        // "Press 'P' to Start" text
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String startText = "Press 'P' to Start";
        FontMetrics startMetrics = g2d.getFontMetrics();
        int startX = (getWidth() - startMetrics.stringWidth(startText)) / 2;
        g2d.drawString(startText, startX, getHeight() / 2);

        // Adjusted box dimensions to fit all text comfortably
        int boxWidth = 320;
        int boxHeight = 200;
        int boxX = (getWidth() - boxWidth) / 2;
        int boxY = getHeight() / 2 + 40;

        // Draw the semi-transparent box behind the controls text
        g2d.setColor(new Color(0, 0, 0, 150));  // Semi-transparent black
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);

        g2d.setColor(Color.YELLOW);
        g2d.drawOval(getWidth() / 2 - 50, getHeight() / 3 - 80, 100, 100);
        // Controls text
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.setColor(Color.CYAN);
        String[] controls = {
                "Controls:",
                "W,A,S,D : Move",
                "Mouse : Rotate",
                "SPACE : Shoot",
                "E - Missile"

        };

        // Draw each line of control text within the box, centered horizontally
        int yOffset = boxY + 35;  // Start drawing text slightly below the top of the box for padding
        for (String control : controls) {
            FontMetrics controlMetrics = g2d.getFontMetrics();
            int controlX = boxX + (boxWidth - controlMetrics.stringWidth(control)) / 2;
            g2d.drawString(control, controlX, yOffset);
            yOffset += 30;  // Adjust line spacing to fit all text within the box comfortably
        }
    }


    private void drawExplosion(Graphics2D g2d) {
        g2d.setColor(Color.ORANGE);
        int size = 40 + (explosionTicks / 2);
        g2d.fillOval((int)player.getX() - size/2, (int)player.getY() - size/2, size, size);
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Lives: " + lives, 20, 60);

        if (bossPhaseStarted && boss != null && boss.isAlive()) {
            g2d.drawString("BOSS BATTLE", getWidth()/2 - 60, 30);
        }
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(gameOverMessage);
        g2d.drawString(gameOverMessage, (getWidth() - textWidth) / 2, getHeight() / 2);
    }


    private void drawGameSucceeded(Graphics2D g2d) {
        // Gradient overlay for the background
        GradientPaint gradientOverlay = new GradientPaint(
                0, 0, new Color(0, 0, 50, 180),
                0, getHeight(), new Color(0, 0, 0, 200)
        );
        g2d.setPaint(gradientOverlay);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Title Text - Mission Completed with a smaller shadow effect
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String victoryText = "MISSION COMPLETED";
        FontMetrics victoryMetrics = g2d.getFontMetrics();
        int victoryX = (getWidth() - victoryMetrics.stringWidth(victoryText)) / 2;
        int victoryY = getHeight() / 3;

        // Simple drop shadow effect for readability
        g2d.setColor(new Color(0, 0, 0, 100));  // Dark shadow with lower opacity
        g2d.drawString(victoryText, victoryX + 2, victoryY + 2);

        // Draw main title text in bright white
        g2d.setColor(Color.WHITE);
        g2d.drawString(victoryText, victoryX, victoryY);

        // Optional: Draw a subtle outline around the text for added clarity
        g2d.setColor(new Color(200, 200, 255, 150));  // Light blue outline color
        g2d.drawString(victoryText, victoryX - 1, victoryY - 1);
        g2d.drawString(victoryText, victoryX + 1, victoryY + 1);

        // Final Score Text with color gradient centered on the screen
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        String scoreText = "Final Score: " + score;
        FontMetrics scoreMetrics = g2d.getFontMetrics();
        int scoreX = (getWidth() - scoreMetrics.stringWidth(scoreText)) / 2;
        int scoreY = getHeight() / 2;

        // Set color gradient for score text
        GradientPaint scoreGradient = new GradientPaint(
                scoreX, scoreY, Color.CYAN,
                scoreX + scoreMetrics.stringWidth(scoreText), scoreY, Color.MAGENTA
        );
        g2d.setPaint(scoreGradient);
        g2d.drawString(scoreText, scoreX, scoreY);

        // Restart Prompt below the Final Score
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.setColor(Color.LIGHT_GRAY);
        String continueText = "Press 'SPACE' to restart";
        FontMetrics continueMetrics = g2d.getFontMetrics();
        int continueX = (getWidth() - continueMetrics.stringWidth(continueText)) / 2;
        int continueY = scoreY + 50;
        g2d.drawString(continueText, continueX, continueY);

        // Draw sparkling stars with reduced frequency
        drawSparklingStars(g2d, 8);  // Adjusted star count for less clutter
    }

    // Helper method to draw sparkling stars with reduced frequency
    private void drawSparklingStars(Graphics2D g2d, int numStars) {
        for (int i = 0; i < numStars; i++) {
            int x = (int) (Math.random() * getWidth());
            int y = (int) (Math.random() * getHeight());
            int size = 2 + (int) (Math.random() * 3);  // Random star size

            g2d.setColor(new Color(255, 255, 255, (int) (Math.random() * 100 + 155)));  // Random opacity
            g2d.fillOval(x, y, size, size);
        }
    }

    // Key Listeners
    @Override
    public void keyPressed(KeyEvent e) {
        if (showStartMenu && e.getKeyCode() == KeyEvent.VK_P) {
            initializeGame();
            return;
        }

        // เพิ่มการตรวจจับปุ่ม SPACE สำหรับเริ่มเกมใหม่
        if (gameSucceeded && e.getKeyCode() == KeyEvent.VK_SPACE) {
            endingAlpha = 0f;
            showStartMenu = true;
            gameSucceeded = false;
            return;
        }

        if (!gameOver && gameStarted) {
            activeKeys.add(e.getKeyCode());
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                player.setShooting(true);
            }
            if (e.getKeyCode() == KeyEvent.VK_E) {
                Timer timer = new Timer(300, new ActionListener() { // Adjust delay in ms (e.g., 100 ms)
                    int shotsFired = 0;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (shotsFired < 4) {
                            player.fireMissile(asteroids, regularEnemies, secondTierEnemies, boss);
                            shotsFired++;
                        } else {
                            ((Timer) e.getSource()).stop(); // Stop the timer after firing 4 missiles
                        }
                    }
                });
                timer.setRepeats(true);
                timer.start();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        activeKeys.remove(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            player.setShooting(false);
        }
    }

    // Required method implementations
    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}

    private void spawnAsteroids() {
        for (int i = 0; i < 3; i++) {
            asteroids.add(new Asteroid(Math.random() * 800, Math.random() * 600, true));
        }
        for (int i = 0; i < 5; i++) {
            asteroids.add(new Asteroid(Math.random() * 800, Math.random() * 600, false));
        }
    }

    private void spawnRegularEnemies() {
        for (int i = 0; i < 4; i++) {
            double x = Math.random() * 800;
            double y = Math.random() * 600;
            double velocityX = Math.random() * 2 - 1;
            double velocityY = Math.random() * 2 - 1;
            RegularEnemy enemy = new RegularEnemy(x, y, velocityX, velocityY, 0, 50);
            enemy.setTarget(player);
            regularEnemies.add(enemy);
        }
    }

    private void spawnSecondTierEnemies() {
        for (int i = 0; i < 3; i++) {
            double x = Math.random() * 800;
            double y = Math.random() * 600;
            double velocityX = Math.random() * 2 - 1;
            double velocityY = Math.random() * 2 - 1;
            SecondTier enemy = new SecondTier(x, y, velocityX, velocityY, 0, 75);
            enemy.setTarget(player);
            secondTierEnemies.add(enemy);
        }
    }
}