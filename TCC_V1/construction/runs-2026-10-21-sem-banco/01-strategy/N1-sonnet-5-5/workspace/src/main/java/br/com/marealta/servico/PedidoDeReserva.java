package br.com.marealta.servico;

import java.time.LocalDate;

public record PedidoDeReserva(
        int numeroDoQuarto,
        String nome,
        String email,
        String cpf,
        LocalDate entrada,
        LocalDate saida,
        int hospedes,
        String plano,
        String codigoEmpresa) {
}
