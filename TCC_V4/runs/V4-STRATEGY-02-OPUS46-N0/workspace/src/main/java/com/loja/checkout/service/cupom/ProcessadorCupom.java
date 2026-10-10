package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ProcessadorCupom {

    String codigo();

    boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens);

    BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);
}
