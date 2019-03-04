/**
 * Communicates to Manager.java using TCP and UDP protocols.
 * Written by Sovann Chak
 *
 */

#include <stdio.h>
#include <time.h>
#include <string.h>
#include <pthread.h>
#include <ifaddrs.h>
#include <errno.h>
#include <stdlib.h>
#include <unistd.h>

#include <sys/socket.h>
#include <sys/types.h> 

#include <arpa/inet.h> 
#include <netinet/in.h> 

//Macros to decide which OS the agent is using
#if defined(_WIN32) || defined(_WIN64) 
         char* os = "Windows";            
        #include <Windows.h> 
    #elif defined(__linux) || defined(__linux__)
         char* os = "Linux";
        #include <unistd.h>
    #elif defined(__unix__) || defined(__unix) || (defined(__APPLE__) && defined(__MACH__))
         char* os = "Mac OS X";
        #include <unistd.h>
#endif

#define PORT 4726 //some unique port
#define PORT2 4727 //some unique port
#define MAXLINE 1024

//Method stubs
void GetLocalOS(char OS[16], int *valid); 
void GetLocalTime(int *time_, int *valid); 
void *udp_connection(void* client);
void *tcp_connection();

/*
 * This struct BEACON acts as a packet.
 */
struct BEACON {
    int ID;
    double StartUpTime;
    char IP[4];
    int CmdPort;
};

int main(int argc, char *argv[]) {
 

    pthread_t thread_id;
    pthread_t thread_id2;
        
    //Generate current agent information.
    struct BEACON *client = (struct BEACON *)malloc(sizeof(struct BEACON)); 
    
    struct timeval  tv;
    gettimeofday(&tv, NULL);
    double time_millis = (tv.tv_sec) * 1000 + (tv.tv_usec) / 1000 ;
    client->StartUpTime = time_millis;

    srand(time(NULL));
    if(strcmp(os, "Windows") == 0) {
        client->ID = rand();
    } else {
        client->ID = getppid();

    }
    client->CmdPort = PORT;
    


   
   
    if(pthread_create(&thread_id, NULL, &udp_connection, (void*) client)) {
        perror("could not create thread");
        return 1;
    }

    if(pthread_create(&thread_id2, NULL, &tcp_connection, 0)) {
        perror("could not create thread");
        return 1;
    }
    
    pthread_join(thread_id, NULL); 
    pthread_join(thread_id2, NULL); 

    free(client);
    
    printf("%s\n", "agent disconnected successfully");
    return 0;
}

/*
 * Here OS[16] contains the name of the operating system where the agent is running
 * and the integer pointed by valid indicates the execution result. If it is 1, the * data in OS is valid.
 */
void GetLocalOS(char OS[16], int *valid) {
    if(strcmp(OS, "Windows") == 0 || strcmp(OS, "Mac OS X") == 0 || strcmp(OS, "Linux") == 0) {
        *valid = 1;  
    }
    else {
        *valid = 0;
    }
}

/*
 * Here the integer pointed by time represents the current system clock, and the
 * integer pointed by valid indicates the execution result. If it is 1, the data 
 * pointed by time is valid. 
 */
void GetLocalTime(int *time, int *valid) {
    if(time >= 0) {
        *valid = 1;
    } else {
        *valid = 0;
    }
}

/*
 * The function below is meant to be used along with pthread_create function to start sending
 * the beacon each minute
 */ 
void *udp_connection(void* client) {
    int udp_socket;            
    struct sockaddr_in  servaddr;
                 
    char * id = malloc(sizeof(((struct BEACON * )client)->ID));
    sprintf(id, "%d", ((struct BEACON * )client)->ID);

    //Preparing the start up time to be sent over the port
    char * start_up = malloc(sizeof(((struct BEACON * )client)->StartUpTime));
    sprintf(start_up, "%0.0lf", ((struct BEACON * )client)->StartUpTime);

    memset(&servaddr, 0, sizeof(servaddr));

    // Filling server information
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(PORT);
    servaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
                               
    //Initializing the udp_socket
    if((udp_socket = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        perror("UDP socket creation has failed!"); 
        exit(EXIT_FAILURE);    
    }                          
    while(1) {
        //Send ID                       
        sendto(udp_socket, (const char *) id, strlen(id),
               0, (const struct sockaddr *) &servaddr,
                sizeof(servaddr));

        //Send start up time
        sendto(udp_socket, (const char *) start_up, strlen(start_up),
               0, (const struct sockaddr *) &servaddr,
                sizeof(servaddr)); 
        sleep(60);
    } 
    
    close(udp_socket);
    
    return 0;
}

void *tcp_connection() {
   
    sleep(2); 
    int tcp_socket;            
    struct sockaddr_in  servaddr;
    char * delim = "-"; 
    memset(&servaddr, 0, sizeof(servaddr));

    // Filling server information
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(PORT2);
    servaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
                               
    //Initializing the udp_socket
    if((tcp_socket = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("TCP socket creation has failed!"); 
        exit(EXIT_FAILURE);    
    }                          
                               
    if(connect(tcp_socket, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0) {
        printf("Cannot connect to server\n");    
        exit(EXIT_FAILURE);    
    }                        
   
    int *valid_agent = malloc(sizeof(int));
    int *valid_time = malloc(sizeof(int));    
    
    char * time_str; 
    time_t t = time(NULL);
    int *time_ = (int *) t;  
    

    GetLocalOS(os, valid_agent);
    GetLocalTime(time_, valid_time);
    
    if(valid_agent) {
        
    } 
    else {
        printf("This os is not compatible\n");
        exit(EXIT_FAILURE);
    }
    
    if(valid_time) {
        time_str = ctime(&t);
        time_str[strlen(time_str)-1] = '\0';
        printf("Current Time : %s\n", time_str);
    }
    else {
        printf("The time is garbage.\n");
        exit(EXIT_FAILURE);
    }
    
    if(send(tcp_socket, os, strlen(os), 0) < 0) {       
        printf("Cannot send.\n");
        exit(EXIT_FAILURE);    
    }                          
    
    if(send(tcp_socket, delim, strlen(delim), 0) < 0) {       
        printf("Cannot send.\n");
        exit(EXIT_FAILURE);    
    }                          
   
    if(send(tcp_socket, time_str, strlen(time_str), 0) < 0) {       
        printf("Cannot send.\n");
        exit(EXIT_FAILURE);    
    }                          
    
  
    free(valid_agent);
    free(valid_time);
    
    close(tcp_socket);
    return 0;
}

