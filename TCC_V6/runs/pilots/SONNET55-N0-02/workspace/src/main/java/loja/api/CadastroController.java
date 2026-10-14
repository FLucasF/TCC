package loja.api;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import loja.dominio.Aviso;
import loja.dominio.NegocioException;
import loja.dominio.Produto;
import loja.infra.AvisoRepository;
import loja.infra.ClienteRepository;
import loja.infra.ProdutoRepository;

@RestController
public class CadastroController {

    private final ProdutoRepository produtos;
    private final ClienteRepository clientes;
    private final AvisoRepository avisos;

    public CadastroController(ProdutoRepository produtos, ClienteRepository clientes, AvisoRepository avisos) {
        this.produtos = produtos;
        this.clientes = clientes;
        this.avisos = avisos;
    }

    record Saldo(String cpf, BigDecimal saldoCredito) {
    }

    @GetMapping("/produtos/{sku}")
    public Produto produto(@PathVariable String sku) {
        return produtos.buscar(sku).orElseThrow(() -> NegocioException.naoEncontrado("PRODUTO_NAO_ENCONTRADO"));
    }

    @GetMapping("/clientes/{cpf}")
    public Saldo cliente(@PathVariable String cpf) {
        return new Saldo(cpf, clientes.saldo(cpf)
                .orElseThrow(() -> NegocioException.naoEncontrado("CLIENTE_NAO_ENCONTRADO")));
    }

    @GetMapping("/clientes/{cpf}/avisos")
    public List<Aviso> avisos(@PathVariable String cpf) {
        clientes.saldo(cpf).orElseThrow(() -> NegocioException.naoEncontrado("CLIENTE_NAO_ENCONTRADO"));
        return avisos.listar(cpf);
    }

    @PostMapping("/avisos/{id}/leitura")
    public Aviso lerAviso(@PathVariable String id) {
        long numero;
        try {
            numero = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw NegocioException.naoEncontrado("AVISO_NAO_ENCONTRADO");
        }
        avisos.buscar(numero).orElseThrow(() -> NegocioException.naoEncontrado("AVISO_NAO_ENCONTRADO"));
        avisos.marcarLido(numero);
        return avisos.buscar(numero).orElseThrow();
    }
}
