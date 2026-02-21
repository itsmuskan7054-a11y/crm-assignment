package com.palmonas.crm.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
}
