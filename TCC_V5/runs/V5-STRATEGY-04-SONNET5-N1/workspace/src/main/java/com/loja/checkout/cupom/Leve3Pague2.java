package com.loja.checkout.cupom;

import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_LEVA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : pedido.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_LEVA;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return Dinheiro.arredondar(total);
    }
}
