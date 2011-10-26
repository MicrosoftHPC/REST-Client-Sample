#include "soaclient.h"

const char SESSIONIDTOKEN[] = "Id";
const char BROKERNODE[] = "BrokerNode";
const char APIVERSION[] = "api-version: 2011-11";

char USERPWD[1024];
char USERNAME[1024];
char PASSWORD[1024];
char HOSTNAME[1024];
char BROKERADDR[1024];

const char *get_property(const char *jsonstr, const char *name)
{
	struct json_object *root, *node;
	root = json_tokener_parse(jsonstr);
	node = json_object_object_get(root, name);
	return json_object_to_json_string(node);
}

const char *replace_str(const char *input, const char *source, const char *target)
{
	static char buf[1024];
	char *pos;
	if(!(pos = strstr(input, source)))
		return input;
	strncpy(buf, input, pos - input);
	sprintf(buf + (pos - input), "%s%s", target, pos + strlen(source));
	return buf;
}

int init_client(const char *hostname, const char *username, const char *password)
{
	int err = 0;
	if(!setlocale(LC_CTYPE, "en_US.UTF-8")) 
	{
		fprintf(stderr, "Can't set the locale to UTF-8!");
		err++;
	}
	strncpy(USERNAME, username, strlen(username));
	strncpy(PASSWORD,  password, strlen(password));
	strncpy(HOSTNAME, hostname, strlen(hostname));
	create_authinfo(username, password);
	return err;
}

void create_authinfo(const char *username, const char *password)
{
	int len = strlen(username) + strlen(password) + 2;
	snprintf(USERPWD, len, "%s:%s", username, password);
}

size_t read_callback(void *ptr, size_t size, size_t nmemb, void *userp)
{
	struct HttpRequest * request = (struct HttpRequest *)userp;
	if(size * nmemb < 1)
	{
		return 0;
	}

	if(request->sizeleft) 
	{
		*(char *)ptr = request->readptr[0];
		request->readptr++;
		request->sizeleft--;
		return 1;
	}
	return 0;
}

void init_httpresponse(struct HttpResponse * r)
{
	r->size = 0;
	r->writeptr = malloc(r->size + 1);
	if(r->writeptr == NULL) 
	{
		fprintf(stderr, "malloc() failed\n");
		return;
	}
	r->writeptr[0] = '\0';
}

size_t write_callback(void *ptr, size_t size, size_t nmemb, struct HttpResponse *r)
{
	size_t new_size = size * nmemb;
	r->writeptr = malloc(new_size + 1);
	if(r->writeptr == NULL) 
	{
		fprintf(stderr, "realloc() failed\n");
		return 0;
	}
	memcpy(r->writeptr, ptr, size * nmemb);
	r->size = new_size;
	r->writeptr[new_size] = '\0';
	return size * nmemb;
}

void config_proxy(CURL *curl)
{
	char *proxy;
	if((proxy = getenv("http_proxy")) != NULL)
		curl_easy_setopt(curl, CURLOPT_PROXY, proxy);
	else if((proxy = getenv("HTTP_PROXY")) != NULL)
		curl_easy_setopt(curl, CURLOPT_PROXY, proxy);
}

int create_session()
{
	int session_id = -1;
	CURL *curl;
	CURLcode res;

	const char *username;
	char data[1024];
	/* JSON requires double escape */
	username = replace_str(USERNAME, "\\", "\\\\");
	sprintf(data, "{\"ServiceName\":\"CcpEchoSvc\",\"Username\":\"%s\",\"Password\":\"%s\",\"MaxMessageSize\":2147483647,\"Runtime\":-1}", username, PASSWORD);
	printf("create_session: data = %s\n", data);
	struct HttpRequest request;
	request.readptr = data;
	request.sizeleft = strlen(data);

	struct HttpResponse response;
	init_httpresponse(&response);

	char url[1024];
	sprintf(url, "https://%s/WindowsHPC/HPCCluster/sessions/Create?durable=false", HOSTNAME);

	curl = curl_easy_init();
	if(curl) 
	{
		curl_easy_setopt(curl, CURLOPT_URL, url);
#ifdef _CURL_SSL_PEERVERIFICATION
		curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
#endif
		curl_easy_setopt(curl, CURLOPT_POST, 1L);
		curl_easy_setopt(curl, CURLOPT_READFUNCTION, read_callback);
		curl_easy_setopt(curl, CURLOPT_READDATA, &request);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

#ifdef _CURLDEBUG
		curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
#endif
		curl_easy_setopt(curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
		curl_easy_setopt(curl, CURLOPT_USERPWD, USERPWD);

		config_proxy(curl);

		struct curl_slist *chunk = NULL;
		chunk = curl_slist_append(chunk, "Transfer-Encoding: chunked");
		chunk = curl_slist_append(chunk, "CONTENT-TYPE: application/json; charset=utf-8");
		chunk = curl_slist_append(chunk, "Accept: application/json");
		chunk = curl_slist_append(chunk, APIVERSION);

		res = curl_easy_setopt(curl, CURLOPT_HTTPHEADER, chunk);

		res = curl_easy_perform(curl);
		printf("createsession response: %s\n", response.writeptr);
		printf("result %d\n", res);

		curl_easy_cleanup(curl);

	}
	/* Parse session id */
	const char *str_sessionid;
	str_sessionid = get_property(response.writeptr, SESSIONIDTOKEN);
	session_id = atoi(str_sessionid);
	printf("session %d is created successfully\n", session_id);
	const char *broker_addr;
	broker_addr = get_property(response.writeptr, BROKERNODE);
	strncpy(BROKERADDR, broker_addr + 1, strlen(broker_addr) - 2); // trim double quote
	printf("broker node: %s\n", BROKERADDR);

	return session_id;
}

void attach_session(int session_id)
{
	CURL *curl;
	CURLcode res;

	curl = curl_easy_init();

	struct HttpResponse response;
	init_httpresponse(&response);

	char url[1024];
	sprintf(url, "https://%s/WindowsHPC/HPCCluster/session/%d/Attach?durable=false", HOSTNAME, session_id);
	printf("%s\n", url);
	if(curl)
	{
		printf("begin to attach session\n");
		curl_easy_setopt(curl, CURLOPT_URL, url);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

#ifdef _CURL_SSL_PEERVERIFICATION
		curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
#endif
		curl_easy_setopt(curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
		curl_easy_setopt(curl, CURLOPT_USERPWD, USERPWD);

#ifdef _CURLDEBUG
		curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
#endif
		struct curl_slist *chunk = NULL;
		chunk = curl_slist_append(chunk, "Accept: application/json");
		chunk = curl_slist_append(chunk, APIVERSION);
		res = curl_easy_setopt(curl, CURLOPT_HTTPHEADER, chunk);

		config_proxy(curl);

		res = curl_easy_perform(curl);
		curl_easy_cleanup(curl);
		printf("result code: %d\n", res);
	}
	/* Parse session id */
	const char *str_sessionid;
	str_sessionid = get_property(response.writeptr, SESSIONIDTOKEN);
	session_id = atoi(str_sessionid);
	printf("session %d is attached successfully\n", session_id);
	const char *broker_addr;
	broker_addr = get_property(response.writeptr, BROKERNODE);
	strncpy(BROKERADDR, broker_addr + 1, strlen(broker_addr) - 2); // trim double quote
	printf("broker node: %s\n", BROKERADDR);
}

void close_session(int session_id)
{
	CURL *curl;
	CURLcode res;

	curl = curl_easy_init();
	char url[1024];
	sprintf(url, "https://%s/WindowsHPC/HPCCluster/session/%d/Close", HOSTNAME, session_id);
	printf("%s\n", url);
	if(curl)
	{
		printf("begin to close session\n");
		curl_easy_setopt(curl, CURLOPT_URL, url);
		curl_easy_setopt(curl, CURLOPT_POST, 1L);
#ifdef _CURL_SSL_PEERVERIFICATION
		curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
#endif
		curl_easy_setopt(curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
		curl_easy_setopt(curl, CURLOPT_USERPWD, USERPWD);

#ifdef _CURLDEBUG
		curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
#endif
		struct curl_slist *chunk = NULL;
		chunk = curl_slist_append(chunk, "Accept: application/json");
		chunk = curl_slist_append(chunk, APIVERSION);
		chunk = curl_slist_append(chunk, "Content-Length: 0");
		res = curl_easy_setopt(curl, CURLOPT_HTTPHEADER, chunk);

		config_proxy(curl);

		res = curl_easy_perform(curl);
		curl_easy_cleanup(curl);
		printf("result code: %d\n", res);

	}
}

int construct_request(char **requests, char **userdata, int count, char *content)
{
	int i = 0;
	int len = 0;
	int totallen = 0;
	char *req = malloc(1024);
	char *pos = req;
	memset(req, 0, 1024);
	sprintf(req, "%s\xEF\xBF\xBF%s\x00", requests[i], userdata[i]);
	strncpy(content, req, strlen(req) + 1);
	totallen = strlen(req) + 1;
	for(i = 1; i < count; i++)
	{
		sprintf(req, "%s\xEF\xBF\xBF%s\x00", requests[i], userdata[i]);
		strncpy(&content[totallen], req, strlen(req) + 1);
		totallen += strlen(req) + 1;

	}
	return totallen;
}

void send_request(int session_id, char **requests, char **userdata, int count)
{
	CURL *curl;
	CURLcode res;
	struct HttpRequest request;


	curl = curl_easy_init();
	char url[1024];
	sprintf(url, "https://%s/WindowsHPC/HPCCluster/session/%d/batch/batchid?genericservice=true&commit=true", BROKERADDR, session_id);
	printf("%s\n", url);
	/* construct request */
	char *content = malloc(10240);
	memset(content, 0, 10240);
	int len =  construct_request(requests, userdata, count, content);

	request.readptr = content;
	request.sizeleft = len;
	
	if(curl)
	{
		curl_easy_setopt(curl, CURLOPT_URL, url);
#ifdef _CURL_SSL_PEERVERIFICATION
		curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
#endif

		curl_easy_setopt(curl, CURLOPT_POST, 1L);
		curl_easy_setopt(curl, CURLOPT_READFUNCTION, read_callback);
		curl_easy_setopt(curl, CURLOPT_READDATA, &request);

#ifdef _CURLDEBUG
		curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
#endif
		curl_easy_setopt(curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
		curl_easy_setopt(curl, CURLOPT_USERPWD, USERPWD);

		config_proxy(curl);

		struct curl_slist *chunk = NULL;
		chunk = curl_slist_append(chunk, "Transfer-Encoding: chunked");
		chunk = curl_slist_append(chunk, "CONTENT-TYPE: application/octet-stream");
		chunk = curl_slist_append(chunk, "Accept: application/json");
		chunk = curl_slist_append(chunk, APIVERSION);
		res = curl_easy_setopt(curl, CURLOPT_HTTPHEADER, chunk);

		res = curl_easy_perform(curl);
		printf("result %d\n", res);

		curl_easy_cleanup(curl);
	}
}

void get_response(int session_id)
{
	CURL *curl;
	CURLcode res;

	char url[1024];
	sprintf(url, "https://%s/WindowsHPC/HPCCluster/session/%d/batch/batchid/Response?genericservice=true", BROKERADDR, session_id);
	printf("%s\n", url);
	struct HttpResponse response;
	init_httpresponse(&response);

	curl = curl_easy_init();
	if(curl) 
	{ 
		curl_easy_setopt(curl, CURLOPT_URL, url);
#ifdef _CURL_SSL_PEERVERIFICATION
		curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
#endif

		curl_easy_setopt(curl, CURLOPT_POST, 1L);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

#ifdef _CURLDEBUG
		curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
#endif
		curl_easy_setopt(curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
		curl_easy_setopt(curl, CURLOPT_USERPWD, USERPWD);
		config_proxy(curl);

		struct curl_slist *chunk = NULL;
		chunk = curl_slist_append(chunk, "Accept: application/json");
		chunk = curl_slist_append(chunk, "Content-Length: 0");
		chunk = curl_slist_append(chunk, APIVERSION);
		res = curl_easy_setopt(curl, CURLOPT_HTTPHEADER, chunk);

		res = curl_easy_perform(curl);
		printf("result %d\n", res);

		curl_easy_cleanup(curl);
	}
	/* responses are separated by  \x00 
	 * response is always constructed with three parts:
	 * Action\xffffRequest\xffff\UserData
	 */
	char *start, *end;
	char *p;
	char tmp[1024];
	start = end = response.writeptr;
	while(response.writeptr + response.size > end)
	{
		while(*end != '\x00')
			end++;
		p = strtok(start, "\xEF\xBF\xBF");
		printf("Action: %s\t", p);
		p = strtok(NULL, "\xEF\xBF\xBF");
		printf("response: %s\t", p);
		p = strtok(NULL, "\xEF\xBF\xBF");
		if(p != NULL)
		{
			printf("userdata: %s\n", p);
		} else 
		{
			printf("\n");
		}
		start = ++end;
	}
}


