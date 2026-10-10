package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ate' 3x sem juros; de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
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
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        return parcelas <= PARCELAS_SEM_JUROS
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    /** O valor final e' o proprio total do pedido, dividido em parcelas iguais. */
    private Pagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.arredonda(
                totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
        return new Pagamento(totalPedido, parcela);
    }

    /** Parcela pela tabela Price; o valor final e' a parcela x o numero de parcelas. */
    private Pagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CALCULO);
        BigDecimal parcela = Dinheiro.arredonda(totalPedido
                .multiply(TAXA_MENSAL)
                .multiply(fator, Dinheiro.CALCULO)
                .divide(fator.subtract(BigDecimal.ONE), Dinheiro.CALCULO));
        return new Pagamento(Dinheiro.arredonda(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }
}
