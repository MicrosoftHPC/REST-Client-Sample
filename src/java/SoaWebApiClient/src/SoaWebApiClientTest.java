import java.util.ArrayList;
import java.util.List;



public class SoaWebApiClientTest {
    
    private static String headnode = "soatest.cloudapp.net";
    private static String username = "admin";
    private static String password = "!!123abc";
    private static String servicename = "CcpEchoSvc";
    
    public static void main(String[] args) {
        runBasicTest();
        runMultiBatchTest();
        runPartialSendRequestTest();
        runPartialSendRequestTest2();
        runPartialGetResponseTest();
        runAttachSessionTest();
    }
    
    private static void runBasicTest() {
        String[] requests = { "Request1", "Request2", "Request3" };
        String[] userdata = { "UserData1", "UserData2", "UserData3" };
        String response = null;
        SoaWebApiClient client = null;

        try {
            client = new SoaWebApiClient(headnode, servicename, username, password);
            client.createSession();
            client.sendRequest(requests, userdata);
            client.getBatchStatus();
            response = client.getResponse();
            client.getBatchStatus();
            client.closeSession();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("runBasicTest failed");
            return;
        }
        System.out.println("runBasicTest passed");
    }
    
    private static void runPartialSendRequestTest() {
        String response = null;
        List<String> requests = new ArrayList<String>();
        List<String> userdata = new ArrayList<String>();
        SoaWebApiClient client = null;
        try {
            client = new SoaWebApiClient(headnode, servicename, username, password);
            client.createSession();
            for(int i = 0; i < 10; i++) {
                requests.clear();
                userdata.clear();
                for(int j = 0; j < 10; j++) {
                    requests.add(String.format("Request%d_%d", i, j));
                    userdata.add(String.format("UserData%d_%d", i, j));
                }
                client.sendRequest(requests.toArray(new String[0]), userdata.toArray(new String[0]), false);
            }
            client.getBatchStatus();
            client.endRequests();
            response = client.getResponse();
            client.getBatchStatus();
            client.purgeBatch();
            client.closeSession();
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("runPartialSendRequestTest failed");
            return;
        }
        System.out.println("runPartialSendRequestTest passed");
    }
    
    private static void runPartialSendRequestTest2() {
        String response = null;
        int partialSendCount = 10;
        List<String> requests = new ArrayList<String>();
        List<String> userdata = new ArrayList<String>();
        SoaWebApiClient client = null;
        try {
            client = new SoaWebApiClient(headnode, servicename, username, password);
            client.createSession();
            for(int i = 0; i < partialSendCount; i++) {
                requests.clear();
                userdata.clear();
                for(int j = 0; j < 10; j++) {
                    requests.add(String.format("Request%d_%d", i, j));
                    userdata.add(String.format("UserData%d_%d", i, j));
                }
                client.sendRequest(requests.toArray(new String[0]), 
                        userdata.toArray(new String[0]), 
                        i == (partialSendCount -1) ? true : false // commit on the last send request
                                );
            }
            response = client.getResponse();
            client.purgeBatch();
            client.closeSession();
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("runPartialSendRequestTest2 failed");
            return;
        }
        System.out.println("runPartialSendRequestTest2 passed");
    }
    
    private static void runMultiBatchTest() {
        String[] requests = { "Request1", "Request2", "Request3" };
        String[] userdata = { "UserData1", "UserData2", "UserData3" };
        String response = null;
        SoaWebApiClient client = null;
        try {
            client = new SoaWebApiClient(headnode, servicename, username, password);
            client.createSession();
            client.sendRequest(requests, userdata, "batch1");
            response = client.getResponse("batch1");
            client.purgeBatch("batch1");
            client.sendRequest(requests, userdata, "batch2");
            response = client.getResponse("batch2");
            client.purgeBatch("batch2");
            client.closeSession();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("runMultiBatchTest failed");
            return;
        }
        System.out.println("runMultiBatchTest passed");
    }
    
    private static void runPartialGetResponseTest() {
        String response = null;
        int partialGetResponseCount = 10;
        int batchSize = 10;
        List<String> requests = new ArrayList<String>();
        List<String> userdata = new ArrayList<String>();
        for(int i = 0; i < 100; i++) {
            requests.add(String.format("Request%d", i));
            userdata.add(String.format("UserData%d", i));
        }
        SoaWebApiClient client= null;
        try {
            client = client = new SoaWebApiClient(headnode, servicename, username, password);
            client.createSession();
            client.sendRequest(requests.toArray(new String[0]), 
                    userdata.toArray(new String[0]));
            client.getResponse(batchSize, true);
            for(int i = 0; i < partialGetResponseCount; i++) {
                System.out.println(String.format("Get %d batch of response...", i));
                response = client.getResponse(batchSize, false);
            }
            client.purgeBatch();
            client.closeSession();
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("runPartialGetResponseTest failed");
            return;
        }
        System.out.println("runPartialGetResponseTest passed");
    }
    
    private static void runAttachSessionTest() {
        SoaWebApiClient client = null;
        SoaWebApiClient client2 = null;
        try {
            client = new SoaWebApiClient(headnode, servicename, username, password);
            client2 = new SoaWebApiClient(headnode, servicename, username, password);
            int sessionId = client.createSession();
            client2.attachSession(sessionId);
            client2.closeSession();
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("runAttachSessionTest failed");
            return;
        }
        System.out.println("runAttachSessionTest passed");
    }

}
