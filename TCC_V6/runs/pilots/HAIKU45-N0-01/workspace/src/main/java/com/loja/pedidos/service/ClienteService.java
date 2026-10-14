package com.loja.pedidos.service;

import com.loja.pedidos.dto.RespostaCliente;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clienteRepository;

    public RespostaCliente obterCliente(String cpf) {
        var cliente = clienteRepository.findById(cpf)
            .orElseThrow(ErroNegocio::clienteNaoEncontrado);

        return RespostaCliente.builder()
            .cpf(cliente.getCpf())
            .saldoCredito(cliente.getSaldoCredito())
            .build();
    }
}
