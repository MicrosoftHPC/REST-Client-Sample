#include <getopt.h>
#include "soaclient.h"


int main(int argc, char **argv)
{
	int session_id = -1;
	char *requests[] = { "req1", "req2", "req3"};
	char *userdata[] = { "userdata1", "userdata2", "userdata3" };
	
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


	init_client(hostname, username, password);
	session_id = create_session();
	if(session_id < 0)
	{
		fprintf(stderr, "create_session() failed\n");
		return 1;
	}
	send_request(session_id, &requests[0], &userdata[0], 3);
	get_response(session_id);
	close_session(session_id);
	return 0;
}
