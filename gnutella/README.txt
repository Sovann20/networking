PLEASE READ THE FOLLOWING BEFORE RUNNING THIS PROGRAM:

There are 3 classes, ChakNode.java, ChakNodeNetworkSimulator.java and
Quote.java. Do not worry about Quote.java - it is used to auto generate some
file data to simulate a real network without cumbersome, manual file creation.

Before running ChakNode.java you should know that this enters a SINGLE node into 
the network. So, when calling java ChakNode a ChakNode will be created.It is up 
to the user to then join the network and do various operations with your newly 
created node. The creation of a ChakNode will autogenerate a file directory and
a random files. These files are to assist with streamlining the simulation of a
real network! 

!IMPORTANT!: When you are finished with the ChakNode OR the ChakNodeSimulatedNetwork
you are ADVISED to call exit. If you do not call exit, you will most likely have auto-
generated garbage files left in your directory. Calling exit on either class will auto-
matically clean out those garbage files.

The default setting for the ChakNodeNetworkSimulator is to generate 100 nodes (which
opens up 100 ports) however, when running the program with
   
    java ChakNodeNetworkSimulator 'someNumber'

this will start a simulated network of 'someNumber' number of nodes. But remember 
that this number will open up this many ports on your localhost!

I believe the best way to play with my implementation of a Gnutella network is to start
the ChakNodeNetworkSimulator with however many nodes you wish (or default 100) and then
start the ChakNode. Together these interfaces will provide the best view of how the
network is communicating. As the ChakNode will provide the view of a single node
within the network, the ChakNodeNetworkSimulator will provide a view of many nodes!

I hope that these instructions are clear, please let me know if you would like a demo!

The steps to successfully compile this program are as follows:

    javac *.java

To run the ChakNode program enter

    java ChakNode

To run the ChakNodeNetworkSimulator program with the default 100 nodes enter

    java ChakNodeNetworkSimulator

To run the ChakNodeNetworkSimulator program with x (make sure x is an integer value)
nodes enter

    java ChakNodeNetworkSimulator x


