package searchengine.repositories;

import lombok.Getter;

@Getter
public class DBRepository {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public DBRepository(IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository) {
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }
}
