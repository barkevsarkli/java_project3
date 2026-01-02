package com.greengrocer.controllers;

import com.greengrocer.app.SessionManager;
import com.greengrocer.models.Order;
import com.greengrocer.models.User;
import com.greengrocer.models.CarrierRating;
import com.greengrocer.services.OrderService;
import com.greengrocer.services.CarrierService;
import com.greengrocer.services.InvoiceService;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.itextpdf.text.Rectangle;
import com.greengrocer.models.OrderItem;

import io.github.palexdev.materialfx.controls.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller for customer order history view using MaterialFX components.
 * Displays past orders and allows cancellation, rating, and PDF invoice generation.
 * * @author Emir Kıraç Varol
 * @version 1.1.0
 */
public class OrderHistoryController implements Initializable {

    /** Orders list container */
    @FXML
    private VBox ordersListContainer;
    
    /** Order info section */
    @FXML
    private VBox orderInfoBox;
    
    /** Order items container (ScrollPane content) */
    @FXML
    private VBox orderItemsContainer;
    
    /** Order totals section */
    @FXML
    private VBox orderTotalsBox;
    
    /** Rating combo box (MaterialFX) */
    @FXML
    private MFXComboBox<Integer> ratingCombo;
    
    /** Rating comment field (MaterialFX) */
    @FXML
    private MFXTextField ratingCommentField;

    /** Services */
    private OrderService orderService;
    private CarrierService carrierService;
    private InvoiceService invoiceService;
    
    /** Currently selected order */
    private Order selectedOrder = null;
    private HBox selectedOrderCard = null;
    
    /** Date formatter */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Initializes the controller.
     * * @param location FXML location
     * @param resources Resource bundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        orderService = new OrderService();
        carrierService = new CarrierService();
        invoiceService = new InvoiceService();
        
        setupTable();
        loadOrders();
        
        // Set up rating options in MaterialFX combo
        ratingCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    }

    /**
     * Sets up the orders list.
     */
    private void setupTable() {
        // No longer using table, setup is done in loadOrders
    }

    /**
     * Loads customer orders into styled list.
     */
    private void loadOrders() {
        ordersListContainer.getChildren().clear();
        selectedOrder = null;
        selectedOrderCard = null;
        
        List<Order> orders = orderService.getCurrentCustomerOrders();
        if (orders == null || orders.isEmpty()) {
            Label emptyLabel = new Label("No orders found");
            emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 20;");
            ordersListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Order order : orders) {
            HBox card = createOrderCard(order);
            ordersListContainer.getChildren().add(card);
        }
    }
    
    /**
     * Creates a styled card for an order.
     */
    private HBox createOrderCard(Order order) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10; -fx-cursor: hand;");
        
        // Order number badge
        Label idLabel = new Label("#" + order.getId());
        idLabel.setMinWidth(50);
        idLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Order info
        VBox infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        String dateStr = order.getOrderTime() != null ? order.getOrderTime().format(DATE_FORMATTER) : "N/A";
        Label dateLabel = new Label("📅 " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        
        Label itemsLabel = new Label("Cart " + getItemsCount(order) + " items");
        itemsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        
        infoBox.getChildren().addAll(dateLabel, itemsLabel);
        
        // Status badge
        String statusColor = getStatusColor(order.getStatus());
        Label statusLabel = new Label(order.getStatus().toUpperCase());
        statusLabel.setPadding(new Insets(4, 10, 4, 10));
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 6; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        // Total
        VBox totalBox = new VBox(2);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label totalLabel = new Label(String.format("₺%.2f", order.getTotalCost()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        totalBox.getChildren().add(totalLabel);
        
        card.getChildren().addAll(idLabel, infoBox, statusLabel, totalBox);
        
        // Selection handling
        card.setOnMouseClicked(e -> selectOrder(order, card));
        card.setOnMouseEntered(e -> {
            if (selectedOrder != order) {
                card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 10; -fx-border-color: #BDBDBD; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedOrder != order) {
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });
        
        return card;
    }
    
    private void selectOrder(Order order, HBox card) {
        if (selectedOrderCard != null) {
            selectedOrderCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10; -fx-cursor: hand;");
        }
        selectedOrder = order;
        selectedOrderCard = card;
        card.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 10; -fx-border-color: #1976D2; -fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;");
        showOrderDetails(order);
    }
    
    private String getStatusColor(String status) {
        if (status == null) return "#757575";
        switch (status.toLowerCase()) {
            case "pending": return "#FF9800";
            case "confirmed": return "#2196F3";
            case "assigned": return "#9C27B0";
            case "delivered": return "#4CAF50";
            case "cancelled": return "#F44336";
            default: return "#757575";
        }
    }
    
    private int getItemsCount(Order order) {
        String summary = order.getProductsSummary();
        if (summary == null || summary.isEmpty()) return 0;
        return summary.split("\n").length;
    }

    /**
     * Shows order details with items in ScrollPane.
     * @param order Selected order
     */
    private void showOrderDetails(Order order) {
        // Clear all containers
        orderInfoBox.getChildren().clear();
        orderItemsContainer.getChildren().clear();
        orderTotalsBox.getChildren().clear();
        
        if (order == null) {
            return;
        }
        
        // Order info section
        Label orderIdLabel = new Label("Order #" + order.getId());
        orderIdLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Label dateLabel = new Label("📅 " + order.getOrderTime().format(DATE_FORMATTER));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        
        String statusColor = getStatusColor(order.getStatus());
        Label statusLabel = new Label("📊 " + order.getStatus().toUpperCase());
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");
        
        orderInfoBox.getChildren().addAll(orderIdLabel, dateLabel, statusLabel);
        
        // Carrier info if assigned
        if (order.getCarrierName() != null) {
            Label carrierLabel = new Label("🚚 Carrier: " + order.getCarrierName());
            carrierLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
            orderInfoBox.getChildren().add(carrierLabel);
        }
        
        // Delivery time
        if (order.getActualDeliveryTime() != null) {
            Label deliveredLabel = new Label("✅ Delivered: " + order.getActualDeliveryTime().format(DATE_FORMATTER));
            deliveredLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4CAF50;");
            orderInfoBox.getChildren().add(deliveredLabel);
        } else if (order.getRequestedDeliveryTime() != null) {
            Label scheduledLabel = new Label("📅 Scheduled: " + order.getRequestedDeliveryTime().format(DATE_FORMATTER));
            scheduledLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF9800;");
            orderInfoBox.getChildren().add(scheduledLabel);
        }
        
        // Order items in ScrollPane
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (com.greengrocer.models.OrderItem item : order.getItems()) {
                HBox itemRow = createOrderItemRow(item);
                orderItemsContainer.getChildren().add(itemRow);
            }
        } else {
            Label noItemsLabel = new Label("No items found");
            noItemsLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-style: italic;");
            orderItemsContainer.getChildren().add(noItemsLabel);
        }
        
        // Totals section
        HBox subtotalRow = createTotalRow("💵 Subtotal:", String.format("%.2f TL", order.getSubtotal()));
        orderTotalsBox.getChildren().add(subtotalRow);
        
        if (order.getDiscountAmount() > 0) {
            HBox discountRow = createTotalRow("🏷️ Discount:", String.format("-%.2f TL", order.getDiscountAmount()));
            discountRow.lookup(".amount-label").setStyle("-fx-font-size: 12px; -fx-text-fill: #E9762B;");
            orderTotalsBox.getChildren().add(discountRow);
        }
        
        HBox vatRow = createTotalRow("📋 VAT:", String.format("%.2f TL", order.getVatAmount()));
        orderTotalsBox.getChildren().add(vatRow);
        
        HBox totalRow = createTotalRow("💰 Total:", String.format("%.2f TL", order.getTotalCost()));
        totalRow.setStyle("-fx-padding: 6 0 0 0;");
        Label totalAmountLabel = (Label) totalRow.lookup(".amount-label");
        if (totalAmountLabel != null) {
            totalAmountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        }
        orderTotalsBox.getChildren().add(totalRow);
    }
    
    /**
     * Creates a row for an order item.
     */
    private HBox createOrderItemRow(com.greengrocer.models.OrderItem item) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 6;");
        
        Label nameLabel = new Label(item.getProductName());
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        
        Label qtyLabel = new Label(String.format("%.2f kg", item.getQuantity()));
        qtyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        
        Label priceLabel = new Label(String.format("₺%.2f", item.getTotalPrice()));
        priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        row.getChildren().addAll(nameLabel, qtyLabel, priceLabel);
        return row;
    }
    
    /**
     * Creates a row for totals display.
     */
    private HBox createTotalRow(String label, String amount) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        HBox.setHgrow(labelNode, Priority.ALWAYS);
        
        Label amountNode = new Label(amount);
        amountNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        amountNode.getStyleClass().add("amount-label");
        
        row.getChildren().addAll(labelNode, amountNode);
        return row;
    }

    /**
     * Handles cancelling an order.
     * * @param event Action event
     */
    @FXML
    private void handleCancelOrder(ActionEvent event) {
        if (selectedOrder == null) {
            showError("Please select an order to cancel");
            return;
        }
        
        if (!orderService.canCancelOrder(selectedOrder.getId())) {
            // Provide specific reason for cancellation denial
            String status = selectedOrder.getStatus();
            String reason;
            if ("assigned".equals(status)) {
                reason = "A carrier has already accepted this delivery.\nOrders cannot be cancelled once a carrier is assigned.";
            } else if ("delivered".equals(status)) {
                reason = "This order has already been delivered.";
            } else if ("cancelled".equals(status)) {
                reason = "This order has already been cancelled.";
            } else {
                reason = "The 2-hour cancellation window has passed.";
            }
            showError("This order cannot be cancelled.\n" + reason);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Cancel order #" + selectedOrder.getId() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                if (orderService.cancelOrder(selectedOrder.getId())) {
                    loadOrders();
                }
            } catch (IllegalStateException e) {
                showError(e.getMessage());
            }
        }
    }

    /**
     * Handles rating the carrier.
     * * @param event Action event
     */
    @FXML
    private void handleRateCarrier(ActionEvent event) {
        if (selectedOrder == null) {
            showError("Please select an order");
            return;
        }
        
        if (!selectedOrder.isDelivered()) {
            showError("You can only rate delivered orders");
            return;
        }
        
        if (selectedOrder.getCarrierId() == null) {
            showError("No carrier assigned to this order");
            return;
        }
        
        Integer rating = ratingCombo.getSelectedItem();
        if (rating == null) {
            showError("Please select a rating");
            return;
        }
        
        String comment = ratingCommentField.getText().trim();
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Check if already rated - if so, show existing rating
        if (!carrierService.canRateOrder(selectedOrder.getId(), currentUser.getId())) {
            CarrierRating existingRating = carrierService.getExistingRating(selectedOrder.getId());
            if (existingRating != null) {
                String message = "You have already rated this delivery.\n\n" +
                               "Your previous rating: " + existingRating.getRating() + "/5";
                if (existingRating.getComment() != null && !existingRating.getComment().trim().isEmpty()) {
                    message += "\nYour previous comment: " + existingRating.getComment();
                }
                showInfo(message);
                
                // Display the existing rating in the UI
                ratingCombo.selectItem(existingRating.getRating());
                ratingCommentField.setText(existingRating.getComment() != null ? existingRating.getComment() : "");
                return;
            } else {
                showError("You have already rated this delivery");
                return;
            }
        }
        
        try {
            carrierService.rateCarrier(selectedOrder.getCarrierId(), selectedOrder.getId(), rating, comment);
            ratingCombo.clearSelection();
            ratingCommentField.clear();
            showInfo("✓ Rating submitted successfully!");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Handles downloading invoice as PDF.
     * Uses iText library to generate a professional, aesthetic PDF invoice.
     * 
     * @param event Action event
     */
    @FXML
    private void handleDownloadInvoice(ActionEvent event) {
        if (selectedOrder == null) {
            showError("Please select an order");
            return;
        }
        
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Open file chooser dialog for PDF
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Invoice");
        fileChooser.setInitialFileName("invoice_order_" + selectedOrder.getId() + ".pdf");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        Stage stage = (Stage) ordersListContainer.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                // Initialize iText PDF Document with margins
                Document document = new Document(PageSize.A4, 50, 50, 60, 50);
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // Color scheme
                BaseColor primaryGreen = new BaseColor(46, 125, 50); // #2E7D32
                BaseColor lightGreen = new BaseColor(200, 230, 201); // #C8E6C9
                BaseColor darkGray = new BaseColor(66, 66, 66); // #424242
                BaseColor lightGray = new BaseColor(245, 245, 245); // #F5F5F5

                // Fonts
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, primaryGreen);
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.WHITE);
                Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, darkGray);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
                Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkGray);
                Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);
                Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryGreen);

                // ========== HEADER SECTION ==========
                // Company Title
                Paragraph title = new Paragraph("GREEN GROCER", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(5);
                document.add(title);
                
                Paragraph tagline = new Paragraph("Fresh Produce Delivery", 
                    FontFactory.getFont(FontFactory.HELVETICA, 12, Font.ITALIC, darkGray));
                tagline.setAlignment(Element.ALIGN_CENTER);
                tagline.setSpacingAfter(15);
                document.add(tagline);

                // Invoice Label Box
                PdfPTable headerTable = new PdfPTable(2);
                headerTable.setWidthPercentage(100);
                headerTable.setWidths(new float[]{60, 40});
                
                // Left: Invoice Info
                PdfPCell leftCell = new PdfPCell(new Phrase("INVOICE", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryGreen)));
                leftCell.setBorder(PdfPCell.NO_BORDER);
                leftCell.setPadding(10);
                leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerTable.addCell(leftCell);
                
                // Right: Order Number
                PdfPCell rightCell = new PdfPCell(new Phrase("#" + selectedOrder.getId(), 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.WHITE)));
                rightCell.setBackgroundColor(primaryGreen);
                rightCell.setBorder(PdfPCell.NO_BORDER);
                rightCell.setPadding(10);
                rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerTable.addCell(rightCell);
                
                document.add(headerTable);
                document.add(new Paragraph("\n"));

                // ========== COMPANY & CUSTOMER INFO ==========
                PdfPTable infoTable = new PdfPTable(2);
                infoTable.setWidthPercentage(100);
                infoTable.setWidths(new float[]{50, 50});
                infoTable.setSpacingBefore(10);
                infoTable.setSpacingAfter(15);

                // Company Info (Left)
                PdfPCell companyCell = new PdfPCell();
                companyCell.setBorder(PdfPCell.NO_BORDER);
                companyCell.setPadding(8);
                companyCell.addElement(new Paragraph("From:", labelFont));
                companyCell.addElement(new Paragraph("Green Grocer", subHeaderFont));
                companyCell.addElement(new Paragraph("123 Fresh Street", normalFont));
                companyCell.addElement(new Paragraph("Garden City", normalFont));
                infoTable.addCell(companyCell);

                // Customer Info (Right)
                PdfPCell customerCell = new PdfPCell();
                customerCell.setBorder(PdfPCell.NO_BORDER);
                customerCell.setPadding(8);
                customerCell.addElement(new Paragraph("Bill To:", labelFont));
                customerCell.addElement(new Paragraph(currentUser.getUsername(), subHeaderFont));
                if (currentUser.getAddress() != null && !currentUser.getAddress().isEmpty()) {
                    customerCell.addElement(new Paragraph(currentUser.getAddress(), normalFont));
                }
                customerCell.addElement(new Paragraph("Order Date: " + 
                    selectedOrder.getOrderTime().format(DATE_FORMATTER), smallFont));
                infoTable.addCell(customerCell);

                document.add(infoTable);

                // ========== ITEMS TABLE ==========
                Paragraph itemsHeader = new Paragraph("Order Items", subHeaderFont);
                itemsHeader.setSpacingBefore(10);
                itemsHeader.setSpacingAfter(8);
                document.add(itemsHeader);

                PdfPTable itemsTable = new PdfPTable(5);
                itemsTable.setWidthPercentage(100);
                itemsTable.setWidths(new float[]{40, 15, 15, 15, 15});
                itemsTable.setSpacingBefore(5);
                itemsTable.setSpacingAfter(10);

                // Table Header
                String[] headers = {"Product", "Quantity", "Unit Price", "Total"};
                for (String header : headers) {
                    PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                    headerCell.setBackgroundColor(primaryGreen);
                    headerCell.setPadding(8);
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    itemsTable.addCell(headerCell);
                }
                // Empty cell for alignment (5 columns but 4 headers)
                PdfPCell emptyHeader = new PdfPCell(new Phrase("", headerFont));
                emptyHeader.setBackgroundColor(primaryGreen);
                emptyHeader.setBorder(PdfPCell.NO_BORDER);
                itemsTable.addCell(emptyHeader);

                // Table Rows
                List<OrderItem> items = selectedOrder.getItems();
                boolean alternate = false;
                for (OrderItem item : items) {
                    // Product Name
                    PdfPCell nameCell = new PdfPCell(new Phrase(item.getProductName(), normalFont));
                    nameCell.setPadding(8);
                    nameCell.setBackgroundColor(alternate ? lightGray : BaseColor.WHITE);
                    nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    itemsTable.addCell(nameCell);

                    // Quantity
                    PdfPCell qtyCell = new PdfPCell(new Phrase(
                        String.format("%.2f kg", item.getQuantity()), normalFont));
                    qtyCell.setPadding(8);
                    qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    qtyCell.setBackgroundColor(alternate ? lightGray : BaseColor.WHITE);
                    qtyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    itemsTable.addCell(qtyCell);

                    // Unit Price
                    PdfPCell unitCell = new PdfPCell(new Phrase(
                        String.format("₺%.2f", item.getUnitPrice()), normalFont));
                    unitCell.setPadding(8);
                    unitCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    unitCell.setBackgroundColor(alternate ? lightGray : BaseColor.WHITE);
                    unitCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    itemsTable.addCell(unitCell);

                    // Total Price
                    PdfPCell totalCell = new PdfPCell(new Phrase(
                        String.format("₺%.2f", item.getTotalPrice()), normalFont));
                    totalCell.setPadding(8);
                    totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalCell.setBackgroundColor(alternate ? lightGray : BaseColor.WHITE);
                    totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    itemsTable.addCell(totalCell);

                    // Empty cell for alignment
                    PdfPCell emptyCell = new PdfPCell(new Phrase("", normalFont));
                    emptyCell.setBorder(PdfPCell.NO_BORDER);
                    emptyCell.setBackgroundColor(alternate ? lightGray : BaseColor.WHITE);
                    itemsTable.addCell(emptyCell);

                    alternate = !alternate;
                }

                document.add(itemsTable);

                // ========== TOTALS SECTION ==========
                PdfPTable totalsTable = new PdfPTable(2);
                totalsTable.setWidthPercentage(50);
                totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalsTable.setSpacingBefore(10);
                totalsTable.setWidths(new float[]{60, 40});

                // Subtotal
                PdfPCell subtotalLabelCell = new PdfPCell(new Phrase("Subtotal:", labelFont));
                subtotalLabelCell.setBorder(PdfPCell.NO_BORDER);
                subtotalLabelCell.setPadding(5);
                subtotalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                totalsTable.addCell(subtotalLabelCell);

                PdfPCell subtotalValueCell = new PdfPCell(new Phrase(
                    String.format("₺%.2f", selectedOrder.getSubtotal()), normalFont));
                subtotalValueCell.setBorder(PdfPCell.NO_BORDER);
                subtotalValueCell.setPadding(5);
                subtotalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                subtotalValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                totalsTable.addCell(subtotalValueCell);

                // Discount (if applicable)
                if (selectedOrder.getDiscountAmount() > 0) {
                    PdfPCell discountLabelCell = new PdfPCell(new Phrase("Discount:", labelFont));
                    discountLabelCell.setBorder(PdfPCell.NO_BORDER);
                    discountLabelCell.setPadding(5);
                    discountLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    totalsTable.addCell(discountLabelCell);

                    PdfPCell discountValueCell = new PdfPCell(new Phrase(
                        String.format("-₺%.2f", selectedOrder.getDiscountAmount()), normalFont));
                    discountValueCell.setBorder(PdfPCell.NO_BORDER);
                    discountValueCell.setPadding(5);
                    discountValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    discountValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    totalsTable.addCell(discountValueCell);
                }

                // VAT
                PdfPCell vatLabelCell = new PdfPCell(new Phrase("VAT:", labelFont));
                vatLabelCell.setBorder(PdfPCell.NO_BORDER);
                vatLabelCell.setPadding(5);
                vatLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                totalsTable.addCell(vatLabelCell);

                PdfPCell vatValueCell = new PdfPCell(new Phrase(
                    String.format("₺%.2f", selectedOrder.getVatAmount()), normalFont));
                vatValueCell.setBorder(PdfPCell.NO_BORDER);
                vatValueCell.setPadding(5);
                vatValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                vatValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                totalsTable.addCell(vatValueCell);

                // Total (highlighted)
                PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL:", totalFont));
                totalLabelCell.setBorder(Rectangle.TOP);
                totalLabelCell.setBorderColor(primaryGreen);
                totalLabelCell.setBorderWidth(2);
                totalLabelCell.setPadding(10);
                totalLabelCell.setBackgroundColor(lightGreen);
                totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                totalsTable.addCell(totalLabelCell);

                PdfPCell totalValueCell = new PdfPCell(new Phrase(
                    String.format("₺%.2f", selectedOrder.getTotalCost()), totalFont));
                totalValueCell.setBorder(Rectangle.TOP);
                totalValueCell.setBorderColor(primaryGreen);
                totalValueCell.setBorderWidth(2);
                totalValueCell.setPadding(10);
                totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalValueCell.setBackgroundColor(lightGreen);
                totalValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                totalsTable.addCell(totalValueCell);

                document.add(totalsTable);

                // ========== FOOTER SECTION ==========
                document.add(new Paragraph("\n\n"));
                
                if (selectedOrder.getRequestedDeliveryTime() != null) {
                    Paragraph deliveryInfo = new Paragraph(
                        "Requested Delivery: " + 
                        selectedOrder.getRequestedDeliveryTime().format(DATE_FORMATTER), 
                        smallFont);
                    deliveryInfo.setAlignment(Element.ALIGN_CENTER);
                    document.add(deliveryInfo);
                }

                if (selectedOrder.getCarrierName() != null) {
                    Paragraph carrierInfo = new Paragraph(
                        "Carrier: " + selectedOrder.getCarrierName(), smallFont);
                    carrierInfo.setAlignment(Element.ALIGN_CENTER);
                    document.add(carrierInfo);
                }

                document.add(new Paragraph("\n"));
                
                Paragraph thankYou = new Paragraph(
                    "Thank you for shopping with Green Grocer!", 
                    FontFactory.getFont(FontFactory.HELVETICA, 11, Font.ITALIC, primaryGreen));
                thankYou.setAlignment(Element.ALIGN_CENTER);
                thankYou.setSpacingBefore(10);
                document.add(thankYou);

                Paragraph footer = new Paragraph(
                    "For inquiries, please contact us at support@greengrocer.com", 
                    smallFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                footer.setSpacingBefore(5);
                document.add(footer);

                document.close();
                showInfo("✓ PDF Invoice saved successfully!\n" + file.getAbsolutePath());
                
            } catch (Exception e) {
                e.printStackTrace();
                showError("Could not generate PDF: " + e.getMessage());
            }
        }
    }

    /**
     * Handles refreshing orders.
     * * @param event Action event
     */
    @FXML
    private void handleRefresh(ActionEvent event) {
        loadOrders();
    }

    /**
     * Shows error alert.
     * * @param message Error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows info alert.
     * * @param message Info message
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}