package br.com.marealta.relatorio;

import br.com.marealta.dominio.Pousada;
import br.com.marealta.dominio.Reserva;
import br.com.marealta.dominio.ReservaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class Relatorios {

    private final Pousada pousada;
    private final ReservaService reservas;

    public Relatorios(Pousada pousada, ReservaService reservas) {
        this.pousada = pousada;
        this.reservas = reservas;
    }

    public String ocupacao(LocalDate noite) {
        List<Reserva> ocupando = reservas.todas().stream()
                .filter(r -> r.paga() && r.ocupaNoite(noite))
                .sorted(Comparator.comparingInt((Reserva r) -> r.quarto().numero()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reserva r : ocupando) {
            csv.append(r.quarto().numero()).append(',').append(r.quarto().tipo()).append(',').append(r.id())
                    .append(',').append(r.entrada()).append(',').append(r.saida()).append('\n');
        }
        // uma reserva paga por quarto e por noite, então cada linha é um quarto ocupado
        return csv.append("total,").append(ocupando.size()).append('/').append(pousada.quartos().size())
                .append('\n').toString();
    }

    public String financeiro(YearMonth mes) {
        List<Reserva> doMes = reservas.todas().stream()
                .filter(r -> YearMonth.from(r.entrada()).equals(mes) && r.valorRetido().signum() > 0)
                .sorted(Comparator.comparing(Reserva::entrada).thenComparingInt(Reserva::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal soma = BigDecimal.ZERO.setScale(2);
        for (Reserva r : doMes) {
            csv.append(r.id()).append(',').append(r.quarto().numero()).append(',').append(r.entrada())
                    .append(',').append(r.saida()).append(',').append(r.status()).append(',')
                    .append(r.valorRetido().toPlainString()).append('\n');
            soma = soma.add(r.valorRetido());
        }
        return csv.append("total,").append(soma.toPlainString()).append('\n').toString();
    }
}
