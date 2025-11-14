package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.util.IconLoader;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Updated Main Frame dengan method untuk switch tabs dan search
 */
public class MainFrame extends JFrame {
    private MusicPlayerController controller;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Navigation panels
    private LibraryPanel libraryPanel;
    private NowPlayingPanel nowPlayingPanel;
    private RecommendationPanel recommendationPanel;
    private FingerprintPanel fingerprintPanel;

    public MainFrame() {
        initializeComponents();
        setupLayout();
        setupController();
        setupWindowListener();
    }

    private void initializeComponents() {
        setTitle("Smart Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Initialize controller first
        controller = new MusicPlayerController();

        // Initialize panels (pass this reference to FingerprintPanel)
        libraryPanel = new LibraryPanel(controller);
        nowPlayingPanel = new NowPlayingPanel(controller);
        recommendationPanel = new RecommendationPanel(controller);
        fingerprintPanel = new FingerprintPanel(controller, this);

        // Set now playing panel as listener
        controller.setNowPlayingPanel(nowPlayingPanel);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(0, 0));

        // Sidebar navigation
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // Content area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(libraryPanel, "Library");
        contentPanel.add(nowPlayingPanel, "NowPlaying");
        contentPanel.add(recommendationPanel, "Recommendations");
        contentPanel.add(fingerprintPanel, "Fingerprint");
        add(contentPanel, BorderLayout.CENTER);

        // Player controls at bottom
        PlayerControlPanel controlPanel = new PlayerControlPanel(controller);
        add(controlPanel, BorderLayout.SOUTH);

        // Show library by default
        showPanel("Library");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new MigLayout("fillx, insets 10", "[grow]", "[]10[]10[]10[]10[]push"));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(new Color(25, 25, 28));

        // App title
        JLabel titleLabel = new JLabel("SONORA");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        sidebar.add(titleLabel, "wrap, center, gaptop 10, gapbottom 20");

        // Navigation buttons
        addNavButton(sidebar, "Library", "Library", IconLoader.Icons.HOME);
        addNavButton(sidebar, "Now Playing", "NowPlaying", IconLoader.Icons.NOW_PLAYING);
        addNavButton(sidebar, "Recommendations", "Recommendations", IconLoader.Icons.RECOMMENDATIONS);
        addNavButton(sidebar, "Identify Song", "Fingerprint", IconLoader.Icons.FINGERPRINT);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String text, String panelName, String iconName) {
        JButton button = new JButton(text, IconLoader.loadNavIcon(iconName));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setForeground(new Color(180, 180, 180));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(180, 180, 180));
            }
        });

        button.addActionListener(e -> showPanel(panelName));

        sidebar.add(button, "wrap, growx, h 40!");
    }


    private void showPanel(String panelName) {
        cardLayout.show(contentPanel, panelName);
    }

    /**
     * Public method untuk switch ke Library dan perform search
     * Dipanggil dari FingerprintPanel setelah "Search in Player"
     */
    public void showLibraryAndSearch(String query) {
        // Switch to Library tab
        showPanel("Library");

        // Perform search
        libraryPanel.performSearchWithQuery(query);
    }

    private void setupController() {
        controller.initialize();
    }

    private void setupWindowListener() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Cleanup resources
                controller.shutdown();
            }
        });
    }
}

