package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.dto.ItemDto;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {

    @Override
    public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        // sem restrição
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        // desconto = valor do frete (que pode ser 0 se clube OURO)
        return frete;
    }
}
