package com.palmonas.crm.module.order.controller;

import com.palmonas.crm.common.dto.ApiResponse;
import com.palmonas.crm.common.dto.PagedResponse;
import com.palmonas.crm.module.order.dto.*;
import com.palmonas.crm.module.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders with filtering, sorting, and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getOrders(
            @ModelAttribute OrderFilterRequest filter) {
        PagedResponse<OrderResponse> result = orderService.getOrders(filter);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        OrderResponse order = orderService.updateStatus(id, request, userId);
        return ResponseEntity.ok(ApiResponse.ok(order, "Status updated successfully"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        DashboardStats stats = orderService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
