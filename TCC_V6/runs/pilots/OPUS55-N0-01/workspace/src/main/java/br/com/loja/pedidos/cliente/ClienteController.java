package br.com.loja.pedidos.cliente;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.pedidos.aviso.Aviso;
import br.com.loja.pedidos.aviso.AvisoRepositorio;
import br.com.loja.pedidos.comum.ErroNegocio;

@RestController
public class ClienteController {

    public record SaldoCliente(String cpf, BigDecimal saldoCredito) {
    }

    private final ClienteRepositorio clientes;
    private final AvisoRepositorio avisos;

    public ClienteController(ClienteRepositorio clientes, AvisoRepositorio avisos) {
        this.clientes = clientes;
        this.avisos = avisos;
    }

    @GetMapping("/clientes/{cpf}")
    public SaldoCliente consultar(@PathVariable String cpf) {
        return new SaldoCliente(cpf, clientes.saldo(cpf).orElseThrow(ClienteController::naoEncontrado));
    }

    @GetMapping("/clientes/{cpf}/avisos")
    public List<Aviso> avisos(@PathVariable String cpf) {
        clientes.saldo(cpf).orElseThrow(ClienteController::naoEncontrado);
        return avisos.doCliente(cpf);
    }

    private static ErroNegocio naoEncontrado() {
        return ErroNegocio.naoEncontrado("CLIENTE_NAO_ENCONTRADO");
    }
}
