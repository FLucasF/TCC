package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.dto.ItemDto;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    /**
     * Verifica se o cupom pode ser aplicado.
     * Lança CheckoutException com CUPOM_NAO_APLICAVEL se a condição não for cumprida.
     */
    void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens);

    BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens);
}
