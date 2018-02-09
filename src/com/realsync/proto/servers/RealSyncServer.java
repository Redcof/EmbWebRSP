/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.realsync.proto.servers;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author int soumen
 */
public abstract class RealSyncServer implements Runnable{

    protected int          serverPort   = 2090;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected ArrayList<RealSyncClientWorker> EventClientArray = new ArrayList<>();
    protected ArrayList<RealSyncClientWorker> FileClientArray = new ArrayList<>();
    protected ArrayList<RealSyncClientWorker> DisposableClientArray = new ArrayList<>();
    
    public RealSyncServer(int port){
        this.serverPort = port;
    }
    public abstract void statusMessage(String s);    
    public abstract void serverStarted();
    public abstract void serverClosed();
    private int ClientSerialNo = 0;
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                this.statusMessage("Waiting for client...");
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    this.statusMessage("Server Stopped.");
                    //System.out.println("Server Stopped.");
                    return;
                }
                this.statusMessage("Error accepting client connection");
                //throw new RuntimeException("Error accepting client connection", e);
            }
            ClientSerialNo++;
            this.statusMessage("Accepting Client " + ClientSerialNo);            
            RealSyncClientWorker clientWorker = new RealSyncClientWorker(
                    clientSocket, "Client-" + ClientSerialNo) {
                @Override
                public void statusClientMessage(String s) {
                    statusMessage(this.getServerText() +": "+ s);
                }

                @Override
                public void processingStarted() {                    
                    statusMessage(this.getServerText() +": "+"Client process started.");
                }

                @Override
                public void clientClosed() {
                    
                    statusMessage(this.getServerText() +": "+"Client process completed.");
                }

                @Override
                public void clientType(EnumSocketClientType clientType) {
                    switch(clientType){
                       case en_ClientTypeEventClient:
                            EventClientArray.add(this);
                            break;
                        default:
                            //DisposableClientArray.add(this);                            
                    }
                }

                @Override
                public void handshake(boolean status) {
                    if(status == false)
                    {
                        switch(this.getEN_CLIENT_TYPE()){
                            case en_ClientTypeEventClient:
                                EventClientArray.remove(this);
                                break;
                            default: 
                        }
                    }
                }
                
            };
            new Thread(clientWorker).start();
        }
        this.statusMessage("Server Stopped.");
        //System.out.println("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
            this.serverClosed();
        } catch (IOException e) {
            this.statusMessage("Error closing server.");
            //throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.statusMessage("Starting server...");
            this.serverSocket = new ServerSocket(this.serverPort);
            this.serverStarted();
        } catch (IOException e) {
            this.statusMessage("Cannot create server on it.");
            //throw new RuntimeException("Cannot open port 8080", e);
        }
        this.statusMessage("OK");
    }

}
