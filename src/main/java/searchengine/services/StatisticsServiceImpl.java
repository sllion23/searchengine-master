package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesList sites;

    private final DBRepository dbRepository;

    public StatisticsServiceImpl(SitesList sites, IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository) {
        this.sites = sites;
        this.dbRepository = new DBRepository(indexRepository, lemmaRepository, pageRepository, siteRepository);
    }

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteEntity> siteEntityList = dbRepository.getSiteRepository().findAll();
        for (Site site : sites.getSites()) {
            SiteEntity siteEntity = findSiteEntityByUrl(siteEntityList, site.getUrl());
            if (siteEntity == null) {
                continue;
            }
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());
            int pages = siteEntity.getPageEntityList().size();
            int lemmas = siteEntity.getLemmaEntityList().size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity.getStatus().toString());
            if (siteEntity.getLastError() != null) {
                item.setError(siteEntity.getLastError());
            }
            item.setStatusTime(siteEntity.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private SiteEntity findSiteEntityByUrl(List<SiteEntity> siteEntityList, String url) {
        for (SiteEntity siteEntity : siteEntityList) {
            if (siteEntity.getUrl().equals(url)) {
                return siteEntity;
            }
        }
        return null;
    }
}
