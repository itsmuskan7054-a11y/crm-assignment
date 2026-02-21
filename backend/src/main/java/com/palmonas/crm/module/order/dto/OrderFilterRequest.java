package com.palmonas.crm.module.order.dto;

import lombok.Data;

@Data
public class OrderFilterRequest {

    private String channel;
    private String status;
    private String search;
    private String from;
    private String to;
    private int page = 0;
    private int size = 20;
    private String sortBy = "orderedAt";
    private String sortDir = "desc";
}
