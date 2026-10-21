package br.com.marealta.servico;

import br.com.marealta.dominio.ApiException;
import br.com.marealta.dominio.ApiException.Codigo;
import br.com.marealta.dominio.Estadia;
import br.com.marealta.dominio.Hospede;
import br.com.marealta.dominio.PlanosDeTarifa;
import br.com.marealta.dominio.Quarto;
import br.com.marealta.dominio.Reserva;
import br.com.marealta.dominio.StatusReserva;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ReservaServico {

    private static final String ASSUNTO_CONFIRMACAO = "Reserva confirmada - Pousada Maré Alta";

    private final Pousada pousada;
    private final CaixaDeSaida caixaDeSaida;
    private final List<Reserva> reservas = new ArrayList<>();

    public ReservaServico(Pousada pousada, CaixaDeSaida caixaDeSaida) {
        this.pousada = pousada;
        this.caixaDeSaida = caixaDeSaida;
    }

    public synchronized Reserva criar(PedidoDeReserva pedido) {
        if (!pedido.saida().isAfter(pedido.entrada())) {
            throw ApiException.invalida("A saída deve ser depois da entrada");
        }
        if (pedido.hospedes() < 1) {
            throw ApiException.invalida("É preciso pelo menos um hóspede");
        }
        Quarto quarto = pousada.quarto(pedido.numeroDoQuarto())
                .orElseThrow(() -> new ApiException(Codigo.ROOM_NOT_FOUND, "Quarto não encontrado"));
        var plano = PlanosDeTarifa.porCodigo(pedido.plano());
        var estadia = new Estadia(quarto, pedido.entrada(), pedido.saida(), pedido.codigoEmpresa());
        var hospede = new Hospede(pedido.nome(), pedido.email(), pedido.cpf());
        Reserva reserva = new Reserva(reservas.size() + 1, hospede, estadia, pedido.hospedes(), plano);
        if (pedido.hospedes() > quarto.capacidade()) {
            throw new ApiException(Codigo.CAPACITY_EXCEEDED, "O quarto comporta até " + quarto.capacidade() + " pessoas");
        }
        boolean ocupado = reservas.stream()
                .filter(r -> r.status() != StatusReserva.CANCELLED)
                .anyMatch(r -> r.conflitaCom(quarto, pedido.entrada(), pedido.saida()));
        if (ocupado) {
            throw new ApiException(Codigo.ROOM_UNAVAILABLE, "Quarto já reservado em alguma das noites");
        }
        reservas.add(reserva);
        return reserva;
    }

    public synchronized Reserva buscar(int id) {
        if (id < 1 || id > reservas.size()) {
            throw new ApiException(Codigo.RESERVATION_NOT_FOUND, "Reserva não encontrada");
        }
        return reservas.get(id - 1);
    }

    public synchronized List<Reserva> todas() {
        return List.copyOf(reservas);
    }

    public synchronized Reserva confirmar(int id) {
        Reserva reserva = buscar(id);
        reserva.confirmar();
        caixaDeSaida.enviarEmail(reserva.hospede().email(), ASSUNTO_CONFIRMACAO, textoDaConfirmacao(reserva));
        return reserva;
    }

    public synchronized Reserva cancelar(int id) {
        return alterar(id, Reserva::cancelar);
    }

    public synchronized Reserva fazerCheckIn(int id) {
        return alterar(id, Reserva::fazerCheckIn);
    }

    public synchronized Comprovante fazerCheckOut(int id) {
        return Comprovante.de(alterar(id, Reserva::fazerCheckOut));
    }

    public synchronized Comprovante segundaVia(int id) {
        Reserva reserva = buscar(id);
        if (!reserva.status().paga()) {
            throw new ApiException(Codigo.OPERATION_NOT_ALLOWED, "A segunda via só existe para reserva paga");
        }
        return Comprovante.de(reserva);
    }

    private Reserva alterar(int id, Consumer<Reserva> operacao) {
        Reserva reserva = buscar(id);
        operacao.accept(reserva);
        return reserva;
    }

    private static String textoDaConfirmacao(Reserva reserva) {
        return "Olá, " + reserva.hospede().nome() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Comprovante.de(reserva).texto() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
