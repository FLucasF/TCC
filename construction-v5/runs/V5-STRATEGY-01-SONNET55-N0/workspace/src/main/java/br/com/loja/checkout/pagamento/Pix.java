package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Pix implements FormaPagamento {

    public String codigo() {
        return "PIX";
    }

    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
        BigDecimal total = totalPedido.subtract(desconto);
        return new Cobranca(total, total);
    }
}
