package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    @Query("SELECT p FROM page p WHERE p.siteEntity.id = :site_id and p.path = :path")
    PageEntity findPageBySiteIdAndPath(
            @Param("site_id") long siteId,
            @Param("path") String path);

    @Query("SELECT p " +
            " FROM page p " +
            "      INNER JOIN index_ i ON i.pageEntity.id = p.id " +
            "WHERE i.lemmaEntity.id = :lemma_id")
    List<PageEntity> findAllByLemmaId(@Param("lemma_id") long lemmaId);
}
