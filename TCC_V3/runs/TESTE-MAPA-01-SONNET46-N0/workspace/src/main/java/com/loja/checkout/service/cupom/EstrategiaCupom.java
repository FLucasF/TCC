package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface EstrategiaCupom {
    String getCodigo();
    boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete);
    BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete);
}
