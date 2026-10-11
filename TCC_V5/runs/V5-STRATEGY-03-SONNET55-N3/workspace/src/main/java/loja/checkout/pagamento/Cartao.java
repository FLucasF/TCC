package loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {
    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128)));
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA).divide(divisor, MathContext.DECIMAL128));
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }
}
