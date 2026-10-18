package com.loja.service.coupon;

import com.loja.domain.*;
import com.loja.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Buy3Pay2Coupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "BUY3PAY2";
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, Map<String, Product> productMap, boolean isFirstPurchase) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productsSubtotal, BigDecimal shippingCost, List<OrderItem> items, Map<String, Product> productMap) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item.getQuantity() >= 3) {
                Product product = productMap.get(item.getSku());
                int freeItems = item.getQuantity() / 3;
                BigDecimal discount = product.getPrice().multiply(new BigDecimal(freeItems));
                totalDiscount = totalDiscount.add(discount);
            }
        }
        return MoneyUtils.roundMoney(totalDiscount);
    }
}
