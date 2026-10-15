package com.loja.checkout.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.loja.checkout.model.Regiao;

@Service
public class SeguroService {
    public BigDecimal calcular(BigDecimal subtotal, Regiao regiao) {
        BigDecimal taxa = switch (regiao) {
            case SUDESTE -> new BigDecimal("0.01");
            case SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };

        BigDecimal seguro = subtotal.multiply(taxa);
        return Arredondamento.arredondar(seguro);
    }
}
