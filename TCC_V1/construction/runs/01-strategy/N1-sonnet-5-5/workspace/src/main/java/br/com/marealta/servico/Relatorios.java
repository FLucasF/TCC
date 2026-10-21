package br.com.marealta.servico;

import br.com.marealta.dominio.Dinheiro;
import br.com.marealta.dominio.Reserva;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class Relatorios {

    private final Pousada pousada;
    private final ReservaServico reservas;

    public Relatorios(Pousada pousada, ReservaServico reservas) {
        this.pousada = pousada;
        this.reservas = reservas;
    }

    public String ocupacao(LocalDate noite) {
        List<Reserva> ocupadas = reservas.todas().stream()
                .filter(r -> r.status().paga() && r.ocupaANoite(noite))
                .sorted(Comparator.comparingInt((Reserva r) -> r.quarto().numero()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reserva r : ocupadas) {
            csv.append(r.quarto().numero()).append(',')
                    .append(r.quarto().tipo()).append(',')
                    .append(r.id()).append(',')
                    .append(r.entrada()).append(',')
                    .append(r.saida()).append('\n');
        }
        long quartosOcupados = ocupadas.stream().map(Reserva::quarto).distinct().count();
        return csv.append("total,").append(quartosOcupados).append('/').append(pousada.quartos().size())
                .append('\n').toString();
    }

    public String financeiro(YearMonth mes) {
        List<Reserva> comReceita = reservas.todas().stream()
                .filter(r -> YearMonth.from(r.entrada()).equals(mes) && r.receita().signum() > 0)
                .sorted(Comparator.comparing(Reserva::entrada).thenComparingInt(Reserva::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal soma = Dinheiro.ZERO;
        for (Reserva r : comReceita) {
            csv.append(r.id()).append(',')
                    .append(r.quarto().numero()).append(',')
                    .append(r.entrada()).append(',')
                    .append(r.saida()).append(',')
                    .append(r.status()).append(',')
                    .append(r.receita().toPlainString()).append('\n');
            soma = soma.add(r.receita());
        }
        return csv.append("total,").append(soma.toPlainString()).append('\n').toString();
    }
}
