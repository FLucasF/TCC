package com.loja.checkout.coupon;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CouponRegistry {

    private final Map<String, Coupon> cupons;

    public CouponRegistry(List<Coupon> cupons) {
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Coupon::codigo, Function.identity()));
    }

    public Optional<Coupon> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(cupons::get);
    }
}
