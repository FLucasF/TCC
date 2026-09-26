package com.loja.checkout.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class CartaoCalculadora implements CalculadoraPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public FormaPagamento formaPagamento() {
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
            BigDecimal valorParcela = Money.round(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
            return new ResultadoPagamento(BigDecimal.ZERO, totalPedido, valorParcela);
        }

        // Tabela Price: parcela = total x taxa / (1 - (1+taxa)^-n)
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fatorAcumulado = umMaisTaxa.pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(fatorAcumulado);
        BigDecimal denominador = fatorAcumulado.subtract(BigDecimal.ONE);
        BigDecimal valorParcela = Money.round(numerador.divide(denominador, MathContext.DECIMAL64));

        BigDecimal totalFinal = Money.round(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
