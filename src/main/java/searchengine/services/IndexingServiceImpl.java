package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.repositories.*;
import searchengine.utils.ThreadIndex;

import java.util.ArrayList;
import java.util.List;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final JsoupConfig jsoupConfig;
    private final List<ThreadIndex> threadIndexList = new ArrayList<>();
    private final DBRepository dbRepository;


    public IndexingServiceImpl(IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository, SitesList sitesList, JsoupConfig jsoupConfig) {
        this.sitesList = sitesList;
        this.jsoupConfig = jsoupConfig;
        dbRepository = new DBRepository(indexRepository, lemmaRepository, pageRepository, siteRepository);
    }


    @Override
    public IndexingResponse startIndexing() {
        if (isIndexing()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        threadIndexList.clear();
        sitesList.getSites().forEach(s -> threadIndexList.add(new ThreadIndex(s.getName(), s.getUrl(), dbRepository, jsoupConfig)));
        threadIndexList.forEach(Thread::start);
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexing()) {
            return new IndexingResponse(false, "Индексация не запущена");
        }
        threadIndexList.forEach(Thread::interrupt);
        return new IndexingResponse(true);
    }

    private Boolean isIndexing() {
        Boolean isAliveOne = false;
        for (ThreadIndex ti : threadIndexList) {
            isAliveOne = ti.isAlive() && !ti.isInterrupted();

            if (isAliveOne) {
                break;
            }
        }
        return isAliveOne;
    }

    @Override
    public IndexingResponse indexPage(String url) {
        if (isIndexing()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        Site site = findSiteConfigByUrl(url);
        if (site == null) {
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов указанных в конфигурационном файле");
        }
        threadIndexList.clear();
        threadIndexList.add(new ThreadIndex(site.getName(), site.getUrl(), url.replaceAll(site.getUrl(), ""), dbRepository, jsoupConfig));
        threadIndexList.forEach(Thread::start);
        return new IndexingResponse(true);
    }

    private Site findSiteConfigByUrl(String url) {
        Site site = null;
        for (Site s : sitesList.getSites()) {
            if (url.contains(s.getUrl())) {
                site = s;
                break;
            }
        }
        return site;
    }


}
