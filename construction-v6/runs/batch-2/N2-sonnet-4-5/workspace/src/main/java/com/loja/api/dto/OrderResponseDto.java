package com.loja.api.dto;

import java.util.List;

public class OrderResponseDto {
    public String id;
    public String status;
    public String createdAt;
    public SummaryResponseDto summary;
    public List<OrderItemResponseDto> items;
    public String trackingCode;

    public static class OrderItemResponseDto {
        public String sku;
        public int quantity;
    }
}
