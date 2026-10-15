package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.PedidoContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2Cupom implements Cupom {

    private static final int TAMANHO_LEVA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : pedido.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_LEVA;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return Dinheiro.arredondar(desconto);
    }
}
