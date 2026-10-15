package com.loja.pedidos.controller;

import com.loja.pedidos.domain.Aviso;
import com.loja.pedidos.domain.Cliente;
import com.loja.pedidos.dto.AvisoDTO;
import com.loja.pedidos.dto.ClienteSaldoDTO;
import com.loja.pedidos.service.AvisoService;
import com.loja.pedidos.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
    @Autowired
    private ClienteService clienteService;

    @Autowired
    private AvisoService avisoService;

    @GetMapping("/{cpf}")
    public ResponseEntity<ClienteSaldoDTO> obterCliente(@PathVariable String cpf) {
        Cliente cliente = clienteService.obterCliente(cpf);
        ClienteSaldoDTO dto = new ClienteSaldoDTO(cliente.getCpf(), cliente.getSaldoCredito());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{cpf}/avisos")
    public ResponseEntity<List<AvisoDTO>> obterAvisos(@PathVariable String cpf) {
        List<Aviso> avisos = avisoService.obterAvisosCliente(cpf);
        List<AvisoDTO> dtos = avisos.stream()
            .map(aviso -> new AvisoDTO(aviso.getId(), aviso.getTipo(), aviso.getPedidoId(), aviso.getSituacao()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
