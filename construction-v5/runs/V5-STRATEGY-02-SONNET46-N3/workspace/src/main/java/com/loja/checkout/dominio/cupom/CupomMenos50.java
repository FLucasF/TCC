package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ResumoRequest;

import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ResumoRequest.Item> itens) {
        return new BigDecimal("50.00");
    }
}
