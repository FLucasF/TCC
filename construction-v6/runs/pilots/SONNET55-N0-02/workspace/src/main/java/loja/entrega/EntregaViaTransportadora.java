package loja.entrega;

/** Base das opções cotadas pela transportadora: soma os dias extras de despacho da região. */
public abstract class EntregaViaTransportadora implements ModalidadeEntrega {

    private final Transportadora transportadora;

    protected EntregaViaTransportadora(Transportadora transportadora) {
        this.transportadora = transportadora;
    }

    @Override
    public Cotacao cotar(ContextoEntrega ctx) {
        Cotacao c = transportadora.cotar(ctx.cep(), ctx.pesoKg(), codigo());
        return new Cotacao(c.valor(), c.prazoDias() + ctx.regiao().diasExtraTransportadora());
    }
}
