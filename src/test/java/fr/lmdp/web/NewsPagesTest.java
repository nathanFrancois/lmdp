package fr.lmdp.web;

import fr.lmdp.news.*;
import fr.lmdp.catalog.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {NewsController.class, NewsAdminController.class, PageController.class},
        properties = {"spring.thymeleaf.cache=false", "server.address=127.0.0.1", "server.port=0"})
@AutoConfigureMockMvc(addFilters = false)
class NewsPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean NewsService service;
    @MockitoBean ProductService products;

    @Test void publicPageRendersNewsAndInstagramWithoutHtmlInjection() throws Exception {
        var news = new News(); news.setTitle("Marché <script>alert(1)</script>");
        news.setDescription("À Thonon-les-Bains"); news.setPublished(true);
        when(service.published()).thenReturn(List.of(news));
        mvc.perform(get("/actualites")).andExpect(status().isOk())
                .andExpect(content().string(containsString("https://www.instagram.com/lesmerveillesdephilomen/")))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }
    @Test void emptyPageStillOffersInstagram() throws Exception {
        when(service.published()).thenReturn(List.of());
        mvc.perform(get("/actualites")).andExpect(status().isOk())
                .andExpect(content().string(containsString("De nouvelles actualités arrivent bientôt")));
    }
    @Test void oldAtelierUrlRedirects() throws Exception {
        mvc.perform(get("/atelier")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/actualites"));
    }
    @Test void newFormRendersAndInvalidInputIsRedisplayed() throws Exception {
        mvc.perform(get("/admin/actualites/nouvelle")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Publier sur le site")));
        mvc.perform(multipart("/admin/actualites").param("title", "").param("description", ""))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("newsForm", "title", "description"));
        verify(service, never()).save(any(), any(), any());
    }
    @Test void editingWithoutNewImagePreservesPublicationChoice() throws Exception {
        mvc.perform(multipart("/admin/actualites/1").param("title", "Marché").param("description", "À Thonon")
                .param("published", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/actualites"));
        verify(service).save(eq(1L), argThat(NewsForm::isPublished), isNull());
    }
}
