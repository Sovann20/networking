Comile using:    
    gcc Agent.c -o Agent
    javac Manager.java

Run using:
    1. java Manager
    2. ./Agent

What to expect: 
    Each terminal opened will connect via UDP and send a beacon which is received and then the TCP port will send the operating system and the local time of the agent/client.

    If a agent/client disconnects, the program will wait four beacon cycles (or two minutes) before declaring the client inactive (or dead). After this declaration is made, if the same agent/client reconnects a message will be shown that the agent/c lient has reconnected and beacons will be sent as normal.

Notes: 

    This program works with as many clients as you can open terminals and is portable between the main operating systems of Windows, Linux, and Mac OS X.
