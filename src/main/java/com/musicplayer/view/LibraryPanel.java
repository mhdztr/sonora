package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Track;
import com.musicplayer.util.IconLoader;
import com.musicplayer.util.ImageUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Enhanced Library Panel dengan featured track (hasil pertama besar)
 */
public class LibraryPanel extends JPanel {
    private MusicPlayerController controller;
    private JTextField searchField;
    private JButton searchButton;

    private JPanel featuredTrackPanel;
    private JPanel trackListPanel;

    private List<Track> currentSearchResults;

    public LibraryPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 35));

        // Search panel at top
        JPanel searchPanel = createSearchPanel();
        add(searchPanel, BorderLayout.NORTH);

        // Scrollable content area
        JPanel contentPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]", "[]10[]"));
        contentPanel.setOpaque(false);

        // Featured track (hasil pertama - BESAR)
        featuredTrackPanel = new JPanel();
        featuredTrackPanel.setOpaque(false);
        contentPanel.add(featuredTrackPanel, "growx, hidemode 3");

        // Track list (sisanya - list kecil)
        trackListPanel = new JPanel(new MigLayout("fillx, wrap 1, insets 0", "[grow]", "[]5"));
        trackListPanel.setOpaque(false);
        contentPanel.add(trackListPanel, "growx");

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 20", "[grow]10[]", "[]"));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel("🎵 Music Library");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, "wrap, gapbottom 15");

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search songs from YouTube Music...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.addActionListener(e -> performSearch());
        panel.add(searchField, "growx, h 40!");

        searchButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.SEARCH));
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> performSearch());
        panel.add(searchButton, "h 40!, w 100!");

        return panel;
    }

    /**
     * Perform search dengan query
     */
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term");
            return;
        }

        performSearchWithQuery(query);
    }

    /**
     * Public method untuk search dari komponen lain (e.g., dari AudD result)
     */
    public void performSearchWithQuery(String query) {
        searchField.setText(query);

        // Show loading
        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

        // Clear previous results
        featuredTrackPanel.removeAll();
        trackListPanel.removeAll();
        featuredTrackPanel.setVisible(false);

        // Search in background thread
        SwingWorker<List<Track>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Track> doInBackground() throws Exception {
                return controller.searchYouTubeMusic(query);
            }

            @Override
            protected void done() {
                try {
                    List<Track> tracks = get();
                    currentSearchResults = tracks;
                    displayTracks(tracks);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            LibraryPanel.this,
                            "Error searching: " + e.getMessage(),
                            "Search Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    searchButton.setEnabled(true);
                    searchButton.setText("Search");
                }
            }
        };
        worker.execute();
    }

    /**
     * Display tracks: pertama BESAR, sisanya list
     */
    private void displayTracks(List<Track> tracks) {
        featuredTrackPanel.removeAll();
        trackListPanel.removeAll();

        if (tracks == null || tracks.isEmpty()) {
            JLabel noResults = new JLabel("No results found");
            noResults.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            noResults.setForeground(new Color(150, 150, 150));
            trackListPanel.add(noResults);
        } else {
            // Featured track (hasil pertama - BESAR)
            Track featuredTrack = tracks.get(0);
            JPanel featuredCard = createFeaturedTrackCard(featuredTrack, 0);
            featuredTrackPanel.setLayout(new BorderLayout());
            featuredTrackPanel.add(featuredCard, BorderLayout.CENTER);
            featuredTrackPanel.setVisible(true);

            // Sisanya sebagai list kecil
            for (int i = 1; i < tracks.size(); i++) {
                Track track = tracks.get(i);
                JPanel trackCard = createSmallTrackCard(track, i);
                trackListPanel.add(trackCard, "growx, h 60!");
            }
        }

        featuredTrackPanel.revalidate();
        featuredTrackPanel.repaint();
        trackListPanel.revalidate();
        trackListPanel.repaint();
    }

    /**
     * Create FEATURED track card (BESAR untuk hasil pertama)
     */
    private JPanel createFeaturedTrackCard(Track track, int index) {
        JPanel card = new JPanel(new MigLayout("fill, insets 20", "[200!]20[grow]", "[][grow][]"));
        card.setBackground(new Color(45, 45, 50));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 255, 100), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Album art (BESAR)
        JLabel albumArt = new JLabel();
        albumArt.setPreferredSize(new Dimension(200, 200));
        albumArt.setHorizontalAlignment(SwingConstants.CENTER);
        albumArt.setOpaque(true);
        albumArt.setBackground(new Color(60, 60, 65));
        albumArt.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75), 1));

        // Load thumbnail in background
        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            new Thread(() -> {
                ImageIcon thumbnail = ImageUtils.loadImageFromUrl(track.getThumbnailUrl(), 200, 200);
                SwingUtilities.invokeLater(() -> albumArt.setIcon(thumbnail));
            }).start();
        } else {
            albumArt.setIcon(ImageUtils.getPlaceholderIcon(200, 200));
        }

        card.add(albumArt, "spany 3");

        // Track info
        JLabel titleLabel = new JLabel(track.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        card.add(titleLabel, "wrap");

        JLabel artistLabel = new JLabel(track.getArtist());
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        artistLabel.setForeground(new Color(180, 180, 180));
        card.add(artistLabel, "wrap");

        // Play button (BESAR)
        JButton playButton = new JButton("PLAY NOW", IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        playButton.setFocusPainted(false);
        playButton.setPreferredSize(new Dimension(200, 50));
        playButton.setBackground(new Color(100, 100, 255));
        playButton.setForeground(Color.WHITE);
        playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> playTrack(track, index));
        card.add(playButton, "gaptop 10, width 200!, height 50!");

        return card;
    }

    /**
     * Create SMALL track card untuk list
     */
    private JPanel createSmallTrackCard(Track track, int index) {
        JPanel card = new JPanel(new MigLayout("fillx, insets 10", "[]10[grow]10[]", ""));
        card.setBackground(new Color(40, 40, 45));
        card.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55), 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Small thumbnail
        JLabel thumbnail = new JLabel();
        thumbnail.setPreferredSize(new Dimension(50, 50));
        thumbnail.setHorizontalAlignment(SwingConstants.CENTER);
        thumbnail.setOpaque(true);
        thumbnail.setBackground(new Color(60, 60, 65));

        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            new Thread(() -> {
                ImageIcon img = ImageUtils.loadImageFromUrl(track.getThumbnailUrl(), 50, 50);
                SwingUtilities.invokeLater(() -> thumbnail.setIcon(img));
            }).start();
        } else {
            thumbnail.setText("🎵");
            thumbnail.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        }

        card.add(thumbnail);

        // Track info
        JPanel infoPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]5[]"));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(track.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        infoPanel.add(titleLabel, "wrap");

        JLabel artistLabel = new JLabel(track.getArtist());
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        artistLabel.setForeground(new Color(150, 150, 150));
        infoPanel.add(artistLabel);

        card.add(infoPanel, "grow");

        // Play button
        JButton playButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playButton.setFocusPainted(false);
        playButton.setPreferredSize(new Dimension(40, 40));
        playButton.addActionListener(e -> playTrack(track, index));
        card.add(playButton);

        // Hover effect
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(50, 50, 55));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(40, 40, 45));
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    playTrack(track, index);
                }
            }
        });

        return card;
    }

    /**
     * Play track
     */
    private void playTrack(Track track, int index) {
        // Set queue dan play
        controller.setQueueAndPlay(currentSearchResults, index);
    }
}