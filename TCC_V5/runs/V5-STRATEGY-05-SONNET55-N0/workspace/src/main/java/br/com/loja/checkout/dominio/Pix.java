package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("5");

    public String codigo() {
        return "PIX";
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal total = totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO));
        return new Cobranca(total, total);
    }
}
