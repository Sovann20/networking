import java.util.Map; 
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.io.*;
import java.net.*;

import java.time.*;

public class Manager {

     private static Map<String, Agent> map = Collections.synchronizedMap(new HashMap<String, Agent>());

     public static void main(String args[]) throws Exception {


  
        ExecutorService  service = Executors.newFixedThreadPool(2);
        Manager m = new Manager();
        service.execute(m.new BeaconListener());
        service.execute(m.new AgentMonitor());
        //service.execute(m.new ClientAgent());
        service.shutdownNow();

      }
   

    class AgentMonitor implements Runnable {
        @Override
        public void run(){
            while(true) {
                try {
                    synchronized(map) {
                        for(Map.Entry<String, Agent> ent : map.entrySet()) {
                            System.out.println("AgentMonitor Thread: Client "+ent.getValue().getId() + 
                                               " last sent a beacon at time "+ent.getValue().getBeaconTime()+"\n");
                            if(System.currentTimeMillis() - 
                                    ent.getValue().getBeaconTime() >= 120000 && ent.getValue().isActive()) {
                                ent.getValue().kill();
                                System.out.println("AgentMonitor Thread: Client "+ent.getValue().getId()+" has missed two beacon cycles and is now inactive.\n");
                            }
                        }
                    }
                    Thread.sleep(30000);
                } catch(InterruptedException e) { }
            }
        }
    }
    
    class BeaconListener implements Runnable {
        @Override
        public void run(){
            while(true) {   
                try {
                    byte[] recData = new byte[1024];
                    DatagramSocket ds = new DatagramSocket(4726, InetAddress.getLoopbackAddress());
                    DatagramPacket incoming = new DatagramPacket(recData, recData.length);
                      
                    ds.receive(incoming);
                    String ID = new String( incoming.getData() );
                      
                    ds.receive(incoming);
                    String startup_timestamp = new String( incoming.getData() );
                    //sanitizing input 
                    String line = startup_timestamp.split("\\.", -1)[0];
                        
                    synchronized(map) {
                        //If the agent isn't in the list of clients, add them.    
                        if(null != map.putIfAbsent(ID, new Agent(true, 
                                        Long.parseLong(line.trim()), 
                                        System.currentTimeMillis(), 
                                        Integer.parseInt(ID.trim())))) {
                               //If the agent is exisiting - update the beacon ping time! 
                                map.get(ID).updateBeacon(System.currentTimeMillis());    
                            if(!map.get(ID).isActive()) {
                                //Client has respawned
                                System.out.println("BeaconListener Thread: Client " + ID + " has reconnected!\n");
                                map.get(ID).respawn();
                                map.get(ID).updateStartUp(System.currentTimeMillis());
                            } else {
                                System.out.println("BeaconListener Thread: Client " + ID + " has sent a beacon at time "+
                                        +map.get(ID).getBeaconTime()+"\n");
                            } 

                         } else {
                             System.out.println("BeaconListener Thread: Adding new client with ID: " + ID + " at time "+ map.get(ID).getBeaconTime());
                            Thread t = new Thread(new ClientAgent());
                            t.start(); 
                         }
                    }
                    
                } catch (Exception e) {
                
                }
            } 
        }
    }
    
    class ClientAgent implements Runnable {
        public void run(){
        
            // starts server and waits for a connection 
            try { 

                ServerSocket server = new ServerSocket(4727); 
  
                Socket socket = server.accept(); 
                
                //Reads from socket
                BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream())); 
                
                String line = ""; 
                try { 
                    line = in.readLine();
                    String lines[] = line.split("\\-");
                    System.out.println("ClientAgent Thread: Client Operating System - "+lines[0]); 
                    System.out.println("ClientAgent Thread: Client Local Time - "+lines[1]+"\n"); 
                } 
                catch(IOException e) 
                { 
                    System.out.println(e); 
                } 
            
                socket.close(); 
                in.close();
                server.close(); 
            } 
            catch(IOException e) { 
                System.out.println(e); 
            } 
        
        }
    }


      static class Agent {
        private boolean active;
        private long startUp;
        private long recievedBeaconTime;
        private final int ID;

        public Agent(boolean active, long startUp, long recievedBeaconTime, int ID) {
            this.active = active;
            this.startUp = startUp;
            this.recievedBeaconTime = recievedBeaconTime;
            this.ID = ID;
        }

        public boolean isActive() {
            return active;
        }

        public long getStartUp() {
            return startUp;
        }

        public long getBeaconTime() {
            return recievedBeaconTime;
        }

        public int getId() {
            return ID;
        }

        public void kill() {
            active = false;
        }

        public void respawn() {
            active = true;
        }

        public void updateStartUp(long time) {
            startUp = time;
        }

        public void updateBeacon(long time) {
            recievedBeaconTime = time;
        }
    }
}
