package com.loja.resumo.pagamento;

import com.loja.resumo.util.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Arredondamento.paraCentavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new AjustePagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
        }

        // parcela = total * taxa / (1 - (1+taxa)^-n), reescrito sem expoente negativo:
        // parcela = total * taxa * (1+taxa)^n / ((1+taxa)^n - 1)
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(potencia);
        BigDecimal denominador = potencia.subtract(BigDecimal.ONE);
        BigDecimal valorParcela = Arredondamento.paraCentavos(
                numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new AjustePagamento(ajuste, totalFinal, valorParcela);
    }
}
