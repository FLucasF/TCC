package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomBemVindo10 implements Cupom {

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public void verificarAplicabilidade(BigDecimal subtotalProdutos, BigDecimal frete) {
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
    }
}
