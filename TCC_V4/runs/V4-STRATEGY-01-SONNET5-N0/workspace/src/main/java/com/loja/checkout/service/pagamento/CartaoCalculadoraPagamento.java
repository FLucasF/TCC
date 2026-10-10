package com.loja.checkout.service.pagamento;

import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class CartaoCalculadoraPagamento implements CalculadoraPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(50);

    @Override
    public FormaPagamento getForma() {
        return FormaPagamento.CARTAO;
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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(Dinheiro.zero(), totalPedido, valorParcela);
        }

        BigDecimal fatorAcumulado = BigDecimal.ONE.add(TAXA_JUROS_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal fatorInverso = BigDecimal.ONE.divide(fatorAcumulado, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorInverso);
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_JUROS_MENSAL).divide(denominador, PRECISAO));

        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
