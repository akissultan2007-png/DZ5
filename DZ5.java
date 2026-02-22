package DZmodule5;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class DZmodule5 {

    static class ConfigNotFoundException extends RuntimeException {
        public ConfigNotFoundException(String key) {
            super("Настройка не найдена: " + key);
        }
    }

    static class ConfigurationManager {
        private static volatile ConfigurationManager instance;
        private static final Object LOCK = new Object();

        private final Map<String, String> settings = new HashMap<>();
        private boolean loadedOnce = false;

        private ConfigurationManager() {}

        public static ConfigurationManager getInstance() {
            if (instance == null) {
                synchronized (LOCK) {
                    if (instance == null) {
                        instance = new ConfigurationManager();
                    }
                }
            }
            return instance;
        }

        public void loadFromFileOnce(String filePath) {
            synchronized (LOCK) {
                if (loadedOnce) return;

                File f = new File(filePath);
                if (!f.exists()) {
                    throw new RuntimeException("Файл конфигурации не найден: " + filePath);
                }

                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;

                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            settings.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                    loadedOnce = true;
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка чтения конфигурации: " + e.getMessage(), e);
                }
            }
        }

        public void saveToFile(String filePath) {
            synchronized (LOCK) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
                    for (Map.Entry<String, String> e : settings.entrySet()) {
                        pw.println(e.getKey() + "=" + e.getValue());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка сохранения конфигурации: " + e.getMessage(), e);
                }
            }
        }

        public String get(String key) {
            synchronized (LOCK) {
                if (!settings.containsKey(key)) throw new ConfigNotFoundException(key);
                return settings.get(key);
            }
        }

        public String getOrDefault(String key, String def) {
            synchronized (LOCK) {
                return settings.getOrDefault(key, def);
            }
        }

        public void set(String key, String value) {
            synchronized (LOCK) {
                settings.put(key, value);
            }
        }

        public void printAll() {
            synchronized (LOCK) {
                System.out.println("=== CONFIG ===");
                settings.forEach((k, v) -> System.out.println(k + " = " + v));
            }
        }
    }

    static class Report {
        private final String format;
        private String header;
        private String content;
        private String footer;

        public Report(String format) {
            this.format = format;
        }

        public void setHeader(String header) { this.header = header; }
        public void setContent(String content) { this.content = content; }
        public void setFooter(String footer) { this.footer = footer; }

        public String render() {
            if ("HTML".equals(format)) {
                return "<html>\n" +
                        "  <body>\n" +
                        "    <h1>" + esc(header) + "</h1>\n" +
                        "    <p>" + esc(content) + "</p>\n" +
                        "    <footer>" + esc(footer) + "</footer>\n" +
                        "  </body>\n" +
                        "</html>";
            }
            return "=== " + nn(header) + " ===\n" +
                    nn(content) + "\n" +
                    "--- " + nn(footer) + " ---\n";
        }

        private String nn(String s) { return s == null ? "" : s; }

        private String esc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    interface IReportBuilder {
        void setHeader(String header);
        void setContent(String content);
        void setFooter(String footer);
        Report getReport();
    }

    static class TextReportBuilder implements IReportBuilder {
        private final Report report = new Report("TEXT");

        public void setHeader(String header) { report.setHeader(header); }
        public void setContent(String content) { report.setContent(content); }
        public void setFooter(String footer) { report.setFooter(footer); }
        public Report getReport() { return report; }
    }

    static class HtmlReportBuilder implements IReportBuilder {
        private final Report report = new Report("HTML");

        public void setHeader(String header) { report.setHeader(header); }
        public void setContent(String content) { report.setContent(content); }
        public void setFooter(String footer) { report.setFooter(footer); }
        public Report getReport() { return report; }
    }

    static class ReportDirector {
        public void constructReport(IReportBuilder builder, String header, String content, String footer) {
            builder.setHeader(header);
            builder.setContent(content);
            builder.setFooter(footer);
        }
    }

    static class Product implements Cloneable {
        private String name;
        private double price;
        private int quantity;

        public Product(String name, double price, int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double total() { return price * quantity; }

        @Override
        public Product clone() {
            try {
                return (Product) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return name + " x" + quantity + " (" + price + ")";
        }
    }

    static class Discount implements Cloneable {
        private String name;
        private double amount;

        public Discount(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }

        @Override
        public Discount clone() {
            try {
                return (Discount) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public double getAmount() { return amount; }

        @Override
        public String toString() {
            return name + " (-" + amount + ")";
        }
    }

    static class Order implements Cloneable {
        private List<Product> products = new ArrayList<>();
        private List<Discount> discounts = new ArrayList<>();
        private double deliveryCost;
        private String paymentMethod;

        public Order(double deliveryCost, String paymentMethod) {
            this.deliveryCost = deliveryCost;
            this.paymentMethod = paymentMethod;
        }

        public void addProduct(Product p) { products.add(p); }
        public void addDiscount(Discount d) { discounts.add(d); }

        public void setDeliveryCost(double deliveryCost) { this.deliveryCost = deliveryCost; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public double total() {
            double sum = deliveryCost;
            for (Product p : products) sum += p.total();
            for (Discount d : discounts) sum -= d.getAmount();
            return Math.max(0, sum);
        }

        @Override
        public Order clone() {
            try {
                Order copy = (Order) super.clone();
                copy.products = new ArrayList<>();
                for (Product p : this.products) copy.products.add(p.clone());

                copy.discounts = new ArrayList<>();
                for (Discount d : this.discounts) copy.discounts.add(d.clone());

                return copy;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "Order{" +
                    "products=" + products +
                    ", discounts=" + discounts +
                    ", deliveryCost=" + deliveryCost +
                    ", paymentMethod='" + paymentMethod + '\'' +
                    ", total=" + total() +
                    '}';
        }
    }

    public static void main(String[] args) {

        System.out.println("===== SINGLETON TEST =====");

        String configPath = "config.txt";

        int threads = 6;
        CountDownLatch latch = new CountDownLatch(threads);

        List<ConfigurationManager> refs = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ConfigurationManager cm = ConfigurationManager.getInstance();
                refs.add(cm);
                latch.countDown();
            }).start();
        }

        try { latch.await(); } catch (InterruptedException e) { e.printStackTrace(); }

        boolean allSame = true;
        ConfigurationManager first = refs.get(0);
        for (ConfigurationManager cm : refs) {
            if (cm != first) { allSame = false; break; }
        }
        System.out.println("Один экземпляр во всех потоках: " + allSame);

        ConfigurationManager cm = ConfigurationManager.getInstance();
        try {
            cm.loadFromFileOnce(configPath);
            cm.loadFromFileOnce(configPath);
        } catch (RuntimeException e) {
            System.out.println("Ошибка конфигурации: " + e.getMessage());
        }

        cm.set("lastRun", new Date().toString());
        cm.printAll();

        cm.saveToFile("config_saved.txt");
        System.out.println("Сохранено в: config_saved.txt");

        try {
            System.out.println("unknownKey = " + cm.get("unknownKey"));
        } catch (ConfigNotFoundException e) {
            System.out.println("Ожидаемое исключение: " + e.getMessage());
        }

        System.out.println("\n===== BUILDER TEST =====");

        ReportDirector director = new ReportDirector();

        IReportBuilder textBuilder = new TextReportBuilder();
        director.constructReport(textBuilder,
                "Отчет по продажам",
                "Продажи выросли на 15% за месяц.",
                "Конец отчета");
        Report textReport = textBuilder.getReport();
        System.out.println(textReport.render());

        IReportBuilder htmlBuilder = new HtmlReportBuilder();
        director.constructReport(htmlBuilder,
                "HTML Report",
                "Это HTML-отчет с <тегами> & символами.",
                "Footer");
        Report htmlReport = htmlBuilder.getReport();
        System.out.println(htmlReport.render());

        System.out.println("\n===== PROTOTYPE TEST =====");

        Order prototype = new Order(500, "CARD");
        prototype.addProduct(new Product("Laptop", 250000, 1));
        prototype.addProduct(new Product("Mouse", 5000, 1));
        prototype.addDiscount(new Discount("WelcomeDiscount", 3000));

        System.out.println("Prototype: " + prototype);

        Order order2 = prototype.clone();
        order2.setPaymentMethod("CASH");
        order2.setDeliveryCost(800);
        order2.products.get(1).setQuantity(2);
        order2.addDiscount(new Discount("Promo", 2000));

        System.out.println("Order2 (clone+changes): " + order2);
        System.out.println("Prototype (unchanged): " + prototype);
    }
}
