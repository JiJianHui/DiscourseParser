import common.Cache;
import common.Constants;
import common.util;
import entity.DSAConnective;
import entity.DSAParagraph;
import entity.DSASentence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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

    public static void main(String[] args)
    {
        try
        {
            WebServer server = new WebServer();

            server.run();
        }
        catch (Exception e)
        {
            System.out.println("Start Server failed.");
            e.printStackTrace();
        }
    }

}