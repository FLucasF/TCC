package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal JUROS_AO_MES = new BigDecimal("0.0199");

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
        BigDecimal quantidade = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return new Pagamento(totalPedido, Dinheiro.centavos(totalPedido.divide(quantidade, MathContext.DECIMAL128)));
        }
        BigDecimal valorParcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
        return new Pagamento(Dinheiro.centavos(valorParcela.multiply(quantidade)), valorParcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n). */
    private static BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(JUROS_AO_MES).pow(parcelas);
        BigDecimal descontado = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        return total.multiply(JUROS_AO_MES).divide(descontado, MathContext.DECIMAL128);
    }
}
