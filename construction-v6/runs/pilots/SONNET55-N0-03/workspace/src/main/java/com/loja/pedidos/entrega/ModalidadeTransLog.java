package com.loja.pedidos.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

/** Base das opções cotadas pela TransLog. */
public abstract class ModalidadeTransLog implements ModalidadeEntrega {

    private final TransLogClient client;
    private final String servico;

    protected ModalidadeTransLog(TransLogClient client, String servico) {
        this.client = client;
        this.servico = servico;
    }

    @Override
    public Cotacao cotar(PedidoEntrega pedido) {
        int gramas = pedido.pesoKg().multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.CEILING).intValueExact();
        RespostaTransLog resposta = client.cotar(pedido.cep(), gramas, servico);
        return switch (resposta) {
            case RespostaTransLog.Preco p -> new Cotacao(
                    BigDecimal.valueOf(p.centavos(), 2),
                    (p.horas() + 23) / 24 + pedido.regiao().diasExtraDespacho());
            case RespostaTransLog.CepNaoAtendido n -> throw new ErroNegocio(CodigoErro.MODALIDADE_INDISPONIVEL);
            case RespostaTransLog.ForaDoAr f -> throw new ErroNegocio(CodigoErro.TRANSPORTADORA_INDISPONIVEL);
        };
    }
}
