package com.loja.resumo.pagamento;

import com.loja.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public int maxParcelas() {
        return 1;
    }

    @Override
    public Pagamento aplicar(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.subtract(Dinheiro.arredondar(total.multiply(DESCONTO)));
        return new Pagamento(totalFinal, parcelas, totalFinal);
    }
}
