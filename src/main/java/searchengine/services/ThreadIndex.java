package searchengine.services;

import searchengine.config.JsoupConfig;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.DBRepository;

import java.time.LocalDateTime;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;

public class ThreadIndex extends Thread {
    private final static String DEFAULT_LINK = "/";

    private final String url;
    private final DBRepository dbRepository;
    private final JsoupConfig jsoupConfig;
    private final String name;
    private ForkJoinPool forkJoinPool;
    private Thread timer;
    private SiteEntity siteEntity;
    private IndexingOneSite indexingOneSite;
    private final String link;

    public ThreadIndex(String name, String url, DBRepository dbRepository, JsoupConfig jsoupConfig) {
        this(name, url, DEFAULT_LINK, dbRepository, jsoupConfig);
    }

    public ThreadIndex(String name, String url, String link, DBRepository dbRepository, JsoupConfig jsoupConfig) {
        this.url = url;
        this.dbRepository = dbRepository;
        this.jsoupConfig = jsoupConfig;
        forkJoinPool = new ForkJoinPool();
        this.link = link;
        this.name = name;
    }

    @Override
    public synchronized void start() {
        createNewSite(name);
        startTimer(siteEntity);
        super.start();
    }

    private void createNewSite(String name) {
        siteEntity = dbRepository.getSiteRepository().findByUrl(url);
        if (!link.equals(DEFAULT_LINK)) {
            if (siteEntity == null) {
                createSiteEntity(name);
            } else {
                PageEntity pageEntity = dbRepository.getPageRepository().findPageBySiteIdAndPath(siteEntity.getId(), link);
                if (pageEntity != null) {
                    dbRepository.getPageRepository().delete(pageEntity);
                }
                siteEntity.setStatus(Status.INDEXING);
                dbRepository.getSiteRepository().save(siteEntity);
            }
            return;
        }
        if (siteEntity != null) {
            dbRepository.getSiteRepository().delete(siteEntity);
        }
        createSiteEntity(name);
    }

    private void createSiteEntity(String name) {
        siteEntity = new SiteEntity();
        siteEntity.setName(name);
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setUrl(url);
        dbRepository.getSiteRepository().save(siteEntity);
    }

    @Override
    public void run() {
        try {
            indexingOneSite = new IndexingOneSite(siteEntity, link, dbRepository, jsoupConfig);
            forkJoinPool.invoke(indexingOneSite);

            if (!isInterrupted()) {
                siteEntity.setStatusTime(LocalDateTime.now());
                siteEntity.setStatus(Status.INDEXED);
                dbRepository.getSiteRepository().save(siteEntity);
            }
        } catch (CancellationException e) {

        } catch (Exception e) {
            siteEntity.setStatus(Status.FAILED);
            siteEntity.setLastError(e.toString());
            dbRepository.getSiteRepository().save(siteEntity);
        } finally {
            timer.interrupt();
        }
    }

    private void startTimer(SiteEntity siteEntity) {
        timer = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                if (isInterrupted()) {
                    break;
                }
                siteEntity.setStatusTime(LocalDateTime.now());
                dbRepository.getSiteRepository().save(siteEntity);
            }
        });
        timer.start();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        timer.interrupt();
        indexingOneSite.cancel(true);
        forkJoinPool.shutdownNow();
        siteEntity.setStatus(Status.FAILED);
        siteEntity.setLastError("Индексация остановлена пользователем");
        dbRepository.getSiteRepository().save(siteEntity);
    }
}

