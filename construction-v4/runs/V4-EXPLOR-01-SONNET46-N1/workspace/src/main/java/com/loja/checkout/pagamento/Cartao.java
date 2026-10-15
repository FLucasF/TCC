package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela);
        }
        BigDecimal valorParcela = calcularParcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }

    private BigDecimal calcularParcelaPrice(BigDecimal total, int n) {
        // parcela = total * taxa / (1 - (1 + taxa)^-n)
        MathContext mc = new MathContext(20, RoundingMode.HALF_EVEN);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal fatorInverso = BigDecimal.ONE.divide(umMaisTaxa.pow(n, mc), mc);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorInverso);
        BigDecimal parcela = total.multiply(TAXA_JUROS, mc).divide(denominador, mc);
        return parcela.setScale(2, RoundingMode.HALF_EVEN);
    }
}
