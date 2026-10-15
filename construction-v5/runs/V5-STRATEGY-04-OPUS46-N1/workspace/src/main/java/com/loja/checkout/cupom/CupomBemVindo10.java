package com.loja.checkout.cupom;

import com.loja.checkout.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomBemVindo10 implements CalculoCupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete) {
        return subtotalProdutos.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }
}
