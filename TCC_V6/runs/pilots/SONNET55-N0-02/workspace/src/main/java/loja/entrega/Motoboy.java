package loja.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;
import loja.dominio.NegocioException;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");
    private static final BigDecimal VALOR = Dinheiro.arredondar(new BigDecimal("18.00"));

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    /** Motoboy é terceirizado: o clube não zera este frete. */
    @Override
    public boolean aceitaIsencaoDoClube() {
        return false;
    }

    @Override
    public Cotacao cotar(ContextoEntrega ctx) {
        if (ctx.pesoKg().compareTo(PESO_MAXIMO) > 0) {
            throw NegocioException.invalido("MODALIDADE_INDISPONIVEL");
        }
        return new Cotacao(VALOR, 0);
    }
}
