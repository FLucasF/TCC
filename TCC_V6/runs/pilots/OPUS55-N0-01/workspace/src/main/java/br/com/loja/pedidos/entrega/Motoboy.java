package br.com.loja.pedidos.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.comum.Dinheiro;
import br.com.loja.pedidos.comum.ErroNegocio;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final BigDecimal FRETE = Dinheiro.reais("18.00");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public CotacaoEntrega cotar(ContextoEntrega contexto) {
        if (contexto.pesoKg().compareTo(PESO_MAXIMO_KG) > 0) {
            throw ErroNegocio.invalido("MODALIDADE_INDISPONIVEL");
        }
        return new CotacaoEntrega(FRETE, 0);
    }

    /** O motoboy é terceirizado: o clube não isenta o frete. */
    @Override
    public boolean isentavelPeloClube() {
        return false;
    }
}
