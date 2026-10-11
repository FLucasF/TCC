package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import com.loja.checkout.dto.ItemRequest;

public interface Cupom {

    String getCodigo();

    boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);

    BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);
}
