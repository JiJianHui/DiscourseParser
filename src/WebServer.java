import common.Cache;
import entity.recognize.DSAParagraph;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Flexǰ�˴���ĺ�̨�������ڽ���Flexǰ��javaԶ�̶��������
 * @author rainbow
 * @time   Mar 14, 2013
 */
public class WebServer
{

    //private static Tms tms = null;
    private  DiscourseParser dsaParser;
    private  Cache cache;

    public void run() throws Exception
    {
        System.out.println("Start server...");

        //tms = new Tms();
        dsaParser = new DiscourseParser();	//��̨�������
        cache     = new Cache();

        ServerSocket serverSocket = new ServerSocket(8090);
        System.out.println("Server is listening to 8090 port:");

        while (true)
        {
            try
            {
                //�����ͻ��˵�����
                Socket client = serverSocket.accept();

                InputStreamReader cIn = new InputStreamReader(client.getInputStream());
                BufferedReader reader = new BufferedReader(cIn);

                String line = reader.readLine();
                System.out.println("get a query:" + line);

                String response;
                boolean needSegment;

                if( line.startsWith("0") ) needSegment = false;
                else needSegment = true;

                line = line.substring(1);

                //��������ͻ��˵�����
                if( cache.get(line) != null ) {
                    response = cache.get(line);
                }
                else{

                    DSAParagraph paragraph = dsaParser.parseRawFile(line, needSegment);
                    response = paragraph.toXML();
                    cache.put(line, response);
                }

                //����XML���
                PrintStream print = new PrintStream(client.getOutputStream());

                print.println(response);
                print.close();

                System.out.println(response);

                client.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void connectToClient() throws IOException
    {
        ServerSocket serverSocket = new ServerSocket(8090);
        System.out.println("Server is listening to 8090 port:");

        Socket client = serverSocket.accept();

        System.out.println("Accept the Client: " + client);

        //����IO���

        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter   out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);

        String str = "hhaha";
        System.out.println("In Server reveived the info: " );
        out.println("<html>hhah</html>");

        serverSocket.close();
    }

    public static void main(String[] args)
    {
        try
        {
            WebServer server = new WebServer();

            server.run();
            //server.connectToClient();
        }
        catch (Exception e)
        {
            System.out.println("Start Server failed.");
            e.printStackTrace();
        }
    }

}