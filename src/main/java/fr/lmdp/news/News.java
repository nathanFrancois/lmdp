package fr.lmdp.news;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Base64;

@Entity
@Table(name = "news")
public class News {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 5000)
    private String description;
    @Column(nullable = false)
    private boolean published;
    private LocalDateTime publishedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private byte[] imageData;
    @Column(length = 100)
    private String imageContentType;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) {
        if (published && !this.published) publishedAt = LocalDateTime.now();
        this.published = published;
    }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isHasImage() { return imageData != null && imageData.length > 0; }
    public void setImage(byte[] data, String contentType) { imageData = data; imageContentType = contentType; }
    public String getImageUrl() {
        return isHasImage() ? "data:" + imageContentType + ";base64," + Base64.getEncoder().encodeToString(imageData) : null;
    }
}
