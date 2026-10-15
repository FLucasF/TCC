package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Cartao de credito: ate 3x sem juros, de 4x a 12x com juros de 1,99% ao mes. */
@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento liquidar(ContextoPagamento contexto) {
        BigDecimal total = contexto.totalPedido();
        int parcelas = contexto.parcelas();

        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Moeda.centavos(
                    total.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(Moeda.centavos(total), valorParcela);
        }

        BigDecimal valorParcela = Moeda.centavos(parcelaPrice(total, parcelas));
        BigDecimal totalFinal = Moeda.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }

    /** Tabela Price: total x taxa / (1 - (1 + taxa)^-parcelas). */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        return total.multiply(TAXA_MENSAL).divide(divisor, PRECISAO);
    }
}
