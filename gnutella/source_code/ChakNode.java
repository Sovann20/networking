import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Random;


/**
 * @author Sovann Chak
 *
 * A ChakNode is my implementation of a <i>Gnutella</i> servant. A servant is a node
 * within a peer-to-peer network which provides a client-interface allowing for users
 * to issue queries and view/receieve results. A servant can simultaneously accept
 * queries from other users (i.e. servents make up the peer-to-peer network).
 */
public class ChakNode {

    /**
     * Number of hops before search is abandoned (Time To Live).
     */
    private final int TTL = 10;

    /**
     * ExecutorService is used so we can shut down all of the threads safely.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * Bounds for the port range generation
     */
    private final int LOWER_PORT_RANGE =  1024;
    private final int UPPER_PORT_RANGE = 10000;

    /**
     * the following final variables represent the byte value of the potential
     * ChakNode actions.
     */
    private final byte PING =     0x00;
    private final byte PONG =     0x01;
    private final byte QUERY =    0x70;
    private final byte QUERY_HIT= 0x71;

    /**
     * ip will store this ChakNode's ip address, all servants
     * within the peer-to-peer network will have an IPv4
     * address.
     */
    private Inet4Address ip;

    /**
     * port will store this ChakNode's port number
     */
    private int port;

    /**
     * serverSocket will store this ChakNode's socket
     */
    private ServerSocket server;

    /**
     * radius will store this ChakNode's search radius
     * which will be used for neighbor discovery
     */
    private int radius;

    /**
     * this will represent the id of the
     */
    private String id;

    /**
     * this represents this ChakNode's active neighbors.
     */
    private CopyOnWriteArrayList<Integer> neighbors;

    /**
     * this represents the maximum number of connections
     * this ChakNode can have
     */
    private int maximumConnections;

    /**
     * this represents the number of bytes of data being
     * shared on this ChakNode
     */
    private int fileByteCount;

    /**
     * stores the path to the generated file directory for this ChakNode
     */
    private Path fileDirectory;

    /**
     * stores the path to all of the files within the file directory for this ChakNode
     */
    private ArrayList<Path> files;

    /**
     * stores the ids of all of the unique query ids received and created
     */
    private HashMap<String, Integer> queryCache;

    /**
     * Creates a ChakNode with ip address of localhost (127.0.0.1) and
     * the first available port, also initializes serverSocket so the
     * node can begin the discovery phase.
     * @throws Exception if port is in use.
     */
    public ChakNode() throws Exception {
        ip = (Inet4Address) Inet4Address.getByName("localhost");
        port = generateRandomPort();
        server = new ServerSocket(port, 1000, ip);
        radius = 100;
        id = generateId();
        files = new ArrayList<>();
        queryCache = new HashMap<>();
        neighbors = new CopyOnWriteArrayList<>();
        maximumConnections = 5;
        generateDirectoryAndFiles();
    }

    /**
     * Creates a ChakNode with ip address of localhost (127.0.0.1) and
     * with a provided port, also initializes serverSocket so the
     * node can begin the discovery phase.
     * @throws Exception if port is in use.
     */
    public ChakNode(int port) throws Exception {
        ip = (Inet4Address) Inet4Address.getByName("localhost");
        this.port = port;
        server = new ServerSocket(port, 1000, ip);
        radius = 100;
        id = generateId();
        files = new ArrayList<>();
        queryCache = new HashMap<>();
        neighbors = new CopyOnWriteArrayList<>();
        maximumConnections = 5;
        generateDirectoryAndFiles();
    }

    /**
     * Getter for this ChakNode's port variable
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     * Nicely prints out this ChakNode's neighbors for your viewing
     * pleasure.
     */
    public void printNeighbors() {
        System.out.println(Arrays.toString(neighbors.toArray()));
    }


    /**
     * Used to actively discover hosts on the network. A servant receiving a Ping
     * descriptor is expected to respond with one or more Pong descriptors.
     * @param port to send ping to
     * @return a boolean notifying the node pinging if the ping was successful
     */
    public boolean ping(int port) {
        try {
            Socket socket = new Socket(ip, port);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            dOut.write(generateDescriptor(PING, TTL, 0, 0));
            dOut.writeUTF("GNUTELLA CONNECT\n\n");
            dOut.writeInt(this.port);
            dOut.flush();
            dOut.close();
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This method is private because it is only meant to be called internally, when a port
     * receives a ping from a accepted port, it is going to forward the ping to its neighbor
     * in attempt to expand the network.
     * @param port to send ping to
     * @param descriptors descriptor packet to forward
     * @param initialPort port which sent the initial ping
     * @return a boolean notifying the node pinging if the ping was successful
     */
    private boolean ping(int port, byte[] descriptors, int initialPort) {
        try {
            Socket socket = new Socket(ip, port);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            dOut.write(descriptors);
            dOut.writeUTF("GNUTELLA CONNECT\n\n");
            dOut.writeInt(initialPort);
            dOut.flush();
            dOut.close();
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * The response to a Ping. Includes the address of a connected Gnutella servant and
     * information regarding the amount of data it is making available to the network.
     * @param port to send pong
     * @return a boolean notifying the node pinging if the ping was successful
     */
    private boolean pong(int port)  {

        try{
            Socket socket = new Socket(ip, port);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            dOut.write(generateDescriptor(PONG, TTL, 0, 14));
            dOut.writeUTF("GNUTELLA OK\n\n");

            //generate a 14 byte packet to be sent
            byte[] pongPacket = new byte[16];

            ByteBuffer.wrap(pongPacket, 0, 4).putInt(this.port);
            ByteBuffer.wrap(pongPacket, 4, 4).put(ip.getAddress());
            ByteBuffer.wrap(pongPacket, 8, 4).putInt(files.size());
            ByteBuffer.wrap(pongPacket, 12, 4).putInt(fileByteCount);

            dOut.write(pongPacket);
            dOut.flush();
            dOut.close();
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    /**
     * Searches the local storage for a file which matches the search
     * criteria
     * @param searchCriteria name of the file to be searched for
     * @return a boolean representing whether the search was successful or not.
     */
    private boolean localFileSearch(String searchCriteria) {
        for(Path file : files) {
            if(file.getName(file.getNameCount()-1).toString().equals(searchCriteria)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method takes string and finds the matching filepath on this ChakNode.
     * @param searchCriteria the file to be searched for
     * @return the path of the file
     */
    private Path getFilePath(String searchCriteria) {
        for(Path file : files) {
            if(file.getName(file.getNameCount()-1).toString().equals(searchCriteria)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Begins the searching process for a file, starts on this ChakNode and then
     * if unsuccessful this ChakNode will query it's neighbors.
     * @param searchCriteria
     */
    public void beginFileSearch(String searchCriteria) {
        if(!localFileSearch(searchCriteria)) {

            // generate a query to send to neighbors
            String requestID = generateId();
            while(queryCache.containsValue(requestID)) {
                requestID = generateId();
            }
            queryCache.put(requestID, port);


            for(Integer neighbor : neighbors) {
                query(neighbor, searchCriteria, requestID, TTL);
            }

        }
    }

    /**
     * This will list all of the files on this ChakNode
     */
    public void listAllFiles() {
        if(files.size() == 0) {
            System.out.println("No files!");
        }
        for(Path file : files) {
            System.out.println(file.getName(file.getNameCount()-1).toString());
        }
    }


    /**
     * This method will search for a specified string and if a match is found, it will let the requesting node
     * know, so that it can be downloaded.
     * @param port node to search
     * @param searchString the string to search for
     * @param requestID unique request ID
     * @return a boolean indicating if the search was successful or not
     */
    private boolean query(int port, String searchString, String requestID, int ttl) {

        try{
            Socket socket = new Socket(ip, port);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            byte[] queryPacket = new byte[8];

            dOut.write(generateDescriptor(QUERY, ttl, 0, queryPacket.length));
            dOut.writeUTF(requestID);
            dOut.writeUTF(searchString);
            ByteBuffer.wrap(queryPacket, 0, 4).putInt(this.port);
            ByteBuffer.wrap(queryPacket, 4, 4).putInt(ttl);

            dOut.write(queryPacket);

            dOut.flush();
            dOut.close();
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This method forwards the data of a successful query
     * @param requestId is a unique identifier of a query request
     * @param fileData is the byte data of a file to be downloaded
     * @param searchString is the name of the file being searched for
     * @return a boolean which represents if the method was successfully sent or not
     */
    private boolean queryHit(String requestId, byte[] fileData, String searchString) {

        int requestingPort = queryCache.get(requestId);

        try{
            Socket socket = new Socket(ip, requestingPort);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            dOut.write(generateDescriptor(QUERY_HIT, TTL, 0, fileData.length));
            dOut.writeUTF(requestId);
            dOut.writeUTF(searchString);
            dOut.write(fileData);

            dOut.flush();
            dOut.close();
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This private method returns true if the port is available, other wise false.
     * @param port to check if available
     * @return true if available, else false.
     */
    private boolean isPortAvailable(int port) {
        try {
            new ServerSocket(port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This method randomly generates a port which is not in use.
     * @return a port number between 1024 and 10000 (inclusive)
     */
    private int generateRandomPort() {
        Random random = new Random();
        int port;

        //loops until an available port is found
        while(!isPortAvailable((port = random.nextInt((UPPER_PORT_RANGE-LOWER_PORT_RANGE))+LOWER_PORT_RANGE)));

        return port;
    }

    /**
     * will generate a string (of length 16 bytes) which will represent
     * the id of this ChakNode.
     * @return string of length 16 bytes
     */
    private String generateId() {
        StringBuilder s = new StringBuilder();
        Random random = new Random();

        s.append(port);
        while(s.length() < 16) {
            s.append(random.nextInt(10));
        }

        return s.toString();
    }

    /**
     * This method assists with simulating an actual peer-to-peer network by creating temporary files
     * to be shared across the network.
     * @throws IOException shouldn't throw anything as long as the program is properly terminated.
     */
    private void generateDirectoryAndFiles() throws IOException {

        fileDirectory = Files.createDirectory(
                Paths.get(Paths.get(".").toAbsolutePath()+"/port_"+port));

        int fileCount = new Random().nextInt(5);

        for(int i = 0; i < fileCount; ++i) {
            files.add(Files.createTempFile(
                    fileDirectory, "port_"+port+"_file_", ".txt"));
            Files.write(files.get(i), Collections.singleton(Quote.getQuote()));
            fileByteCount += Files.size(files.get(i));
        }
    }


    /**
     * Generates a byte[] representing a descriptor header of length 22. The first 16
     * bytes is this ChakNode's id.
     * @param payload is represented by the next byte
     * @param timeToLive is represented by the next byte
     * @param hops is represented by the next byte
     * @param payloadLength is represented by the next 4 bytes
     * @return descriptor headers in a byte[] of size 22
     */
    private byte[] generateDescriptor(byte payload, int timeToLive, int hops, int payloadLength) {
        byte[] descriptor = new byte[23];

        ByteBuffer.wrap(descriptor, 0, 16).put(id.getBytes());
        ByteBuffer.wrap(descriptor, 16, 1).put(payload);
        ByteBuffer.wrap(descriptor, 17, 1).put((byte) timeToLive);
        ByteBuffer.wrap(descriptor, 18, 1).put((byte) hops);
        ByteBuffer.wrap(descriptor, 19, 4).putInt(payloadLength);

        return descriptor;
    }

    /**
     * The following thread will constantly looking to fill the neighbors list while maximum capacity has
     * not been reached and it is also responsible for the periodic pinging of the neighbor ChakNodes.
     */
    public class DiscoveryThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if(neighbors.size() < maximumConnections) {
                    //continue to add neighbors in our radius ..
                    int lowerRangeBound = Math.max(LOWER_PORT_RANGE, port - radius);
                    int upperRangeBound = Math.min(UPPER_PORT_RANGE, port + radius);

                    for (int portToCheck = lowerRangeBound; portToCheck <= upperRangeBound; portToCheck++) {
                        if (portToCheck != port && !neighbors.contains(portToCheck) && ping(portToCheck));
                    }

                    // if no neighbors are found, increase the radius
                    if (neighbors.isEmpty() || neighbors.size() < maximumConnections) {
                        radius += 100;
                    }
                }

                //periodically ping neighbors
                for (Integer neighbor : neighbors) {
                    //if pinging the neighbor fails, simply remove it from our list
                    if(!ping(neighbor) || neighbor == port) {
                        neighbors.remove(neighbor);
                    }
                }

                try {
                    Thread.sleep(60000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This thread is responsible for constantly listening to different requests from different nodes
     * and acts appropriately to the defined protocols.
     */
    public class ConnectionListener implements Runnable {
        @Override
        public void run() {
            // starts server and waits for a connection
            while (true) {
                try {
                    Socket listenSocket = server.accept();

                    DataInputStream in = new DataInputStream(
                            new BufferedInputStream(listenSocket.getInputStream()));

                    byte[] id =           new byte[16];
                    byte[] payload =       new byte[1];
                    byte[] timeToLive =    new byte[1];
                    byte[] hops =          new byte[1];
                    byte[] payloadLength = new byte[4];

                    in.read(id, 0, 16);
                    in.read(payload, 0, 1);
                    in.read(timeToLive, 0, 1);
                    in.read(hops, 0, 1);
                    in.read(payloadLength, 0, 4);


                    int timeToLiveInt = timeToLive[0];
                    int hopsInt = hops[0];
                    int payloadLengthInt = ByteBuffer.wrap(payloadLength).getInt();
                    int requestingPort;

                    switch (payload[0]) {
                        case PING:
                            String utfMessage = in.readUTF();
                            int socketPortNumber = in.readInt();

                            if (utfMessage.equals("GNUTELLA CONNECT\n\n") && !neighbors.contains(socketPortNumber)
                                    && neighbors.size() <= maximumConnections && pong(socketPortNumber)) {

                                neighbors.add(socketPortNumber);

                                if(timeToLiveInt-1 > 0) {
                                    pingNeighbors(generateDescriptor(payload[0], timeToLiveInt-1,
                                            hopsInt+1, payloadLengthInt), socketPortNumber);
                                }

                            } else if(neighbors.contains(socketPortNumber)) {
                                // wellness check on neighbors
                                if(!pong(socketPortNumber)) {
                                    neighbors.remove(socketPortNumber);
                                }
                            }

                            break;
                        case PONG:
                            utfMessage = in.readUTF();
                            byte[] portB = new byte[4];
                            byte[] ipAddrB = new byte[4];
                            byte[] numberFilesB = new byte[4];
                            byte[] byteCountB = new byte[4];

                            in.read(portB, 0, 4);
                            in.read(ipAddrB, 0, 4);
                            in.read(numberFilesB, 0, 4);
                            in.read(byteCountB, 0, 4);

                            requestingPort = ByteBuffer.wrap(portB).getInt();

                            if(utfMessage.equals("GNUTELLA OK\n\n") && !neighbors.contains(requestingPort)) {
                                neighbors.add(requestingPort);
                            }

                            break;
                        case QUERY:

                            String requestID = in.readUTF();
                            String searchString = in.readUTF();

                            byte[] requestPort = new byte[4];
                            byte[] requestTTL = new byte[4];

                            in.read(requestPort, 0, 4);
                            in.read(requestTTL, 0, 4);

                            requestingPort = ByteBuffer.wrap(requestPort).getInt();
                            int ttl = ByteBuffer.wrap(requestTTL).getInt();

                            if(!queryCache.containsKey(requestID) && ttl != 0) {
                                // now we know the request and the requesting port if a query hit is found
                                // we simply send a queryHit to this requesting port
                                queryCache.put(requestID, requestingPort);

                                if(localFileSearch(searchString)) {
                                    Path filePath = getFilePath(searchString);
                                    byte[] fileData = Files.readAllBytes(filePath);

                                    // send a queryHit
                                    if(!queryHit(requestID, fileData, searchString)) {
                                        System.out.println("File failed to send!");
                                    }


                                } else {

                                    for(Integer neighbor : neighbors) {
                                        if(!query(neighbor, searchString, requestID, ttl-1)) {
                                            System.out.println("Query to neighbor failed!");
                                        }
                                    }
                                }
                            }
                            break;
                        case QUERY_HIT:
                            requestID = in.readUTF();
                            String filename = in.readUTF();

                            byte[] fileData = new byte[payloadLengthInt];
                            in.readFully(fileData);

                            if(queryCache.get(requestID) == port) {
                                fileByteCount+=payloadLengthInt;
                                files.add(Files.write( Paths.get(fileDirectory+"/"+filename), fileData));
                                System.out.println("File with requestId "+requestID+" downloaded.");
                            } else {
                                queryHit(requestID, fileData, filename);
                            }

                            break;
                        default:
                            break;
                    }

                    in.close();

                } catch (IOException e) {
                    //Fails are expected to be common due to the nature of the port searching.
                }
            }
        }
    }

    /**
     * Kills the node and rids of the temporary directory/files which were
     * created to simulate the peer-to-peer network.
     */
    public void kill() {
        try {

            for (Path p : files) {
                Files.deleteIfExists(p);
            }

            Files.deleteIfExists(fileDirectory);

        } catch(IOException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
    }

    /**
     * Add a temporary file to this ChakNode's temporary directory
     * @param filename name of the file you wish to generate
     * @return boolean to represent if the file was added without error.
     */
    public boolean addFile(String filename) {
        try {

            Path newFilePath = Files.createFile(Paths.get(fileDirectory+"/"+filename));
            files.add(newFilePath);
            Files.write(files.get(files.size()-1), Collections.singleton(Quote.getQuote()));
            fileByteCount += Files.size(files.get(files.size()-1));

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads a file on this ChakNode (some randomly generated quote).
     * @param filename of the file to read
     * @return a boolean representing if the method was successful or not
     */
    public boolean readFile(String filename) {
        try {
            Path filePath = getFilePath(filename);
            if(filePath!= null) {
                System.out.println();
                for(String line : Files.readAllLines(filePath)) {
                    System.out.println(line);
                }
                System.out.println();
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Begins both threads which allow the node to connect to, and discover other nodes.
     */
    public void joinNetwork() {
        executorService.execute(new ConnectionListener());
        executorService.execute(new DiscoveryThread());
    }

    /**
     * Pings neighbors with descriptors to forward and the initial port which sent first ping
     * @param descriptors forwarded descriptor header in bytes
     * @param initialPort port which sent first initial ping
     */
    private void pingNeighbors(byte[] descriptors, int initialPort) {
        //periodically ping neighbors
        for (Integer neighbor : neighbors) {
            if(neighbor != initialPort) {
                if(!ping(neighbor, descriptors, initialPort)) {
                    // removes dead neighbors
                    neighbors.remove(neighbor);
                }
            }
        }
    }

    public static void main(String args[]) throws Exception{
        Scanner scanner = new Scanner(System.in);
        String input;
        ChakNode c;

        System.out.println("Please enter 0 if you would like to create a ChakNode with a random port, otherwise enter any input to provide your own port.");

        if(scanner.next().equals("0")) {
            c = new ChakNode();
        } else {
            System.out.println("Please enter an integer to represent the port you would like this ChakNode to use.");
            input = scanner.next();

            c = new ChakNode(Integer.parseInt(input));
        }


        System.out.println("ChakNode port: "+c.getPort());

        System.out.println("\nEnter a command\n" +
                "\n\thelp: to view all options"+
                "\n\t0: to join network" +
                "\n\t1: to print neighbors" +
                "\n\t2 'someFileName': to create a new file with name: 'someFileName'" +
                "\n\t3 'someFileName': to search for a file with name: 'someFileName'" +
                "\n\t4: to list all files on this node"+
                "\n\t5 'someFileName': to read a file with name: 'someFileName'" +
                "\n\texit to terminate the program and remove the node from the network\n");

        while(!(input = scanner.next()).equals("exit")) {

            switch (input){
                case "0":
                    c.joinNetwork();
                    System.out.println("Network joined.");
                    break;
                case "1":
                    c.printNeighbors();
                    break;
                case "2":
                    String fileName = scanner.next()+".txt";
                    if(c.addFile(fileName)) {
                        System.out.println("New file created with name "+fileName);
                    } else {
                        System.out.println("File creation failed.");
                    }
                    break;
                case "3":
                    if(scanner.hasNext()){
                        fileName = scanner.next();
                        c.beginFileSearch(fileName);
                    } else{
                        System.out.println("Incorrect command. You must enter: " +
                                "\n\t3 'someFileName' to search for a file with name: 'someFileName'");
                    }
                    break;
                case "4":
                    c.listAllFiles();
                    break;
                case "5":
                    if(!c.readFile(scanner.next())) {
                        System.out.println("File doesn't exist!");
                    }
                    break;
                default:
                    System.out.println("\nEnter a command\n" +
                            "\n\thelp: to view all options"+
                            "\n\t0: to join network" +
                            "\n\t1: to print neighbors" +
                            "\n\t2 'someFileName': to create a new file with name: 'someFileName'" +
                            "\n\t3 'someFileName': to search for a file with name: 'someFileName'" +
                            "\n\t4: to list all files on this node"+
                            "\n\t5 'someFileName': to read a file with name: 'someFileName'" +
                            "\n\texit to terminate the program and remove the node from the network\n");
                    break;
            }
        }

        c.kill();
        System.exit(0);
    }
}
