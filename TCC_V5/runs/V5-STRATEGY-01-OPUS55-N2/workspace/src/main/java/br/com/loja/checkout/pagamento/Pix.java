package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Pix implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        return Pagamento.aVista(totalPedido.subtract(Dinheiro.percentual(totalPedido, "5")));
    }
}
