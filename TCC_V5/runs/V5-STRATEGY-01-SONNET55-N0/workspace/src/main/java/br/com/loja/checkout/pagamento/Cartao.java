package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");

    public String codigo() {
        return "CARTAO";
    }

    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(totalPedido.divide(n, MathContext.DECIMAL128)));
        }
        // Tabela Price: parcela = total * i / (1 - (1 + i)^-n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA).divide(denominador, MathContext.DECIMAL128));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(n)), parcela);
    }
}
