package org.examples.client.gui;

import org.examples.client.MiniJson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Panneau d'affichage des résultats de recherche
 */
public class ResultsPanel extends JPanel {

    private HotelClientGUI mainFrame;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JButton reserveButton;
    private JButton backButton;
    private JLabel infoLabel;

    private String searchCity;
    private Date searchStart;
    private Date searchEnd;
    private int searchBeds;
    private String currentOffersJson;
    private List<String> imageUrls = new ArrayList<>();
    private List<Integer> agencyPorts = new ArrayList<>(); // Port de l'agence pour chaque offre

    public ResultsPanel(HotelClientGUI mainFrame) {
        this.mainFrame = mainFrame;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 250));

        // Titre et info
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📋 Résultats de Recherche");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        infoLabel = new JLabel(" ");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        infoLabel.setForeground(Color.BLACK);
        infoLabel.setBorder(new EmptyBorder(5, 0, 10, 0));
        topPanel.add(infoLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Table des résultats
        String[] columns = {"Hôtel", "Ville", "Catégorie", "Chambre", "Lits", "Prix/Nuit", "Agence", "Référence", "Image"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setRowHeight(35);
        resultsTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultsTable.setForeground(Color.BLACK);
        // Forcer la couleur de texte de sélection en noir
        resultsTable.setSelectionForeground(Color.BLACK);
        resultsTable.setSelectionBackground(Color.LIGHT_GRAY);
        resultsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        resultsTable.getTableHeader().setBackground(Color.LIGHT_GRAY);
        resultsTable.getTableHeader().setForeground(Color.BLACK);
        resultsTable.setGridColor(new Color(200, 200, 200));

        // Alternance de couleurs des lignes avec texte NOIR forcé
        resultsTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Forcer la couleur du texte en NOIR pour toutes les cellules
                if (c instanceof JLabel) {
                    ((JLabel) c).setForeground(Color.BLACK);
                }
                c.setForeground(Color.BLACK);

                if (isSelected) {
                    c.setBackground(Color.LIGHT_GRAY);
                    // Forcer le texte en noir même en sélection
                    if (c instanceof JLabel) {
                        ((JLabel) c).setForeground(Color.BLACK);
                    }
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }

                return c;
            }
        });

        // Gestionnaire de clic pour la colonne Image
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());

                // Colonne 8 = colonne Image (après ajout colonne Agence)
                if (row >= 0 && col == 8 && row < imageUrls.size()) {
                    String imageUrl = imageUrls.get(row);
                    System.out.println("DEBUG - Clic sur image row=" + row + ", imageUrl='" + imageUrl + "'");
                    System.out.println("DEBUG - imageUrl != null: " + (imageUrl != null));
                    System.out.println("DEBUG - !imageUrl.isEmpty(): " + (imageUrl != null && !imageUrl.isEmpty()));
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        showImageDialog(imageUrl, row);
                    } else {
                        System.out.println("DEBUG - Affichage message 'Image non disponible'");
                        JOptionPane.showMessageDialog(ResultsPanel.this,
                            "Aucune image disponible pour cette chambre.\nDEBUG: imageUrl='" + imageUrl + "'",
                            "Image non disponible",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        // Changer le curseur au survol de la colonne Image
        resultsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());

                if (col == 8 && row >= 0 && row < imageUrls.size() &&
                    imageUrls.get(row) != null && !imageUrls.get(row).isEmpty()) {
                    resultsTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    resultsTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        add(scrollPane, BorderLayout.CENTER);

        // Panneau de boutons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        bottomPanel.setOpaque(false);

        backButton = new JButton("⬅️ Nouvelle Recherche");
        backButton.setFont(new Font("Arial", Font.PLAIN, 14));
        backButton.setPreferredSize(new Dimension(200, 45));
        backButton.setBackground(Color.LIGHT_GRAY);
        backButton.setForeground(Color.BLACK);
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> mainFrame.showPanel("SEARCH"));

        reserveButton = new JButton("✅ Réserver la Chambre Sélectionnée");
        reserveButton.setFont(new Font("Arial", Font.BOLD, 16));
        reserveButton.setPreferredSize(new Dimension(320, 45));
        reserveButton.setBackground(Color.LIGHT_GRAY);
        reserveButton.setForeground(Color.BLACK);
        reserveButton.setFocusPainted(false);
        reserveButton.setBorderPainted(false);
        reserveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reserveButton.setEnabled(false);
        reserveButton.addActionListener(e -> proceedToReservation());

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            reserveButton.setEnabled(resultsTable.getSelectedRow() >= 0);
        });

        bottomPanel.add(backButton);
        bottomPanel.add(reserveButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void displayResults(String offersJson, String city, Date start, Date end, int beds) {
        this.currentOffersJson = offersJson;
        this.searchCity = city;
        this.searchStart = start;
        this.searchEnd = end;
        this.searchBeds = beds;

        // Mise à jour info
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        infoLabel.setText(String.format("📍 %s | 📅 %s → %s | 🛏️ %d lit(s)",
            city, sdf.format(start), sdf.format(end), beds));

        // Vider la table et les listes
        tableModel.setRowCount(0);
        imageUrls.clear();
        agencyPorts.clear();

        try {
            List<String> offers = MiniJson.getStringArray(offersJson, "offers");

            if (offers == null || offers.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Aucune offre trouvée pour vos critères.",
                    "Aucun résultat",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            for (String offer : offers) {
                String hotelName = MiniJson.getString(offer, "hotelName");
                String hotelCity = MiniJson.getString(offer, "ville");
                String category = MiniJson.getString(offer, "categorie");
                // Extraire les données de la chambre depuis l'objet "room"
                String roomObj = MiniJson.getObject(offer, "room");
                Integer roomNumInt = roomObj != null ? MiniJson.getInt(roomObj, "numero") : null;
                String roomNum = roomNumInt != null ? String.valueOf(roomNumInt) : null;
                Integer bedsInt = roomObj != null ? MiniJson.getInt(roomObj, "nbLits") : null;
                String bedsStr = bedsInt != null ? String.valueOf(bedsInt) : null;
                Integer priceInt = MiniJson.getInt(offer, "prixTotal");
                String priceStr = priceInt != null ? String.valueOf(priceInt) : null;
                String reference = MiniJson.getString(offer, "offerId");
                // Lire imageUrl depuis room.imageUrl
                String imageUrl = roomObj != null ? MiniJson.getString(roomObj, "imageUrl") : null;

                // Extraire l'info de l'agence
                String agencyName = MiniJson.getString(offer, "_agencyName");
                Integer agencyPort = MiniJson.getInt(offer, "_agencyPort");

                // DEBUG: Afficher ce qui est lu
                System.out.println("DEBUG - Chambre " + roomNum + ": hotelName=" + hotelName +
                                 ", agence=" + agencyName + ", port=" + agencyPort);

                // Stocker l'URL de l'image et le port de l'agence
                imageUrls.add(imageUrl != null ? imageUrl : "");
                agencyPorts.add(agencyPort != null ? agencyPort : 0);

                tableModel.addRow(new Object[]{
                    hotelName != null ? hotelName : "?",
                    hotelCity != null ? hotelCity : "?",
                    category != null ? category : "?",
                    roomNum != null ? "N°" + roomNum : "?",
                    bedsStr != null ? bedsStr : "?",
                    priceStr != null ? priceStr + " €" : "?",
                    agencyName != null ? agencyName : "?",  // Colonne Agence
                    reference != null ? reference : "?",
                    imageUrl != null && !imageUrl.isEmpty() ? "🖼️ Voir" : "-"
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'affichage des résultats:\n" + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void proceedToReservation() {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        try {
            List<String> offers = MiniJson.getStringArray(currentOffersJson, "offers");
            if (offers != null && selectedRow < offers.size()) {
                String selectedOffer = offers.get(selectedRow);
                int agencyPort = agencyPorts.get(selectedRow);

                // Passer au panneau de réservation
                ReservationPanel reservationPanel = (ReservationPanel) ((JPanel) mainFrame.getContentPane().getComponent(0))
                    .getComponent(3);
                reservationPanel.setReservationData(selectedOffer, searchStart, searchEnd, agencyPort);
                mainFrame.showPanel("RESERVATION");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de la sélection:\n" + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Affiche l'image de la chambre dans une nouvelle fenêtre
     */
    private void showImageDialog(String imageUrl, int row) {
        // ✅ Dialog NON-MODAL pour permettre le chargement asynchrone
        JDialog imageDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "🖼️ Image de la Chambre", false);
        imageDialog.setLayout(new BorderLayout());
        imageDialog.setSize(650, 550);
        imageDialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        try {
            // Déterminer le serveur à partir de l'offre sélectionnée
            List<String> offers = MiniJson.getStringArray(currentOffersJson, "offers");
            String selectedOffer = offers.get(row);
            String hotelName = MiniJson.getString(selectedOffer, "hotelName");

            System.out.println("DEBUG showImageDialog - hotelName: " + hotelName + ", imageUrl: " + imageUrl);

            // Déterminer le port du serveur en fonction de l'hôtel
            int serverPort = 8082; // Par défaut Rivage
            if (hotelName != null && hotelName.toLowerCase().contains("opera")) {
                serverPort = 8084; // Opera
            }

            // Construire l'URL complète
            String fullUrl = "http://localhost:" + serverPort + imageUrl;
            System.out.println("DEBUG showImageDialog - URL complète: " + fullUrl);

            // Message de chargement
            JLabel loadingLabel = new JLabel("⏳ Chargement de l'image...", SwingConstants.CENTER);
            loadingLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            contentPanel.add(loadingLabel, BorderLayout.CENTER);
            imageDialog.add(contentPanel, BorderLayout.CENTER);

            // ✅ Afficher la fenêtre AVANT de lancer le thread (dialog non-modal maintenant)
            imageDialog.setVisible(true);

            // Récupérer imageData depuis l'offre (avant le thread)
            String roomObj = MiniJson.getObject(selectedOffer, "room");
            final String imageData = roomObj != null ? MiniJson.getString(roomObj, "imageData") : null;

            // Charger l'image en arrière-plan
            new Thread(() -> {
                System.out.println("DEBUG Thread - Démarrage du chargement de: " + fullUrl);
                try {
                    System.out.println("DEBUG Thread - Création URL objet...");
                    java.net.URL url = new java.net.URL(fullUrl);
                    System.out.println("DEBUG Thread - URL créée: " + url);

                    System.out.println("DEBUG Thread - imageData présent: " + (imageData != null && !imageData.isEmpty()));

                    if (imageData != null && !imageData.isEmpty()) {
                        System.out.println("DEBUG Thread - Décodage image Base64...");
                        // Afficher l'image décodée depuis Base64
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("DEBUG SwingUtilities - Affichage image Base64...");
                            contentPanel.removeAll();

                            try {
                                // Décoder le Base64
                                byte[] imageBytes = java.util.Base64.getDecoder().decode(imageData);
                                System.out.println("DEBUG - Image décodée: " + imageBytes.length + " bytes");

                                // Convertir les bytes en String (SVG est du texte XML)
                                String svgContent = new String(imageBytes, "UTF-8");
                                System.out.println("DEBUG - Contenu SVG: " + svgContent.substring(0, Math.min(100, svgContent.length())));

                                // Extraire les informations du SVG pour l'affichage
                                String hotelText = extractTextFromSVG(svgContent, 0);
                                String typeText = extractTextFromSVG(svgContent, 1);
                                String infoText = extractTextFromSVG(svgContent, 2);
                                Color bgColor = extractColorFromSVG(svgContent);

                                System.out.println("DEBUG - Textes extraits: " + hotelText + ", " + typeText + ", " + infoText);

                                // Créer un panneau personnalisé qui dessine l'aperçu
                                JPanel imagePanel = new JPanel() {
                                    @Override
                                    protected void paintComponent(Graphics g) {
                                        super.paintComponent(g);
                                        Graphics2D g2d = (Graphics2D) g;
                                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                                        int width = getWidth();
                                        int height = getHeight();

                                        // Fond coloré (couleur extraite du SVG)
                                        g2d.setColor(bgColor);
                                        g2d.fillRect(0, 0, width, height);

                                        // Rectangle central avec bordure
                                        int rectWidth = Math.min(width - 100, 400);
                                        int rectHeight = Math.min(height - 100, 300);
                                        int rectX = (width - rectWidth) / 2;
                                        int rectY = (height - rectHeight) / 2;

                                        Color rectColor = bgColor.darker();
                                        g2d.setColor(rectColor);
                                        g2d.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 20, 20);

                                        Color borderColor = rectColor.darker();
                                        g2d.setColor(borderColor);
                                        g2d.setStroke(new java.awt.BasicStroke(4));
                                        g2d.drawRoundRect(rectX, rectY, rectWidth, rectHeight, 20, 20);

                                        // Textes centrés
                                        g2d.setColor(borderColor.darker());

                                        // Titre principal
                                        g2d.setFont(new Font("Arial", Font.BOLD, 24));
                                        FontMetrics fm1 = g2d.getFontMetrics();
                                        int text1Width = fm1.stringWidth(hotelText);
                                        g2d.drawString(hotelText, (width - text1Width) / 2, rectY + rectHeight / 2 - 30);

                                        // Type de chambre
                                        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
                                        FontMetrics fm2 = g2d.getFontMetrics();
                                        int text2Width = fm2.stringWidth(typeText);
                                        g2d.drawString(typeText, (width - text2Width) / 2, rectY + rectHeight / 2 + 5);

                                        // Informations
                                        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                                        FontMetrics fm3 = g2d.getFontMetrics();
                                        int text3Width = fm3.stringWidth(infoText);
                                        g2d.drawString(infoText, (width - text3Width) / 2, rectY + rectHeight / 2 + 35);
                                    }
                                };
                                imagePanel.setPreferredSize(new Dimension(500, 400));
                                imagePanel.setBackground(Color.WHITE);
                                contentPanel.add(imagePanel, BorderLayout.CENTER);

                                System.out.println("DEBUG - Image affichée avec succès");

                                JLabel infoLabel = new JLabel("🖼️ Image de la chambre", SwingConstants.CENTER);
                                infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
                                infoLabel.setBorder(new EmptyBorder(10, 5, 5, 5));
                                contentPanel.add(infoLabel, BorderLayout.NORTH);

                                // Boutons
                                JPanel buttonPanel = new JPanel(new FlowLayout());

                                // Bouton pour ouvrir dans le navigateur
                                JButton browserButton = new JButton("🌐 Ouvrir dans le navigateur");
                                browserButton.setPreferredSize(new Dimension(220, 40));
                                browserButton.setBackground(new Color(70, 130, 180));
                                browserButton.setForeground(Color.BLACK); // ✅ CHANGÉ EN NOIR
                                browserButton.setFont(new Font("Arial", Font.BOLD, 13));
                                browserButton.setFocusPainted(false);
                                browserButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                                browserButton.addActionListener(e -> {
                                    try {
                                        java.awt.Desktop.getDesktop().browse(new java.net.URI(fullUrl));
                                        System.out.println("DEBUG - Ouverture navigateur: " + fullUrl);
                                    } catch (Exception ex) {
                                        System.err.println("DEBUG - Erreur ouverture navigateur: " + ex.getMessage());
                                        JOptionPane.showMessageDialog(imageDialog,
                                            "Impossible d'ouvrir le navigateur.\nURL: " + fullUrl,
                                            "Erreur",
                                            JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                                buttonPanel.add(browserButton);

                                // Bouton fermer
                                JButton closeButton = new JButton("Fermer");
                                closeButton.setPreferredSize(new Dimension(100, 40));
                                closeButton.setBackground(Color.LIGHT_GRAY);
                                closeButton.setForeground(Color.BLACK);
                                closeButton.setFont(new Font("Arial", Font.BOLD, 13));
                                closeButton.setFocusPainted(false);
                                closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                                closeButton.addActionListener(e -> imageDialog.dispose());
                                buttonPanel.add(closeButton);

                                contentPanel.add(buttonPanel, BorderLayout.SOUTH);

                                contentPanel.revalidate();
                                contentPanel.repaint();

                                System.out.println("DEBUG SwingUtilities - Interface mise à jour avec succès");

                            } catch (Exception ex) {
                                System.err.println("DEBUG SwingUtilities - ERREUR: " + ex.getMessage());
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(imageDialog,
                                    "Erreur de décodage de l'image:\n" + ex.getMessage(),
                                    "Erreur",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    } else {
                        // Pour les images PNG/JPG/etc.
                        ImageIcon imageIcon = new ImageIcon(url);

                        SwingUtilities.invokeLater(() -> {
                            contentPanel.removeAll();

                            if (imageIcon.getIconWidth() > 0) {
                                // Redimensionner si nécessaire
                                Image image = imageIcon.getImage();
                                Image scaledImage = image.getScaledInstance(600, 450, Image.SCALE_SMOOTH);
                                ImageIcon scaledIcon = new ImageIcon(scaledImage);

                                JLabel imageLabel = new JLabel(scaledIcon);
                                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

                                JScrollPane scrollPane = new JScrollPane(imageLabel);
                                contentPanel.add(scrollPane, BorderLayout.CENTER);
                            } else {
                                JLabel errorLabel = new JLabel("❌ Impossible de charger l'image", SwingConstants.CENTER);
                                errorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                                contentPanel.add(errorLabel, BorderLayout.CENTER);
                            }

                            // Bouton de fermeture
                            JPanel buttonPanel = new JPanel();
                            JButton closeButton = new JButton("Fermer");
                            closeButton.setPreferredSize(new Dimension(100, 35));
                            closeButton.setBackground(Color.LIGHT_GRAY);
                            closeButton.setForeground(Color.BLACK);
                            closeButton.addActionListener(e -> imageDialog.dispose());
                            buttonPanel.add(closeButton);
                            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

                            contentPanel.revalidate();
                            contentPanel.repaint();
                        });
                    }

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(imageDialog,
                            "Erreur lors du chargement de l'image:\n" + e.getMessage() +
                            "\n\nURL: " + fullUrl,
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                        imageDialog.dispose();
                    });
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'affichage de l'image:\n" + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            imageDialog.dispose();
        }
    }

    /**
     * Extrait le texte d'un élément <text> du SVG
     */
    private String extractTextFromSVG(String svg, int index) {
        try {
            int count = 0;
            int pos = 0;
            while ((pos = svg.indexOf("<text", pos)) != -1) {
                if (count == index) {
                    int start = svg.indexOf(">", pos) + 1;
                    int end = svg.indexOf("</text>", start);
                    if (start > 0 && end > start) {
                        return svg.substring(start, end).trim();
                    }
                }
                count++;
                pos++;
            }
        } catch (Exception e) {
            System.err.println("Erreur extraction texte: " + e.getMessage());
        }
        return "";
    }

    /**
     * Extrait la couleur de fond du SVG
     */
    private Color extractColorFromSVG(String svg) {
        try {
            int rectPos = svg.indexOf("<rect");
            if (rectPos != -1) {
                int fillPos = svg.indexOf("fill=", rectPos);
                if (fillPos != -1 && fillPos < svg.indexOf(">", rectPos)) {
                    int start = svg.indexOf("\"", fillPos) + 1;
                    int end = svg.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        String colorStr = svg.substring(start, end);
                        return parseColor(colorStr);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur extraction couleur: " + e.getMessage());
        }
        return new Color(230, 230, 250);
    }

    /**
     * Parse une couleur CSS
     */
    private Color parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return Color.decode(colorStr);
            }
            switch (colorStr.toLowerCase()) {
                case "gold":
                case "#f0e68c":
                    return new Color(240, 230, 140);
                case "lavender":
                case "#e6e6fa":
                    return new Color(230, 230, 250);
                case "lightblue":
                case "#add8e6":
                    return new Color(173, 216, 230);
                case "lightgreen":
                case "#98fb98":
                    return new Color(152, 251, 152);
                default:
                    return Color.decode(colorStr);
            }
        } catch (Exception e) {
            return new Color(230, 230, 250);
        }
    }
}
