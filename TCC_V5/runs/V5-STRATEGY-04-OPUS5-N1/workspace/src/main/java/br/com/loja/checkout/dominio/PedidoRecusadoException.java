package br.com.loja.checkout.dominio;

public class PedidoRecusadoException extends RuntimeException {

    private final MotivoRecusa motivo;

    public PedidoRecusadoException(MotivoRecusa motivo) {
        super(motivo.name());
        this.motivo = motivo;
    }

    public MotivoRecusa motivo() {
        return motivo;
    }
}
