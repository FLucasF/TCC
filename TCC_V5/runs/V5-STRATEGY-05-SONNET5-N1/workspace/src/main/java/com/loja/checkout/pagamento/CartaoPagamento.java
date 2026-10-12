package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CartaoPagamento implements FormaPagamentoCalculadora {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public FormaPagamento getTipo() {
        return FormaPagamento.CARTAO;
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Arredondamento.monetario(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
        }

        BigDecimal valorParcela = Arredondamento.monetario(calcularParcelaPrice(totalPedido, parcelas));
        BigDecimal totalFinal = Arredondamento.monetario(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Arredondamento.monetario(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }

    /**
     * Tabela Price: parcela = total × taxa ÷ (1 − (1+taxa)^−n), reescrita sem expoente
     * negativo para usar apenas BigDecimal exato: parcela = total × taxa × (1+taxa)^n ÷
     * ((1+taxa)^n − 1).
     */
    private BigDecimal calcularParcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fatorElevado = umMaisTaxa.pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(fatorElevado);
        BigDecimal denominador = fatorElevado.subtract(BigDecimal.ONE);
        return numerador.divide(denominador, 20, RoundingMode.HALF_EVEN);
    }
}
