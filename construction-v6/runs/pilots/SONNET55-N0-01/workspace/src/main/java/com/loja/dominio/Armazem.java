package com.loja.dominio;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Guarda o estado em memória e o espelha no banco. Toda operação roda sozinha (uma por vez),
 * o que garante que duas vendas da última peça nunca passem juntas e o estoque nunca fique negativo.
 */
@Component
public class Armazem {

    private static final Logger log = LoggerFactory.getLogger(Armazem.class);

    private final ReentrantLock trava = new ReentrantLock();
    private final Persistencia persistencia;
    private Estado estado;

    public Armazem(Persistencia persistencia) {
        this.persistencia = persistencia;
        this.estado = persistencia.carregar();
    }

    public <T> T ler(Function<Estado, T> leitura) {
        trava.lock();
        try {
            return leitura.apply(estado);
        } finally {
            trava.unlock();
        }
    }

    /**
     * Executa uma alteração e grava o que mudou numa transação. Se a operação recusar o pedido
     * depois de já ter alterado algo (ex.: cancelamento por pagamento expirado), o que mudou é gravado
     * mesmo assim e o erro segue em frente.
     */
    public <T> T escrever(BiFunction<Estado, Mudancas, T> alteracao) {
        trava.lock();
        try {
            Mudancas mudancas = new Mudancas();
            T resultado;
            try {
                resultado = alteracao.apply(estado, mudancas);
            } catch (ErroNegocio e) {
                gravarOuRecarregar(mudancas);
                throw e;
            } catch (RuntimeException e) {
                recarregar();
                throw e;
            }
            gravarOuRecarregar(mudancas);
            return resultado;
        } finally {
            trava.unlock();
        }
    }

    private void gravarOuRecarregar(Mudancas mudancas) {
        if (mudancas.vazia()) {
            return;
        }
        try {
            persistencia.gravar(mudancas);
        } catch (RuntimeException e) {
            recarregar();
            throw e;
        }
    }

    private void recarregar() {
        try {
            estado = persistencia.carregar();
        } catch (RuntimeException e) {
            log.error("Não foi possível recarregar o estado do banco", e);
        }
    }
}
