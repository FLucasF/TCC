package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.checkout.entrega.transportadora.CepNaoAtendidoException;
import br.com.loja.pedidos.checkout.entrega.transportadora.CotacaoTransportadora;
import br.com.loja.pedidos.checkout.entrega.transportadora.ServicoTransporte;
import br.com.loja.pedidos.checkout.entrega.transportadora.Transportadora;
import br.com.loja.pedidos.checkout.entrega.transportadora.TransportadoraForaDoArException;
import java.math.RoundingMode;

/**
 * Entrega feita por transportadora, que informa frete e prazo. O envio sai do centro de
 * distribuição, que demora mais para despachar em algumas regiões.
 */
abstract class EntregaPorTransportadora implements ModalidadeEntrega {

    private final Transportadora transportadora;
    private final ServicoTransporte servico;

    EntregaPorTransportadora(Transportadora transportadora, ServicoTransporte servico) {
        this.transportadora = transportadora;
        this.servico = servico;
    }

    @Override
    public CotacaoEntrega cotar(PedidoParaEntrega pedido) {
        long pesoGramas = pedido.pesoKg().movePointRight(3).setScale(0, RoundingMode.CEILING).longValueExact();
        try {
            CotacaoTransportadora cotacao = transportadora.cotar(pedido.cep(), pesoGramas, servico);
            int prazo = cotacao.prazoDias() + pedido.regiao().diasAdicionaisDespacho();
            return new CotacaoEntrega(cotacao.frete(), prazo);
        } catch (CepNaoAtendidoException e) {
            throw Indisponivel.modalidade();
        } catch (TransportadoraForaDoArException e) {
            throw Indisponivel.transportadora();
        }
    }
}
