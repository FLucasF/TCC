package br.com.loja.pedidos.cliente;

import br.com.loja.pedidos.aviso.AvisoRepositorio;
import br.com.loja.pedidos.aviso.AvisoResposta;
import br.com.loja.pedidos.comum.ErroNegocio;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes/{cpf}")
public class ClienteController {

    private final ClienteRepositorio clientes;
    private final AvisoRepositorio avisos;

    public ClienteController(ClienteRepositorio clientes, AvisoRepositorio avisos) {
        this.clientes = clientes;
        this.avisos = avisos;
    }

    @GetMapping
    public SaldoResposta saldo(@PathVariable String cpf) {
        BigDecimal saldo = clientes.saldo(cpf).orElseThrow(ClienteController::naoEncontrado);
        return new SaldoResposta(cpf, saldo);
    }

    @GetMapping("/avisos")
    public List<AvisoResposta> avisos(@PathVariable String cpf) {
        if (!clientes.existe(cpf)) {
            throw naoEncontrado();
        }
        return avisos.doCliente(cpf).stream().map(AvisoResposta::de).toList();
    }

    private static ErroNegocio naoEncontrado() {
        return ErroNegocio.naoEncontrado("CLIENTE_NAO_ENCONTRADO");
    }

    public record SaldoResposta(String cpf, BigDecimal saldoCredito) {
    }
}
