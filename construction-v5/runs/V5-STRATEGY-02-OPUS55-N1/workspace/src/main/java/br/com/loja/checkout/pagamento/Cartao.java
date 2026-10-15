package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Até 3x sem juros; de 4x a 12x com juros pela tabela Price. */
@Component
class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new Pagamento(totalPedido, parcelas, parcela);
        }
        BigDecimal fatorDesconto = BigDecimal.ONE.add(TAXA_MENSAL).pow(-parcelas, PRECISAO);
        BigDecimal parcela = Dinheiro.centavos(totalPedido.multiply(TAXA_MENSAL)
                .divide(BigDecimal.ONE.subtract(fatorDesconto), PRECISAO));
        return new Pagamento(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcelas, parcela);
    }
}
