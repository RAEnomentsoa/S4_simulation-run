import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CarSimulationApp extends JFrame {
    static class Car {
        int id;
        String name;
        double acceleration;
        double maxSpeed;

        public Car(int id, String name, double acceleration, double maxSpeed) {
            this.id = id;
            this.name = name;
            this.acceleration = acceleration;
            this.maxSpeed = maxSpeed;
        }

        @Override
        public String toString() {
            return name.toUpperCase() + " (" + maxSpeed + " km/h max)";
        }
    }

    private static final int TARGET_DISTANCE = 400; // 400 meters
    private static final int FPS = 60; // frames per second for smooth simulation
    private static final double DT = 1.0 / FPS; // time step in seconds

    // Physics state
    private double speedMpS = 0.0; // speed in meters per second
    private double distance = 0.0; // distance in meters
    private double carRaceTime = 0.0; // time the car has been racing

    // Timer state (independent of the car)
    private double timerValue = -5.0; // independent timer starts at -5 seconds
    private boolean isTimerRunning = false;

    // Config state
    private List<Car> availableCars = new ArrayList<>();
    private double maxSpeedKmH = 200.0; // default max speed
    private double accelerationKmHS = 15.0; // default acceleration in km/h per second
    private boolean isHoldingAccelerate = false;
    private boolean hasFinished = false;

    // UI Components
    private TrackPanel trackPanel;
    private SpeedometerPanel speedometerPanel;
    private DashboardPanel dashboardPanel;
    private JComboBox<Car> carSelector;
    private JButton accelerateBtn;

    private Timer gameLoop;
    private DecimalFormat timeDf = new DecimalFormat("0.00");

    // Color scheme
    private static final Color PRIMARY_COLOR = new Color(0, 242, 204); // Bright cyan
    private static final Color SECONDARY_COLOR = new Color(255, 64, 129); // Hot pink
    private static final Color DARK_BG = new Color(12, 17, 30); // Deep navy
    private static final Color ACCENT_COLOR = new Color(100, 200, 255); // Light blue

    public CarSimulationApp() {
        setTitle("DRAG RACE 400M");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(DARK_BG);
        setLayout(new BorderLayout(0, 0));

        loadConfig();

        // Top: Track Panel with modern styling
        trackPanel = new TrackPanel();
        trackPanel.setPreferredSize(new Dimension(1000, 120));
        add(trackPanel, BorderLayout.NORTH);

        // Main content area
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBackground(DARK_BG);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left: Dashboard
        dashboardPanel = new DashboardPanel();
        dashboardPanel.setPreferredSize(new Dimension(220, 500));
        contentPanel.add(dashboardPanel, BorderLayout.WEST);

        // Center: Speedometer
        JPanel speedometerWrapper = new JPanel(new BorderLayout()); // Use BorderLayout instead of GridBagLayout
        speedometerWrapper.setBackground(DARK_BG);
        speedometerPanel = new SpeedometerPanel();
        speedometerPanel.setPreferredSize(new Dimension(400, 400));
        speedometerWrapper.add(speedometerPanel, BorderLayout.CENTER);
        contentPanel.add(speedometerWrapper, BorderLayout.CENTER);

        // Right: Controls
        JPanel rightPanel = createControlPanel();
        rightPanel.setPreferredSize(new Dimension(200, 500));
        contentPanel.add(rightPanel, BorderLayout.EAST);

        add(contentPanel, BorderLayout.CENTER);

        setupListeners();

        // Setup game loop
        gameLoop = new Timer(1000 / FPS, e -> updatePhysics());
        gameLoop.start();

        setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DARK_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        // Title
        JLabel titleLabel = new JLabel("CONTROLS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));

        // Start Button
        JButton startBtn = createModernButton("START", ACCENT_COLOR);
        startBtn.addActionListener(e -> isTimerRunning = true);
        panel.add(startBtn);
        panel.add(Box.createVerticalStrut(8));

        // Stop Button
        JButton stopBtn = createModernButton("STOP", new Color(255, 100, 100));
        stopBtn.addActionListener(e -> isTimerRunning = false);
        panel.add(stopBtn);
        panel.add(Box.createVerticalStrut(8));

        // Reset Button
        JButton resetBtn = createModernButton("RESET", new Color(150, 150, 150));
        resetBtn.addActionListener(e -> {
            isTimerRunning = false;
            timerValue = -5.0;
            distance = 0.0;
            speedMpS = 0.0;
            carRaceTime = 0.0;
            hasFinished = false;
            isHoldingAccelerate = false;
            dashboardPanel.updateDashboard(timerValue, carRaceTime, hasFinished);
        });
        panel.add(resetBtn);
        panel.add(Box.createVerticalStrut(20));

        // Timer state (independent of the car)
        JLabel timerLabel = new JLabel("Time: -5.00 s");
        panel.add(Box.createVerticalStrut(20));

        JLabel carLabel = new JLabel("SELECT CAR");
        carLabel.setFont(new Font("Arial", Font.BOLD, 12));
        carLabel.setForeground(PRIMARY_COLOR);
        carLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(carLabel);
        panel.add(Box.createVerticalStrut(5));

        carSelector = new JComboBox<>(availableCars.toArray(new Car[0]));
        carSelector.setMaximumSize(new Dimension(180, 30));
        carSelector.setBackground(DARK_BG);
        carSelector.setForeground(Color.WHITE);
        carSelector.addActionListener(e -> {
            Car selected = (Car) carSelector.getSelectedItem();
            if (selected != null) {
                selectCar(selected);
            }
        });
        panel.add(carSelector);

        // Accelerate Button
        panel.add(Box.createVerticalStrut(20));
        accelerateBtn = new JButton("ACCELERATE");
        accelerateBtn.setFont(new Font("Arial", Font.BOLD, 18));
        accelerateBtn.setBackground(SECONDARY_COLOR);
        accelerateBtn.setForeground(Color.WHITE);
        accelerateBtn.setFocusPainted(false);
        accelerateBtn.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        accelerateBtn.setOpaque(true);
        accelerateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(accelerateBtn);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JButton createModernButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void loadConfig() {
        File configFile = new File("car_config.txt");
        if (!configFile.exists()) {
            try (PrintWriter out = new PrintWriter(configFile)) {
                out.println("1,ferrari,15,200");
                out.println("2,porsche,20,250");
                out.println("3,nissan,12,180");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String name = parts[1].trim();
                        double accel = Double.parseDouble(parts[2].trim());
                        double maxSpeed = Double.parseDouble(parts[3].trim());
                        availableCars.add(new Car(id, name, accel, maxSpeed));
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping invalid line: " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load config, using defaults.");
        }

        if (availableCars.isEmpty()) {
            availableCars.add(new Car(1, "ferrari", 15, 200));
        }
        selectCar(availableCars.get(0));
    }

    private void selectCar(Car car) {
        this.accelerationKmHS = car.acceleration;
        this.maxSpeedKmH = car.maxSpeed;
        System.out.println(
                "Selected " + car.name + " - Max Speed: " + maxSpeedKmH + " km/h, Acceleration: " + accelerationKmHS
                        + " km/h/s");
    }

    private void setupListeners() {
        accelerateBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isHoldingAccelerate = true;
                accelerateBtn.setBackground(new Color(255, 100, 150));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isHoldingAccelerate = false;
                accelerateBtn.setBackground(SECONDARY_COLOR);
            }
        });
    }

    private void updatePhysics() {
        // Independent timer updates if running
        if (isTimerRunning) {
            timerValue += DT;
        }

        // Car updates continuously regardless of timer state
        if (!hasFinished) {
            // Measure race time if the car is currently moving or user starts accelerating
            if (isHoldingAccelerate || speedMpS > 0) {
                carRaceTime += DT;
            }

            // Apply acceleration if button is held
            if (isHoldingAccelerate) {
                // convert acceleration from km/h per second to m/s^2
                double accelerationMpS2 = accelerationKmHS * (1000.0 / 3600.0);
                speedMpS += accelerationMpS2 * DT;

                // cap at max speed
                double maxSpeedMpS = maxSpeedKmH * (1000.0 / 3600.0);
                if (speedMpS > maxSpeedMpS) {
                    speedMpS = maxSpeedMpS;
                }
            }

            // Update distance based on current speed
            distance += speedMpS * DT;

            // Check finish line
            if (distance >= TARGET_DISTANCE) {
                distance = TARGET_DISTANCE;
                hasFinished = true;
                saveRaceResults();
            }
        }

        updateUIState();
    }

    private void saveRaceResults() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("resultats.txt", true))) {
            Car selectedCar = (Car) carSelector.getSelectedItem();
            String carName = selectedCar != null ? selectedCar.name : "Unknown Car";
            writer.println("Name: " + carName + ", Distance: " + TARGET_DISTANCE + "m, Time: "
                    + timeDf.format(carRaceTime) + "s, Max Speed: " + maxSpeedKmH + " km/h");
        } catch (IOException e) {
            System.err.println("Error saving results: " + e.getMessage());
        }
    }

    private void updateUIState() {
        trackPanel.setCarPosition(distance / TARGET_DISTANCE);
        trackPanel.repaint();

        double currentSpeedKmH = speedMpS * 3.6; // convert m/s to km/h
        speedometerPanel.setSpeed(currentSpeedKmH);
        speedometerPanel.setDistance(distance);
        speedometerPanel.repaint();
        dashboardPanel.updateDashboard(timerValue, carRaceTime, hasFinished, timeDf);
    }

    // Inner class for the top track view with modern styling
    class TrackPanel extends JPanel {
        private double progress = 0.0; // 0.0 to 1.0
        private int animationFrame = 0;

        public void setCarPosition(double progress) {
            this.progress = Math.max(0.0, Math.min(1.0, progress));
            animationFrame++;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int trackLength = width - 120;
            int startX = 60;

            // Draw animated grid pattern background
            g2d.setColor(new Color(20, 30, 50));
            for (int i = 0; i < width; i += 30) {
                g2d.drawLine(i + (animationFrame % 30), 0, i + (animationFrame % 30), height);
            }

            // Draw track with gradient
            g2d.setColor(new Color(30, 40, 60));
            g2d.fillRect(startX - 5, height / 2 - 25, trackLength + 10, 50);

            // Draw lane marks
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    10, new float[] { 10, 10 }, 0));
            g2d.setColor(new Color(100, 150, 200));
            g2d.drawLine(startX, height / 2, startX + trackLength, height / 2);

            // Draw start and finish marks with neon glow
            g2d.setStroke(new BasicStroke(6));
            g2d.setColor(ACCENT_COLOR);
            g2d.drawLine(startX, height / 2 - 20, startX, height / 2 + 20);

            g2d.setColor(SECONDARY_COLOR);
            g2d.drawLine(startX + trackLength, height / 2 - 20, startX + trackLength, height / 2 + 20);

            // Labels
            g2d.setColor(PRIMARY_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("START", startX - 30, height / 2 - 35);
            g2d.drawString(TARGET_DISTANCE + "m", startX + trackLength - 30, height / 2 - 35);

            // Draw Car with enhanced style
            int carX = startX + (int) (progress * trackLength);
            drawCar(g2d, carX, height / 2);

            // Progress percentage
            g2d.setColor(PRIMARY_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(String.format("%.0f%%", progress * 100), carX - 20, height / 2 + 50);
        }

        private void drawCar(Graphics2D g2d, int x, int y) {
            // Car body
            g2d.setColor(SECONDARY_COLOR);
            RoundRectangle2D carBody = new RoundRectangle2D.Double(x - 30, y - 18, 60, 36, 8, 8);
            g2d.fill(carBody);

            // Windows
            g2d.setColor(ACCENT_COLOR);
            g2d.fillRect(x - 22, y - 12, 16, 10);
            g2d.fillRect(x + 6, y - 12, 16, 10);

            // Wheels
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 26, y + 12, 12, 12);
            g2d.fillOval(x + 14, y + 12, 12, 12);

            // Neon outline
            g2d.setColor(PRIMARY_COLOR);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(carBody);
        }
    }

    // Dashboard panel for timer, distance and results
    class DashboardPanel extends JPanel {
        private String timerText = "Time: -5.00 s";
        private String raceText = "Ready...";
        private String distanceText = "0/" + TARGET_DISTANCE + " m";

        public void updateDashboard(double timerValue, double carRaceTime, boolean finished, DecimalFormat format) {
            timerText = "Time: " + format.format(timerValue) + " s";
            distanceText = (int) distance + "/" + TARGET_DISTANCE + " m";
            if (finished) {
                raceText = "FINISHED! " + format.format(carRaceTime) + " s";
            } else {
                raceText = carRaceTime > 0 ? "Racing: " + format.format(carRaceTime) + " s" : "Ready...";
            }
            repaint();
        }

        public void updateDashboard(double timerValue, double carRaceTime, boolean finished) {
            updateDashboard(timerValue, carRaceTime, finished, timeDf);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background with gradient
            GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 30, 50),
                    width, height, new Color(15, 20, 35));
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, width, height, 15, 15);

            // Border
            g2d.setColor(PRIMARY_COLOR);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(0, 0, width - 1, height - 1, 15, 15);

            // Timer section
            g2d.setColor(PRIMARY_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("TIMER", 15, 40);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 28));
            g2d.drawString(timerText.split(": ")[1], 15, 85);

            // Distance section
            g2d.setColor(PRIMARY_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawLine(15, 110, width - 15, 110);
            g2d.drawString("DISTANCE", 15, 140);

            g2d.setColor(ACCENT_COLOR);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 26));
            g2d.drawString(distanceText, 15, 180);

            // Race section
            g2d.setColor(PRIMARY_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawLine(15, 200, width - 15, 200);
            g2d.drawString("STATUS", 15, 225);

            g2d.setColor(SECONDARY_COLOR);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2d.drawString(raceText, 15, 255);

            // Stats
            g2d.setColor(PRIMARY_COLOR);
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            g2d.drawString("Max: " + (int) maxSpeedKmH + " km/h", 15, 290);
            g2d.drawString("Accel: " + accelerationKmHS + " km/h/s", 15, 310);
        }
    }

    // Traditional car dashboard speedometer with odometer
    class SpeedometerPanel extends JPanel {
        private double speed = 0.0;
        private double displayDistance = 0.0;
        private final double maxDisplaySpeed = 500.0;

        public SpeedometerPanel() {
            setBackground(DARK_BG);
        }

        public void setSpeed(double speed) {
            this.speed = speed;
            repaint();
        }

        public void setDistance(double dist) {
            this.displayDistance = dist;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(width, height) / 2 - 20;

            // Safety check
            if (radius <= 0)
                return;

            // Draw gauge background (distinguishable dusty-looking grey)
            g2d.setColor(new Color(50, 55, 60));
            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            // Draw gauge border
            g2d.setColor(new Color(120, 120, 125));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();

            // Draw ticks and numbers (0-500 km/h to match the request)
            for (int i = 0; i <= 500; i += 25) {
                // Angle: sweep from 210° (bottom-left) to -30° (bottom-right)
                double angleDegree = 210.0 - (i / maxDisplaySpeed) * 240.0;
                double angleRad = Math.toRadians(angleDegree);

                boolean isMajor = (i % 50 == 0);
                int tickLen = isMajor ? 20 : 10;

                int x1 = centerX + (int) (Math.cos(angleRad) * (radius - tickLen));
                int y1 = centerY - (int) (Math.sin(angleRad) * (radius - tickLen));
                int x2 = centerX + (int) (Math.cos(angleRad) * radius);
                int y2 = centerY - (int) (Math.sin(angleRad) * radius);

                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(isMajor ? 3 : 2));
                g2d.drawLine(x1, y1, x2, y2);

                // Draw numbers every 50
                if (isMajor) {
                    int labelRadius = radius - 40;
                    int tx = centerX + (int) (Math.cos(angleRad) * labelRadius);
                    int ty = centerY - (int) (Math.sin(angleRad) * labelRadius);
                    String text = String.valueOf(i);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(text, tx - fm.stringWidth(text) / 2, ty + fm.getAscent() / 2);
                }
            }

            // Draw "km/h" label at bottom right like in the image
            g2d.setColor(new Color(180, 180, 180));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("km/h", centerX + 50, centerY + radius - 50);

            // Odometer Font setup
            Font odoFont = new Font("Monospaced", Font.BOLD, 18);
            g2d.setFont(odoFont);
            FontMetrics odoFm = g2d.getFontMetrics(odoFont);

            // Draw Trip Odometer (Top block)
            int tripBoxW = 80;
            int tripBoxH = 25;
            int tripBoxX = centerX - tripBoxW / 2;
            int tripBoxY = centerY - 105;
            g2d.setColor(new Color(25, 25, 25));
            g2d.fillRect(tripBoxX, tripBoxY, tripBoxW, tripBoxH);

            g2d.setColor(new Color(200, 200, 200));

            String tripText = String.format("%04.1f", (displayDistance / (double) TARGET_DISTANCE) * 10.0).replace(".",
                    " ");
            int tripTextW = odoFm.stringWidth(tripText);
            g2d.drawString(tripText, centerX - tripTextW / 2,
                    tripBoxY + odoFm.getAscent() + (tripBoxH - odoFm.getHeight()) / 2);

            // Draw Main Odometer (Middle block)
            int odoBoxW = 100;
            int odoBoxH = 25;
            int odoBoxX = centerX - odoBoxW / 2;
            int odoBoxY = centerY - 50;
            g2d.setColor(new Color(25, 25, 25));
            g2d.fillRect(odoBoxX, odoBoxY, odoBoxW, odoBoxH);

            g2d.setColor(Color.WHITE);
            String odoText = String.format("%06d", (int) displayDistance + 191600);
            int odoTextW = odoFm.stringWidth(odoText);
            g2d.drawString(odoText, centerX - odoTextW / 2,
                    odoBoxY + odoFm.getAscent() + (odoBoxH - odoFm.getHeight()) / 2);

            // Draw needle for speedometer
            double currentAngleDeg = 210.0 - (Math.min(speed, maxDisplaySpeed) / maxDisplaySpeed) * 240.0;
            double currentAngleRad = Math.toRadians(currentAngleDeg);

            // Needle
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int needleRadius = radius - 25;
            int nx = centerX + (int) (Math.cos(currentAngleRad) * needleRadius);
            int ny = centerY - (int) (Math.sin(currentAngleRad) * needleRadius);
            g2d.drawLine(centerX, centerY, nx, ny);

            // Pointer tail
            int tailRadius = 25;
            int tx = centerX - (int) (Math.cos(currentAngleRad) * tailRadius);
            int ty = centerY + (int) (Math.sin(currentAngleRad) * tailRadius);
            g2d.drawLine(centerX, centerY, tx, ty);

            // Center cap (black, large circle like the image)
            g2d.setColor(new Color(30, 30, 30));
            g2d.fillOval(centerX - 25, centerY - 25, 50, 50);
            g2d.setColor(new Color(60, 60, 60));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(centerX - 25, centerY - 25, 50, 50);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new CarSimulationApp();
            frame.setVisible(true);
        });
    }
}