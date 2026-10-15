package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO_PORCENTAGEM = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        return Cobranca.aVista(totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO_PORCENTAGEM)));
    }
}
