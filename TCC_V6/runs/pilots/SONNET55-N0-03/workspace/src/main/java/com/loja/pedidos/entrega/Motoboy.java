package com.loja.pedidos.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final BigDecimal FRETE = Dinheiro.valor("18.00");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public Cotacao cotar(PedidoEntrega pedido) {
        if (pedido.pesoKg().compareTo(PESO_MAXIMO_KG) > 0) {
            throw new ErroNegocio(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        return new Cotacao(FRETE, 0);
    }

    /** Motoboy é terceirizado: benefícios que zeram o frete não valem. */
    @Override
    public boolean permiteIsencaoFrete() {
        return false;
    }
}
