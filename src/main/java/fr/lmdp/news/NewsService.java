package fr.lmdp.news;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
public class NewsService {
    private final NewsRepository repository;
    public NewsService(NewsRepository repository) { this.repository = repository; }
    public List<News> published() { return repository.findByPublishedTrueOrderByPublishedAtDescIdDesc(); }
    public List<News> all() { return repository.findAllByOrderByCreatedAtDescIdDesc(); }
    public News get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Actualité introuvable"));
    }
    @Transactional
    public News save(Long id, NewsForm form, MultipartFile image) {
        News news = id == null ? new News() : get(id);
        if (image != null && !image.isEmpty()) {
            if (image.getSize() > 3L * 1024 * 1024) throw new IllegalArgumentException("L’image ne doit pas dépasser 3 Mo.");
            String extension = StringUtils.getFilenameExtension(image.getOriginalFilename());
            String type = switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                default -> throw new IllegalArgumentException("Formats acceptés : JPG, PNG, GIF, WEBP.");
            };
            try { news.setImage(image.getBytes(), type); }
            catch (IOException error) { throw new IllegalArgumentException("Impossible de lire l’image.", error); }
        }
        if (form.isPublished() && !news.isHasImage()) {
            throw new IllegalArgumentException("Ajoutez une image avant de publier cette actualité.");
        }
        news.setTitle(form.getTitle());
        news.setDescription(form.getDescription());
        news.setPublished(form.isPublished());
        return repository.save(news);
    }
}
