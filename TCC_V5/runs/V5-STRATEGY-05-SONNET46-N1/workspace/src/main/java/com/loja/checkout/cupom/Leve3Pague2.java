package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Leve3Pague2 implements Cupom {
    @Override
    public String codigo() { return "LEVE3PAGUE2"; }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {}

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
        return itens.stream()
                .map(item -> item.precoUnitario()
                        .multiply(BigDecimal.valueOf((long) item.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
