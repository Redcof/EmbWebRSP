package test;


import com.realsync.proto.servers.RealSyncServer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author int soumen
 */
public class MyServer {

    public static void main(String aa[]) {
        RealSyncServer server = new RealSyncServer(26033) {
            @Override
            public void statusMessage(String s) {
                System.out.println(s);
            }

            @Override
            public void serverStarted() {
                 System.out.println("Server started.");
            }

            @Override
            public void serverClosed() {
                System.out.println("Server closed.");
            }
        };
        new Thread(server).start();

        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping Server");
        server.stop();
    }
}
