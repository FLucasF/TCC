package loja.infra;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import loja.dominio.Produto;

@Component
public class CatalogoInicial implements ApplicationRunner {

    private final ProdutoRepository produtos;

    public CatalogoInicial(ProdutoRepository produtos) {
        this.produtos = produtos;
    }

    @Override
    public void run(ApplicationArguments args) {
        produtos.semear(List.of(
                ProdutoRepository.produto("CAM-001", "Camiseta básica", "79.90", "0.30", 50, false),
                ProdutoRepository.produto("TEN-001", "Tênis corrida", "249.90", "1.20", 10, false),
                ProdutoRepository.produto("FON-001", "Fone sem fio", "199.90", "0.25", 5, false),
                ProdutoRepository.produto("MEI-001", "Meia esportiva", "19.90", "0.10", 100, false),
                ProdutoRepository.produto("JAQ-001", "Jaqueta corta-vento", "389.90", "0.80", 3, false),
                ProdutoRepository.produto("MOC-001", "Mochila urbana", "159.90", "0.90", 1, false),
                ProdutoRepository.produto("BON-001", "Boné aba reta", "59.90", "0.15", 0, false),
                ProdutoRepository.produto("REL-001", "Relógio digital", "129.90", "0.20", 8, true)));
    }
}
