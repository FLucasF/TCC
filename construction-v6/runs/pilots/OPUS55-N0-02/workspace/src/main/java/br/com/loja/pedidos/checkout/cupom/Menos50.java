package br.com.loja.pedidos.checkout.cupom;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 50,00 nos produtos, para compras a partir de R$ 300,00 em produtos. */
@Component
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = Dinheiro.reais("50.00");
    private static final BigDecimal MINIMO = Dinheiro.reais("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public BigDecimal desconto(PedidoParaCupom pedido) {
        if (pedido.subtotalProdutos().compareTo(MINIMO) < 0) {
            throw NaoAplicavel.erro();
        }
        return DESCONTO;
    }
}
