import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Properties;

public class CarSimulationApp extends JFrame {
    private static final int TARGET_DISTANCE = 400; // 400 meters
    private static final int FPS = 50; // frames per second for smooth simulation
    private static final double DT = 1.0 / FPS; // time step in seconds

    // Physics state
    private double speedMpS = 0.0; // speed in meters per second
    private double distance = 0.0; // distance in meters
    private double carRaceTime = 0.0; // time the car has been racing

    // Timer state (independent of the car)
    private double timerValue = -5.0; // independent timer starts at -5 seconds
    private boolean isTimerRunning = false;

    // Config state
    private double maxSpeedKmH = 200.0; // default max speed
    private double accelerationKmHS = 15.0; // default acceleration in km/h per second
    private boolean isHoldingAccelerate = false;
    private boolean hasFinished = false;

    // UI Components
    private TrackPanel trackPanel;
    private SpeedometerPanel speedometerPanel;
    private JLabel timerLabel;
    private JLabel resultLabel;
    private JButton startBtn;
    private JButton stopBtn;
    private JButton resetBtn;
    private JButton accelerateBtn;

    private Timer gameLoop;
    private DecimalFormat df = new DecimalFormat("0.0");
    private DecimalFormat timeDf = new DecimalFormat("0.00");

    public CarSimulationApp() {
        setTitle("Car Simulation - 400m Race");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        loadConfig();

        // Top: Track Panel
        trackPanel = new TrackPanel();
        trackPanel.setPreferredSize(new Dimension(800, 100));
        add(trackPanel, BorderLayout.NORTH);

        // Middle: Speedometer
        JPanel middlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        speedometerPanel = new SpeedometerPanel();
        middlePanel.add(speedometerPanel);
        add(middlePanel, BorderLayout.CENTER);

        // Left: Timer and Controls
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        timerLabel = new JLabel("Time: -5.00 s");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        leftPanel.add(timerLabel);

        startBtn = new JButton("Start");
        stopBtn = new JButton("Stop");
        resetBtn = new JButton("Reset");

        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(startBtn);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(stopBtn);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(resetBtn);
        add(leftPanel, BorderLayout.WEST);

        // Right: Acceleration and Result
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        accelerateBtn = new JButton("ACCELERATE (Hold)");
        accelerateBtn.setFont(new Font("Arial", Font.BOLD, 16));

        resultLabel = new JLabel("Result: --");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 16));

        rightPanel.add(accelerateBtn);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(resultLabel);
        add(rightPanel, BorderLayout.EAST);

        setupListeners();

        // Setup game loop (swing timer)
        gameLoop = new Timer(1000 / FPS, e -> updatePhysics());
        gameLoop.start(); // Always keep running

        setLocationRelativeTo(null);
    }

    private void loadConfig() {
        File configFile = new File("car_config.txt");
        if (!configFile.exists()) {
            try (PrintWriter out = new PrintWriter(configFile)) {
                out.println("acceleration=15");
                out.println("max_speed=200");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            Properties props = new Properties();
            props.load(reader);
            accelerationKmHS = Double.parseDouble(props.getProperty("acceleration", "15.0"));
            maxSpeedKmH = Double.parseDouble(props.getProperty("max_speed", "200.0"));
            System.out.println("Loaded config - Max Speed: " + maxSpeedKmH + " km/h, Acceleration: " + accelerationKmHS
                    + " km/h/s");
        } catch (Exception e) {
            System.err.println("Failed to load config, using defaults.");
        }
    }

    private void setupListeners() {
        startBtn.addActionListener(e -> isTimerRunning = true);

        stopBtn.addActionListener(e -> isTimerRunning = false);

        resetBtn.addActionListener(e -> {
            isTimerRunning = false;
            timerValue = -5.0;
            // Also reset the car for convenience
            distance = 0.0;
            speedMpS = 0.0;
            carRaceTime = 0.0;
            hasFinished = false;
            isHoldingAccelerate = false;
            updateUIState();
        });

        accelerateBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isHoldingAccelerate = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isHoldingAccelerate = false;
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
                resultLabel.setText("Result: " + timeDf.format(carRaceTime) + " s");
            }
        }

        updateUIState();
    }

    private void updateUIState() {
        trackPanel.setCarPosition(distance / TARGET_DISTANCE);
        trackPanel.repaint();

        double currentSpeedKmH = speedMpS * 3.6; // convert m/s to km/h
        speedometerPanel.setSpeed(currentSpeedKmH);
        timerLabel.setText("Time: " + timeDf.format(timerValue) + " s");

        if (!hasFinished) {
            resultLabel.setText(carRaceTime > 0 ? "Racing: " + timeDf.format(carRaceTime) + " s" : "Ready...");
        }
    }

    // Inner class for the top track view
    class TrackPanel extends JPanel {
        private double progress = 0.0; // 0.0 to 1.0

        public void setCarPosition(double progress) {
            this.progress = Math.max(0.0, Math.min(1.0, progress));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            int width = getWidth();
            int height = getHeight();
            int trackLength = width - 100;
            int startX = 50;

            // Draw track line
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(startX, height / 2, startX + trackLength, height / 2);

            // Draw start and finish marks
            g2d.setColor(Color.RED);
            g2d.fillRect(startX - 2, height / 2 - 10, 4, 20); // Start
            g2d.setColor(Color.GREEN);
            g2d.fillRect(startX + trackLength - 2, height / 2 - 10, 4, 20); // Finish

            g2d.setColor(Color.BLACK);
            g2d.drawString("Start", startX - 15, height / 2 + 25);
            g2d.drawString("400m Finish", startX + trackLength - 30, height / 2 + 25);

            // Draw Car
            int carX = startX + (int) (progress * trackLength);
            g2d.setColor(Color.BLUE);
            g2d.fillRect(carX - 25, height / 2 - 15, 50, 30);

            g2d.setColor(Color.WHITE);
            g2d.drawString("CAR", carX - 12, height / 2 + 5);
        }
    }

    // Inner class for the analog speedometer
    class SpeedometerPanel extends JPanel {
        private double speed = 0.0;
        private final double maxDisplaySpeed = 500.0;

        public SpeedometerPanel() {
            setPreferredSize(new Dimension(350, 350));
            setBackground(new Color(30, 30, 30));
        }

        public void setSpeed(double speed) {
            this.speed = speed;
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
            int centerY = height / 2 + 10;
            int radius = Math.min(width, height) / 2 - 30;

            // Draw outer circle (dial background)
            g2d.setColor(Color.BLACK);
            g2d.fillOval(centerX - radius - 15, centerY - radius - 15, (radius + 15) * 2, (radius + 15) * 2);
            g2d.setColor(new Color(40, 40, 40));
            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            // Draw ticks and labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();

            // Draw from 0 to 500 km/h
            for (int i = 0; i <= 500; i += 10) {
                // Angle from 210 degrees down to -30 degrees (sweep of 240 degrees)
                double angleDegree = 210.0 - (i / maxDisplaySpeed) * 240.0;
                double angleRad = Math.toRadians(angleDegree);

                boolean isMajor = (i % 50 == 0);
                int len = isMajor ? 20 : 10;

                int x1 = centerX + (int) (Math.cos(angleRad) * (radius - len));
                int y1 = centerY - (int) (Math.sin(angleRad) * (radius - len));
                int x2 = centerX + (int) (Math.cos(angleRad) * radius);
                int y2 = centerY - (int) (Math.sin(angleRad) * radius);

                g2d.setStroke(new BasicStroke(isMajor ? 3 : 1));
                g2d.drawLine(x1, y1, x2, y2);

                // Draw numbers only every 50 km/h
                if (isMajor) {
                    int tx = centerX + (int) (Math.cos(angleRad) * (radius - 35));
                    int ty = centerY - (int) (Math.sin(angleRad) * (radius - 35));

                    String text = String.valueOf(i);
                    g2d.drawString(text, tx - fm.stringWidth(text) / 2, ty + fm.getAscent() / 2 - 2);
                }
            }

            // Draw km/h text
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.drawString("km/h", centerX - fm.stringWidth("km/h") / 2, centerY - 60);

            // Digital speed below center (odometer style)
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 22));
            String digitalSpeed = String.format("%03.0f", speed);
            g2d.fillRect(centerX - 30, centerY + 35, 60, 30);
            g2d.setColor(Color.BLACK);
            g2d.drawString(digitalSpeed, centerX - fm.stringWidth(digitalSpeed) / 2 - 5, centerY + 58);

            // Draw needle
            double currentAngleDeg = 210.0 - (Math.min(speed, maxDisplaySpeed) / maxDisplaySpeed) * 240.0;
            double currentAngleRad = Math.toRadians(currentAngleDeg);

            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int nx = centerX + (int) (Math.cos(currentAngleRad) * (radius - 15));
            int ny = centerY - (int) (Math.sin(currentAngleRad) * (radius - 15));
            g2d.drawLine(centerX, centerY, nx, ny);

            // Center base
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval(centerX - 15, centerY - 15, 30, 30);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillOval(centerX - 8, centerY - 8, 16, 16);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CarSimulationApp().setVisible(true);
        });
    }
}