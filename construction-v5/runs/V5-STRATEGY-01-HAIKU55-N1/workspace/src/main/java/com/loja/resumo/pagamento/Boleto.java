package com.loja.resumo.pagamento;

import com.loja.resumo.ErroResumo;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public int maxParcelas() {
        return 1;
    }

    @Override
    public void verificarDisponivel(BigDecimal total) {
        if (total.compareTo(TOTAL_MAXIMO) > 0) {
            throw new ErroResumo("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    @Override
    public Pagamento aplicar(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.add(TARIFA);
        return new Pagamento(totalFinal, parcelas, totalFinal);
    }
}
