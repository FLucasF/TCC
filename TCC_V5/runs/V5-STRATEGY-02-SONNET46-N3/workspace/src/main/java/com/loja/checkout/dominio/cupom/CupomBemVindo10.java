package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ResumoRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomBemVindo10 implements Cupom {

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ResumoRequest.Item> itens) {
        return subtotalProdutos.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
