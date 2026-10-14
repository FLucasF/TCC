package com.loja.pedidos.service;

import com.loja.pedidos.dto.RespostaAviso;
import com.loja.pedidos.entity.Aviso;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.repository.AvisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvisoService {
    private final AvisoRepository avisoRepository;

    public List<RespostaAviso> obterAvisos(String cpf) {
        return avisoRepository.findByCpfClienteOrderByIdAsc(cpf)
            .stream()
            .map(this::mapearAviso)
            .collect(Collectors.toList());
    }

    @Transactional
    public RespostaAviso marcarComoLido(Long id) {
        var aviso = avisoRepository.findById(id)
            .orElseThrow(ErroNegocio::avisoNaoEncontrado);

        aviso.setSituacao(Aviso.SituacaoAviso.LIDO);
        avisoRepository.save(aviso);

        return mapearAviso(aviso);
    }

    private RespostaAviso mapearAviso(Aviso aviso) {
        return RespostaAviso.builder()
            .id(aviso.getId())
            .tipo(aviso.getTipo().name())
            .pedidoId(aviso.getPedidoId())
            .situacao(aviso.getSituacao().name())
            .build();
    }
}
