package br.com.saborecia.vistoria.persistencia.repositorio;

import br.com.saborecia.vistoria.persistencia.entidade.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
    List<Unidade> findByAtivaTrueOrderByNomeAsc();
}
