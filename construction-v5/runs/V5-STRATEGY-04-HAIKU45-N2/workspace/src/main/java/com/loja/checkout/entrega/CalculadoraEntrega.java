package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;

public interface CalculadoraEntrega {
    BigDecimal calcular(List<Item> itens);
    int getPrazo();
    boolean podeEntregar(List<Item> itens);
}
