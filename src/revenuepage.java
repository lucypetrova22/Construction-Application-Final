import javax.swing.*;
import java.sql.*;
import java.time.LocalDate;

public class revenuepage extends JFrame {
    private final Connection conn;
    private JComboBox<String> yearSelector;
    private JLabel totalRevenueLabel;
    private JLabel yearLabel;
    private JLabel avgRevenueLabel;
    private JLabel salesCountLabel;
    private JPanel panel1;

    public revenuepage(Connection connection) {
        this.conn = connection;
        setTitle("Revenue Summary");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setContentPane(panel1); // GUI handles layout

        setupListeners();
        loadRevenueData();

        setVisible(true);
    }

    private void setupListeners() {
        yearSelector.addActionListener(e -> loadRevenueData());
    }

    private void loadRevenueData() {
        String selectedYear = (String) yearSelector.getSelectedItem();

        // Query construction
        StringBuilder query = new StringBuilder(
                "SELECT SUM(FinalRevenue) AS TotalRevenue, COUNT(*) AS TotalSales " +
                        "FROM Revenue "
        );

        // If "All" is selected, no year filter is applied
        boolean filterByYear = selectedYear != null && !selectedYear.isEmpty() && !"All".equals(selectedYear);

        if (filterByYear) {
            query.append(" WHERE YEAR(RevenueDate) = ?");
        }

        try {
            PreparedStatement ps = conn.prepareStatement(query.toString());

            // If a year other than "All" is selected, add the year parameter
            if (filterByYear) {
                ps.setInt(1, Integer.parseInt(selectedYear));
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double totalRevenue = rs.getDouble("TotalRevenue");
                int totalSales = rs.getInt("TotalSales");

                double average = totalSales > 0 ? totalRevenue / totalSales : 0.0;

                totalRevenueLabel.setText("Total Revenue: $" + String.format("%.2f", totalRevenue));
                avgRevenueLabel.setText("Average per Sale: $" + String.format("%.2f", average));
                salesCountLabel.setText("Total Sales: " + totalSales);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading revenue data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
