package com.loja.checkout.pagamento;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("CARTAO")
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final double TAXA_MENSAL = 0.0199;

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < PARCELAS_MINIMAS || parcelas > PARCELAS_MAXIMAS) {
            parcelamentoInvalido();
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, Dinheiro.arredondar(parcela));
        }

        double fator = 1 - Math.pow(1 + TAXA_MENSAL, -parcelas);
        double parcelaBruta = totalPedido.doubleValue() * TAXA_MENSAL / fator;
        BigDecimal parcela = Dinheiro.arredondar(BigDecimal.valueOf(parcelaBruta));
        BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
