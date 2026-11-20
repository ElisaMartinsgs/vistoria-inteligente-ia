package br.com.saborecia.vistoria.persistencia.repositorio;

import br.com.saborecia.vistoria.persistencia.entidade.NormaRegulamentadora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface NormaRepository extends JpaRepository<NormaRegulamentadora, String> {

    @Query("select n.codigo from NormaRegulamentadora n where n.ativa = true")
    Set<String> codigosAtivos();
}
