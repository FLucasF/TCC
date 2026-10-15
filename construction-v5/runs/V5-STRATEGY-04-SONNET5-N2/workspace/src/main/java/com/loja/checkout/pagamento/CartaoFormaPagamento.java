package com.loja.checkout.pagamento;

import com.loja.checkout.dinheiro.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class CartaoFormaPagamento implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean indisponivel(BigDecimal totalPedido) {
        return false;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
        }
        BigDecimal valorParcela = Dinheiro.arredondar(calcularParcelaComJuros(totalPedido, parcelas));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fatorComposto = umMaisTaxa.pow(parcelas, PRECISAO);
        BigDecimal fatorInverso = BigDecimal.ONE.divide(fatorComposto, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorInverso);
        return totalPedido.multiply(TAXA_MENSAL).divide(denominador, PRECISAO);
    }
}
