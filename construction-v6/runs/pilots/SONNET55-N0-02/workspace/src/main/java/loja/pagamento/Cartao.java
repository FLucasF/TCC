package loja.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;
import loja.dominio.NegocioException;

@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final MathContext MC = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            throw NegocioException.invalido("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public Cobranca cobrar(BigDecimal total, int parcelas, boolean comCupom) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_SEM_JUROS) {
            return new Cobranca(total, Dinheiro.arredondar(total.divide(n, MC)));
        }
        // Tabela Price: parcela = total × i ÷ (1 − (1 + i)^−n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas, MC);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MC), MC);
        BigDecimal parcela = Dinheiro.arredondar(total.multiply(TAXA).divide(divisor, MC));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(n)), parcela);
    }
}
