package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.DetailedSearchResult;
import searchengine.dto.search.SearchQuery;
import searchengine.dto.search.SearchResponse;
import searchengine.model.PageEntity;
import searchengine.model.Status;
import searchengine.repositories.*;
import searchengine.utils.LemmaFinder;
import searchengine.utils.SearchInSite;
import searchengine.utils.SnippetHtml;

import java.io.IOException;
import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {
    private final DBRepository dbRepository;
    private final SitesList sitesList;

    public SearchServiceImpl(IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository, SitesList sitesList) {
        this.sitesList = sitesList;
        dbRepository = new DBRepository(indexRepository, lemmaRepository, pageRepository, siteRepository);
    }


    @Override
    public SearchResponse search(SearchQuery searchQuery) {
        if (searchQuery.getQuery().trim().length() == 0) {
            return new SearchResponse(false, "Задан пустой поисковый запрос");
        }
        LemmaFinder lemmaFinder = getLemmaFinder();
        if (lemmaFinder == null) {
            return new SearchResponse(false, "Не удалсоь создать лемматизатор слов");
        }
        if (!isIndexing(searchQuery)) {
            return new SearchResponse(false, "Сайт не проиндексирован");
        }

        int offset = nvl(searchQuery.getOffset(), 0);
        int limit = nvl(searchQuery.getLimit(), 20);

        List<PageEntity> pageEntityList = getPages(searchQuery, lemmaFinder);
        if (pageEntityList.size() == 0) {
            return new SearchResponse(true, 0);
        }

        Double rAbsMax = pageEntityList.stream().mapToDouble(PageEntity::getRAbs).max().orElseThrow(NoSuchElementException::new);

        SnippetHtml snippetHtml = new SnippetHtml(lemmaFinder);
        List<DetailedSearchResult> detailList = new ArrayList<>();
        for (PageEntity page : pageEntityList) {
            DetailedSearchResult detail = new DetailedSearchResult();
            detail.setRelevance(page.getRAbs() / rAbsMax);
            detail.setSite(page.getSiteEntity().getUrl());
            detail.setSiteName(page.getSiteEntity().getName());
            detail.setTitle(getTitle(page.getContent()));
            detail.setSnippet(snippetHtml.getSnippet(searchQuery.getQuery(), page.getContent()));
            detail.setUri(page.getPath());
            detailList.add(detail);
        }
        detailList.sort(Comparator.comparing(DetailedSearchResult::getRelevance).reversed());

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        searchResponse.setCount(pageEntityList.size());
        searchResponse.setData(detailList.subList(offset, Math.min(offset + limit, detailList.size())));
        return searchResponse;
    }

    private boolean isIndexing(SearchQuery searchQuery) {
        if (searchQuery.getSite() != null) {
            return dbRepository.getSiteRepository().findByUrl(searchQuery.getSite()).getStatus() == Status.INDEXED;
        }
        for (Site s : sitesList.getSites()) {
            if (dbRepository.getSiteRepository().findByUrl(s.getUrl()).getStatus() != Status.INDEXED) {
                return false;
            }
        }
        return true;
    }

    private List<PageEntity> getPages(SearchQuery searchQuery, LemmaFinder lemmaFinder) {
        List<PageEntity> pageEntityList = new ArrayList<>();
        Set<String> lemmasQuery = lemmaFinder.getLemmaSet(searchQuery.getQuery());
        SearchInSite searchInSite = new SearchInSite(dbRepository);
        if (searchQuery.getSite() == null) {
            for (Site s : sitesList.getSites()) {
                pageEntityList.addAll(searchInSite.getPages(s.getUrl(), lemmasQuery));
            }

        } else {
            pageEntityList.addAll(searchInSite.getPages(searchQuery.getSite(), lemmasQuery));
        }
        return pageEntityList;
    }

    private LemmaFinder getLemmaFinder() {
        LemmaFinder lemmaFinder;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return lemmaFinder;
    }

    private String getTitle(String html) {
        Document doc;
        try {
            doc = Jsoup.parse(html);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return doc.title();
    }

    private <T> T nvl(T expr1, T expr2) {
        return (expr1 != null) ? expr1 : expr2;
    }

}
