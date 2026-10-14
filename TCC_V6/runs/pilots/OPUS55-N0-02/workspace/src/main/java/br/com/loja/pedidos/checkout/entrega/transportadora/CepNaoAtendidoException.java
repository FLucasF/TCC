package br.com.loja.pedidos.checkout.entrega.transportadora;

public class CepNaoAtendidoException extends RuntimeException {

    public CepNaoAtendidoException(String cep) {
        super("CEP não atendido: " + cep);
    }
}
