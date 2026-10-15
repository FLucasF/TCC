package br.com.loja.pedidos.checkout.entrega.transportadora;

/** Transportadora que cota frete e prazo. Trocar de transportadora é trocar esta implementação. */
public interface Transportadora {

    /**
     * @throws CepNaoAtendidoException se a transportadora não atende o CEP
     * @throws TransportadoraForaDoArException se não foi possível obter a cotação
     */
    CotacaoTransportadora cotar(String cep, long pesoGramas, ServicoTransporte servico);
}
