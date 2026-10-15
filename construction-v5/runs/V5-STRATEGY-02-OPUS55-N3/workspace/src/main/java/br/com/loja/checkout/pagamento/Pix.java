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

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, "5"));
        return new Pagamento(totalFinal, totalFinal);
    }
}
