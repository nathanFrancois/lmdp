package fr.lmdp.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stocke les photos des créations sur le disque (hors classpath, pour rester
 * modifiable une fois l'application packagée) et les expose via /uploads/**
 * (voir {@code fr.example.cawl.web.WebConfig}).
 */
@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path root;

    public ImageStorageService(@Value("${cawl.uploads-dir:./data/uploads/products}") String uploadsDir) {
        this.root = Paths.get(uploadsDir);
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier d'upload : " + root, e);
        }
    }

    /**
     * Enregistre le fichier envoyé et retourne le nom de fichier généré, ou {@code null}
     * si aucun fichier n'a été fourni.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format d'image non supporté (formats acceptés : jpg, jpeg, png, gif, webp).");
        }
        String filename = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'enregistrer l'image.", e);
        }
        return filename;
    }

    /**
     * Supprime un fichier précédemment stocké (silencieusement, s'il n'existe déjà plus).
     */
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(filename));
        } catch (IOException ignored) {
            // Suppression best-effort : un fichier orphelin n'est pas bloquant.
        }
    }

    private String extractExtension(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0 || dot == cleaned.length() - 1) {
            return "";
        }
        return cleaned.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

