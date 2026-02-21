package com.palmonas.crm.module.channel.adapter;

import com.palmonas.crm.module.channel.model.ChannelOrder;

import java.util.List;

public interface ChannelAdapter {

    String getChannelName();

    List<ChannelOrder> fetchOrders();

    boolean isAvailable();
}
