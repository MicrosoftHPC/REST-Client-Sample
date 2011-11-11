#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <locale.h>
#include <regex.h>
#include <curl/curl.h>
#include <json/json.h>

//#define _CURLDEBUG
#define _CURL_SSL_PEERVERIFICATION
#define _HTTPPROXY

struct HttpRequest
{
	const char *readptr;
	size_t sizeleft;
};

struct HttpResponse
{
	char *writeptr;
	size_t size;
};

/* Get property value by name from json object */
const char *get_property(const char *jsonstr, const char *name);

/* Initialize http response struct */
void init_httpresponse(struct HttpResponse *r);

/* Create http basic authentication information. Format username:password */
void create_authinfo(const char *username, const char *password);

/* Construct SOA generic request string from request and user data array
 * Format: request1 0xffff userdata1 0x0000 request2 0xffff userdata2
 */
int construct_request(char **requests, char **userdata, int count, char *content);

/* Read callback function to send http request through libcurl */
size_t read_callback(void *ptr, size_t size, size_t nmemb, void *userp);

/* Write callback function to get http response from libcurl */
size_t write_callback(void *ptr, size_t size, size_t nmemb, struct HttpResponse *r);

/* Initial SOA WebApi client */
int init_client(const char *hostname, const char *username, const char *password);

/* Create a SOA session, return valid session id if succeded */
int create_session();

/* Close an active SOA session by session id */
void close_session(int session_id);

/* Send requests w/ userdata to a SOA session */
void send_request(int session_id, char *batchid, char **requests, char **userdata, int count, bool commit);

/* Get response from a SOA session */
void get_response(int session_id, char *batchid, char *action, char *clientdata, int count, bool reset);

/* End requests after sending requests operation is completed */
void end_requests(int sessionid, char *batchid);

/* Purge batch after retrieving response operation is completed */
void purge_batch(int session_id, char *batch_id);

/* Get current batch status */
char *get_batchstatus(int session_id, char *batch_id);

/* Get cluster name */
char *get_clustername();



