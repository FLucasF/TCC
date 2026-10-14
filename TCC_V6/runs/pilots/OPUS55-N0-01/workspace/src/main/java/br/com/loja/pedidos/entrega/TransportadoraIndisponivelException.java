package br.com.loja.pedidos.entrega;

public class TransportadoraIndisponivelException extends RuntimeException {

    public TransportadoraIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
