#include <getopt.h>
#include "soaclient.h"

int runbasictest(char *hostname, char *username, char *password)
{
	int session_id = -1;
	char *batchid = "batchid";
	char *requests[] = { "req1", "req2", "req3"};
	char *userdata[] = { "userdata1", "userdata2", "userdata3" };

	init_client(hostname, username, password);
	session_id = create_session();
	if(session_id < 0)
	{
		fprintf(stderr, "create_session() failed\n");
		return -1;
	}
	send_request(session_id, batchid, &requests[0], &userdata[0], 3, true);
	get_response(session_id, batchid, "", "", -1, true);
	get_batchstatus(session_id, batchid);
	close_session(session_id);
	printf("BasicTest passed\n");
	return 0;
}

int runpartialsendtest(char *hostname, char *username, char *password)
{
	int session_id = -1;
	int i = 0;
	char *batchid = "batchid";
	char *requests[] = { "req1", "req2", "req3"};
	char *userdata[] = { "userdata1", "userdata2", "userdata3" };
	init_client(hostname, username, password);
	session_id = create_session();
	if(session_id < 0)
	{
		fprintf(stderr, "create_session failed\n");
		return -1;
	}
	send_request(session_id, batchid, requests, userdata, 3, false);
	send_request(session_id, batchid, requests, userdata, 3, false);
	end_requests(session_id, batchid);
	get_response(session_id, batchid, "", "", -1, true);
	close_session(session_id);
	printf("PartialSendTest passwd\n");
	return 0;
}

int runpartialsendtest2(char *hostname, char *username, char *password)
{
	int session_id = -1;
	int i = 0;
	char *batchid = "batchid";
	char *requests[] = { "req1", "req2", "req3"};
	char *userdata[] = { "userdata1", "userdata2", "userdata3" };
	init_client(hostname, username, password);
	session_id = create_session();
	if(session_id < 0)
	{
		fprintf(stderr, "create_session failed\n");
		return -1;
	}
	send_request(session_id, batchid, requests, userdata, 3, false);
	send_request(session_id, batchid, requests, userdata, 3, true);
	get_response(session_id, batchid, "", "", -1, true);
	close_session(session_id);
	printf("PartialSendTest2 passed\n");
	return 0;
}

int runmultibatchtest(char *hostname, char *username, char *password)
{
	int session_id = -1;
	char *batch1 = "batch1";
	char *batch2 = "batch2";
	char *requests[] = { "req1", "req2", "req3"};
	char *userdata[] = { "userdata1", "userdata2", "userdata3" };
	init_client(hostname, username, password);
	session_id = create_session();
	if(session_id < 0)
	{
		fprintf(stderr, "create_session failed\n");
		return -1;
	}
	send_request(session_id, batch1, requests, userdata, 3, true);
	get_response(session_id, batch1, "", "", -1, true);
	purge_batch(session_id, batch1);
	send_request(session_id, batch2, requests, userdata, 3, true);
	get_response(session_id, batch2, "", "", -1, true);
	purge_batch(session_id, batch2);
	close_session(session_id);
	printf("MultiBatch test passed\n");
	return 0;
}

int runpartialgetresponsetest(char *hostname, char *username, char *password)
{
	int session_id = -1;
	int i = 0;
	int total= 100;
	int batchsize = 10;
	char *batchid = "batchid";
	char *request[total];
	char *userdata[total];
	for(; i < total; i++) 
	{
		request[i] = malloc(1024);
		sprintf(request[i], "req%d", i);
		userdata[i] = malloc(1024);
		sprintf(userdata[i], "userdata%d", i);
	}
	init_client(hostname, username, password);
	session_id = create_session();
	send_request(session_id, batchid, request, userdata, total, true);
	get_response(session_id, batchid, "", "", batchsize, true);
	i = 1;
	for(; i < 9; i++)
	{
		get_response(session_id, batchid, "", "", batchsize, false);
	}
	close_session(session_id);
	return 0;
}

int runattachsessiontest(char *hostname, char *username, char *password)
{
	int session_id = -1;
	init_client(hostname, username, password);
	session_id = create_session();
	if(session_id < 0)
	{
		fprintf(stderr, "create_session() failed\n");
		return -1;
	}
	attach_session(session_id);
	close_session(session_id);
	printf("AttachSessionTest passed\n");
	return 0;
}

int main(int argc, char **argv)
{
	
	char *username; 
	char *password;
	char *hostname;

	/* Parse input argument */
	int c;
	while((c = getopt(argc, argv, "u:p:")) != -1)
	{
		switch(c)
		{
			case 'u':
				username = optarg;
				break;
			case 'p':
				password = optarg;
				break;
			case '?':
				fprintf(stderr, "soaclient -u username -p password hostname\n");
				return 1;
			default:
				abort();
		}
	}

	hostname = argv[optind];
	runbasictest(hostname, username, password);
	runpartialsendtest(hostname, username, password);
	/*
	runpartialsendtest2(hostname, username, password);
	runmultibatchtest(hostname, username, password);
	runpartialgetresponsetest(hostname, username, password);
	runattachsessiontest(hostname, username, password);
	*/
	return 0;
}
