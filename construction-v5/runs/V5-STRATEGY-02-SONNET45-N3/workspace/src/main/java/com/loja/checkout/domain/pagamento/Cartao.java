package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.math.MathContext;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public AjustePagamento calcularAjuste(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return calcularSemJuros(total, parcelas);
        } else {
            return calcularComJuros(total, parcelas);
        }
    }

    private AjustePagamento calcularSemJuros(BigDecimal total, int parcelas) {
        BigDecimal valorParcela = Arredondamento.arredondar(
            total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128)
        );
        BigDecimal totalFinal = total;
        BigDecimal ajuste = BigDecimal.ZERO.setScale(2);

        return new AjustePagamento(ajuste, totalFinal, valorParcela);
    }

    private AjustePagamento calcularComJuros(BigDecimal total, int parcelas) {
        // Fórmula Price: parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal fatorPotencia = umMaisTaxa.pow(-parcelas, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorPotencia);

        BigDecimal valorParcela = total.multiply(TAXA_JUROS)
            .divide(denominador, MathContext.DECIMAL128);
        valorParcela = Arredondamento.arredondar(valorParcela);

        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(total);

        return new AjustePagamento(ajuste, totalFinal, valorParcela);
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean estaDisponivel(BigDecimal total) {
        return true;
    }
}
