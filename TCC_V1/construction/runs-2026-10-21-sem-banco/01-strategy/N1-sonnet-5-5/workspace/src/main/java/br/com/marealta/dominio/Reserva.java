package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reserva {

    private final int id;
    private final Quarto quarto;
    private final Hospede hospede;
    private final Estadia estadia;
    private final int hospedes;
    private final PlanoDeTarifa plano;
    private final Cobranca cobranca;
    private StatusReserva status = StatusReserva.PENDING;
    private BigDecimal valorPago = Dinheiro.ZERO;
    private BigDecimal devolvido;

    public Reserva(int id, Hospede hospede, Estadia estadia, int hospedes, PlanoDeTarifa plano) {
        this.id = id;
        this.quarto = estadia.quarto();
        this.hospede = hospede;
        this.estadia = estadia;
        this.hospedes = hospedes;
        this.plano = plano;
        this.cobranca = plano.cobrar(estadia);
    }

    public void confirmar() {
        exigir(StatusReserva.PENDING, "Só é possível confirmar uma reserva pendente");
        valorPago = cobranca.total();
        status = StatusReserva.CONFIRMED;
    }

    public void fazerCheckIn() {
        exigir(StatusReserva.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = StatusReserva.CHECKED_IN;
    }

    public void fazerCheckOut() {
        exigir(StatusReserva.CHECKED_IN, "Só é possível fazer check-out de uma reserva com check-in");
        status = StatusReserva.CHECKED_OUT;
    }

    public void cancelar() {
        if (status != StatusReserva.PENDING && status != StatusReserva.CONFIRMED) {
            throw naoPermitida("Só é possível cancelar uma reserva pendente ou confirmada");
        }
        devolvido = plano.devolucao(valorPago);
        status = StatusReserva.CANCELLED;
    }

    private void exigir(StatusReserva esperado, String mensagem) {
        if (status != esperado) {
            throw naoPermitida(mensagem);
        }
    }

    private static ApiException naoPermitida(String mensagem) {
        return new ApiException(ApiException.Codigo.OPERATION_NOT_ALLOWED, mensagem);
    }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal receita() {
        return devolvido == null ? valorPago : valorPago.subtract(devolvido);
    }

    /** Duas reservas disputam o quarto quando alguma noite coincide. */
    public boolean conflitaCom(Quarto outroQuarto, LocalDate entrada, LocalDate saida) {
        return quarto.equals(outroQuarto) && estadia.entrada().isBefore(saida) && entrada.isBefore(estadia.saida());
    }

    public boolean ocupaANoite(LocalDate noite) {
        return !noite.isBefore(estadia.entrada()) && noite.isBefore(estadia.saida());
    }

    public int id() {
        return id;
    }

    public Quarto quarto() {
        return quarto;
    }

    public Hospede hospede() {
        return hospede;
    }

    public LocalDate entrada() {
        return estadia.entrada();
    }

    public LocalDate saida() {
        return estadia.saida();
    }

    public int noites() {
        return estadia.noites();
    }

    public int hospedes() {
        return hospedes;
    }

    public PlanoDeTarifa plano() {
        return plano;
    }

    public BigDecimal diarias() {
        return cobranca.diarias();
    }

    public BigDecimal desconto() {
        return cobranca.desconto();
    }

    public BigDecimal total() {
        return cobranca.total();
    }

    public StatusReserva status() {
        return status;
    }

    public BigDecimal valorPago() {
        return valorPago;
    }

    public BigDecimal devolvido() {
        return devolvido;
    }
}
