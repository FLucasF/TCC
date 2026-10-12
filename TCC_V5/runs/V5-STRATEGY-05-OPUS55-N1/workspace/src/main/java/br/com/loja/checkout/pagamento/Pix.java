package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Pix implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        return Pagamento.aVista(totalPedido.subtract(Dinheiro.percentual(totalPedido, "5")));
    }
}
