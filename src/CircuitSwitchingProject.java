package src;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

// ==========================================
// 1. TDM LINK MODEL (Time-Division Multiplexing)
// ==========================================
class TDMThreadSafeLink {
    private final String src;
    private final String dst;
    private final boolean[] timeSlots; // true = allocated, false = free
    private final ReentrantLock lock = new ReentrantLock();

    public TDMThreadSafeLink(String src, String dst, int totalSlots) {
        this.src = src;
        this.dst = dst;
        this.timeSlots = new boolean[totalSlots];
    }

    public int reserveSlot() {
        lock.lock();
        try {
            for (int i = 0; i < timeSlots.length; i++) {
                if (!timeSlots[i]) {
                    timeSlots[i] = true;
                    return i;
                }
            }
            return -1; // Blocked
        } finally {
            lock.unlock();
        }
    }

    public void releaseSlot(int slotIndex) {
        lock.lock();
        try {
            if (slotIndex >= 0 && slotIndex < timeSlots.length) {
                timeSlots[slotIndex] = false;
            }
        } finally {
            lock.unlock();
        }
    }

    public int getFreeSlotsCount() {
        lock.lock();
        try {
            int count = 0;
            for (boolean slot : timeSlots) {
                if (!slot) count++;
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    public String getSrc() { return src; }
    public String getDst() { return dst; }
    public int getTotalSlots() { return timeSlots.length; }
}

// ==========================================
// 2. ERLANG-B MATHEMATICAL ENGINE
// ==========================================
class ErlangBAnalyzer {
    public static double calculateErlangB(double trafficErlangs, int channels) {
        if (channels <= 0) return 1.0;
        double invB = 1.0;
        for (int i = 1; i <= channels; i++) {
            invB = 1.0 + invB * (i / trafficErlangs);
        }
        return 1.0 / invB;
    }
}

// ==========================================
// 3. MAIN GUI, TRAFFIC SIMULATOR & CHART SYSTEM
// ==========================================
public class CircuitSwitchingProject extends JFrame {

    static class NodeModel {
        String name;
        int x, y;
        boolean isHost;

        NodeModel(String name, int x, int y, boolean isHost) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.isHost = isHost;
        }

        boolean contains(Point p) {
            return Math.hypot(x - p.x, y - p.y) <= 22;
        }
    }

    static class ActiveCircuit {
        String id;
        List<NodeModel> path;
        Map<String, Integer> linkSlotAssignments;

        ActiveCircuit(String id, List<NodeModel> path, Map<String, Integer> linkSlotAssignments) {
            this.id = id;
            this.path = path;
            this.linkSlotAssignments = linkSlotAssignments;
        }
    }

    // Graph & Circuit State
    private final Map<String, NodeModel> nodes = new LinkedHashMap<>();
    private final Map<String, TDMThreadSafeLink> links = new HashMap<>();
    private final Map<String, ActiveCircuit> activeCircuits = new ConcurrentHashMap<>();

    // Performance Counters
    private int totalRequests = 0;
    private int blockedRequests = 0;
    private int circuitCounter = 0;

    // Traffic Generator State
    private boolean autoTrafficRunning = false;
    private Thread trafficThread;
    private double currentLambda = 2.0; // Calls per second
    private double currentMeanHoldTime = 1.5; // Seconds

    // Chart Data Points (Offered Load vs Blocking Rate)
    private final List<Double> offeredLoadHistory = new ArrayList<>();
    private final List<Double> blockingRateHistory = new ArrayList<>();

    // GUI Panels & Components
    private NetworkCanvas canvas;
    private LiveChartPanel chartPanel;
    private JComboBox<String> comboSrc, comboDst, comboRelease;
    private JLabel lblTotal, lblBlocked, lblProb, lblErlangTheory, lblErlangs;
    private JSlider sliderLambda;
    private JButton btnToggleAutoTraffic;
    private float packetAnimationPhase = 0.0f;

    public CircuitSwitchingProject() {
        setTitle("Enterprise Circuit Switching & Automated Poisson Traffic Simulator");
        setSize(1350, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        buildInitialTopology();

        // Center Split Pane: Canvas on top, Real-time Chart on bottom
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        canvas = new NetworkCanvas();
        chartPanel = new LiveChartPanel();
        centerSplit.setTopComponent(canvas);
        centerSplit.setBottomComponent(chartPanel);
        centerSplit.setDividerLocation(480);
        centerSplit.setResizeWeight(0.7);

        add(centerSplit, BorderLayout.CENTER);
        add(createControlDashboard(), BorderLayout.WEST);

        // Animation Loop for packet streaming
        Timer timer = new Timer(30, e -> {
            packetAnimationPhase += 0.05f;
            if (packetAnimationPhase > 1.0f) packetAnimationPhase = 0.0f;
            canvas.repaint();
        });
        timer.start();
    }

    private void buildInitialTopology() {
        nodes.put("HostA", new NodeModel("HostA", 120, 220, true));
        nodes.put("Switch1", new NodeModel("Switch1", 340, 120, false));
        nodes.put("Switch2", new NodeModel("Switch2", 580, 120, false));
        nodes.put("Switch3", new NodeModel("Switch3", 460, 340, false));
        nodes.put("HostB", new NodeModel("HostB", 800, 220, true));

        addLink("HostA", "Switch1", 3);
        addLink("Switch1", "Switch2", 1); // Intentional Bottleneck
        addLink("Switch1", "Switch3", 2);
        addLink("Switch3", "Switch2", 2);
        addLink("Switch2", "HostB", 3);
    }

    private void addLink(String u, String v, int slots) {
        links.put(u + "-" + v, new TDMThreadSafeLink(u, v, slots));
        links.put(v + "-" + u, new TDMThreadSafeLink(v, u, slots));
    }

    private JPanel createControlDashboard() {
        JPanel dashboard = new JPanel();
        dashboard.setPreferredSize(new Dimension(350, 800));
        dashboard.setBackground(new Color(20, 24, 33));
        dashboard.setLayout(new BoxLayout(dashboard, BoxLayout.Y_AXIS));
        dashboard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("CIRCUIT SWITCHING SUITE");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        dashboard.add(title);
        dashboard.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- 1. MANUAL CONTROLS ---
        dashboard.add(createHeaderLabel("1. Manual Call Control"));
        comboSrc = new JComboBox<>(nodes.keySet().toArray(new String[0]));
        comboDst = new JComboBox<>(nodes.keySet().toArray(new String[0]));
        comboSrc.setSelectedItem("HostA");
        comboDst.setSelectedItem("HostB");

        dashboard.add(createFieldRow("Source:", comboSrc));
        dashboard.add(createFieldRow("Destination:", comboDst));

        JButton btnSetup = new JButton("Establish Connection");
        styleButton(btnSetup, new Color(40, 167, 69));
        btnSetup.addActionListener(e -> executeCircuitEstablishment((String) comboSrc.getSelectedItem(), (String) comboDst.getSelectedItem(), 0));
        dashboard.add(btnSetup);

        comboRelease = new JComboBox<>();
        dashboard.add(createFieldRow("Active ID:", comboRelease));
        JButton btnRelease = new JButton("Tear Down Circuit");
        styleButton(btnRelease, new Color(220, 53, 69));
        btnRelease.addActionListener(e -> executeCircuitRelease((String) comboRelease.getSelectedItem()));
        dashboard.add(btnRelease);

        dashboard.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- 2. AUTOMATED POISSON TRAFFIC GENERATOR ---
        dashboard.add(createHeaderLabel("2. Automated Poisson Traffic Engine"));
        
        sliderLambda = new JSlider(1, 10, 2);
        sliderLambda.setOpaque(false);
        sliderLambda.setForeground(Color.LIGHT_GRAY);
        sliderLambda.addChangeListener(e -> {
            currentLambda = sliderLambda.getValue();
            updateMetricsDisplay();
        });
        dashboard.add(createFieldRow("Arrival Rate (λ):", sliderLambda));

        btnToggleAutoTraffic = new JButton("Start Poisson Traffic");
        styleButton(btnToggleAutoTraffic, new Color(0, 123, 255));
        btnToggleAutoTraffic.addActionListener(e -> toggleAutoTraffic());
        dashboard.add(btnToggleAutoTraffic);

        dashboard.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- 3. LIVE METRICS ---
        dashboard.add(createHeaderLabel("3. Telecom Performance Analytics"));
        lblErlangs = createMetricLabel("Offered Load (E): 3.00 Erlangs");
        lblTotal = createMetricLabel("Total Call Requests: 0");
        lblBlocked = createMetricLabel("Blocked Calls (P_block): 0");
        lblProb = createMetricLabel("Simulated Block Rate: 0.00%");
        lblErlangTheory = createMetricLabel("Erlang-B Theory Model: 0.00%");

        dashboard.add(lblErlangs);
        dashboard.add(lblTotal);
        dashboard.add(lblBlocked);
        dashboard.add(lblProb);
        dashboard.add(lblErlangTheory);

        dashboard.add(Box.createVerticalGlue());

        JButton btnReset = new JButton("Reset Simulator Statistics");
        styleButton(btnReset, new Color(108, 117, 125));
        btnReset.addActionListener(e -> resetStats());
        dashboard.add(btnReset);

        return dashboard;
    }

    private JLabel createHeaderLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(52, 152, 219));
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel createFieldRow(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(320, 28));
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(180, 190, 205));
        p.add(lbl, BorderLayout.WEST);
        p.add(comp, BorderLayout.EAST);
        comp.setPreferredSize(new Dimension(160, 24));
        return p;
    }

    private JLabel createMetricLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setMaximumSize(new Dimension(320, 32));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // --- AUTOMATED TRAFFIC GENERATOR ENGINE ---
    private void toggleAutoTraffic() {
        if (!autoTrafficRunning) {
            autoTrafficRunning = true;
            btnToggleAutoTraffic.setText("Stop Poisson Traffic");
            btnToggleAutoTraffic.setBackground(new Color(255, 193, 7));
            btnToggleAutoTraffic.setForeground(Color.BLACK);

            trafficThread = new Thread(() -> {
                Random rand = new Random();
                List<String> hostKeys = new ArrayList<>();
                for (NodeModel n : nodes.values()) if (n.isHost) hostKeys.add(n.name);

                while (autoTrafficRunning) {
                    try {
                        // Poisson arrival interval: t = -ln(U) / lambda
                        double interArrivalSec = -Math.log(1.0 - rand.nextDouble()) / currentLambda;
                        Thread.sleep((long) (interArrivalSec * 1000));

                        String src = hostKeys.get(rand.nextInt(hostKeys.size()));
                        String dst = hostKeys.get(rand.nextInt(hostKeys.size()));
                        while (src.equals(dst)) dst = hostKeys.get(rand.nextInt(hostKeys.size()));

                        // Exponential call duration: d = -ln(U) * meanHoldTime
                        double holdDurationSec = -Math.log(1.0 - rand.nextDouble()) * currentMeanHoldTime;
                        long holdDurationMs = (long) (holdDurationSec * 1000);

                        executeCircuitEstablishment(src, dst, holdDurationMs);

                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            trafficThread.start();
        } else {
            autoTrafficRunning = false;
            btnToggleAutoTraffic.setText("Start Poisson Traffic");
            btnToggleAutoTraffic.setBackground(new Color(0, 123, 255));
            btnToggleAutoTraffic.setForeground(Color.WHITE);
        }
    }

    private List<NodeModel> findShortestPath(NodeModel src, NodeModel dst) {
        Queue<List<NodeModel>> queue = new LinkedList<>();
        Set<NodeModel> visited = new HashSet<>();
        queue.add(Collections.singletonList(src));
        visited.add(src);

        while (!queue.isEmpty()) {
            List<NodeModel> path = queue.poll();
            NodeModel current = path.get(path.size() - 1);

            if (current == dst) return path;

            for (String key : links.keySet()) {
                String[] parts = key.split("-");
                if (parts[0].equals(current.name)) {
                    NodeModel neighbor = nodes.get(parts[1]);
                    if (neighbor != null && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        List<NodeModel> nextPath = new ArrayList<>(path);
                        nextPath.add(neighbor);
                        queue.add(nextPath);
                    }
                }
            }
        }
        return null;
    }

    private synchronized void executeCircuitEstablishment(String srcName, String dstName, long autoReleaseMs) {
        totalRequests++;
        NodeModel src = nodes.get(srcName);
        NodeModel dst = nodes.get(dstName);

        List<NodeModel> path = findShortestPath(src, dst);
        if (path == null) {
            blockedRequests++;
            updateMetricsDisplay();
            return;
        }

        Map<String, Integer> assignedSlots = new HashMap<>();
        boolean success = true;

        for (int i = 0; i < path.size() - 1; i++) {
            NodeModel u = path.get(i);
            NodeModel v = path.get(i + 1);
            String linkKey = u.name + "-" + v.name;
            TDMThreadSafeLink link = links.get(linkKey);

            int slotIdx = link.reserveSlot();
            if (slotIdx != -1) {
                assignedSlots.put(linkKey, slotIdx);
            } else {
                success = false;
                for (Map.Entry<String, Integer> entry : assignedSlots.entrySet()) {
                    links.get(entry.getKey()).releaseSlot(entry.getValue());
                }
                break;
            }
        }

        if (success) {
            circuitCounter++;
            String circuitId = String.format("CIRCUIT-%03d", circuitCounter);
            ActiveCircuit circuit = new ActiveCircuit(circuitId, path, assignedSlots);
            activeCircuits.put(circuitId, circuit);
            comboRelease.addItem(circuitId);

            if (autoReleaseMs > 0) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(autoReleaseMs);
                        executeCircuitRelease(circuitId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } else {
            blockedRequests++;
        }

        updateMetricsDisplay();
        canvas.repaint();
    }

    private synchronized void executeCircuitRelease(String circuitId) {
        if (circuitId == null) return;
        ActiveCircuit circuit = activeCircuits.remove(circuitId);
        if (circuit != null) {
            for (Map.Entry<String, Integer> entry : circuit.linkSlotAssignments.entrySet()) {
                if (links.containsKey(entry.getKey())) {
                    links.get(entry.getKey()).releaseSlot(entry.getValue());
                }
            }
            comboRelease.removeItem(circuitId);
            updateMetricsDisplay();
            canvas.repaint();
        }
    }

    private void resetStats() {
        totalRequests = 0;
        blockedRequests = 0;
        offeredLoadHistory.clear();
        blockingRateHistory.clear();
        updateMetricsDisplay();
        chartPanel.repaint();
    }

    private void updateMetricsDisplay() {
        double trafficErlangs = currentLambda * currentMeanHoldTime;
        lblErlangs.setText(String.format("Offered Load (E): %.2f Erlangs", trafficErlangs));
        lblTotal.setText("Total Call Requests: " + totalRequests);
        lblBlocked.setText("Blocked Calls (P_block): " + blockedRequests);

        double empiricalRate = totalRequests == 0 ? 0.0 : ((double) blockedRequests / totalRequests) * 100.0;
        lblProb.setText(String.format("Simulated Block Rate: %.2f%%", empiricalRate));

        // Theoretical Erlang-B for bottleneck channel count = 1
        double theoreticalRate = ErlangBAnalyzer.calculateErlangB(trafficErlangs, 1) * 100.0;
        lblErlangTheory.setText(String.format("Erlang-B Theory Model: %.2f%%", theoreticalRate));

        if (totalRequests % 5 == 0 && totalRequests > 0) {
            offeredLoadHistory.add(trafficErlangs);
            blockingRateHistory.add(empiricalRate);
            if (offeredLoadHistory.size() > 30) {
                offeredLoadHistory.remove(0);
                blockingRateHistory.remove(0);
            }
            chartPanel.repaint();
        }
    }

    // --- CANVAS DRAWING PANEL ---
    private class NetworkCanvas extends JPanel {
        private NodeModel activeDragNode = null;

        public NetworkCanvas() {
            setBackground(new Color(11, 15, 22));

            MouseAdapter dragAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (NodeModel n : nodes.values()) {
                        if (n.contains(e.getPoint())) {
                            activeDragNode = n;
                            break;
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    activeDragNode = null;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (activeDragNode != null) {
                        activeDragNode.x = e.getX();
                        activeDragNode.y = e.getY();
                        repaint();
                    }
                }
            };
            addMouseListener(dragAdapter);
            addMouseMotionListener(dragAdapter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Set<String> renderedPairs = new HashSet<>();
            for (Map.Entry<String, TDMThreadSafeLink> entry : links.entrySet()) {
                TDMThreadSafeLink link = entry.getValue();
                String pairKey = link.getSrc().compareTo(link.getDst()) < 0 ? link.getSrc() + "-" + link.getDst() : link.getDst() + "-" + link.getSrc();

                if (renderedPairs.contains(pairKey)) continue;
                renderedPairs.add(pairKey);

                NodeModel n1 = nodes.get(link.getSrc());
                NodeModel n2 = nodes.get(link.getDst());
                if (n1 == null || n2 == null) continue;

                int freeSlots = link.getFreeSlotsCount();
                int totalSlots = link.getTotalSlots();
                int usedSlots = totalSlots - freeSlots;

                if (usedSlots == totalSlots && totalSlots > 0) {
                    g2.setColor(new Color(220, 53, 69));
                    g2.setStroke(new BasicStroke(4));
                } else if (usedSlots > 0) {
                    g2.setColor(new Color(40, 167, 69));
                    g2.setStroke(new BasicStroke(3));
                } else {
                    g2.setColor(new Color(55, 68, 88));
                    g2.setStroke(new BasicStroke(2));
                }

                g2.draw(new Line2D.Double(n1.x, n1.y, n2.x, n2.y));

                int midX = (n1.x + n2.x) / 2;
                int midY = (n1.y + n2.y) / 2;
                g2.setColor(new Color(25, 30, 42, 220));
                g2.fillRoundRect(midX - 32, midY - 12, 64, 22, 6, 6);
                g2.setColor(usedSlots == totalSlots ? new Color(255, 120, 120) : Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString("Slots: " + usedSlots + "/" + totalSlots, midX - 28, midY + 3);
            }

            for (ActiveCircuit circuit : activeCircuits.values()) {
                for (int i = 0; i < circuit.path.size() - 1; i++) {
                    NodeModel start = circuit.path.get(i);
                    NodeModel end = circuit.path.get(i + 1);

                    int px = (int) (start.x + packetAnimationPhase * (end.x - start.x));
                    int py = (int) (start.y + packetAnimationPhase * (end.y - start.y));

                    g2.setColor(new Color(255, 193, 7));
                    g2.fillOval(px - 6, py - 6, 12, 12);
                }
            }

            for (NodeModel n : nodes.values()) {
                g2.setColor(n.isHost ? new Color(0, 123, 255) : new Color(253, 126, 20));
                g2.fillOval(n.x - 24, n.y - 24, 48, 48);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(n.x - 24, n.y - 24, 48, 48);

                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(n.name);
                g2.drawString(n.name, n.x - (textWidth / 2), n.y + 38);
            }
        }
    }

    // --- LIVE GRAPH CHART PANEL ---
    private class LiveChartPanel extends JPanel {
        public LiveChartPanel() {
            setBackground(new Color(15, 18, 25));
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(40, 50, 65)), "Live Blocking Probability Curve P_block vs. Time", 0, 0, new Font("SansSerif", Font.BOLD, 12), Color.CYAN));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int pad = 40;

            // Axes
            g2.setColor(Color.GRAY);
            g2.drawLine(pad, h - pad, w - pad, h - pad);
            g2.drawLine(pad, pad, pad, h - pad);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("100%", 10, pad + 10);
            g2.drawString("0%", 15, h - pad);
            g2.drawString("Traffic Samples →", w - 100, h - 10);

            if (blockingRateHistory.size() < 2) return;

            // Plot Experimental Line (Green)
            g2.setColor(new Color(46, 204, 113));
            g2.setStroke(new BasicStroke(2));
            double stepX = (double) (w - 2 * pad) / (blockingRateHistory.size() - 1);

            for (int i = 0; i < blockingRateHistory.size() - 1; i++) {
                int x1 = (int) (pad + i * stepX);
                int y1 = (int) ((h - pad) - (blockingRateHistory.get(i) / 100.0) * (h - 2 * pad));
                int x2 = (int) (pad + (i + 1) * stepX);
                int y2 = (int) ((h - pad) - (blockingRateHistory.get(i + 1) / 100.0) * (h - 2 * pad));

                g2.drawLine(x1, y1, x2, y2);
                g2.fillOval(x1 - 3, y1 - 3, 6, 6);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CircuitSwitchingProject().setVisible(true));
    }
}