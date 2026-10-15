package com.loja.checkout.cupom;

import com.loja.checkout.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomFreteGratis implements CalculoCupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }
}
