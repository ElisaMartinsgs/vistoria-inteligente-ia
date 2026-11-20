package br.com.saborecia.vistoria.servico;

import br.com.saborecia.vistoria.config.VistoriaProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Guarda a evidência original em disco (pasta configurável / volume Docker).
 * O banco guarda só o caminho. Em produção, poderia ser trocado por S3/Azure Blob.
 */
@Service
public class ArmazenamentoService {

    private final Path raiz;

    public ArmazenamentoService(VistoriaProperties props) {
        this.raiz = Path.of(props.armazenamento().diretorio()).toAbsolutePath().normalize();
    }

    public String salvar(byte[] conteudo, String extensao) {
        try {
            Path pasta = raiz.resolve(LocalDate.now().toString());
            Files.createDirectories(pasta);
            Path arquivo = pasta.resolve(UUID.randomUUID() + "." + extensao);
            Files.write(arquivo, conteudo);
            return raiz.relativize(arquivo).toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar a evidência", e);
        }
    }

    public Resource abrir(String caminhoRelativo) {
        Path p = raiz.resolve(caminhoRelativo).normalize();
        if (!p.startsWith(raiz)) throw new IllegalArgumentException("Caminho inválido");
        return new FileSystemResource(p);
    }
}
