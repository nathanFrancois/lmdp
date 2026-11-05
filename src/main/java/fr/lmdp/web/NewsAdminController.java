package fr.lmdp.web;

import fr.lmdp.news.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/actualites")
public class NewsAdminController {
    private final NewsService service;
    public NewsAdminController(NewsService service) { this.service = service; }
    @GetMapping
    public String list(Model model) {
        model.addAttribute("news", service.all());
        return "admin/actualites";
    }
    @GetMapping("/nouvelle")
    public String createForm(Model model) {
        model.addAttribute("newsForm", new NewsForm());
        return "admin/actualite-form";
    }
    @GetMapping("/{id}/modifier")
    public String editForm(@PathVariable Long id, Model model) {
        News news = service.get(id);
        NewsForm form = new NewsForm();
        form.setTitle(news.getTitle());
        form.setDescription(news.getDescription());
        form.setPublished(news.isPublished());
        model.addAttribute("newsForm", form);
        return formView(id, model);
    }
    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) Long id,
                       @Valid @ModelAttribute("newsForm") NewsForm form, BindingResult binding,
                       @RequestParam(value = "image", required = false) MultipartFile image,
                       Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return formView(id, model);
        try { service.save(id, form, image); }
        catch (IllegalArgumentException error) {
            binding.reject("image", error.getMessage());
            return formView(id, model);
        }
        redirect.addFlashAttribute("message", form.isPublished() ? "Actualité publiée sur le site." : "Actualité enregistrée en brouillon.");
        return "redirect:/admin/actualites";
    }
    private String formView(Long id, Model model) {
        model.addAttribute("newsId", id);
        if (id != null) model.addAttribute("imageUrl", service.get(id).getImageUrl());
        return "admin/actualite-form";
    }
}
