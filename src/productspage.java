import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class productspage extends JFrame {
    private JPanel mainPanel;
    private JTable productTable;
    private JTextField searchField;
    private JButton addButton;
    private JButton updateButton;
    private JButton removeButton;
    private JButton exportButton;
    private JButton topProductsButton;
    private JComboBox<String> topProductsComboBox;
    private JButton viewProductButton;
    private JPanel chartPanel;

    private DefaultTableModel tableModel;
    private final Connection conn;

    private final List<String> chartNames = new ArrayList<>();
    private final List<Double> chartValues = new ArrayList<>();

    public productspage(Connection connection) {
        this.conn = connection;

        setTitle("Products Management");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);

        setupTableModel();
        setupListeners();
        setupChartPanel();
        loadProducts();

        setVisible(true);
    }

    private void setupTableModel() {
        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Price", "Category"}, 0);
        productTable.setModel(tableModel);
    }

    private void setupListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterProducts(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterProducts(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterProducts(searchField.getText()); }
        });

        addButton.addActionListener(e -> openProductDialog(false));
        updateButton.addActionListener(e -> openProductDialog(true));
        removeButton.addActionListener(e -> removeProduct());
        exportButton.addActionListener(e -> exportToCSV());
        viewProductButton.addActionListener(e -> openProductProfile());
        topProductsButton.addActionListener(e -> showTopProductsChart());
    }

    private void setupChartPanel() {
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setLayout(new BorderLayout());

        JComponent chartComponent = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart(g);
            }

            @Override
            public Dimension getPreferredSize() {
                int barWidth = 60; // Width of each bar
                int totalBars = chartNames.size();
                int width = Math.max(1200, totalBars * (barWidth * 2)); // Ensuring at least 1200px of space for chart
                return new Dimension(width, 400); // Make sure it’s large enough for all items
            }

        };

        JScrollPane chartScrollPane = new JScrollPane(chartComponent,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chartScrollPane.getHorizontalScrollBar().setUnitIncrement(16); // make it smoother

        chartPanel.add(chartScrollPane, BorderLayout.CENTER);
    }



    private void loadProducts() {
        filterProducts("");
    }

    private void filterProducts(String keyword) {
        try {
            tableModel.setRowCount(0);
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM Products WHERE ProductName LIKE ? OR Category LIKE ? OR ProductPrice LIKE ?"
            );
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getDouble("ProductPrice"),
                        rs.getString("Category")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading products.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openProductDialog(boolean isUpdate) {
        int productId = -1;
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField categoryField = new JTextField();

        if (isUpdate) {
            int row = productTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a product to update.");
                return;
            }
            productId = (int) tableModel.getValueAt(row, 0);
            nameField.setText((String) tableModel.getValueAt(row, 1));
            priceField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
            categoryField.setText((String) tableModel.getValueAt(row, 3));
        }

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Product Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Product Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                isUpdate ? "Update Product" : "Add Product", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                conn.setAutoCommit(false);
                PreparedStatement ps;
                if (isUpdate) {
                    ps = conn.prepareStatement("UPDATE Products SET ProductName=?, ProductPrice=?, Category=? WHERE ProductID=?");
                    ps.setString(1, nameField.getText());
                    ps.setDouble(2, Double.parseDouble(priceField.getText()));
                    ps.setString(3, categoryField.getText());
                    ps.setInt(4, productId);
                } else {
                    ps = conn.prepareStatement("INSERT INTO Products (ProductName, ProductPrice, Category) VALUES (?, ?, ?)");
                    ps.setString(1, nameField.getText());
                    ps.setDouble(2, Double.parseDouble(priceField.getText()));
                    ps.setString(3, categoryField.getText());
                }
                ps.executeUpdate();
                conn.commit();
                loadProducts();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void removeProduct() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to remove.");
            return;
        }

        int productId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this product?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                conn.setAutoCommit(false);
                PreparedStatement ps = conn.prepareStatement("DELETE FROM Products WHERE ProductID = ?");
                ps.setInt(1, productId);
                ps.executeUpdate();
                conn.commit();
                loadProducts();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Error removing product.", "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void exportToCSV() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                FileWriter csvWriter = new FileWriter(fileChooser.getSelectedFile() + ".csv");

                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    csvWriter.append(tableModel.getColumnName(i)).append(",");
                }
                csvWriter.append("\n");

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        csvWriter.append(String.valueOf(tableModel.getValueAt(i, j))).append(",");
                    }
                    csvWriter.append("\n");
                }

                csvWriter.flush();
                csvWriter.close();
                JOptionPane.showMessageDialog(this, "Exported successfully!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exporting to CSV.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTopProductsChart() {
        int limit = switch ((String) topProductsComboBox.getSelectedItem()) {
            case "Top 5" -> 5;
            case "Top 10" -> 10;
            default -> 3;
        };

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.ProductName, SUM(s.ProductPrice * s.QuantitySold) AS TotalSales " +
                            "FROM Products p JOIN Sales s ON p.ProductID = s.ProductID " +
                            "GROUP BY p.ProductID, p.ProductName " +
                            "ORDER BY TotalSales DESC LIMIT ?"
            );
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            chartNames.clear();
            chartValues.clear();

            while (rs.next()) {
                chartNames.add(rs.getString("ProductName"));
                chartValues.add(rs.getDouble("TotalSales"));
            }

            chartPanel.repaint();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading chart data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void drawChart(Graphics g) {
        if (chartNames.isEmpty() || chartValues.isEmpty()) return;

        int width = chartPanel.getWidth();
        int height = chartPanel.getHeight();
        double maxValue = chartValues.stream().mapToDouble(Double::doubleValue).max().orElse(1);

        int barWidth = 50; // Width of each bar
        int spacing = 20;  // Space between bars

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < chartValues.size(); i++) {
            int barHeight = (int) ((chartValues.get(i) / maxValue) * (height - 100)); // Leave space at the bottom for names
            int x = i * (barWidth + spacing) + spacing;
            int y = height - barHeight - 60; // Leave space for value text above the bar

            // Draw bar
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x, y, barWidth, barHeight);

            // Adjust the position of the product name to be closer to the bar's bottom
            g2d.setColor(Color.BLACK);
            String productName = chartNames.get(i);
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(productName);

            // Position the name closer to the bar's bottom and ensure it doesn't overlap
            int nameX = x + (barWidth - nameWidth) / 2;
            int nameY = height - 40; // Close to the bottom but still within the chart bounds

            // Draw the product name (wrapping if needed)
            g2d.drawString(productName, nameX, nameY);

            // Draw value on top of the bar
            String valueText = "$" + String.format("%.0f", chartValues.get(i));
            int valueWidth = fm.stringWidth(valueText);
            int valueX = x + (barWidth - valueWidth) / 2;
            int valueY = y - 5;
            g2d.drawString(valueText, valueX, valueY);
        }
    }




    private void openProductProfile() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to view.");
            return;
        }

        int productId = (int) tableModel.getValueAt(selectedRow, 0);

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT ProductName, ProductPrice, Category, ProductImage FROM Products WHERE ProductID = ?");
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String productName = rs.getString("ProductName");
                double productPrice = rs.getDouble("ProductPrice");
                String category = rs.getString("Category");
                InputStream imageStream = rs.getBinaryStream("ProductImage");

                JPanel profilePanel = new JPanel(new BorderLayout(10, 10));
                JTextArea infoArea = new JTextArea(
                        "Product Name: " + productName + "\n" +
                                "Price: $" + productPrice + "\n" +
                                "Category: " + category
                );
                infoArea.setEditable(false);
                infoArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
                profilePanel.add(infoArea, BorderLayout.CENTER);

                if (imageStream != null) {
                    Image image = ImageIO.read(imageStream);
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(200, 200, Image.SCALE_SMOOTH));
                    JLabel imageLabel = new JLabel(icon);
                    profilePanel.add(imageLabel, BorderLayout.EAST);
                } else {
                    profilePanel.add(new JLabel("No image available."), BorderLayout.EAST);
                }

                JOptionPane.showMessageDialog(this, profilePanel, "Product Profile", JOptionPane.PLAIN_MESSAGE);

            } else {
                JOptionPane.showMessageDialog(this, "Product not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading product profile: " + e.getMessage());
        }
    }
}
