package com.loja.strategy.coupon;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.ItemRequest;
import com.loja.domain.Product;
import com.loja.service.MoneyRounding;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class Buy3Pay2Coupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "BUY3PAY2";
    }

    @Override
    public BigDecimal calculateDiscount(CheckoutRequest request, Map<String, Product> products, BigDecimal productsSubtotal, BigDecimal shippingCost) {
        BigDecimal discount = BigDecimal.ZERO;

        for (ItemRequest item : request.getItems()) {
            if (item.getQuantity() >= 3) {
                Product product = products.get(item.getSku());
                int freeItems = item.getQuantity() / 3;
                discount = discount.add(product.getPrice().multiply(new BigDecimal(freeItems)));
            }
        }

        return MoneyRounding.round(discount);
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, List<String> customerOrderStatuses) {
        return true;
    }
}
