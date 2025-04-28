import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;

public class clientsSalesPage extends JFrame {
    private final Connection conn;
    private JPanel panel1;
    private JTable salesTable;
    private JTextField searchField;
    private JComboBox<String> yearFilter;
    private JButton backButton;
    private JButton exportButton;
    private JButton addSaleButton;
    private JButton viewProfileButton;
    private JButton updateSaleButton;
    private JButton viewInsightsButton;
    private DefaultTableModel tableModel;

    public clientsSalesPage(Connection connection) {
        this.conn = connection;

        setTitle("Client Sales");
        setSize(950, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(panel1); // Important: panel1 is from GUI designer.

        // Here: setup table model MANUALLY
        tableModel = new DefaultTableModel(new Object[]{
                "Sale ID", "Product", "Client", "Price", "Quantity", "Total", "Sale Date"
        }, 0);

        if (salesTable != null) { // ✅ SAFETY CHECK
            salesTable.setModel(tableModel);
        } else {
            JOptionPane.showMessageDialog(this, "salesTable not initialized properly!");
        }

        setupListeners();
        loadClientSales();

        setVisible(true);
    }

    private void setupListeners() {
        backButton.addActionListener(e -> {
            dispose();
            new salespage(conn);
        });

        exportButton.addActionListener(e -> exportToCSV());
        addSaleButton.addActionListener(e -> addSaleFlow());
        updateSaleButton.addActionListener(e -> updateSale());
        viewProfileButton.addActionListener(e -> viewClientProfile());
        viewInsightsButton.addActionListener(e -> viewInsights());

        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                loadClientSales();
            }
        });

        yearFilter.addActionListener(e -> loadClientSales());
    }

    private void loadClientSales() {
        try {
            tableModel.setRowCount(0);

            String keyword = searchField.getText().trim().toLowerCase();
            String selectedYear = (String) yearFilter.getSelectedItem();
            LocalDate today = LocalDate.now();

            StringBuilder query = new StringBuilder(
                    "SELECT s.SaleID, p.ProductName, c.ClientFullName, " +
                            "s.ProductPrice, s.QuantitySold, (s.ProductPrice * s.QuantitySold) AS Total, s.SaleDate " +
                            "FROM Sales s " +
                            "JOIN Products p ON s.ProductID = p.ProductID " +
                            "JOIN Clients c ON s.ClientID = c.ClientID " +
                            "WHERE s.SubconstructorID IS NULL "
            );

            boolean hasKeyword = !keyword.isEmpty();
            boolean hasYear = selectedYear != null && !"All".equals(selectedYear) && !"Select...".equals(selectedYear);

            if (hasKeyword) {
                query.append("AND LOWER(c.ClientFullName) LIKE ? ");
            }
            if (hasYear) {
                query.append("AND YEAR(s.SaleDate) = ? ");
            }
            query.append("ORDER BY s.SaleDate DESC");

            PreparedStatement ps = conn.prepareStatement(query.toString());

            int paramIndex = 1;
            if (hasKeyword) {
                ps.setString(paramIndex++, "%" + keyword + "%");
            }
            if (hasYear) {
                int year = "This Year".equals(selectedYear) ? today.getYear() : Integer.parseInt(selectedYear);
                ps.setInt(paramIndex++, year);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("SaleID"),
                        rs.getString("ProductName"),
                        rs.getString("ClientFullName"),
                        rs.getDouble("ProductPrice"),
                        rs.getInt("QuantitySold"),
                        rs.getDouble("Total"),
                        rs.getDate("SaleDate")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading client sales: " + e.getMessage());
        }
    }


    private void exportToCSV() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                FileWriter writer = new FileWriter(fileChooser.getSelectedFile() + ".csv");

                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.append(tableModel.getColumnName(i)).append(",");
                }
                writer.append("\n");

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        writer.append(tableModel.getValueAt(i, j).toString()).append(",");
                    }
                    writer.append("\n");
                }

                writer.flush();
                writer.close();
                JOptionPane.showMessageDialog(this, "Export successful!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
        }
    }

    private void addSaleFlow() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Is the Client already existing?", "Add Sale",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            addSaleToExistingClient();
        } else if (choice == JOptionPane.NO_OPTION) {
            addNewClientThenSale();
        }
    }

    private void addSaleToExistingClient() {
        try {
            String clientName = JOptionPane.showInputDialog(this, "Enter Client Full Name:");
            if (clientName == null || clientName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Client name cannot be empty!");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT ClientID FROM Clients WHERE ClientFullName = ?");
            ps.setString(1, clientName.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int clientId = rs.getInt("ClientID");
                addSale(clientId);
            } else {
                JOptionPane.showMessageDialog(this, "Client not found!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching client: " + e.getMessage());
        }
    }

    private void addNewClientThenSale() {
        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextField addressField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Client Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Contact Info:"));
        panel.add(contactField);
        panel.add(new JLabel("Address:"));
        panel.add(addressField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Client", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Clients (ClientFullName, ClientContactInfo, ClientAddress, ClientPurchaseDate, ProfilePicture) " +
                                "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, nameField.getText().trim());
                ps.setString(2, contactField.getText().trim());
                ps.setString(3, addressField.getText().trim());
                ps.setDate(4, Date.valueOf(LocalDate.now()));
                ps.setNull(5, Types.BLOB);

                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int clientId = rs.getInt(1);
                    JOptionPane.showMessageDialog(this, "Client added successfully!");
                    addSale(clientId);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding client: " + e.getMessage());
            }
        }
    }

    private void addSale(int clientId) {
        JTextField productIdField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Product ID:"));
        panel.add(productIdField);
        panel.add(new JLabel("Product Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Quantity Sold:"));
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Sale", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Sales (ProductID, ClientID, SubconstructorID, ProductPrice, QuantitySold, SaleDate) " +
                                "VALUES (?, ?, NULL, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, Integer.parseInt(productIdField.getText().trim()));
                ps.setInt(2, clientId);
                ps.setDouble(3, Double.parseDouble(priceField.getText().trim()));
                ps.setInt(4, Integer.parseInt(quantityField.getText().trim()));
                ps.setDate(5, Date.valueOf(LocalDate.now()));

                ps.executeUpdate();
                loadClientSales();
                JOptionPane.showMessageDialog(this, "Sale added successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding sale: " + e.getMessage());
            }
        }
    }

    private void updateSale() {
        int row = salesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a sale to update.");
            return;
        }

        int saleId = (int) tableModel.getValueAt(row, 0);
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("New Product Price:"));
        panel.add(priceField);
        panel.add(new JLabel("New Quantity Sold:"));
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Update Sale", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Sales SET ProductPrice=?, QuantitySold=? WHERE SaleID=?");
                ps.setDouble(1, Double.parseDouble(priceField.getText().trim()));
                ps.setInt(2, Integer.parseInt(quantityField.getText().trim()));
                ps.setInt(3, saleId);

                ps.executeUpdate();
                loadClientSales();
                JOptionPane.showMessageDialog(this, "Sale updated successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error updating sale: " + e.getMessage());
            }
        }
    }

    private void viewInsights() {
        try {
            String selectedYear = (String) yearFilter.getSelectedItem();
            LocalDate today = LocalDate.now();

            StringBuilder query = new StringBuilder(
                    "SELECT SUM(ProductPrice * QuantitySold) AS TotalSales " +
                            "FROM Sales WHERE SubconstructorID IS NULL "
            );

            if (!"All".equals(selectedYear)) {
                query.append("AND YEAR(SaleDate) = ?");
            }

            PreparedStatement ps = conn.prepareStatement(query.toString());
            if (!"All".equals(selectedYear)) {
                int year = "This Year".equals(selectedYear) ? today.getYear() : Integer.parseInt(selectedYear);
                ps.setInt(1, year);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double totalSales = rs.getDouble("TotalSales");
                JOptionPane.showMessageDialog(this,
                        "Total Sales: $" + String.format("%.2f", totalSales),
                        "Insights", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No sales data available.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching insights: " + e.getMessage());
        }
    }

    private void viewClientProfile() {
        int row = salesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a client first.");
            return;
        }

        String clientName = (String) tableModel.getValueAt(row, 2);

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT ClientFullName, ClientContactInfo, ClientAddress, ClientPurchaseDate, ProfilePicture " +
                            "FROM Clients WHERE ClientFullName = ?");
            ps.setString(1, clientName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("ClientFullName");
                String contact = rs.getString("ClientContactInfo");
                String address = rs.getString("ClientAddress");
                Date purchaseDate = rs.getDate("ClientPurchaseDate");
                InputStream imageStream = rs.getBinaryStream("ProfilePicture");

                JPanel profilePanel = new JPanel(new BorderLayout(10, 10));
                JTextArea infoArea = new JTextArea(
                        "Name: " + fullName + "\n" +
                                "Contact: " + contact + "\n" +
                                "Address: " + address + "\n" +
                                "First Purchase: " + purchaseDate
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

                JOptionPane.showMessageDialog(this, profilePanel, "Client Profile", JOptionPane.PLAIN_MESSAGE);

            } else {
                JOptionPane.showMessageDialog(this, "Client not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching client profile: " + e.getMessage());
        }
    }
}
