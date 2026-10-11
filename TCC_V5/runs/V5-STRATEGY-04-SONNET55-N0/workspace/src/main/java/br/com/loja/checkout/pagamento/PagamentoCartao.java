package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class PagamentoCartao implements FormaPagamento {

    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(totalPedido.divide(n, MathContext.DECIMAL128)));
        }
        // Tabela Price: total * i / (1 - (1 + i)^-n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, MathContext.DECIMAL128));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(n)), parcela);
    }
}
