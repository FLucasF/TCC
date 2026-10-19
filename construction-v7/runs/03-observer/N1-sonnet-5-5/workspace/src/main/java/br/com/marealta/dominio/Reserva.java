package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.marealta.dominio.StatusReserva.*;

public class Reserva {

    private final int id;
    private final Quarto quarto;
    private final String nomeHospede;
    private final String emailHospede;
    private final String cpfHospede;
    private final LocalDate entrada;
    private final LocalDate saida;
    private final int hospedes;
    private final PlanoTarifa plano;
    private final Preco preco;

    private StatusReserva status = PENDING;
    private BigDecimal valorPago = Dinheiro.ZERO;
    private BigDecimal reembolso;

    public Reserva(int id, Quarto quarto, String nomeHospede, String emailHospede, String cpfHospede,
                   LocalDate entrada, LocalDate saida, int hospedes, PlanoTarifa plano) {
        this.id = id;
        this.quarto = quarto;
        this.nomeHospede = nomeHospede;
        this.emailHospede = emailHospede;
        this.cpfHospede = cpfHospede;
        this.entrada = entrada;
        this.saida = saida;
        this.hospedes = hospedes;
        this.plano = plano;
        this.preco = Preco.calcular(quarto, entrada, saida, plano);
    }

    public void confirmar() {
        exigir(status == PENDING, "confirmada", "pendente");
        status = CONFIRMED;
        valorPago = preco.total();
    }

    public void fazerCheckIn() {
        exigir(status == CONFIRMED, "com check-in feito", "confirmada");
        status = CHECKED_IN;
    }

    public void fazerCheckOut() {
        exigir(status == CHECKED_IN, "com check-out feito", "com check-in");
        status = CHECKED_OUT;
    }

    public void cancelar() {
        exigir(status == PENDING || status == CONFIRMED, "cancelada", "pendente ou confirmada");
        reembolso = status == CONFIRMED && plano.reembolsavel() ? valorPago : Dinheiro.ZERO;
        status = CANCELLED;
    }

    private void exigir(boolean permitido, String acao, String exigida) {
        if (!permitido) {
            throw new ErroNegocio(CodigoErro.OPERATION_NOT_ALLOWED,
                    "Só uma reserva " + exigida + " pode ficar " + acao + "; esta está " + status);
        }
    }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean paga() {
        return status == CONFIRMED || status == CHECKED_IN || status == CHECKED_OUT;
    }

    public boolean ocupaNoite(LocalDate noite) {
        return status != CANCELLED && !noite.isBefore(entrada) && noite.isBefore(saida);
    }

    public boolean sobrepoe(LocalDate outraEntrada, LocalDate outraSaida) {
        return status != CANCELLED && entrada.isBefore(outraSaida) && outraEntrada.isBefore(saida);
    }

    /** O que foi pago menos o que foi devolvido. */
    public BigDecimal valorRetido() {
        return reembolso == null ? valorPago : valorPago.subtract(reembolso);
    }

    public int id() { return id; }
    public Quarto quarto() { return quarto; }
    public String nomeHospede() { return nomeHospede; }
    public String emailHospede() { return emailHospede; }
    public String cpfHospede() { return cpfHospede; }
    public LocalDate entrada() { return entrada; }
    public LocalDate saida() { return saida; }
    public int hospedes() { return hospedes; }
    public PlanoTarifa plano() { return plano; }
    public Preco preco() { return preco; }
    public StatusReserva status() { return status; }
    public BigDecimal valorPago() { return valorPago; }
    public BigDecimal reembolso() { return reembolso; }
}
