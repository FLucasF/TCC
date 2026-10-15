package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import java.util.List;

public interface CupomAplicador {
    double calcularDesconto(List<Item> itens, double subtotal, double frete);
    boolean ehAplicavel(List<Item> itens, double subtotal, double frete);
}
