package searchengine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.JsoupConfig;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.DBRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

public class IndexingOneSite extends RecursiveTask {

    private final SiteEntity siteEntity;
    private final DBRepository dbRepository;
    private final JsoupConfig jsoupConfig;
    private final List<String> hrefList;
    private final List<IndexingOneSite> taskList = new ArrayList<>();



    public IndexingOneSite(SiteEntity siteEntity, String link, DBRepository dbRepository, JsoupConfig jsoupConfig) {
        this.dbRepository = dbRepository;
        this.jsoupConfig = jsoupConfig;
        this.hrefList = new ArrayList<>();
        this.siteEntity = siteEntity;

        findNext(siteEntity.getUrl(), link);
    }

    private void findNext(String prefix, String url) {
        Document doc = getDocument(prefix, url);
        if (doc == null) {
            return;
        }

        Elements lines = doc.select("a[href]");
        for (Element line : lines) {
            if (isCancelled()) {
                break;
            }
            String href = line.attr("href");
            if (href.indexOf(url) == 0 && !hasHref(href) && !href.contains("#")) {
                hrefList.add(href);
                savePage(href);
            }
        }
    }

    private Document getDocument(String prefix, String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(prefix + url)
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReferrer())
                    .get();
            PageEntity currentPageEntity = dbRepository.getPageRepository().findPageBySiteIdAndPath(siteEntity.getId(), url);
            if (currentPageEntity != null) {
                int statusCode = doc.connection().response().statusCode();
                currentPageEntity.setCode(statusCode);
                currentPageEntity.setContent(doc.html());
                dbRepository.getPageRepository().save(currentPageEntity);
                if (statusCode < 400) {
                    saveLemmaAndIndex(currentPageEntity, doc.html());
                }
            }
        } catch (Exception e) {
            PageEntity currentPageEntity = dbRepository.getPageRepository().findPageBySiteIdAndPath(siteEntity.getId(), url);
            if (currentPageEntity != null) {
                if (doc != null) {
                    currentPageEntity.setCode(doc.connection().response().statusCode());
                } else {
                    currentPageEntity.setCode(400);
                }
                currentPageEntity.setContent(e.toString());
                dbRepository.getPageRepository().save(currentPageEntity);
            } else {
                currentPageEntity = new PageEntity();
                currentPageEntity.setCode(400);
                currentPageEntity.setPath(url);
                currentPageEntity.setSiteEntity(siteEntity);
                currentPageEntity.setContent(e.toString());
                dbRepository.getPageRepository().save(currentPageEntity);
            }
            e.printStackTrace();
            return null;
        }
        return doc;
    }

    private synchronized void saveLemmaAndIndex(PageEntity pageEntity, String html) throws IOException {
        Map<String, Integer> lemmasMap = LemmaFinder.getInstance().collectLemmas(html);

        for (Map.Entry<String, Integer> lemma : lemmasMap.entrySet()) {
            LemmaEntity lemmaEntity = dbRepository.getLemmaRepository().findLemmaBySiteIdAndLemma(siteEntity.getId(), lemma.getKey());
            if (lemmaEntity == null) {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSiteEntity(siteEntity);
                lemmaEntity.setFrequency(lemma.getValue());
                lemmaEntity.setLemma(lemma.getKey());
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + lemma.getValue());
            }
            dbRepository.getLemmaRepository().save(lemmaEntity);

            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setLemmaEntity(lemmaEntity);
            indexEntity.setPageEntity(pageEntity);
            indexEntity.setRank(lemma.getValue().doubleValue());
            dbRepository.getIndexRepository().save(indexEntity);
        }
    }

    private void savePage(String href) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setPath(href);
        dbRepository.getPageRepository().save(pageEntity);
    }

    private boolean hasHref(String href) {
        return dbRepository.getPageRepository().findPageBySiteIdAndPath(siteEntity.getId(), href) != null;
    }

    @Override
    protected Object compute() {
        for (String s : hrefList) {
            if (isCancelled()) {
                break;
            }
            IndexingOneSite task = new IndexingOneSite(siteEntity, s, dbRepository, jsoupConfig);
            task.fork();
            taskList.add(task);
        }

        for (IndexingOneSite t : taskList) {
            if (isCancelled()) {
                break;
            }
            t.join();
        }
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        taskList.forEach((t) -> t.cancel(true));
        return true;
    }
}
