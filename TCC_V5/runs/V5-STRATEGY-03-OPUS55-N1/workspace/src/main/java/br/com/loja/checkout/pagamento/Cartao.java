package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento aplicar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new Pagamento(totalPedido, parcelas, parcela);
        }
        BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
        return new Pagamento(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcelas, parcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n). */
    private static BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal descontoComposto = BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, mc), mc);
        return total.multiply(TAXA_MENSAL, mc).divide(BigDecimal.ONE.subtract(descontoComposto, mc), mc);
    }
}
