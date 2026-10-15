package br.com.loja.pedidos.checkout.cupom;

import br.com.loja.pedidos.checkout.ItemPrecificado;
import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo produto, uma sai de graça. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(PedidoParaCupom pedido) {
        BigDecimal desconto = Dinheiro.ZERO;
        for (ItemPrecificado item : pedido.itens()) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        if (desconto.signum() == 0) {
            throw NaoAplicavel.erro();
        }
        return Dinheiro.arredondar(desconto);
    }
}
