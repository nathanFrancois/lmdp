package fr.lmdp.web;

import fr.lmdp.news.NewsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NewsController {
    private final NewsService service;
    public NewsController(NewsService service) { this.service = service; }
    @GetMapping("/actualites")
    public String news(Model model) {
        model.addAttribute("activePage", "actualites");
        model.addAttribute("news", service.published());
        return "actualites";
    }
}
