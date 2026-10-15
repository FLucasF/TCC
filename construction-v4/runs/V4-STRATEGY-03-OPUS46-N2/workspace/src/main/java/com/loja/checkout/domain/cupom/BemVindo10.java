package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return subtotalProdutos.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }
}
