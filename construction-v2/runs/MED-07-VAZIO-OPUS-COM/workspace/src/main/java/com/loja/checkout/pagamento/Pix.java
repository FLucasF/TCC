package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A vista, com 5% de desconto no total do pedido. */
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
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.arredondar(
                totalPedido.subtract(Dinheiro.arredondar(totalPedido.multiply(DESCONTO))));
        return new Cobranca(totalFinal, totalFinal);
    }
}
