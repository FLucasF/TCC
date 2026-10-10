package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/** Cartão de crédito: até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
@Component
public class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MES = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new Pagamento(totalPedido, parcelas, parcela);
        }
        BigDecimal parcela = Dinheiro.arredondar(parcelaPrice(totalPedido, parcelas));
        return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
    }

    /** parcela = total × taxa ÷ (1 − (1 + taxa)^−n), sem arredondar. */
    private static BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS_MES).pow(-parcelas, PRECISAO);
        return total.multiply(TAXA_JUROS_MES).divide(BigDecimal.ONE.subtract(fator), PRECISAO);
    }
}
