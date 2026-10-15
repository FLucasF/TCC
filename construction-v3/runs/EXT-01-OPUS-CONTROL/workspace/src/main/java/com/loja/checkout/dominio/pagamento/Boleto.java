package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Boleto: tarifa do banco de R$ 3,49, sempre a vista e ate R$ 1.000,00. */
@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TETO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(ContextoPagamento contexto) {
        return contexto.totalSemImposto().compareTo(TETO) <= 0;
    }

    @Override
    public ResultadoPagamento liquidar(ContextoPagamento contexto) {
        BigDecimal totalFinal = Moeda.centavos(contexto.totalPedido().add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
