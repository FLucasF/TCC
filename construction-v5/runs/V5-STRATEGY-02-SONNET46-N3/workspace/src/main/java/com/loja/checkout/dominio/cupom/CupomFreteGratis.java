package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ResumoRequest;

import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements Cupom {

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ResumoRequest.Item> itens) {
        return frete;
    }
}
