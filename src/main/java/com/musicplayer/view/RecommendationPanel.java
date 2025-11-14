package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Recommendation;
import com.musicplayer.model.Track;
import com.musicplayer.util.IconLoader;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel untuk menampilkan rekomendasi lagu
 */
public class RecommendationPanel extends JPanel {
    private MusicPlayerController controller;
    private JPanel dailyMixPanel;
    private JPanel similarSongsPanel;
    private JButton generateButton;

    public RecommendationPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 35));

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Content with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Daily Mix tab
        dailyMixPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]", "[]10"));
        dailyMixPanel.setBackground(new Color(30, 30, 35));
        JScrollPane dailyMixScroll = new JScrollPane(dailyMixPanel);
        dailyMixScroll.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.addTab("Daily Mix", dailyMixScroll);

        // Similar Songs tab
        similarSongsPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]", "[]10"));
        similarSongsPanel.setBackground(new Color(30, 30, 35));
        JScrollPane similarScroll = new JScrollPane(similarSongsPanel);
        similarScroll.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.addTab("Similar Songs", similarScroll);

        add(tabbedPane, BorderLayout.CENTER);

        loadRecommendations();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]push[]", ""));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel("Recommendations");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel);

        generateButton = new JButton("Refresh", IconLoader.loadButtonIcon(IconLoader.Icons.REFRESH));
        generateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        generateButton.setFocusPainted(false);
        generateButton.addActionListener(e -> loadRecommendations());
        panel.add(generateButton, "h 35!");

        return panel;
    }

    private void loadRecommendations() {
        generateButton.setEnabled(false);
        generateButton.setText("Loading...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Recommendation> dailyMix;
            List<Recommendation> similar;

            @Override
            protected Void doInBackground() throws Exception {
                dailyMix = controller.generateDailyMix(20);
                similar = controller.getSimilarToRecent(10);
                return null;
            }

            @Override
            protected void done() {
                try {
                    displayDailyMix(dailyMix);
                    displaySimilarSongs(similar);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            RecommendationPanel.this,
                            "Error loading recommendations: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    generateButton.setEnabled(true);
                    generateButton.setText("Refresh");
                }
            }
        };
        worker.execute();
    }

    private void displayDailyMix(List<Recommendation> recommendations) {
        dailyMixPanel.removeAll();

        if (recommendations == null || recommendations.isEmpty()) {
            JLabel noDataLabel = new JLabel("No recommendations yet. Play some songs first!");
            noDataLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            noDataLabel.setForeground(new Color(150, 150, 150));
            dailyMixPanel.add(noDataLabel, "center, gaptop 40");
        } else {
            for (Recommendation rec : recommendations) {
                JPanel trackCard = createTrackCard(rec);
                dailyMixPanel.add(trackCard, "growx, h 70!");
            }
        }

        dailyMixPanel.revalidate();
        dailyMixPanel.repaint();
    }

    private void displaySimilarSongs(List<Recommendation> recommendations) {
        similarSongsPanel.removeAll();

        if (recommendations == null || recommendations.isEmpty()) {
            JLabel noDataLabel = new JLabel("Play a song to get similar recommendations");
            noDataLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            noDataLabel.setForeground(new Color(150, 150, 150));
            similarSongsPanel.add(noDataLabel, "center, gaptop 40");
        } else {
            for (Recommendation rec : recommendations) {
                JPanel trackCard = createTrackCard(rec);
                similarSongsPanel.add(trackCard, "growx, h 70!");
            }
        }

        similarSongsPanel.revalidate();
        similarSongsPanel.repaint();
    }

    private JPanel createTrackCard(Recommendation rec) {
        Track track = rec.getTrack();

        JPanel card = new JPanel(new MigLayout("fillx, insets 10", "[]10[grow]10[]", ""));
        card.setBackground(new Color(40, 40, 45));
        card.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55), 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Thumbnail placeholder
        JLabel thumbnail = new JLabel("🎵");
        thumbnail.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        thumbnail.setPreferredSize(new Dimension(50, 50));
        thumbnail.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(thumbnail);

        // Track info
        JPanel infoPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]5[]"));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(track.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        infoPanel.add(titleLabel, "wrap");

        JLabel artistLabel = new JLabel(track.getArtist() + " • " + rec.getReason());
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        artistLabel.setForeground(new Color(150, 150, 150));
        infoPanel.add(artistLabel);

        card.add(infoPanel, "grow");

        // Play button
        JButton playButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playButton.setFocusPainted(false);
        playButton.setPreferredSize(new Dimension(40, 40));
        playButton.addActionListener(e -> {
            // Play track
            JOptionPane.showMessageDialog(this,
                    "Playing: " + track.getArtist() + " - " + track.getTitle());
        });
        card.add(playButton);

        // Hover effect
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(50, 50, 55));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(40, 40, 45));
            }
        });

        return card;
    }
}