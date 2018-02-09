/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.realsync.proto.servers.test.simulation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author Arijit
 */
public class MyRealsyncClient {
    
    public static void main(String[] args) throws Exception {
        
        System.out.println("##: Connection to server...");
        Socket sock = new Socket("127.0.0.1", 25004);
        int buffSize = 100;//Byte
        byte buffer[] = new byte[buffSize];
        int read;

        // reading from keyboard (keyRead object)
        InputStream isr = new FileInputStream("F:\\int_soumen\\_setups_\\wamp64_2\\www\\SocketIO\\data.csv");
        //BufferedReader keyRead = new BufferedReader(new InputStreamReader(in));
        
        // sending to client (pwrite object)
        OutputStream ostream = sock.getOutputStream();
        PrintWriter pwrite = new PrintWriter(ostream, true);

        // receiving from server ( receiveRead  object)
        InputStream istream = sock.getInputStream();
        BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

        

        String receiveMessage;
        System.out.println("##: Reading data...");
        while ((read = isr.read(buffer)) != -1) {
            System.out.println("##: Sending: " +new String(buffer));
            ostream.write(buffer, 0, read);
            ostream.flush();
            //pwrite.println(sendMessage);       // sending to server
            //pwrite.flush();                    // flush the data
            if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
            {
                System.out.println("##: Reply From Server  :"+receiveMessage); // displaying at DOS prompt
            }
        }
        
        pwrite.println(".*111*.");       // sending to server
        pwrite.flush();                  // flush the data
        
        while(true)
        {
            if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
            {
                System.out.println("##: Reply From Server  :"+receiveMessage); // displaying at DOS prompt
                if(receiveMessage.trim().equals(".*111*."))
                {
                    break;
                }
            }
        }
        
        sock.close();
        System.out.println("##: Client Closed.");
    }
}
