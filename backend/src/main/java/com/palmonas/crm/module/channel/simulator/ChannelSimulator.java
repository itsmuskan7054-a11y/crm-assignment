package com.palmonas.crm.module.channel.simulator;

import com.palmonas.crm.module.channel.model.ChannelOrder;
import com.palmonas.crm.module.channel.model.ChannelOrder.ChannelOrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ChannelSimulator {

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Reyansh", "Sai", "Arnav",
            "Dhruv", "Kabir", "Ananya", "Isha", "Saanvi", "Aanya", "Myra", "Diya"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Gupta", "Singh", "Kumar", "Patel", "Reddy", "Iyer",
            "Nair", "Joshi", "Malhotra", "Kapoor", "Mehta", "Rao", "Das", "Bhat"
    };

    private static final String[] PRODUCTS = {
            "Organic Cold-Pressed Coconut Oil 1L", "Raw Forest Honey 500g", "Premium Almond Butter 400g",
            "Organic Turmeric Powder 200g", "A2 Cow Ghee 500ml", "Quinoa Seeds 1kg",
            "Chia Seeds 500g", "Green Tea Assortment 50bags", "Organic Moringa Powder 200g",
            "Apple Cider Vinegar 500ml", "Himalayan Pink Salt 1kg", "Whey Protein 1kg",
            "Spirulina Tablets 120ct", "Ashwagandha Capsules 60ct", "Herbal Shampoo 300ml"
    };

    private static final String[] CITIES = {
            "Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Chennai", "Kolkata",
            "Pune", "Ahmedabad", "Jaipur", "Lucknow", "Kochi", "Chandigarh"
    };

    private final Random random = new Random();
    private final double failureRate;

    public ChannelSimulator(double failureRate) {
        this.failureRate = failureRate;
    }

    public void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void maybeThrowError(String channel) {
        if (random.nextDouble() < failureRate) {
            throw new RuntimeException("Simulated " + channel + " API failure: service temporarily unavailable");
        }
    }

    public List<ChannelOrder> generateOrders(String channel, String idPrefix, int count) {
        List<ChannelOrder> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orders.add(generateOrder(channel, idPrefix));
        }
        return orders;
    }

    private ChannelOrder generateOrder(String channel, String idPrefix) {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String city = CITIES[random.nextInt(CITIES.length)];

        List<ChannelOrderItem> items = new ArrayList<>();
        int itemCount = random.nextInt(3) + 1;
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < itemCount; i++) {
            String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
            int qty = random.nextInt(3) + 1;
            BigDecimal price = BigDecimal.valueOf(random.nextInt(2000) + 200).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(qty));
            total = total.add(itemTotal);

            items.add(ChannelOrderItem.builder()
                    .productName(product)
                    .sku("PAL-" + String.format("%03d", random.nextInt(999)))
                    .quantity(qty)
                    .unitPrice(price)
                    .totalPrice(itemTotal)
                    .build());
        }

        String externalId = switch (channel) {
            case "AMAZON" -> idPrefix + String.format("%07d-%07d", random.nextInt(9999999), random.nextInt(9999999));
            case "FLIPKART" -> idPrefix + String.format("%013d", Math.abs(random.nextLong() % 9999999999999L));
            default -> idPrefix + String.format("%05d", random.nextInt(99999));
        };

        Map<String, Object> metadata = switch (channel) {
            case "AMAZON" -> Map.of("marketplace", "amazon.in", "fulfillment", random.nextBoolean() ? "FBA" : "FBM", "prime", random.nextBoolean());
            case "FLIPKART" -> Map.of("seller", "RetailNet", "flipkart_assured", random.nextBoolean(), "supercoins_earned", random.nextInt(100));
            default -> Map.of("source", "organic", "utm_medium", "direct");
        };

        return ChannelOrder.builder()
                .externalOrderId(externalId)
                .channel(channel)
                .status("PENDING")
                .customerName(firstName + " " + lastName)
                .customerEmail(firstName.toLowerCase() + "." + lastName.charAt(0) + "@gmail.com")
                .customerPhone("+91-" + (9000000000L + random.nextInt(999999999)))
                .shippingAddress(random.nextInt(200) + " " + city + ", India")
                .totalAmount(total)
                .currency("INR")
                .metadata(metadata)
                .orderedAt(Instant.now().minus(random.nextInt(48), ChronoUnit.HOURS))
                .items(items)
                .build();
    }
}
