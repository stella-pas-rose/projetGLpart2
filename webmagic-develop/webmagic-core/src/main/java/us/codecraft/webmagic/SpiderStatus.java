package us.codecraft.webmagic;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

public class SpiderStatus {
    protected final static int STAT_INIT = 0;
    protected final static int STAT_RUNNING = 1;
    protected final static int STAT_STOPPED = 2;

    protected Logger logger = LoggerFactory.getLogger(getClass());
    
    protected AtomicInteger stat = new AtomicInteger(STAT_INIT);
    protected Date startTime;


    public enum Status {
        Init(0), Running(1), Stopped(2);
        private Status(int value) {
            this.value = value;
        }
        private int value;
        int getValue() {
            return value;
        }
        public static Status fromValue(int value) {
            for (Status status : Status.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            //default value
            return Init;
        }
    }
    

    public Status getStatus(){
        return Status.fromValue(stat.get()); 
    }

    public Date getStartTime(){
        return startTime; 
    }
    
    public void stop(String uuid) {
        if (stat.compareAndSet(STAT_RUNNING, STAT_STOPPED)) {
            logger.info("Spider " + uuid + " stop success!");
        } else {
            logger.info("Spider " + uuid + " stop fail!");
        }
    }
    
    protected void checkRunningStat() {
        while (true) {
            int statNow = stat.get();
            if (statNow == STAT_RUNNING) {
                throw new IllegalStateException("Spider is already running!");
            }
            if (stat.compareAndSet(statNow, STAT_RUNNING)) {
                break;
            }
        }
        this.startTime= new Date(); 
    }

    protected void checkIfRunning() {
        if (stat.get() == STAT_RUNNING) {
            throw new IllegalStateException("Spider is already running!");
        }
    }

}


