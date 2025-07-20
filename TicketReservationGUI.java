import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class TicketReservationGUI
{
    private static HashSet<String> bookedSeats = new HashSet<>();
    private static Stack<String> bookingStack = new Stack<>();
    private static Stack<String> cancellationStack = new Stack<>();
    private static HashMap<String, String> allSeats = new HashMap<>();

    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Dynamic Ticket Reservation System");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(234, 239, 243));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel seatLabel = new JLabel("Seat Number:");
        JLabel nameLabel = new JLabel("Customer Name:");
        JLabel categoryLabel = new JLabel("Category:");

        JTextField seatField = new JTextField("e.g. E1, P2, V1 (up to 500)", 15);
        JTextField nameField = new JTextField(15);
        JTextField categoryField = new JTextField("Economy, Premium, VIP", 15);
        JComboBox box=new JComboBox<>();
        box.add(categoryField);
        seatField.setForeground(Color.GRAY);
        categoryField.setForeground(Color.GRAY);

        addPlaceholder(seatField, "e.g. E1, P2, V1 (up to 500)");
        addPlaceholder(categoryField, "Economy, Premium, VIP");

        seatLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(seatLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(seatField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(categoryLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(categoryField, gbc);



        frame.add(inputPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        JTextArea messages = new JTextArea(5, 20);
        messages.setEditable(false);
        JScrollPane messageScroll = new JScrollPane(messages);
        messageScroll.setBorder(BorderFactory.createTitledBorder("System Messages"));
        centerPanel.add(messageScroll, BorderLayout.NORTH);

        String[] columnNames = {"Seat Number", "Category", "Price (PKR)", "Booked", "Customer"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("All Seats Overview"));
        centerPanel.add(tableScroll, BorderLayout.CENTER);

        frame.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        JButton bookBtn = new JButton("Book Seat");
        JButton cancelBtn = new JButton("Cancel Seat");
        JButton undoBookBtn = new JButton("Undo Last Booking");
        JButton undoCancelBtn = new JButton("Undo Last Cancellation");
        JButton lookupBtn = new JButton("Lookup Seat");

        bookBtn.setBackground(Color.GREEN);       bookBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(Color.RED);        cancelBtn.setForeground(Color.WHITE);
        undoBookBtn.setBackground(Color.ORANGE);   undoBookBtn.setForeground(Color.BLACK);
        undoCancelBtn.setBackground(Color.YELLOW); undoCancelBtn.setForeground(Color.BLACK);
        lookupBtn.setBackground(Color.BLUE);       lookupBtn.setForeground(Color.WHITE);

        buttonPanel.add(bookBtn);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(undoBookBtn);
        buttonPanel.add(undoCancelBtn);
        buttonPanel.add(lookupBtn);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        loadSeatDataset(tableModel);

        bookBtn.addActionListener(e ->
        {
            String seat = seatField.getText().trim().toUpperCase();
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();

            if (!allSeats.containsKey(seat))
            {
                messages.setText("Seat does not exist in the dataset.");
                return;
            }

            if (seat.isEmpty() || name.isEmpty() || category.isEmpty())
            {
                messages.setText("Please fill all fields properly.");
                return;
            }

            if (!seatMatchesCategory(seat, category)) {
                messages.setText("❌ Seat " + seat + " does not match category " + category + ".");
                return;
            }

            if (bookedSeats.contains(seat))
            {
                messages.setText("Seat " + seat + " is already booked.");
            } else {
                bookedSeats.add(seat);
                bookingStack.push(seat);
                cancellationStack.remove(seat);
                messages.setText("Seat " + seat + " booked for " + name + " (" + category + ").");
                updateSeatInTable(tableModel, seat, "Yes", name);
            }
        });

        cancelBtn.addActionListener(e -> {
            String seat = seatField.getText().trim().toUpperCase();
            if (!bookedSeats.contains(seat)) {
                messages.setText("Seat " + seat + " is not booked.");
            } else {
                bookedSeats.remove(seat);
                cancellationStack.push(seat);
                bookingStack.remove(seat);
                messages.setText("Seat " + seat + " has been cancelled.");
                updateSeatInTable(tableModel, seat, "No", "");
            }
        });

        undoBookBtn.addActionListener(e -> {
            if (bookingStack.isEmpty())
            {
                messages.setText("No booking to undo.");
            } else {
                String seat = bookingStack.pop();
                bookedSeats.remove(seat);
                cancellationStack.push(seat);
                messages.setText("↩ Booking undone for seat " + seat);
                updateSeatInTable(tableModel, seat, "No", "");
            }
        });

        undoCancelBtn.addActionListener(e -> {
            if (cancellationStack.isEmpty())
            {
                messages.setText("No cancellation to undo.");
            } else {
                String seat = cancellationStack.pop();
                bookedSeats.add(seat);
                bookingStack.push(seat);
                String category = allSeats.get(seat);
                messages.setText("Cancellation undone, seat " + seat + " rebooked.");
                updateSeatInTable(tableModel, seat, "Yes", nameField.getText().trim());
            }
        });

        lookupBtn.addActionListener(e -> {
            String seat = seatField.getText().trim().toUpperCase();
            if (seat.isEmpty()) {
                messages.setText("Please enter a seat number.");
            } else if (!allSeats.containsKey(seat)) {
                messages.setText("Seat does not exist.");
            } else if (bookedSeats.contains(seat)) {
                messages.setText("Seat " + seat + " is Booked.");
            } else {
                messages.setText("Seat " + seat + " is Available.");
            }
        });

        frame.setVisible(true);
    }

    private static void addPlaceholder(JTextField field, String placeholder) {
        field.addFocusListener(new FocusAdapter()
        {
            public void focusGained(FocusEvent e)
            {
                if (field.getText().equals(placeholder))
                {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty())
                {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }

    private static void updateSeatInTable(DefaultTableModel model, String seat, String booked, String customer) {
        for (int i = 0; i < model.getRowCount(); i++)
        {
            if (model.getValueAt(i, 0).equals(seat))
            {
                model.setValueAt(booked, i, 3);
                model.setValueAt(customer, i, 4);
                break;
            }
        }
    }

    private static boolean seatMatchesCategory(String seat, String category)
    {
        category = category.toLowerCase();
        return (category.equals("vip") && seat.startsWith("V")) ||
                (category.equals("premium") && seat.startsWith("P")) ||
                (category.equals("economy") && seat.startsWith("E"));
    }

    private static void loadSeatDataset(DefaultTableModel tableModel)
    {
        for (int i = 1; i <= 200; i++)
        {
            String seat = "E" + i;
            allSeats.put(seat, "Economy");
            tableModel.addRow(new Object[]{seat, "Economy", 1000, "No", ""});
        }
        for (int i = 1; i <= 200; i++)
        {
            String seat = "P" + i;
            allSeats.put(seat, "Premium");
            tableModel.addRow(new Object[]{seat, "Premium", 3000, "No", ""});
        }
        for (int i = 1; i <= 100; i++)
        {
            String seat = "V" + i;
            allSeats.put(seat, "VIP");
            tableModel.addRow(new Object[]{seat, "VIP", 5000, "No", ""});
        }
    }
}