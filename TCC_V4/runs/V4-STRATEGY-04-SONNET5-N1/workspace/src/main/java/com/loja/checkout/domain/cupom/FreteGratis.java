package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
