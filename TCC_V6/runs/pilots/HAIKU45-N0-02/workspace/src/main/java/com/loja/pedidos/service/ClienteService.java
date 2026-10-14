package com.loja.pedidos.service;

import com.loja.pedidos.domain.Cliente;
import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    public Cliente obterCliente(String cpf) {
        return clienteRepository.findByCpf(cpf)
            .orElseThrow(() -> new PedidoException("CLIENTE_NAO_ENCONTRADO", "Client not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void criarOuAtualizarCliente(String cpf, String email, String nivelClube) {
        Optional<Cliente> clienteExistente = clienteRepository.findByCpf(cpf);

        if (clienteExistente.isPresent()) {
            Cliente cliente = clienteExistente.get();
            cliente.setEmail(email);
            if (nivelClube != null) {
                cliente.setNivelClube(nivelClube);
            }
            clienteRepository.save(cliente);
        } else {
            Cliente novoCliente = new Cliente(cpf, email);
            if (nivelClube != null) {
                novoCliente.setNivelClube(nivelClube);
            }
            clienteRepository.save(novoCliente);
        }
    }
}
