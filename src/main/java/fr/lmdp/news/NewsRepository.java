package fr.lmdp.news;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByPublishedTrueOrderByPublishedAtDescIdDesc();
    List<News> findAllByOrderByCreatedAtDescIdDesc();
}
