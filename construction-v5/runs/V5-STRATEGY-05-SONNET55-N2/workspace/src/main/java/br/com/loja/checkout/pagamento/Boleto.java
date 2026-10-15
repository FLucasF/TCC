package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Boleto implements FormaPagamento {
    private static final BigDecimal LIMITE = Dinheiro.reais("1000.00");
    private static final BigDecimal TARIFA = Dinheiro.reais("3.49");

    public String codigo() { return "BOLETO"; }

    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal total = totalPedido.add(TARIFA);
        return new Cobranca(total, total);
    }
}
