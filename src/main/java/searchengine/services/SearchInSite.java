package searchengine.services;

import searchengine.dto.search.SearchResponse;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.DBRepository;

import java.util.*;

public class SearchInSite {

    private final DBRepository dbRepository;

    public SearchInSite(DBRepository dbRepository) {
        this.dbRepository = dbRepository;
    }

    public List<PageEntity> getPages(String siteUrl, Set<String> lemmasQuery) {
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        for (String l : lemmasQuery) {
            List<LemmaEntity> lemmas = dbRepository.getLemmaRepository().findAllLemmaBySiteUrlAndLemma(siteUrl, l);
            if (lemmas.size() == 0) {
                return Collections.emptyList();
            }
            lemmaEntityList.addAll(lemmas);
        }
        lemmaEntityList.sort(Comparator.comparing(LemmaEntity::getFrequency));

        List<PageEntity> pageEntityList = new ArrayList<>(dbRepository.getPageRepository().findAllByLemmaId(lemmaEntityList.get(0).getId()));
        for (int i = 1; i < lemmaEntityList.size(); i++) {
            List<PageEntity> findPages = dbRepository.getPageRepository().findAllByLemmaId(lemmaEntityList.get(i).getId());
            pageEntityList.retainAll(findPages);
        }
        for (PageEntity page : pageEntityList) {
            Double rAbs = 0D;
            for (LemmaEntity l : lemmaEntityList) {
                rAbs += dbRepository.getIndexRepository().getSumRankByPageIdAndLemaId(page.getId(), l.getId());
            }
            page.setRAbs(rAbs);
        }
        return  pageEntityList;
    }
}
