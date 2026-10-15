package br.com.loja.pedidos.checkout.cupom;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 10% dos produtos, só na primeira compra do cliente. */
@Component
public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = BigDecimal.TEN;

    private final HistoricoDeCompras historico;

    public BemVindo10(HistoricoDeCompras historico) {
        this.historico = historico;
    }

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(PedidoParaCupom pedido) {
        if (historico.jaComprou(pedido.cpf())) {
            throw NaoAplicavel.erro();
        }
        return Dinheiro.percentual(pedido.subtotalProdutos(), PERCENTUAL);
    }
}
