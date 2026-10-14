package com.loja.pedidos.service;

import com.loja.pedidos.domain.Aviso;
import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.repository.AvisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AvisoService {
    @Autowired
    private AvisoRepository avisoRepository;

    public List<Aviso> obterAvisosCliente(String cpf) {
        return avisoRepository.findByCpfClienteOrderByData(cpf);
    }

    public Aviso obterAviso(Long id) {
        return avisoRepository.findById(id)
            .orElseThrow(() -> new PedidoException("AVISO_NAO_ENCONTRADO", "Notice not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Aviso marcarComoLido(Long id) {
        Aviso aviso = obterAviso(id);
        aviso.setSituacao("LIDO");
        return avisoRepository.save(aviso);
    }
}
