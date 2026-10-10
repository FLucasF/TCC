package com.loja.checkout.coupon;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CouponRegistry {

    private final Map<String, Coupon> porCodigo;

    public CouponRegistry(List<Coupon> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toMap(Coupon::getCodigo, Function.identity()));
    }

    public Coupon buscar(String codigo) {
        return codigo == null ? null : porCodigo.get(codigo);
    }
}
