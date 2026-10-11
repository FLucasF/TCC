package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

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
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal quantidade = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(totalPedido.divide(quantidade, MathContext.DECIMAL128)));
        }
        // Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n)
        BigDecimal fator = BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas), MathContext.DECIMAL128);
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(BigDecimal.ONE.subtract(fator), MathContext.DECIMAL128));
        return new Cobranca(parcela.multiply(quantidade), parcela);
    }
}
