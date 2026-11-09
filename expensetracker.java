
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExpenseTracker extends JFrame {

    Color blue = new Color(45, 105, 220);
    Color background = new Color(240, 244, 250);

    ArrayList<String> categories = new ArrayList<>(
        Arrays.asList("Food", "Travel", "Shopping", "Education")
    );

    ArrayList<Expense> expenses = new ArrayList<>();

    double budget = 10000;

    JLabel totalLabel = new JLabel();
    JLabel monthlyLabel = new JLabel();
    JLabel budgetLabel = new JLabel();
    JLabel remainingLabel = new JLabel();

    DefaultTableModel tableModel;
    JTable expenseTable;

    DefaultListModel<String> categoryModel =
        new DefaultListModel<>();

    JList<String> categoryList;

    JTextField descriptionField = new JTextField(12);
    JTextField amountField = new JTextField(8);
    JComboBox<String> categoryBox = new JComboBox<>();

    JTextField budgetField = new JTextField("10000", 10);
    JTextArea reportArea = new JTextArea();

    public ExpenseTracker() {

        setTitle("Personal Expense Tracker");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(background);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", createDashboard());
        tabs.addTab("Expenses", createExpensesPanel());
        tabs.addTab("Categories", createCategoriesPanel());
        tabs.addTab("Reports", createReportsPanel());

        add(tabs);

        refreshAll();

        setVisible(true);
    }

    // EXPENSE MODEL

    class Expense {
        String description;
        String category;
        double amount;
        LocalDate date;

        Expense(String description, String category,
                double amount, LocalDate date) {

            this.description = description;
            this.category = category;
            this.amount = amount;
            this.date = date;
        }
    }

    // DASHBOARD

    JPanel createDashboard() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Expense Dashboard");
        heading.setFont(new Font("Arial", Font.BOLD, 26));

        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 15));
        cards.setBackground(background);

        cards.add(createCard("Total Expenses", totalLabel));
        cards.add(createCard("This Month", monthlyLabel));
        cards.add(createCard("Total Budget", budgetLabel));
        cards.add(createCard("Remaining", remainingLabel));

        JPanel top = new JPanel(new BorderLayout(10, 15));
        top.setBackground(background);
        top.add(heading, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);

        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBackground(Color.WHITE);
        chartContainer.setBorder(
            BorderFactory.createTitledBorder("Category-wise Expenses")
        );

        chartContainer.add(new PieChart(), BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(chartContainer, BorderLayout.CENTER);

        return panel;
    }

    JPanel createCard(String title, JLabel value) {

        JPanel card = new JPanel(new BorderLayout(5, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 235)),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
            )
        );

        JLabel name = new JLabel(title);
        name.setFont(new Font("Arial", Font.BOLD, 14));

        value.setFont(new Font("Arial", Font.BOLD, 22));
        value.setForeground(blue);

        card.add(name, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    // EXPENSES PANEL

    JPanel createExpensesPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        form.add(new JLabel("Description:"));
        form.add(descriptionField);

        form.add(new JLabel("Amount:"));
        form.add(amountField);

        form.add(new JLabel("Category:"));
        form.add(categoryBox);

        JButton addButton = new JButton("Add Expense");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");

        addButton.addActionListener(e -> addExpense());
        editButton.addActionListener(e -> editExpense());
        deleteButton.addActionListener(e -> deleteExpense());

        form.add(addButton);
        form.add(editButton);
        form.add(deleteButton);

        tableModel = new DefaultTableModel(
            new String[]{"Description", "Category", "Amount", "Date"}, 0
        ) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        expenseTable = new JTable(tableModel);
        expenseTable.setRowHeight(28);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(expenseTable), BorderLayout.CENTER);

        return panel;
    }

    void addExpense() {

        try {
            String description = descriptionField.getText().trim();
            String amountText = amountField.getText().trim();

            if (description.isEmpty() || amountText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please enter description and amount.");
                return;
            }

            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Amount must be greater than zero.");
                return;
            }

            String category = (String) categoryBox.getSelectedItem();

            if (category == null) {
                JOptionPane.showMessageDialog(this,
                    "Please add a category first.");
                return;
            }

            expenses.add(new Expense(
                description, category, amount, LocalDate.now()
            ));

            descriptionField.setText("");
            amountField.setText("");

            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Expense added successfully!");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid amount.");
        }
    }

    void editExpense() {

        int row = expenseTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an expense.");
            return;
        }

        Expense expense = expenses.get(row);

        String description = JOptionPane.showInputDialog(
            this, "Enter description:", expense.description
        );

        if (description == null) return;

        description = description.trim();

        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Description cannot be empty.");
            return;
        }

        String amountText = JOptionPane.showInputDialog(
            this, "Enter amount:", expense.amount
        );

        if (amountText == null) return;

        try {
            double amount = Double.parseDouble(amountText.trim());

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Amount must be greater than zero.");
                return;
            }

            String[] categoryOptions =
                categories.toArray(new String[0]);

            String category = (String) JOptionPane.showInputDialog(
                this,
                "Select category:",
                "Edit Category",
                JOptionPane.QUESTION_MESSAGE,
                null,
                categoryOptions,
                expense.category
            );

            if (category == null) return;

            expense.description = description;
            expense.amount = amount;
            expense.category = category;

            refreshAll();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid amount.");
        }
    }

    void deleteExpense() {

        int row = expenseTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an expense.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this expense?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            expenses.remove(row);
            refreshAll();
        }
    }

    // CATEGORIES PANEL

    JPanel createCategoriesPanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Manage Categories");
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        categoryList = new JList<>(categoryModel);
        categoryList.setFont(new Font("Arial", Font.PLAIN, 17));

        JButton addButton = new JButton("Add Category");
        JButton deleteButton = new JButton("Delete Category");

        addButton.addActionListener(e -> addCategory());
        deleteButton.addActionListener(e -> deleteCategory());

        JPanel buttons = new JPanel();
        buttons.add(addButton);
        buttons.add(deleteButton);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    void addCategory() {

        String input = JOptionPane.showInputDialog(
            this, "Enter category name:"
        );

        if (input == null) return;

        final String name = input.trim();

        boolean exists = false;

        for (String c : categories) {
            if (c.equalsIgnoreCase(name)) {
                exists = true;
                break;
            }
        }

        if (!name.isEmpty() && !exists) {
            categories.add(name);
            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Category added successfully!");
        } else {
            JOptionPane.showMessageDialog(this,
                "Enter a valid, unique category.");
        }
    }

    void deleteCategory() {

        String selected = categoryList.getSelectedValue();

        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a category.");
            return;
        }

        for (Expense expense : expenses) {
            if (expense.category.equals(selected)) {
                JOptionPane.showMessageDialog(this,
                    "This category is used by an expense.");
                return;
            }
        }

        categories.remove(selected);
        refreshAll();
    }

    // REPORTS PANEL

    JPanel createReportsPanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controls.add(new JLabel("Set Monthly Budget:"));
        controls.add(budgetField);

        JButton setBudget = new JButton("Update Budget");
        JButton lowButton = new JButton("Low to High");
        JButton highButton = new JButton("High to Low");

        setBudget.addActionListener(e -> updateBudget());
        lowButton.addActionListener(e -> sortExpenses(true));
        highButton.addActionListener(e -> sortExpenses(false));

        controls.add(setBudget);
        controls.add(lowButton);
        controls.add(highButton);

        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        return panel;
    }

    void updateBudget() {

        try {
            double newBudget =
                Double.parseDouble(budgetField.getText().trim());

            if (newBudget <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Budget must be greater than zero.");
                return;
            }

            budget = newBudget;
            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Budget updated successfully!");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Enter a valid budget.");
        }
    }

    void sortExpenses(boolean ascending) {

        if (ascending) {
            expenses.sort(
                Comparator.comparingDouble(e -> e.amount)
            );
        } else {
            expenses.sort(
                (a, b) -> Double.compare(b.amount, a.amount)
            );
        }

        refreshAll();
    }

    // REFRESH ALL DATA

    void refreshAll() {

        double total = 0;
        double monthly = 0;

        YearMonth currentMonth = YearMonth.now();

        for (Expense expense : expenses) {

            total += expense.amount;

            if (YearMonth.from(expense.date).equals(currentMonth)) {
                monthly += expense.amount;
            }
        }

        totalLabel.setText(String.format("Rs. %.2f", total));
        monthlyLabel.setText(String.format("Rs. %.2f", monthly));
        budgetLabel.setText(String.format("Rs. %.2f", budget));

        double remaining = budget - monthly;

        remainingLabel.setText(
            String.format("Rs. %.2f", remaining)
        );

        remainingLabel.setForeground(
            remaining < 0 ? Color.RED : new Color(30, 150, 80)
        );

        refreshExpenseTable();
        refreshCategories();
        refreshCategoryBox();
        refreshReport();

        repaint();
    }

    void refreshExpenseTable() {

        if (tableModel == null) return;

        tableModel.setRowCount(0);

        for (Expense expense : expenses) {

            tableModel.addRow(new Object[]{
                expense.description,
                expense.category,
                String.format("Rs. %.2f", expense.amount),
                expense.date
            });
        }
    }

    void refreshCategories() {

        if (categoryModel == null) return;

        categoryModel.clear();

        for (String category : categories) {
            categoryModel.addElement(category);
        }
    }

    void refreshCategoryBox() {

        if (categoryBox == null) return;

        String selected = (String) categoryBox.getSelectedItem();

        categoryBox.removeAllItems();

        for (String category : categories) {
            categoryBox.addItem(category);
        }

        if (selected != null) {
            categoryBox.setSelectedItem(selected);
        }
    }

    void refreshReport() {

        if (reportArea == null) return;

        StringBuilder report = new StringBuilder();

        report.append("PERSONAL EXPENSE REPORT\n");
        report.append("============================\n\n");

        double total = 0;
        double monthly = 0;

        for (Expense expense : expenses) {

            total += expense.amount;

            if (YearMonth.from(expense.date).equals(YearMonth.now())) {
                monthly += expense.amount;
            }
        }

        report.append(String.format("Total Expenses : Rs. %.2f\n", total));
        report.append(String.format("This Month     : Rs. %.2f\n", monthly));
        report.append(String.format("Monthly Budget : Rs. %.2f\n", budget));
        report.append(String.format(
            "Remaining      : Rs. %.2f\n\n", budget - monthly
        ));

        report.append("EXPENSE DETAILS\n");
        report.append("----------------------------\n");

        for (Expense expense : expenses) {

            report.append(String.format(
                "%-18s %-12s Rs. %.2f  %s\n",
                expense.description,
                expense.category,
                expense.amount,
                expense.date
            ));
        }

        reportArea.setText(report.toString());
    }

    // CATEGORY PIE CHART

    class PieChart extends JPanel {

        Color[] colors = {
            new Color(45, 105, 220),
            new Color(40, 170, 120),
            new Color(245, 160, 50),
            new Color(180, 90, 200),
            new Color(230, 90, 100),
            new Color(80, 160, 190)
        };

        PieChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(500, 350));
        }

        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            Map<String, Double> data = new LinkedHashMap<>();

            for (Expense expense : expenses) {
                double value = data.containsKey(expense.category)
                    ? data.get(expense.category) : 0;

                data.put(expense.category, value + expense.amount);
            }

            double total = 0;

            for (double value : data.values()) {
                total += value;
            }

            int x = 40;
            int y = 40;
            int size = 230;

            if (total == 0) {
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                g2.drawString("No expenses available", x + 20, y + 120);
                return;
            }

            double startAngle = 0;
            int index = 0;

            for (Map.Entry<String, Double> entry : data.entrySet()) {

                double angle = entry.getValue() / total * 360;

                g2.setColor(colors[index % colors.length]);

                g2.fill(new Arc2D.Double(
                    x, y, size, size,
                    startAngle, angle, Arc2D.PIE
                ));

                int legendY = 55 + index * 30;

                g2.fillRect(330, legendY, 15, 15);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.PLAIN, 13));

                g2.drawString(
                    entry.getKey() + " - Rs. " +
                    String.format("%.2f", entry.getValue()),
                    355, legendY + 13
                );

                startAngle += angle;
                index++;
            }
        }
    }

    // MAIN METHOD

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new ExpenseTracker();
        });
    }
}
