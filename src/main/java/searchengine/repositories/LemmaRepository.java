package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    @Query("SELECT l FROM lemma l WHERE l.siteEntity.id = :site_id and l.lemma = :lemma")
    LemmaEntity findLemmaBySiteIdAndLemma(
            @Param("site_id") long siteId,
            @Param("lemma") String lemma);

    @Query("SELECT l FROM lemma l WHERE l.siteEntity.url = ifnull(:site_url, l.siteEntity.url) and l.lemma = :lemma")
    List<LemmaEntity> findAllLemmaBySiteUrlAndLemma(
            @Param("site_url") String site_url,
            @Param("lemma") String lemma);
}
