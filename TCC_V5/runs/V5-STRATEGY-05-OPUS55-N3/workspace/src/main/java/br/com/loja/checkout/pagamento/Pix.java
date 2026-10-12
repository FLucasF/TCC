package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.Dinheiro;

@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.subtract(Dinheiro.centavos(totalPedido.multiply(DESCONTO)));
        return new Pagamento(totalFinal, totalFinal);
    }
}
