package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import java.util.List;

public class CupomMenos50 implements CupomAplicador {
    @Override
    public double calcularDesconto(List<Item> itens, double subtotal, double frete) {
        return 50.0;
    }

    @Override
    public boolean ehAplicavel(List<Item> itens, double subtotal, double frete) {
        return subtotal >= 300.0;
    }
}
