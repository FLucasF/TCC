package com.loja.pedidos.controller;

import com.loja.pedidos.domain.Aviso;
import com.loja.pedidos.dto.AvisoDTO;
import com.loja.pedidos.service.AvisoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/avisos")
public class AvisoController {
    @Autowired
    private AvisoService avisoService;

    @PostMapping("/{id}/leitura")
    public ResponseEntity<AvisoDTO> marcarComoLido(@PathVariable Long id) {
        Aviso aviso = avisoService.marcarComoLido(id);
        AvisoDTO dto = new AvisoDTO(aviso.getId(), aviso.getTipo(), aviso.getPedidoId(), aviso.getSituacao());
        return ResponseEntity.ok(dto);
    }
}
