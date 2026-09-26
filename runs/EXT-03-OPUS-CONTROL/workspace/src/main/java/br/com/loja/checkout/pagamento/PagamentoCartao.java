package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Cartao de credito: ate 3x sem juros, de 4x a 12x com juros de 1,99% ao mes pela
 * tabela Price.
 */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = Dinheiro.de("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento aplicar(ContextoPagamento contexto) {
        BigDecimal total = contexto.totalPedido();
        int parcelas = contexto.parcelas();

        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.centavos(
                    total.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
            return new ResultadoPagamento(Dinheiro.centavos(total), valorParcela);
        }

        BigDecimal valorParcela = parcelaPrice(total, parcelas);
        BigDecimal totalFinal = Dinheiro.centavos(
                valorParcela.multiply(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
        return Dinheiro.centavos(
                total.multiply(TAXA_MENSAL, Dinheiro.PRECISAO).divide(divisor, Dinheiro.PRECISAO));
    }
}
