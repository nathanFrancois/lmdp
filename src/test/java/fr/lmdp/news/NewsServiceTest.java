package fr.lmdp.news;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.datasource.url=jdbc:h2:mem:news", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@Import(NewsService.class)
class NewsServiceTest {
    @Autowired NewsService service;
    @Autowired NewsRepository repository;
    @Autowired jakarta.persistence.EntityManager entityManager;
    private NewsForm form(boolean published) {
        var form = new NewsForm(); form.setTitle("Marché de Noël");
        form.setDescription("Retrouvez-moi à Thonon-les-Bains."); form.setPublished(published); return form;
    }
    private MockMultipartFile image() { return new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2}); }

    @Test void draftPublishEditAndUnpublish() {
        var draft = service.save(null, form(false), null);
        assertThat(service.published()).isEmpty();
        var published = service.save(draft.getId(), form(true), image());
        String url = published.getImageUrl();
        repository.flush(); entityManager.clear();
        assertThat(service.published()).extracting(News::getId).containsExactly(draft.getId());
        var edit = form(true); edit.setTitle("Nouveau titre");
        service.save(draft.getId(), edit, null);
        repository.flush(); entityManager.clear();
        assertThat(service.get(draft.getId()).getTitle()).isEqualTo("Nouveau titre");
        assertThat(service.get(draft.getId()).getImageUrl()).isEqualTo(url);
        service.save(draft.getId(), form(false), null);
        assertThat(service.published()).isEmpty();
    }
    @Test void publishedNewsRequiresImage() {
        assertThatThrownBy(() -> service.save(null, form(true), null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsUnsupportedImages() {
        assertThatThrownBy(() -> service.save(null, form(false), new MockMultipartFile("image", "x.svg", "image/svg+xml", new byte[]{1})))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void acceptsThreeMegabytes() {
        assertThat(service.save(null, form(true), new MockMultipartFile("image", "x.png", "image/png", new byte[3*1024*1024])).isHasImage()).isTrue();
    }
    @Test void rejectsOversizedImages() {
        assertThatThrownBy(() -> service.save(null, form(true), new MockMultipartFile("image", "x.png", "image/png", new byte[3*1024*1024+1])))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void newestPublicationsComeFirst() {
        var first = service.save(null, form(true), image());
        var second = service.save(null, form(true), image());
        service.save(null, form(false), null);
        assertThat(service.published()).extracting(News::getId).containsExactly(second.getId(), first.getId());
    }
}
