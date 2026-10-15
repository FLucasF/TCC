package loja.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;
import loja.dominio.NegocioException;

@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {
        if (totalPedido.compareTo(LIMITE) > 0) {
            throw NegocioException.invalido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    @Override
    public Cobranca cobrar(BigDecimal total, int parcelas, boolean comCupom) {
        BigDecimal finalPago = Dinheiro.arredondar(total.add(TARIFA));
        return new Cobranca(finalPago, finalPago);
    }
}
