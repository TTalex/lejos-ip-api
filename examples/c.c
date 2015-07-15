#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>
#include <errno.h>

#define REQ_TIMEOUT 0.1

typedef struct{
  char *host;
  int port;
  char *path;
} decoded_url;

// Using select, checks if data is present on socket for REQ_TIMEOUT seconds
int32_t isReadDataAvailableOnSocket(int sock, float waitTimeS){
  fd_set fds;
  int16_t ret = 0;
  struct timeval timeout;
  struct timeval* timeoutPtr = NULL;

  if(waitTimeS>0){
    timeout.tv_sec = waitTimeS;
    timeout.tv_usec = waitTimeS * 1000000;
    timeoutPtr = &timeout;
  }

  FD_ZERO(&fds);
  FD_SET(sock, &fds);

  ret = select(sock+1, &fds, NULL, NULL, timeoutPtr);
  if(ret == -1) {
    return 1;
  }
  return !FD_ISSET(sock, &fds);
}

// Remove http headers
char * removeheaders(char *httpr){
  int l = strlen(httpr)-1;
  int i;
  for (i=0; i<l; i++){
    if(httpr[i] == '\n' && httpr[i+1] == '\r'){
      break;
    }
  }
  return httpr+i+3;
}

// DNS resolution from hostname to IPV4
char *getip(char *hostname){
  struct addrinfo *result;
  struct addrinfo *res;
  int error = getaddrinfo(hostname, NULL, NULL, &result);
  if (error){
    fprintf(stderr, "getip: %s\n", strerror(errno));
    return hostname;
  }
  struct sockaddr_in *inaddr_ptr = (struct sockaddr_in *)result->ai_addr;
  freeaddrinfo(result);
  return inet_ntoa(inaddr_ptr->sin_addr);
}

/*
 Decode an human readable HTTP url into a struct containing host, port and path
 http://localhost:8082/utf decodes into
 ->host: "127.0.0.1"
 ->port: 8082
 ->path: "utf"
*/
decoded_url* decode(char *url){
  char *sub = NULL;
  char *s;
  char *sep_at;
  decoded_url* d_url = (decoded_url*) malloc(sizeof(decoded_url));
  
  // Remove "http://" if present
  if(!strncmp(url, "http://", 7)){
    sub = (char *)malloc(strlen(url)-7);
    memcpy( sub, &url[7], strlen(url)-7);
  }
  if (sub){
    s = strdup(sub);
    free(sub);
  }else{
    s = strdup(url);
  }
  
  // Extract the path (everything after first '/')
  sep_at = strchr(s, '/');
  if(sep_at != NULL){
    *sep_at = '\0';
    d_url->path = sep_at + 1;
  }else{
    d_url->path = "";
  }
  
  // Split hostname and port
  sep_at = strchr(s, ':');

  // Extract port, set to 80 if no ports are specified
  if(sep_at != NULL){
    *sep_at = '\0';
    d_url->port = strtol(sep_at + 1, NULL, 10);  
  }else{
    d_url->port = 80;
  }

  // Convert hostname to IPV4
  d_url->host = getip(s);

  return d_url;
}

/* 
   Sends an HTTP GET request to the given url
   Returns the HTTP response
*/
char* get(char *url){
  int sockfd = socket(AF_INET, SOCK_STREAM, 0);
  char *recvbuff = (char *)malloc(1024);
  struct sockaddr_in serv_addr;
  decoded_url* d_url = decode(url);
  char request[1024];

  //Fill up server address information
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_port = htons(d_url->port);
  
  //Convert IPV4 to binary
  if(inet_pton(AF_INET, d_url->host, &serv_addr.sin_addr)<=0){
    perror(strerror(errno));
    close(sockfd);
    return recvbuff;
  }

  //Establish connection to remote server
  if(connect(sockfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0){
    fprintf(stderr, "Error : Connect Failed %s \n", strerror(errno));
    close(sockfd);
    return recvbuff;
  } 

  // Construct the GET request
  sprintf(request, 
	  "GET /%s HTTP/1.1\r\n"
	  "Host: %s:%d\r\n"
	  "\r\n"
	  ,d_url->path, d_url->host, d_url->port);
  if(write(sockfd, request, strlen(request)) == -1){
    fprintf(stderr, "Write on socket error\n");
    close(sockfd);
    return recvbuff;
  }
  
  int n= 0;
  // Read the response while information is being sent
  if((n = read(sockfd, recvbuff, 1023)) == -1){
    fprintf(stderr, "Read on socket error\n");
    close(sockfd);
    return recvbuff;
  }

  while(isReadDataAvailableOnSocket(sockfd, REQ_TIMEOUT) == 0){
    if( (n = read(sockfd, recvbuff+n, 1023)) == -1){
      fprintf(stderr, "Read on socket error\n");
      close(sockfd);
      return recvbuff;
    }
  }

  close(sockfd);
  return removeheaders(recvbuff);
}

/* 
   Sends an HTTP POST request to the given url, with the given payload
   Returns the HTTP response
*/
char* post(char *url, char* payload){
  int sockfd = socket(AF_INET, SOCK_STREAM, 0);
  char *recvbuff = (char *)malloc(1024);
  struct sockaddr_in serv_addr;
  decoded_url* d_url = decode(url);
  char request[1024];
  
  //Fill up server address information
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_port = htons(d_url->port);
  
  //Convert IPV4 to binary
  if(inet_pton(AF_INET, d_url->host, &serv_addr.sin_addr)<=0){
    perror(strerror(errno));
    close(sockfd);
    return recvbuff;
  }

  //Establish connection to remote server
  if(connect(sockfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0){
    fprintf(stderr, "Error : Connect Failed  %s\n", strerror(errno));
    close(sockfd);
    return recvbuff;
  } 

  // Construct the POST request
  sprintf(request, 
	  "POST /%s HTTP/1.1\r\n"
	  "Host: %s:%d"
	  "Content-Type: text/plain\r\n"
	  "Content-Length: %d\r\n"
	  "\r\n"
	  "%s"
	  ,d_url->path, d_url->host, d_url->port, strlen(payload), payload);

  // Write the request on the socket
  if(write(sockfd, request, strlen(request)) == -1){
    fprintf(stderr, "Write on socket error\n");
    close(sockfd);
    return recvbuff;
  }
  int n=0;
  // Read the response while information is being sent  
  if((n = read(sockfd, recvbuff, 1023)) == -1){
    fprintf(stderr, "Read on socket error\n");
    close(sockfd);
    return recvbuff;
  }

  while(isReadDataAvailableOnSocket(sockfd, REQ_TIMEOUT) == 0){
    if((n = read(sockfd, recvbuff+n, 1023)) == -1){
      fprintf(stderr, "Read on socket error\n");
      close(sockfd);
      return recvbuff;
    }
  }

  close(sockfd);
  return removeheaders(recvbuff);
}

int main(int argc, char *argv[]){
  // Connect to the NXT Robot
  //printf("_______________\nFIRSTPOST\n_______________\n");
  //printf("%s\n",post("http://localhost:8082/connect", "00:16:53:0B:2D:DC"));
  post("http://localhost:8082/connect", "00:16:53:0B:2D:DC");
  // Send the "Hello World" string to the bot
  //printf("_______________\nSECONDPOST\n_______________\n");
  //printf("%s\n",post("http://localhost:8082/utf", "Hello World "));
  post("http://localhost:8082/utf", "Hello World ");
  
  // Retrieve the response from the bot
  //printf("_______________\nGET\n_______________\n");
  printf("%s\n",get("http://localhost:8082/utf"));
  
}
