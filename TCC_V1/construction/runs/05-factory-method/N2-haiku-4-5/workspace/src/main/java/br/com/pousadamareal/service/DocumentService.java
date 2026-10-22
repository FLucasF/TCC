package br.com.pousadamareal.service;

import br.com.pousadamareal.domain.Document;
import br.com.pousadamareal.domain.IdentificationType;
import br.com.pousadamareal.domain.Reservation;
import br.com.pousadamareal.domain.Room;
import br.com.pousadamareal.repository.RoomRepository;
import br.com.pousadamareal.util.DateUtils;
import br.com.pousadamareal.util.MoneyUtils;

import java.sql.SQLException;

public class DocumentService {
    private final RoomRepository roomRepository;

    public DocumentService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Document generateDocument(Reservation reservation) throws SQLException {
        Room room = roomRepository.findById(reservation.getRoomNumber());
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + reservation.getRoomNumber());
        }

        Document doc = new Document();
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setCompanyCnpj(reservation.getCompanyCnpj());
        doc.setCompanyName(reservation.getCompanyName());
        doc.setGuestPassport(reservation.getGuestPassport());
        doc.setGuestCountry(reservation.getGuestCountry());
        doc.setRoomNumber(room.getNumber());
        doc.setRoomType(room.getType().displayName());
        doc.setCheckInDate(reservation.getCheckIn());
        doc.setCheckOutDate(reservation.getCheckOut());
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        if (reservation.getDiscount() > 0) {
            doc.setDiscount(reservation.getDiscount());
        }
        doc.setTotal(reservation.getTotal());

        IdentificationType idType = reservation.getIdentificationType();
        if (idType == IdentificationType.CPF) {
            doc.setTitle("Comprovante de reserva");
            doc.setText(generateComprovante(doc));
        } else if (idType == IdentificationType.CNPJ) {
            doc.setTitle("Fatura de hospedagem");
            doc.setText(generateFatura(doc));
        } else if (idType == IdentificationType.PASSPORT) {
            doc.setTitle("Booking receipt");
            doc.setText(generateBookingReceipt(doc));
        }

        return doc;
    }

    private String generateComprovante(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append(" (CPF ").append(doc.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn()).append(" | Saída: ").append(doc.getCheckOut()).append(" | ");
        sb.append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(MoneyUtils.formatBRL(doc.getSubtotal())).append("\n");
        if (doc.getDiscount() != null && doc.getDiscount() > 0) {
            sb.append("Desconto: ").append(MoneyUtils.formatBRL(doc.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(MoneyUtils.formatBRL(doc.getTotal())).append("\n");
        return sb.toString();
    }

    private String generateFatura(Document doc) {
        double iss = calculateISS(doc.getTotal());
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(doc.getReservationId()).append("\n");
        sb.append("Empresa: ").append(doc.getCompanyName()).append(" (CNPJ ").append(doc.getCompanyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append("\n");
        sb.append("Quarto: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn()).append(" | Saída: ").append(doc.getCheckOut()).append(" | ");
        sb.append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(MoneyUtils.formatBRL(doc.getSubtotal())).append("\n");
        if (doc.getDiscount() != null && doc.getDiscount() > 0) {
            sb.append("Desconto: ").append(MoneyUtils.formatBRL(doc.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(MoneyUtils.formatBRL(doc.getTotal())).append("\n");
        sb.append("ISS retido (5%): ").append(MoneyUtils.formatBRL(iss)).append("\n");
        return sb.toString();
    }

    private String generateBookingReceipt(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(doc.getReservationId()).append("\n");
        sb.append("Guest: ").append(doc.getGuestName()).append(" (passport ").append(doc.getGuestPassport()).append(", ").append(doc.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Check-in: ").append(DateUtils.toEnglish(doc.getCheckInDate())).append(" | Check-out: ").append(DateUtils.toEnglish(doc.getCheckOutDate())).append(" | ");
        sb.append(doc.getNights()).append(doc.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(MoneyUtils.formatBRLEnglish(doc.getSubtotal())).append("\n");
        if (doc.getDiscount() != null && doc.getDiscount() > 0) {
            sb.append("Discount: ").append(MoneyUtils.formatBRLEnglish(doc.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(MoneyUtils.formatBRLEnglish(doc.getTotal())).append("\n");
        return sb.toString();
    }

    private double calculateISS(double total) {
        return MoneyUtils.round(total * 0.05);
    }
}
