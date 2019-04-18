import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;


public class ChakNodeNetworkSimulator {

    static AtomicReference<Boolean> exit = new AtomicReference<>(false);

    public static void main(String args[]) throws Exception {

        int count = 100;
        if(args.length > 0) {
            count = Integer.parseInt(args[0]);
        }

        HashMap<Integer, ChakNode> nodes = new HashMap<>(count);

        while (nodes.size() < count) {
            ChakNode c = new ChakNode();
            nodes.put(c.getPort(), c);
        }

        System.out.println(count+" nodes added!");


        for(ChakNode node : nodes.values()) {
            node.joinNetwork();
        }

        new Thread(()->{
            Scanner scan = new Scanner(System.in);
            String input;

            System.out.println("Enter a command:" +
                    "\n\thelp to view all options"+
                    "\n\t0 'portNumber1' 'portNumber2' to ping a port number 'portNumber2' from 'portNumber1'" +
                    "\n\t1 to print neighbors of each node" +
                    "\n\t2 'someFileName' 'portNumber' to create a new file with name 'someFileName' on a node with 'portNumber" +
                    "\n\t3 'someFileName' 'portNumber' to start a search for a file 'someFileName' on a node with 'portNumber'" +
                    "\n\t4 'portNumber' to list all files on a node with a port number 'portNumber'"+
                    "\n\t5 'someFileName' 'portNumber' to search for a file with a name 'someFileName' on a node with 'portNumber'" +
                    "\n\t6 'portNumber' to add a node with a port 'portNumber'" +
                    "\n\tkill 'portNumber' to kill and remove a node from the network with the port number 'portNumber'"+
                    "\n\texit to terminate the program\n");

            input = scan.next();
            boolean enter = true;

            if(input.equals("exit")) {
                enter = false;
            }

            while (enter) {
                switch (input){
                    case "0":
                        try {
                            int port1 = scan.nextInt();
                            int port2 = scan.nextInt();

                            nodes.get(port1).ping(port2);
                            System.out.println(port1+" has successfully pinged "+port2);
                        } catch(Exception e){
                            System.out.println("Invalid port(s) number");
                        }
                        break;
                    case "1":
                        for(ChakNode node : nodes.values()) {
                            System.out.print(node.getPort());
                            node.printNeighbors();
                        }
                        break;
                    case "2":
                        try {
                            String fileName = scan.next();
                            nodes.get(scan.nextInt()).addFile(fileName);
                            System.out.println("File "+fileName+".txt was successfully added.");
                        } catch (Exception e) {
                            System.out.println("Something went wrong.");
                        }
                        break;
                    case "3":
                        try {
                            String fileName = scan.next();
                            nodes.get(scan.nextInt()).beginFileSearch(fileName);
                        } catch( Exception e) {
                            System.out.println("Something went wrong.");
                        }
                        break;
                    case "4":
                        try {
                            nodes.get(scan.nextInt()).listAllFiles();
                        } catch (Exception e) {
                            System.out.println("Invalid port number!");
                        }
                        break;
                    case "5":
                        try {
                            String filename = scan.next();
                            if(!nodes.get(scan.nextInt()).readFile(filename)) {
                                System.out.println("File doesn't exist or cannot be found!");
                            }
                        } catch (Exception e) {
                            System.out.println("Something went wrong.");
                        }
                        break;
                    case "6":
                        try {
                            int port = scan.nextInt();
                            nodes.put(port, new ChakNode(port));

                        } catch (Exception e) {
                            System.out.println("Invalid port number or port already exists.");
                        }
                        break;
                    case "kill":
                        try {
                            int port = scan.nextInt();
                            nodes.get(port).kill();
                            nodes.remove(port);
                        } catch (Exception e) {
                            System.out.println("Invalid port number.");
                        }
                        break;
                    case "exit":
                        enter = false;
                        break;
                    default:
                        System.out.println("\nEnter a command:" +
                                "\n\thelp to view all options"+
                                "\n\t0 'portNumber1' 'portNumber2' to ping a port number 'portNumber2' from 'portNumber1'" +
                                "\n\t1 to print neighbors of each node" +
                                "\n\t2 'someFileName' 'portNumber' to create a new file with name 'someFileName' on a node with 'portNumber" +
                                "\n\t3 'someFileName' 'portNumber' to start a search for a file 'someFileName' on a node with 'portNumber'" +
                                "\n\t4 'portNumber' to list all files on a node with a port number 'portNumber'"+
                                "\n\t5 'someFileName' 'portNumber' to search for a file with a name 'someFileName' on a node with 'portNumber'" +
                                "\n\tkill 'portNumber' to kill and remove a node from the network with the port number 'portNumber'"+
                                "\n\texit to terminate the program\n");
                        break;
                }
                if(enter) {
                    input = scan.next();
                }
            }

            exit.set(true);
        }).start();



        while (!exit.get());


        for (ChakNode n : nodes.values()) {
            n.kill();
        }

        System.exit(0);
    }
}
