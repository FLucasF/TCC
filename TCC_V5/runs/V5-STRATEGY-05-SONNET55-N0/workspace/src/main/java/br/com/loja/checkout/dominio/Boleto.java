package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    public String codigo() {
        return "BOLETO";
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal total = totalPedido.add(TARIFA);
        return new Cobranca(total, total);
    }
}
