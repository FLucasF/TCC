package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(40, RoundingMode.HALF_EVEN);

    public String codigo() {
        return "CARTAO";
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(totalPedido.divide(n, PRECISAO)));
        }
        // Tabela Price: total * i / (1 - (1 + i)^-n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO), PRECISAO);
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
        return new Cobranca(parcela.multiply(n), parcela);
    }
}
