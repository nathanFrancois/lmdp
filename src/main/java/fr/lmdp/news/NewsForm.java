package fr.lmdp.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NewsForm {
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String title;
    @NotBlank(message = "La description est obligatoire")
    @Size(max = 5000, message = "La description ne doit pas dépasser 5 000 caractères")
    private String description;
    private boolean published;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
