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
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal total = totalPedido.subtract(Dinheiro.percentual(totalPedido, "0.05"));
        return new Cobranca(total, total);
    }
}
