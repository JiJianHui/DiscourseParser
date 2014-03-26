import entity.DSAConnective;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Flex前端处理的后台程序，用于接收Flex前端java远程对象的请求。
 * @author rainbow
 * @time   Mar 14, 2013
 */
public class WebServer
{

    //private static Tms tms = null;
    private static DiscourseParser dsaParser = null;



    public void run() throws Exception
    {
        System.out.println("Start server...");

        //tms = new Tms();
        dsaParser = new DiscourseParser();	//后台处理程序

        ServerSocket serverSocket = new ServerSocket(8090);
        System.out.println("Server is listening to 8090 port:");

        while (true)
        {
            //监听客户端的请求
            Socket client = serverSocket.accept();

            InputStreamReader cIn = new InputStreamReader(client.getInputStream());
            BufferedReader reader = new BufferedReader(cIn);

            String line   = reader.readLine();

            System.out.println("get a query:" + line);

            String response = "";
            //ArrayList<DSAConnective> result = dsaParser.run(line);
            ArrayList<DSAConnective> result = null;

            for(DSAConnective connective:result)
            {
                response = response + connective.getStringContent()+"\n";
            }

            System.out.println(result);
            PrintStream print = new PrintStream(client.getOutputStream());

            print.println(response);
            print.close();

            client.close();
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