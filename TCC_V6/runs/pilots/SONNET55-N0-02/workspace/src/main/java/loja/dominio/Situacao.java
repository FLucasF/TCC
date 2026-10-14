package loja.dominio;

import java.util.Set;

public enum Situacao {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    /** Transições permitidas; é o único lugar a mexer ao criar uma nova situação (ex.: EM_SEPARACAO). */
    private Set<Situacao> destinos() {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> Set.of(PAGO, CANCELADO);
            case PAGO -> Set.of(ENVIADO, CANCELADO);
            case ENVIADO -> Set.of(ENTREGUE);
            case ENTREGUE, CANCELADO -> Set.of();
        };
    }

    public boolean podeIr(Situacao destino) {
        return destinos().contains(destino);
    }

    /** O pagamento já foi confirmado e o pedido não foi cancelado. */
    public boolean pagoEAtivo() {
        return this != AGUARDANDO_PAGAMENTO && this != CANCELADO;
    }
}
