package com.inventory.desktop.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AnalyticsController {

    // KPI Labels
    @FXML private Label lbl_total_revenue;
    @FXML private Label lbl_total_profit;
    @FXML private Label lbl_profit_margin;
    @FXML private Label lbl_today_sales;
    @FXML private Label lbl_today_profit;
    @FXML private Label lbl_total_products;
    @FXML private Label lbl_total_sales;
    @FXML private Label lbl_avg_sale;
    @FXML private Label lbl_inventory_value;
    @FXML private Label lbl_low_stock;
    @FXML private Label lbl_out_of_stock;

    // Charts
    @FXML private BarChart<String, Number> chart_sales_by_day;
    @FXML private PieChart chart_category;

    // Daily Analytics Table
    @FXML private TableView<DailyAnalytics> table_daily_analytics;
    @FXML private TableColumn<DailyAnalytics, String> col_daily_date;
    @FXML private TableColumn<DailyAnalytics, String> col_daily_revenue;
    @FXML private TableColumn<DailyAnalytics, String> col_daily_cost;
    @FXML private TableColumn<DailyAnalytics, String> col_daily_profit;
    @FXML private TableColumn<DailyAnalytics, String> col_daily_margin;
    @FXML private TableColumn<DailyAnalytics, Integer> col_daily_sales;
    @FXML private TableColumn<DailyAnalytics, Integer> col_daily_items;

    // Top Products Table
    @FXML private TableView<TopProduct> table_top_products;
    @FXML private TableColumn<TopProduct, String> col_top_name;
    @FXML private TableColumn<TopProduct, Integer> col_top_qty;
    @FXML private TableColumn<TopProduct, String> col_top_revenue;

    // Recent Sales Table
    @FXML private TableView<RecentSale> table_recent_sales;
    @FXML private TableColumn<RecentSale, String> col_recent_product;
    @FXML private TableColumn<RecentSale, Integer> col_recent_qty;
    @FXML private TableColumn<RecentSale, String> col_recent_total;
    @FXML private TableColumn<RecentSale, String> col_recent_date;

    // Low Stock Table
    @FXML private TableView<LowStockProduct> table_low_stock;
    @FXML private TableColumn<LowStockProduct, String> col_low_name;
    @FXML private TableColumn<LowStockProduct, String> col_low_category;
    @FXML private TableColumn<LowStockProduct, Integer> col_low_qty;

    @FXML
    public void initialize() {
        setupTables();
        loadAllData();
    }

    private void setupTables() {
        // Daily Analytics
        col_daily_date.setCellValueFactory(new PropertyValueFactory<>("date"));
        col_daily_revenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        col_daily_cost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        col_daily_profit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        col_daily_margin.setCellValueFactory(new PropertyValueFactory<>("margin"));
        col_daily_sales.setCellValueFactory(new PropertyValueFactory<>("salesCount"));
        col_daily_items.setCellValueFactory(new PropertyValueFactory<>("itemsSold"));
        
        // Top Products
        col_top_name.setCellValueFactory(new PropertyValueFactory<>("productName"));
        col_top_qty.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        col_top_revenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));

        // Recent Sales
        col_recent_product.setCellValueFactory(new PropertyValueFactory<>("productName"));
        col_recent_qty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        col_recent_total.setCellValueFactory(new PropertyValueFactory<>("total"));
        col_recent_date.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Low Stock
        col_low_name.setCellValueFactory(new PropertyValueFactory<>("productName"));
        col_low_category.setCellValueFactory(new PropertyValueFactory<>("category"));
        col_low_qty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
    }

    private void loadAllData() {
        loadSummary();
        loadDailyAnalytics();
        loadSalesByDay();
        loadSalesByCategory();
        loadTopProducts();
        loadRecentSales();
        loadLowStock();
    }
    
    private void loadDailyAnalytics() {
        new Thread(() -> {
            try {
                String response = httpGet("http://localhost:8080/api/analytics/daily");
                JSONArray jsonArray = new JSONArray(response);

                Platform.runLater(() -> {
                    ObservableList<DailyAnalytics> data = FXCollections.observableArrayList();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject day = jsonArray.getJSONObject(i);
                        data.add(new DailyAnalytics(
                                day.optString("date", ""),
                                "$" + formatNumber(day.optDouble("revenue", 0)),
                                "$" + formatNumber(day.optDouble("cost", 0)),
                                "$" + formatNumber(day.optDouble("profit", 0)),
                                formatNumber(day.optDouble("margin", 0)) + "%",
                                day.optInt("salesCount", 0),
                                day.optInt("itemsSold", 0)
                        ));
                    }

                    table_daily_analytics.setItems(data);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSummary() {
        new Thread(() -> {
            try {
                String response = httpGet("http://localhost:8080/api/analytics/summary");
                JSONObject json = new JSONObject(response);

                Platform.runLater(() -> {
                    lbl_total_revenue.setText("$" + formatNumber(json.optDouble("totalRevenue", 0)));
                    lbl_total_profit.setText("$" + formatNumber(json.optDouble("totalProfit", 0)));
                    lbl_profit_margin.setText(formatNumber(json.optDouble("profitMargin", 0)) + "%");
                    lbl_today_sales.setText("$" + formatNumber(json.optDouble("todaySales", 0)));
                    lbl_today_profit.setText("$" + formatNumber(json.optDouble("todayProfit", 0)));
                    lbl_total_products.setText(String.valueOf(json.optLong("totalProducts", 0)));
                    lbl_total_sales.setText(String.valueOf(json.optLong("totalSalesCount", 0)));
                    lbl_avg_sale.setText("$" + formatNumber(json.optDouble("averageSale", 0)));
                    lbl_inventory_value.setText("$" + formatNumber(json.optDouble("inventoryValue", 0)));
                    lbl_low_stock.setText(String.valueOf(json.optLong("lowStockCount", 0)));
                    lbl_out_of_stock.setText(String.valueOf(json.optLong("outOfStockCount", 0)));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSalesByDay() {
        new Thread(() -> {
            try {
                String response = httpGet("http://localhost:8080/api/analytics/sales-by-day");
                JSONArray jsonArray = new JSONArray(response);

                Platform.runLater(() -> {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Revenue");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject day = jsonArray.getJSONObject(i);
                        series.getData().add(new XYChart.Data<>(
                                day.getString("date"),
                                day.optDouble("total", 0)
                        ));
                    }

                    chart_sales_by_day.getData().clear();
                    chart_sales_by_day.getData().add(series);
                    chart_sales_by_day.setLegendVisible(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSalesByCategory() {
        new Thread(() -> {
            try {
                String response = httpGet("http://localhost:8080/api/analytics/sales-by-category");
                JSONArray jsonArray = new JSONArray(response);

                Platform.runLater(() -> {
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject cat = jsonArray.getJSONObject(i);
                        pieData.add(new PieChart.Data(
                                cat.getString("category"),
                                cat.optDouble("revenue", 0)
                        ));
                    }

                    chart_category.setData(pieData);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadTopProducts() {
        new Thread(() -> {
            try {
                String response = httpGet("http://localhost:8080/api/analytics/top-products");
                JSONArray jsonArray = new JSONArray(response);

                Platform.runLater(() -> {
                    ObservableList<TopProduct> products = FXCollections.observableArrayList();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject p = jsonArray.getJSONObject(i);
                        products.add(new TopProduct(
                                p.optString("productName", "Unknown"),
                                p.optInt("quantitySold", 0),
                                "$" + formatNumber(p.optDouble("revenue", 0))
                        ));
                    }

                    table_top_products.setItems(products);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadRecentSales() {
        new Thread(() -> {
            try {
                String response = httpGet("http://localhost:8080/api/analytics/recent-sales");
                JSONArray jsonArray = new JSONArray(response);

                Platform.runLater(() -> {
                    ObservableList<RecentSale> sales = FXCollections.observableArrayList();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject s = jsonArray.getJSONObject(i);
                        sales.add(new RecentSale(
                                s.optString("productName", "Unknown"),
                                s.optInt("quantity", 0),
                                "$" + formatNumber(s.optDouble("totalAmount", 0)),
                                s.optString("saleDate", "")
                        ));
                    }

                    table_recent_sales.setItems(sales);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadLowStock() {
        new Thread(() -> {
            try {
                String response = httpGet("http://localhost:8080/api/analytics/low-stock");
                JSONArray jsonArray = new JSONArray(response);

                Platform.runLater(() -> {
                    ObservableList<LowStockProduct> products = FXCollections.observableArrayList();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject p = jsonArray.getJSONObject(i);
                        products.add(new LowStockProduct(
                                p.optString("productName", "Unknown"),
                                p.optString("category", ""),
                                p.optInt("quantity", 0)
                        ));
                    }

                    table_low_stock.setItems(products);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }

    private String formatNumber(double value) {
        return String.format("%,.2f", value);
    }

    // Inner classes for TableView
    public static class TopProduct {
        private String productName;
        private int quantitySold;
        private String revenue;

        public TopProduct(String productName, int quantitySold, String revenue) {
            this.productName = productName;
            this.quantitySold = quantitySold;
            this.revenue = revenue;
        }

        public String getProductName() { return productName; }
        public int getQuantitySold() { return quantitySold; }
        public String getRevenue() { return revenue; }
    }

    public static class RecentSale {
        private String productName;
        private int quantity;
        private String total;
        private String date;

        public RecentSale(String productName, int quantity, String total, String date) {
            this.productName = productName;
            this.quantity = quantity;
            this.total = total;
            this.date = date;
        }

        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public String getTotal() { return total; }
        public String getDate() { return date; }
    }

    public static class LowStockProduct {
        private String productName;
        private String category;
        private int quantity;

        public LowStockProduct(String productName, String category, int quantity) {
            this.productName = productName;
            this.category = category;
            this.quantity = quantity;
        }

        public String getProductName() { return productName; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
    }
    
    public static class DailyAnalytics {
        private String date;
        private String revenue;
        private String cost;
        private String profit;
        private String margin;
        private int salesCount;
        private int itemsSold;

        public DailyAnalytics(String date, String revenue, String cost, String profit, String margin, int salesCount, int itemsSold) {
            this.date = date;
            this.revenue = revenue;
            this.cost = cost;
            this.profit = profit;
            this.margin = margin;
            this.salesCount = salesCount;
            this.itemsSold = itemsSold;
        }

        public String getDate() { return date; }
        public String getRevenue() { return revenue; }
        public String getCost() { return cost; }
        public String getProfit() { return profit; }
        public String getMargin() { return margin; }
        public int getSalesCount() { return salesCount; }
        public int getItemsSold() { return itemsSold; }
    }
}
