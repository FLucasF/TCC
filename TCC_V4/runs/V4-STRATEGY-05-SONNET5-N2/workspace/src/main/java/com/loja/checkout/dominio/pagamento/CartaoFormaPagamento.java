package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class CartaoFormaPagamento implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }
        BigDecimal fatorJuros = BigDecimal.ONE.add(TAXA_JUROS_MENSAL)
                .pow(-parcelas, PRECISAO);
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_JUROS_MENSAL)
                        .divide(BigDecimal.ONE.subtract(fatorJuros), PRECISAO));
        BigDecimal valorFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(valorFinal, valorParcela);
    }
}
