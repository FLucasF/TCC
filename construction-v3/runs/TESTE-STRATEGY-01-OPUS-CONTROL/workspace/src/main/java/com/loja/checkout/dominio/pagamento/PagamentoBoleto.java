package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Sempre a vista, com tarifa bancaria de R$ 3,49 e teto de R$ 1.000,00. */
@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TETO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(ContextoPagamento contexto) {
        return contexto.totalSemImposto().compareTo(TETO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(ContextoPagamento contexto, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(contexto.totalPedido().add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
