package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** De 1x a 12x: ate 3x sem juros, de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public Pago calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** O valor final e o proprio total do pedido, dividido em parcelas iguais. */
    private Pago semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO);
        return new Pago(Dinheiro.centavos(totalPedido), Dinheiro.centavos(parcela));
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private Pago comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fatorAcumulado = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fatorAcumulado, Dinheiro.PRECISAO));
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.PRECISAO));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Pago(totalFinal, parcela);
    }
}
