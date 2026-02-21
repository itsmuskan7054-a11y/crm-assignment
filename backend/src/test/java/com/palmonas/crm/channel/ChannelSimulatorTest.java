package com.palmonas.crm.channel;

import com.palmonas.crm.module.channel.model.ChannelOrder;
import com.palmonas.crm.module.channel.simulator.ChannelSimulator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChannelSimulatorTest {

    @Test
    void shouldGenerateCorrectNumberOfOrders() {
        ChannelSimulator simulator = new ChannelSimulator(0.0);
        List<ChannelOrder> orders = simulator.generateOrders("AMAZON", "114-", 5);

        assertEquals(5, orders.size());
        orders.forEach(order -> {
            assertEquals("AMAZON", order.getChannel());
            assertTrue(order.getExternalOrderId().startsWith("114-"));
            assertNotNull(order.getCustomerName());
            assertNotNull(order.getCustomerEmail());
            assertNotNull(order.getTotalAmount());
            assertTrue(order.getItems().size() > 0);
        });
    }

    @Test
    void shouldGenerateFlipkartOrdersWithCorrectPrefix() {
        ChannelSimulator simulator = new ChannelSimulator(0.0);
        List<ChannelOrder> orders = simulator.generateOrders("FLIPKART", "OD", 3);

        assertEquals(3, orders.size());
        orders.forEach(order -> {
            assertEquals("FLIPKART", order.getChannel());
            assertTrue(order.getExternalOrderId().startsWith("OD"));
        });
    }

    @Test
    void shouldThrowErrorBasedOnFailureRate() {
        ChannelSimulator simulator = new ChannelSimulator(1.0);
        assertThrows(RuntimeException.class, () -> simulator.maybeThrowError("Test"));
    }

    @Test
    void shouldNotThrowWithZeroFailureRate() {
        ChannelSimulator simulator = new ChannelSimulator(0.0);
        assertDoesNotThrow(() -> simulator.maybeThrowError("Test"));
    }
}
