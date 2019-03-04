/**
 * Written by Sovann Chak
 */

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <pthread.h>
#include <string.h>
#include <sys/socket.h>

#include <arpa/inet.h>
#include <netinet/in.h>

//Macros to decide which OS the agent is using
#if defined(_WIN32) || defined(_WIN64) 
         char* _os = "Windows";            
        #include <Windows.h> 
    #elif defined(__linux) || defined(__linux__)
         char* _os = "Linux";
        #include <unistd.h>
    #elif defined(__unix__) || defined(__unix) || (defined(__APPLE__) && defined(__MACH__))
         char* _os = "Mac OS X";
        #include <unistd.h>
#endif


#define PORT 4726
#define MAX_CLIENTS 30

typedef struct {
    int* time;
    char* valid;
} GET_LOCAL_TIME;

typedef struct {
    char* os;
    char* valid;
} GET_LOCAL_OS;


void *CmdProcessor(void* client_socket);
void GetLocalTime(GET_LOCAL_TIME* ds);
void GetLocalOS(GET_LOCAL_OS* ds);
int receieve_full_request(int client_socket, char* buffer, int len);
int bytes_to_int(char *bytes);
char* int_to_bytes(int i);

int main(int argc, char *argv[]) {

    pthread_t thread_id[MAX_CLIENTS];
        
    int serv_socket, i, num_clients = 0;
    struct sockaddr_in  servaddr;



    //Initializing the udp_socket
    if((serv_socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0) {
        perror("Server socket creation has failed!");
        return 1;
    }

    memset(&servaddr, 0, sizeof(servaddr));

    // Filling server information
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(PORT);
    servaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
   
    if(bind(serv_socket, (struct sockaddr *) &servaddr, sizeof(servaddr)) != 0) {
        perror("Binding socket has failed!");
        return 1;
    }

    if(listen(serv_socket, MAX_CLIENTS) == -1) {
    
        perror("Listening failed, exiting.");
        return 1;
    }
    
    while(1) {
        struct sockaddr cliaddr;
        unsigned int cli_len;

        int cli_socket;
       
        if((cli_socket = accept(serv_socket, &cliaddr, &cli_len)) == -1) {
            perror("Client was not reached!");
            return 1;
        }

        int *new_cli = malloc(sizeof(int));
        *new_cli = cli_socket;


               
        if(pthread_create(&thread_id[num_clients], NULL, &CmdProcessor, new_cli)) {
            perror("Could not create thread.");
            return 1;
        }
        
        pthread_detach(thread_id[num_clients]);
        num_clients++;
    }

    for(i = 0; i < num_clients; i++) {
        pthread_join(thread_id[i], NULL);
    }


    return 0;
}

void *CmdProcessor(void* client_socket) {
    int cli_socket = *(int*) client_socket;

    //Header to be received
    char header[104];
    char cmd[100];
    char len[4];

    receieve_full_request(cli_socket, header, 104);
    
    strncpy(cmd, header, 100);  
    strncpy(len, header+100, 4);  

    int int_len = bytes_to_int(len); 
    
    if(strcmp(cmd, "GetLocalTime") == 0) {
        printf("C-Server: Getting local time in milliseconds.\n");
        GET_LOCAL_TIME* ds;
        ds = malloc(sizeof(GET_LOCAL_TIME));
        
        GetLocalTime(ds);
        
        printf("C-Server: Local time-%d Valid-%c\n", *(ds->time), *(ds->valid)); 

        int t_time = *(ds->time);
        
        char* param1 = int_to_bytes(t_time);
        char* param2 = malloc(sizeof(char));

        *param2 = *(ds->valid);
    
        send(cli_socket, param1, sizeof(param1), 0);
        send(cli_socket, param2, sizeof(param2), 0);

        free(param1);
        free(param2);
        free(ds->time);
        free(ds->valid);
        free(ds);   
    }
    else if(strcmp(cmd, "GetLocalOS") == 0) {
        printf("C-Server: Getting local operating system.\n");
        GET_LOCAL_OS* ds;
        ds = malloc(sizeof(GET_LOCAL_OS));

        GetLocalOS(ds);
        
        printf("C-Server: Local OS-%s Valid-%c\n", (ds->os), *(ds->valid)); 
        
       
        char * end_line = "\n";
        send(cli_socket, ds->os, sizeof(ds->os), 0);
        send(cli_socket, end_line, sizeof(end_line), 0);

        free(ds->os);
        free(ds->valid);
        free(ds);   
    }
    else {
        printf("C-Server: Command doesn't exist!\n");
        return 0;
    }
    
    free(client_socket);
    return 0;
}

int receieve_full_request(int client_socket, char* buffer, int len) {
    char* current_char = buffer;
    int bytes_recv = 0;
    while(bytes_recv != len) {
        if(recv(client_socket, current_char, 1, 0) == -1) {
            perror("recv failed: ");
            return 1;
        }

        current_char++;
        bytes_recv++;
    }
    
    return 0;
}  

int bytes_to_int(char *bytes) {
    return    
            (bytes[0] & 0xFF)      |
            (bytes[1] & 0xFF) << 8 |
            (bytes[2] & 0xFF) << 16|
            (bytes[3] & 0xFF) << 24;
}

char* int_to_bytes(int i) {
    char* bytes = malloc(4);    
    bytes[0] = (i & 0xFF);      
    bytes[1] = (i >> 8)  & 0xFF;
    bytes[2] = (i >> 16)  & 0xFF;
    bytes[3] = (i >> 24)  & 0xFF;
    return bytes;
}

void GetLocalTime(GET_LOCAL_TIME* ds) {
    
    time_t time_ = time(NULL);
    
    struct tm* t = gmtime(&time_);
    int hr = t->tm_hour*3600000;
    int sec = t->tm_min*60000+t->tm_sec*1000;
    
    ds->time = malloc(sizeof(int));
    ds->valid = malloc(sizeof(char));
    *(ds->time) = hr+sec;
    *(ds->valid) = '1';    
}

void GetLocalOS(GET_LOCAL_OS* ds) {
    ds->os = malloc(sizeof(_os)+1);
    ds->valid = malloc(sizeof(char));
    strcpy(ds->os, _os);


    if(strcmp((ds->os), "Mac OS X") == 0 || strcmp((ds->os), "Linux") == 0
            || strcmp((ds->os), "Windows") == 0) { 
        *(ds->valid) = '1';
    
    } else {
        *(ds->valid) = '0';
        ds->os = malloc(sizeof(strlen("Unknown"))+1);
        strcpy((ds->os), "Unknown");
    }
}
