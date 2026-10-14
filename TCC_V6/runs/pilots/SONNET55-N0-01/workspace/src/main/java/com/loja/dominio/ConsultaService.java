package com.loja.dominio;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private final Armazem armazem;

    public ConsultaService(Armazem armazem) {
        this.armazem = armazem;
    }

    public Produto produto(String sku) {
        return armazem.ler(estado -> {
            Produto produto = estado.produtos.get(sku);
            if (produto == null) {
                throw ErroNegocio.naoEncontrado("PRODUTO_NAO_ENCONTRADO");
            }
            return new Produto(produto.sku(), produto.nome(), produto.preco(), produto.pesoKg(),
                    produto.estoque(), produto.foraDeLinha());
        });
    }

    public Cliente cliente(String cpf) {
        return armazem.ler(estado -> copia(clienteExistente(estado, cpf)));
    }

    public List<Aviso> avisos(String cpf) {
        return armazem.ler(estado -> {
            clienteExistente(estado, cpf);
            return estado.avisosDoCliente(cpf).stream().map(ConsultaService::copia).toList();
        });
    }

    public Aviso marcarLido(String id) {
        return armazem.escrever((estado, mudancas) -> {
            Aviso aviso = estado.avisos.get(id);
            if (aviso == null) {
                throw ErroNegocio.naoEncontrado("AVISO_NAO_ENCONTRADO");
            }
            if (!aviso.lido()) {
                aviso.marcarLido();
                mudancas.avisos.add(aviso);
            }
            return copia(aviso);
        });
    }

    private static Cliente clienteExistente(Estado estado, String cpf) {
        Cliente cliente = estado.clientes.get(cpf);
        if (cliente == null) {
            throw ErroNegocio.naoEncontrado("CLIENTE_NAO_ENCONTRADO");
        }
        return cliente;
    }

    private static Cliente copia(Cliente c) {
        return new Cliente(c.cpf(), c.email(), c.saldoCredito());
    }

    private static Aviso copia(Aviso a) {
        return new Aviso(a.id(), a.seq(), a.cpf(), a.tipo(), a.pedidoId(), a.lido());
    }
}
