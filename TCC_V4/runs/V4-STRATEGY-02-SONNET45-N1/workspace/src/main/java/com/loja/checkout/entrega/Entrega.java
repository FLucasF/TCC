package com.loja.checkout.entrega;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Entrega {
    BigDecimal calcularFrete(List<ItemCarrinho> itens);
    int getPrazoEntregaDias();
    boolean estaDisponivel(List<ItemCarrinho> itens);
}
