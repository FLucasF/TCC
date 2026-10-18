package com.loja.dto;

import com.loja.domain.*;
import java.util.List;

public record OrderRequest(
    CustomerInfo customer,
    List<ItemInfo> items,
    String zipCode,
    Region region,
    ShippingMethod shippingMethod,
    CouponCode coupon,
    PaymentMethod paymentMethod,
    Integer installments,
    ClubTier clubTier
) {
    public record CustomerInfo(String cpf, String email) {}
    public record ItemInfo(String sku, Integer quantity) {}

    public int getInstallments() {
        return installments != null ? installments : 1;
    }
}
