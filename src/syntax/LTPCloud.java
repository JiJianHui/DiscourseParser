package syntax;

/*
 * This example shows how to use Java to build http connection and request
 * the ltp-cloud service for perform full-stack Chinese language analysis
 * and get results in specified formats
 */
import entity.train.SenseRecord;
import org.dom4j.DocumentException;
import resource.Resource;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class LTPCloud
{
    public static String analysis(String text)
    {
        try
        {
            String api_key = "V988t0mL6QZiUjaOhvePdGhvtX0c1PII5cphZxfN";
            text = URLEncoder.encode(text, "utf-8");

            String pattern = "all";
            String format  = "xml";

            URL url = new URL("http://api.ltp-cloud.com/analysis/?"
                    + "api_key=" + api_key + "&"
                    + "text="    + text    + "&"
                    + "format="  + format  + "&"
                    + "pattern=" + pattern);

            URLConnection conn = url.openConnection();
            conn.connect();

            BufferedReader innet = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));

            String xmlResult = "", line = "";

            while ( (line = innet.readLine()) != null )
            {
                xmlResult += line + "\n";
            }

            innet.close();

            return xmlResult;
        }
        catch ( Exception e )
        {
            System.err.println("Error : " + e.getMessage() );
            System.err.println(text);
            return null;
        }
    }

    /**
     * ��p3��ltp���ӷ����������Ϊxml���Ա�����������˷���ֻ���ڳ�ʼ����ʱ������һ�Ρ�
     * @throws DocumentException
     * @throws IOException
     */
    public static void getResult() throws DocumentException, IOException, InterruptedException
    {
        int id = 0;
        FileWriter fw = new FileWriter( new File("./data/p3/sentID.txt") );

        Resource.LoadRawRecord();

        for(SenseRecord record : Resource.Raw_Train_Annotation_p3)
        {
            id++;
            String sentContent = record.getText();
            //System.out.println(id + sentContent);

            String xmlResult   = LTPCloud.analysis(sentContent);

            int times = 0;
            while( xmlResult == null || xmlResult.length() == 0 )
            {
                times++;
                xmlResult = LTPCloud.analysis(sentContent);

                //�������ζ�����Ļ�
                if(times > 3)
                {
                    System.err.println("[Error] " + sentContent );
                    continue;
                }
            }

            fw.write(String.valueOf(id) + "\t" + sentContent + "\r\n");

            FileWriter xmlWrite = new FileWriter( new File("./data/p3/sent" + id + ".xml") );
            xmlWrite.write(xmlResult);
            xmlWrite.close();

            Thread.sleep(1000);
        }

        fw.close();
    }

    public static void main(String[] args) throws Exception
    {

        String text    = "���� ���� ���� ���� �� ��ĸ ̫�� ���� �� �� ˵�� ��ĸ �� �� �� �� Χ¯ �� �� ���� �� ���� ��� һ�� �ð� ��� ��" +
                         "����Ϊ�� ���� ���� ���� �� ���� �� СҰ Ҳ ѧ ������ ���ס�";


        //System.out.println( LTPCloud.analysis(text) );

        LTPCloud.getResult();

    }
}