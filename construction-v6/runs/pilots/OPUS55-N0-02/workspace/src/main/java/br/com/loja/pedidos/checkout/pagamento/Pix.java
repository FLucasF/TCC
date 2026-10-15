package br.com.loja.pedidos.checkout.pagamento;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** À vista, com 5% de desconto no total do pedido para quem não usou cupom. */
@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public CondicaoPagamento condicao(BigDecimal totalPedido, int parcelas, boolean usouCupom) {
        BigDecimal desconto = usouCupom ? Dinheiro.ZERO : Dinheiro.percentual(totalPedido, PERCENTUAL_DESCONTO);
        return CondicaoPagamento.aVista(totalPedido.subtract(desconto));
    }
}
