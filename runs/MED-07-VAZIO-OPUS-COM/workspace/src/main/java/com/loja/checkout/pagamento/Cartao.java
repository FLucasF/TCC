package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** De 1 a 12 vezes: ate 3x sem juros, acima disso com juros pela tabela Price. */
@Component
public class Cartao implements FormaPagamento {

    private static final MathContext PRECISAO = MathContext.DECIMAL128;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new Cobranca(Dinheiro.arredondar(totalPedido), parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }
}
