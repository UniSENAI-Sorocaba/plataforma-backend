package br.senai.plataformadigital.academicpi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeliverableStorage {
    private final Path root;

    public DeliverableStorage(@Value("${platform.storage.root:target/platform-storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public void write(String storageKey, byte[] content) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    public byte[] read(String storageKey) throws IOException {
        return Files.readAllBytes(resolve(storageKey));
    }

    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Chave de storage inválida.");
        return resolved;
    }
}
