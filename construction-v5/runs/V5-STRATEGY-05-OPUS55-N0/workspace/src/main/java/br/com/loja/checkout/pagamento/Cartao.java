package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Cartão de crédito: até 3x sem juros; de 4x a 12x com juros pela tabela Price. */
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
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal quantidade = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(totalPedido.divide(quantidade, MathContext.DECIMAL128));
            return new ResultadoPagamento(totalPedido, parcelas, parcela);
        }
        // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
        BigDecimal fatorDesconto = BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas), MathContext.DECIMAL128);
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(BigDecimal.ONE.subtract(fatorDesconto), MathContext.DECIMAL128));
        return new ResultadoPagamento(parcela.multiply(quantidade), parcelas, parcela);
    }
}
