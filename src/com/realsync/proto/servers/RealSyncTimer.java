/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.realsync.proto.servers;

import java.util.Date;

/**
 *
 * @author Arijit
 */
public abstract class RealSyncTimer implements Runnable {

    private long Timeout_mills = -1;
    private boolean Expired = false;
    private Date Timeout;
    private boolean TimerStatus = false;
    private RealSyncClientWorker Worker = null;

    public synchronized long getTimeout_mills() {
        return Timeout_mills;
    }

    public synchronized boolean isExpired() {
        return Expired;
    }

    public synchronized void reset() {
        this.Timeout = new Date();
    }

    public synchronized void done() {
        this.Expired = true;
    }

    public synchronized RealSyncClientWorker getWorker() {
        return this.Worker;
    }

    public RealSyncTimer(RealSyncClientWorker worker, long timeout_mills, String name) {
        Timeout_mills = timeout_mills;
        Expired = false;
        Worker = worker;
    }

    public abstract void timerDone();

    @Override
    public void run() {
        if (TimerStatus == false) {
            TimerStatus = true;
            Timeout = new Date();
            while (!Expired) {
                Date now = new Date();
                if ((now.getTime() - Timeout.getTime()) > Timeout_mills) {
                    this.Expired = true;
                    this.timerDone();
                }
            }
        }
    }

}
