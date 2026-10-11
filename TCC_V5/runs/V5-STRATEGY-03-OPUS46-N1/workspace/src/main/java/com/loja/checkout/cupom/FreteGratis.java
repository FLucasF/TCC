package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.checkout.dto.ItemRequest;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return frete;
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
