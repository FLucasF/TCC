package br.com.marealta.web;

import br.com.marealta.dominio.Comprovante;
import br.com.marealta.dominio.Quarto;
import br.com.marealta.dominio.Reserva;
import br.com.marealta.notificacao.Mensagem;

import java.math.BigDecimal;

/** Como o site enxerga os objetos do sistema. */
final class Json {

    private Json() {
    }

    record QuartoJson(int number, String type, int capacity, BigDecimal nightlyRate) {
        static QuartoJson de(Quarto q) {
            return new QuartoJson(q.numero(), q.tipo().name(), q.capacidade(), q.diaria());
        }
    }

    record ReservaJson(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       String checkIn, String checkOut, int guests, String ratePlan, int nights,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                       BigDecimal amountPaid, BigDecimal refund) {
        static ReservaJson de(Reserva r) {
            return new ReservaJson(r.id(), r.quarto().numero(), r.nomeHospede(), r.emailHospede(), r.cpfHospede(),
                    r.entrada().toString(), r.saida().toString(), r.hospedes(), r.plano().name(),
                    r.preco().noites(), r.preco().subtotal(), r.preco().desconto(), r.preco().total(),
                    r.status().name(), r.valorPago(), r.reembolso());
        }
    }

    record ComprovanteJson(String title, int reservationId, String guestName, String guestCpf, int roomNumber,
                           String roomType, String checkIn, String checkOut, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        static ComprovanteJson de(Comprovante c) {
            return new ComprovanteJson("Comprovante de reserva", c.reserva(), c.nomeHospede(), c.cpfHospede(),
                    c.quarto(), c.tipoQuarto().name(), c.entrada().toString(), c.saida().toString(), c.noites(),
                    c.subtotal(), c.desconto(), c.total());
        }
    }

    record MensagemJson(int id, String channel, String to, String subject, String body) {
        static MensagemJson de(Mensagem m) {
            return new MensagemJson(m.id(), m.canal().name(), m.para(), m.assunto(), m.texto());
        }
    }
}
