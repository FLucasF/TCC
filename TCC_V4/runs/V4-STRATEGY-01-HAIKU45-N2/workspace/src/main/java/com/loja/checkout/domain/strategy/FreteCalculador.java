package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import java.util.List;

public interface FreteCalculador {
    double calcularFrete(List<Item> itens);
    int getPrazoEntregaDias();
    boolean ehDisponivel(List<Item> itens);
}
