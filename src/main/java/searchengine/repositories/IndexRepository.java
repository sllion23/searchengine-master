package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    @Query("SELECT i FROM index_ i WHERE i.lemmaEntity.id = :lemma_id and i.pageEntity.id = :page_id")
    IndexEntity findIndexByLemmaIdAndPageId(
            @Param("lemma_id") long lemmaId,
            @Param("page_id") long pageId);

    @Query("SELECT sum(i.rank) FROM index_ i WHERE i.pageEntity.id = :page_id and i.lemmaEntity.id = :lemma_id")
    Double getSumRankByPageIdAndLemaId(
            @Param("page_id") long pageId,
            @Param("lemma_id") long lemmaId);
}
