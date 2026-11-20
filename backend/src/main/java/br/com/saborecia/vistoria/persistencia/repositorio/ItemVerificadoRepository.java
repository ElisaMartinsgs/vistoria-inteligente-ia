package br.com.saborecia.vistoria.persistencia.repositorio;

import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.persistencia.entidade.ItemVerificado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemVerificadoRepository extends JpaRepository<ItemVerificado, Long> {

    @EntityGraph(attributePaths = {"vistoria", "vistoria.unidade", "motivos"})
    Optional<ItemVerificado> findDetalhadoById(Long id);

    @EntityGraph(attributePaths = {"vistoria", "vistoria.unidade"})
    List<ItemVerificado> findByStatusOrderByIdAsc(StatusItem status);

    /** [status, quantidade] */
    @Query("select i.status, count(i) from ItemVerificado i group by i.status")
    List<Object[]> contarPorStatus();

    /** [unidade, status, quantidade] */
    @Query("select u.nome, i.status, count(i) from ItemVerificado i join i.vistoria v join v.unidade u group by u.nome, i.status")
    List<Object[]> contarPorUnidadeEStatus();

    /** [codigoRegra, quantidade] dos motivos que reprovaram itens */
     @Query("select m.codigoRegra, count(distinct m.item) from MotivoValidacao m where m.severidade = 'REPROVA' group by m.codigoRegra order by count(distinct m.item) desc")
    List<Object[]> principaisMotivosDeReprovacao();
}
