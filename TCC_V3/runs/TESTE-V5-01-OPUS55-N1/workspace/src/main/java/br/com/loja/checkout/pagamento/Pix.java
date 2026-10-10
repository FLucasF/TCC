package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        return Pagamento.aVista(totalPedido.subtract(Dinheiro.aplicarTaxa(totalPedido, DESCONTO)));
    }
}
