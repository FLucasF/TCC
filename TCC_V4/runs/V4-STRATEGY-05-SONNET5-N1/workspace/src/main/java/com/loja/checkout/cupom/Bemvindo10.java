package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return Arredondamento.centavos(subtotalProdutos.multiply(PERCENTUAL));
    }
}
