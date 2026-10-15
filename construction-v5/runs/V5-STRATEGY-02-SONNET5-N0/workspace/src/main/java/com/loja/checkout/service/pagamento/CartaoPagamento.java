package com.loja.checkout.service.pagamento;

import com.loja.checkout.service.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class CartaoPagamento implements FormaPagamentoCalculo {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(40);

    @Override
    public String getCodigo() {
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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
        }

        BigDecimal fatorDesconto = BigDecimal.ONE.add(TAXA_JUROS_MENSAL).pow(-parcelas, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_JUROS_MENSAL).divide(denominador, PRECISAO));

        BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
