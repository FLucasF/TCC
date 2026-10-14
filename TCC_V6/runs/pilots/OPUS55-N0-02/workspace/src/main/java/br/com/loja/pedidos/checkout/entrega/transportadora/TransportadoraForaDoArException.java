package br.com.loja.pedidos.checkout.entrega.transportadora;

public class TransportadoraForaDoArException extends RuntimeException {

    public TransportadoraForaDoArException(String mensagem) {
        super(mensagem);
    }

    public TransportadoraForaDoArException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
