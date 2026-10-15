package com.loja.checkout.payment;

import com.loja.checkout.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class CartaoPagamento implements FormaPagamentoStrategy {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO_CALCULO = new MathContext(20);

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal totalFinal = totalPedido;
            BigDecimal parcela = MoneyUtils.round(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO_CALCULO));
            return new ResultadoPagamento(MoneyUtils.round(BigDecimal.ZERO), totalFinal, parcela);
        }

        BigDecimal umMaisTaxaElevadoN = BigDecimal.ONE.add(TAXA_JUROS_MENSAL).pow(parcelas);
        BigDecimal fatorDesconto = BigDecimal.ONE.divide(umMaisTaxaElevadoN, PRECISAO_CALCULO);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
        BigDecimal numerador = totalPedido.multiply(TAXA_JUROS_MENSAL);
        BigDecimal parcela = MoneyUtils.round(numerador.divide(denominador, PRECISAO_CALCULO));
        BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, parcela);
    }
}
