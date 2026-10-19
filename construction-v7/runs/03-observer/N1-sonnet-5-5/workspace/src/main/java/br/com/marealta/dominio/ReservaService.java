package br.com.marealta.dominio;

import java.util.ArrayList;
import java.util.List;

public class ReservaService {

    private final Pousada pousada;
    private final List<OuvinteReserva> ouvintes;
    private final List<Reserva> reservas = new ArrayList<>();

    public ReservaService(Pousada pousada, List<OuvinteReserva> ouvintes) {
        this.pousada = pousada;
        this.ouvintes = List.copyOf(ouvintes);
    }

    public synchronized Reserva criar(NovaReserva pedido) {
        if (!pedido.saida().isAfter(pedido.entrada())) {
            throw new ErroNegocio(CodigoErro.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (pedido.hospedes() < 1) {
            throw new ErroNegocio(CodigoErro.INVALID_REQUEST, "A reserva precisa de ao menos um hóspede");
        }
        Quarto quarto = pousada.quarto(pedido.quarto()).orElseThrow(() ->
                new ErroNegocio(CodigoErro.ROOM_NOT_FOUND, "Quarto " + pedido.quarto() + " não existe"));
        if (pedido.hospedes() > quarto.capacidade()) {
            throw new ErroNegocio(CodigoErro.CAPACITY_EXCEEDED,
                    "O quarto " + quarto.numero() + " comporta até " + quarto.capacidade() + " hóspedes");
        }
        boolean ocupado = reservas.stream().anyMatch(r ->
                r.quarto().equals(quarto) && r.sobrepoe(pedido.entrada(), pedido.saida()));
        if (ocupado) {
            throw new ErroNegocio(CodigoErro.ROOM_UNAVAILABLE,
                    "O quarto " + quarto.numero() + " já está reservado em alguma dessas noites");
        }
        Reserva reserva = new Reserva(reservas.size() + 1, quarto, pedido.nomeHospede(), pedido.emailHospede(),
                pedido.cpfHospede(), pedido.entrada(), pedido.saida(), pedido.hospedes(), pedido.plano());
        reservas.add(reserva);
        return reserva;
    }

    public synchronized Reserva buscar(int id) {
        if (id < 1 || id > reservas.size()) {
            throw new ErroNegocio(CodigoErro.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
        }
        return reservas.get(id - 1);
    }

    public synchronized Reserva confirmar(int id) {
        Reserva reserva = buscar(id);
        reserva.confirmar();
        ouvintes.forEach(o -> o.reservaConfirmada(reserva));
        return reserva;
    }

    public synchronized Reserva cancelar(int id) {
        Reserva reserva = buscar(id);
        boolean estavaConfirmada = reserva.status() == StatusReserva.CONFIRMED;
        reserva.cancelar();
        if (estavaConfirmada) {
            ouvintes.forEach(o -> o.reservaConfirmadaCancelada(reserva));
        }
        return reserva;
    }

    public synchronized Reserva fazerCheckIn(int id) {
        Reserva reserva = buscar(id);
        reserva.fazerCheckIn();
        return reserva;
    }

    public synchronized Comprovante fazerCheckOut(int id) {
        Reserva reserva = buscar(id);
        reserva.fazerCheckOut();
        return Comprovante.de(reserva);
    }

    public synchronized Comprovante segundaVia(int id) {
        Reserva reserva = buscar(id);
        if (!reserva.paga()) {
            throw new ErroNegocio(CodigoErro.OPERATION_NOT_ALLOWED, "Só reserva paga tem comprovante");
        }
        return Comprovante.de(reserva);
    }

    public synchronized List<Reserva> todas() {
        return List.copyOf(reservas);
    }
}
