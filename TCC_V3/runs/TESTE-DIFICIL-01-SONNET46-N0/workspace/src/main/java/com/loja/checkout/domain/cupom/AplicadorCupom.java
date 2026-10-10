package com.loja.checkout.domain.cupom;

import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface AplicadorCupom {

    String getCodigo();

    boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens);

    BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens);
}
