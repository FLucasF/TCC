package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemDto;

import java.math.BigDecimal;
import java.util.List;

public interface CupomStrategy {
    String codigo();
    boolean aplicavel(BigDecimal subtotal);
    BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens);
}
