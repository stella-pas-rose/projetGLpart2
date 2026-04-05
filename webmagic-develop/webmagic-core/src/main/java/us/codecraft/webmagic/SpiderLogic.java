package us.codecraft.webmagic;

import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.thread.CountableThreadPool;
import java.util.Date;
import org.slf4j.Logger;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.LoggerFactory;

public class SpiderLogic {
    private long emptySleepTime = 30000;
    private Spider spider; 
    protected Logger logger = LoggerFactory.getLogger(getClass());

    public SpiderLogic(Spider spider){
        this.spider= spider; 
    }


    public void run() {
        spider.status.checkRunningStat();
        initComponent();
        logger.info("Spider {} started!", spider.getUUID());
        
        // interrupt won't be necessarily detected
        while (!Thread.currentThread().isInterrupted() && spider.status.stat.get() == SpiderStatus.STAT_RUNNING) {
            
            Request poll = updatePoll(); 
            
            if (poll == null) {
                if (spider.exitWhenComplete) {
                    break;
                }
                continue;         
            }

            runBis(poll);
        }
        spider.status.stat.set(SpiderStatus.STAT_STOPPED);
        // release some resources
        if (spider.destroyWhenExit) {
            spider.close();
        }
        logger.info("Spider {} closed! {} pages downloaded.", spider.getUUID(), spider.pageCount.get());
    }

    private Request updatePoll(){
        Request poll = spider.getScheduler().poll(this.spider);

        if (poll == null){
            if (spider.threadPool.getThreadAlive() != 0){
                spider.scheduler.waitNewUrl(spider.threadPool, emptySleepTime); 
                return null; 
            }
            else if ( !spider.exitWhenComplete){
                waitP();
            }
            poll = this.spider.getScheduler().poll(this.spider);
        }
        return poll;
    }

    private void waitP(){
        try {
            Thread.sleep(emptySleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runBis(Request request){
        spider.threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        processRequest(request);
                        spider.onSuccess(request);
                    } catch (Exception e) {
                        spider.onError(request, e);
                        logger.error("process request " + request + " error", e);
                    } finally {
                        spider.pageCount.incrementAndGet();
                        spider.scheduler.signalNewUrl();
                    }
                }
            });
    }
    
    protected void initComponent() {

        if (spider.downloader == null) {
            spider.downloader = new HttpClientDownloader();
        }
        if (spider.pipelines.isEmpty()) {
            spider.pipelines.add(new ConsolePipeline());
        }
        spider.downloader.setThread(spider.threadNum);
        if (spider.threadPool == null || spider.threadPool.isShutdown()) {
            if (spider.executorService != null && !spider.executorService.isShutdown()) {
                spider.threadPool = new CountableThreadPool(spider.threadNum, spider.executorService);
            } else {
                spider.threadPool = new CountableThreadPool(spider.threadNum);
            }
        }
        if (spider.startRequests != null) {
            for (Request request : spider.startRequests) {
                spider.addRequest(request);
            }
            spider.startRequests.clear();
        }
        spider.status.startTime = new Date();
    }

    
    void processRequest(Request request) {
        Page page;
        if (null != request.getDownloader()){
            page = request.getDownloader().download(request,spider);
        }else {
            page = spider.downloader.download(request, spider);
        }
        if (page.isDownloadSuccess()){
            onDownloadSuccess(request, page);
        } else {
            onDownloaderFail(request);
        }
    }


    
    private void onDownloadSuccess(Request request, Page page) {
        if (spider.site.getAcceptStatCode().contains(page.getStatusCode())){
            spider.pageProcessor.process(page);
            spider.extractAndAddRequests(page, spider.spawnUrl);
            if (!page.getResultItems().isSkip()) {
                for (Pipeline pipeline : spider.pipelines) {
                    pipeline.process(page.getResultItems(), spider);
                }
            }
        } else {
            logger.info("page status code error, page {} , code: {}", request.getUrl(), page.getStatusCode());
        }
        sleep(spider.site.getSleepTime());
    }

    private void onDownloaderFail(Request request) {
        if (spider.site.getCycleRetryTimes() == 0) {
            sleep(spider.site.getSleepTime());
        } else {
            // for cycle retry
            doCycleRetry(request);
        }
    }


    
    private void doCycleRetry(Request request) {
        Object cycleTriedTimesObject = request.getExtra(Request.CYCLE_TRIED_TIMES);
        if (cycleTriedTimesObject == null) {
            spider.addRequest(SerializationUtils.clone(request).setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, 1));
        } else {
            int cycleTriedTimes = (Integer) cycleTriedTimesObject;
            cycleTriedTimes++;
            if (cycleTriedTimes < spider.site.getCycleRetryTimes()) {
                spider.addRequest(SerializationUtils.clone(request).setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, cycleTriedTimes));
            }
        }
        sleep(spider.site.getRetrySleepTime());
    }


    public void setEmptySleepTime(long emptySleepTime) {
        this.emptySleepTime = emptySleepTime;
    }
    
    protected void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted when sleep",e);
            Thread.currentThread().interrupt();
        }
    }



}


