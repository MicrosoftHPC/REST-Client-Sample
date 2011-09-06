

public class SoaWebApiClientTest {
    
    public static void main(String[] args) {
        String[] requests = { "Request1", "Request2", "Request3" };
        String[] userdata = { "UserData1", "UserData2", "UserData3" };
        String response = null;
        SoaWebApiClient client = new SoaWebApiClient("newang1.cloudapp.net", "MyGenericService", "admin", "!!123abc");
        client.createSession();
        client.sendRequest(requests, userdata);
        response = client.getResponse();
        client.closeSession();
    }

}
