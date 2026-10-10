package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
        }

        BigDecimal base = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal basePowN = base.pow(parcelas, MathContext.DECIMAL64);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(basePowN, MathContext.DECIMAL64));
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_JUROS_MENSAL).divide(denominador, MathContext.DECIMAL64));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
