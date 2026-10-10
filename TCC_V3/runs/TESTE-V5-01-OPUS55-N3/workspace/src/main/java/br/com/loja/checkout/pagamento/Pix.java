package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
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
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        return Cobranca.aVista(totalPedido.subtract(Dinheiro.porcentagem(totalPedido, "5")));
    }
}
