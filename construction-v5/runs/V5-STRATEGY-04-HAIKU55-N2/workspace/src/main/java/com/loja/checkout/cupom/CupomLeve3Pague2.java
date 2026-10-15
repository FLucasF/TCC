package com.loja.checkout.cupom;

import com.loja.checkout.calculo.ContextoCompra;
import com.loja.checkout.calculo.ItemCarrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCompra compra) {
        return compra.itens().stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean aplicavel(ContextoCompra compra) {
        return compra.itens().stream().anyMatch(item -> item.quantidade() >= 3);
    }
}
