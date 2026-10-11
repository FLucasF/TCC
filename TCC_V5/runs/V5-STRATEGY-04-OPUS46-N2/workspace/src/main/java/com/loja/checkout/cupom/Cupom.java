package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String getCodigo();

    BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);

    boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens);
}
