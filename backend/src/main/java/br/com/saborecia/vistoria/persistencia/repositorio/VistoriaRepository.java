package br.com.saborecia.vistoria.persistencia.repositorio;

import br.com.saborecia.vistoria.persistencia.entidade.Vistoria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {

    @EntityGraph(attributePaths = {"unidade", "itens"})
    Optional<Vistoria> findWithItensById(Long id);

    @EntityGraph(attributePaths = {"unidade", "itens"})
    List<Vistoria> findTop50ByOrderByIdDesc();

    @EntityGraph(attributePaths = {"unidade", "itens"})
    List<Vistoria> findTop50ByUnidadeIdOrderByIdDesc(Long unidadeId);
}
