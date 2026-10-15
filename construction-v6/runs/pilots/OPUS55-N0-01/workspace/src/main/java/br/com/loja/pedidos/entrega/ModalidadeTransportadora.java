package br.com.loja.pedidos.entrega;

import org.springframework.http.HttpStatus;

import br.com.loja.pedidos.comum.Dinheiro;
import br.com.loja.pedidos.comum.ErroNegocio;

/** Opção de entrega cotada por uma transportadora parceira, despachada pelo centro de distribuição. */
public abstract class ModalidadeTransportadora implements ModalidadeEntrega {

    private final Transportadora transportadora;
    private final String servico;

    protected ModalidadeTransportadora(Transportadora transportadora, String servico) {
        this.transportadora = transportadora;
        this.servico = servico;
    }

    @Override
    public CotacaoEntrega cotar(ContextoEntrega contexto) {
        CotacaoEntrega cotacao;
        try {
            cotacao = transportadora.cotar(contexto.cep(), contexto.pesoKg(), servico)
                    .orElseThrow(() -> ErroNegocio.invalido("MODALIDADE_INDISPONIVEL"));
        } catch (TransportadoraIndisponivelException e) {
            throw new ErroNegocio("TRANSPORTADORA_INDISPONIVEL", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new CotacaoEntrega(Dinheiro.centavos(cotacao.frete()),
                cotacao.prazoDias() + contexto.regiao().diasAdicionaisDespacho());
    }
}
