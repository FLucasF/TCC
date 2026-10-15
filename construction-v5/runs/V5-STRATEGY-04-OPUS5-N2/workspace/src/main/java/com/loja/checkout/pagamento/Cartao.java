package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** De 1x a 12x: sem juros ate 3x, com juros de 1,99% ao mes de 4x a 12x (tabela Price). */
@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(34, RoundingMode.HALF_EVEN);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        return parcelas <= PARCELAS_SEM_JUROS
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new Cobranca(Dinheiro.centavos(totalPedido), parcelas, valorParcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
        BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(totalFinal, parcelas, valorParcela);
    }
}
