package resource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import common.Constants;
import common.util;
import entity.DSAWordDictItem;
import entity.SenseRecord;
import org.dom4j.*;

/**
 * 主要完成资源的加载和释放
 * User: Ji JianHui
 * Time: 2014-02-20 10:11
 * Email: jhji@ir.hit.edu.cn
 */
public class Resource
{
    public static HashSet<String> ExpConnectivesDict;  //只是存放了关联词词表
    public static HashSet<String> ImpConnectivesDict;

    public static HashMap<String, Integer> ExpParallelWordDict;  //保存了平行连词

    public static LinkedHashMap<String, DSAWordDictItem> allWordsDict;     //存放了与一个关联词对应的关联词类型的整个信息
    public static LinkedHashMap<String, String> connectiveRelationDict;    //存放了连词和关系标号的对应关系

    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p1;   //新体系下的标注结果的封装
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p2;
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p3;

    public static HashSet<String> Stop_Words = new HashSet<String>();   //停用词词表


    public static HashMap<String, Integer> Ltp_Xml_Result_SentID_P3;  //ltp分析结果的句子和ID的对应关系

    static
    {
        ExpConnectivesDict       = new HashSet<String>();
        ImpConnectivesDict       = new HashSet<String>();
        ExpParallelWordDict      = new HashMap<String, Integer>();

        allWordsDict             = new LinkedHashMap<String, DSAWordDictItem>();
        connectiveRelationDict   = new LinkedHashMap<String, String>();

        Raw_Train_Annotation_p1  = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p2  = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p3  = new LinkedHashSet<SenseRecord>();

        Ltp_Xml_Result_SentID_P3 = new HashMap<String, Integer>();
    }


    public static void LoadResource() throws DocumentException
    {
        System.out.println("[--Info--]: Loading Resource....");

        LoadExpConnectivesDict();

        LoadWordRelDict();

        LoadRawRecord();

        LoadParallelWordDict();
    }

    /**
     * 加载显式关联词字典.词典中只包含了常见的连词，并没有包含该连词出现的次数
     */
    public static void LoadExpConnectivesDict()
    {
        //防止重复加载
        if( ExpConnectivesDict.size() > 2 ) return;

        String path = Constants.ExpConWord_Dict_Path;

        try
        {
            //加载单个连词
            System.out.println("[--Info--]: Loading Single Explicit Connective From: " + path);
            ArrayList<String> lines = new ArrayList<String>();
            util.readFileToLines(path, lines);

            for( String line : lines )
            {
                line = line.trim();
                if(line.length() > 0) ExpConnectivesDict.add( line );
            }
        }
        catch (Exception e)
        {
            System.err.println("Sorry, Load resource Failed. Details:\n" + path);
            System.exit(1);
        }
    }

    /**
     * 加载并列连词词表
     */
    public static void LoadParallelWordDict()
    {
        try
        {
            //加载并列连词
            String path = Constants.ExpParallelWord_Dict_Path;
            System.out.println("[--Info--]: Loading Parallel Explicit Connective From: " + path);

            ArrayList<String> paraLines = new ArrayList<String>();
            util.readFileToLines(path, paraLines);

            for(String line : paraLines)
            {
                String[] lists = line.trim().split("\t");

                String wContent = lists[0].replace("...", ";");
                Integer wNum    = Integer.valueOf( lists[1] );

                ExpParallelWordDict.put(wContent, wNum);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 读取关联词和关系的指示文件，文件格式为(10)
     * 实际上	2	1	1	1	1	[X]5-1-1	1	[Y]5-1-1	1
     * 每次加载的是 关联词 + 后面的统计信息
     */
    public static void LoadWordRelDict()
    {
        System.out.println("[--Info--]: Loading Word and Sense Dict From: " + Constants.Connective_Relation_Path);

        if( allWordsDict.size() > 2 ) return;

        String path = Constants.Connective_Relation_Path;
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "GBK"));
            String line;

            while( (line = reader.readLine()) != null )
            {
                DSAWordDictItem dsaWord = new DSAWordDictItem(line, true);

                allWordsDict.put( dsaWord.getContent(), dsaWord );
            }
            reader.close();
        }
        catch (Exception e)
        {
            System.err.println("Sorry, Load resource Failed. Details:\n" + path);
            System.exit(1);
        }
    }



    /**
     * 加载新版本下的训练语料，将训练语料加载为各个record
     * @throws DocumentException
     */
    public static void LoadRawRecord() throws DocumentException
    {
        System.out.println("[--Info--]: Loading Raw Record From: " + Constants.Train_Data_Dir);

        if( Raw_Train_Annotation_p1.size() > 2 ) return;

        String dir = Constants.Train_Data_Dir;
        ArrayList<String> files = new ArrayList<String>();

        util.getFiles(dir, files, Constants.P1_Ending);
        util.getFiles(dir, files, Constants.P2_Ending);
        util.getFiles(dir, files, Constants.P3_Ending);

        for(String fPath:files)
        {
            String content = util.readFileToString(fPath).replaceAll("\r\n", "");
            Document domObj = DocumentHelper.parseText(content);

            Element rootNode = domObj.getRootElement();

            HashSet<SenseRecord> results = new HashSet<SenseRecord>();

            for(Iterator ite = rootNode.elementIterator(); ite.hasNext();)
            {
                Element sentNode = (Element) ite.next();

                if( sentNode.getName().equals("Sense") )
                {
                    String type  = sentNode.attribute("type").getText();
                    String relNO = sentNode.attribute("RelNO").getText();

                    String source  = sentNode.element("Source").getText();
                    String conWord = sentNode.element("Connectives").element("Content").getText();

                    String arg1    = sentNode.element("Arg1").element("Content").getText();
                    String arg2    = sentNode.element("Arg2").element("Content").getText();
                    String annot   = sentNode.element("Annotation").getText();

                    //去除长度过长的语句。
                    if( source.length() > Constants.Max_Sentence_Length ) continue;

                    //去除source里面的人工标签 {implicit = 而且}
                    int begIndex = source.indexOf("{implicit =");
                    if( begIndex != -1 )
                    {
                        int endIndex = source.indexOf("}", begIndex);
                        if( endIndex!= -1 )
                        {
                            source = source.substring(0, begIndex) + source.substring(endIndex + 1);
                        }
                    }

                    SenseRecord record = new SenseRecord(type, relNO);

                    record.setText( util.removeAllBlank(source));
                    record.setConnective(conWord);
                    record.setArg1(arg1);
                    record.setArg2(arg2);
                    record.setAnnotation(annot);

                    results.add(record);
                }
            }

            if( fPath.endsWith(Constants.P1_Ending) )
            {
                Raw_Train_Annotation_p1.addAll(results);
            }
            else if( fPath.endsWith(Constants.P2_Ending) )
            {
                Raw_Train_Annotation_p2.addAll(results);
            }
            else{
                Raw_Train_Annotation_p3.addAll(results);
            }
        }
    }


    /**
     * 加载停用词词表
     * @throws IOException
     */
    public static void LoadStopWords() throws IOException
    {
        System.out.println("[--Info--]: Loading Stop Words From: " + Constants.Stop_Word_Path_cn);
        String path = Constants.Stop_Word_Path_cn;

        ArrayList<String> words = new ArrayList<String>();

        util.readFileToLines(path, words);

        for(String word:words)
        {
            Stop_Words.add(word.trim());
        }
    }

    /**
     * 加载ltp分析结果的句子和分析结果的对应表,文件的存放形式为：ID \t 句子内容
     */
    public static void LoadLtpXMLResultSentID() throws IOException
    {
        System.out.println("[--Info--]: Loading LTP XML Result SentID From: " + Constants.Stop_Word_Path_cn);

        String path = Constants.Ltp_XML_Result_P3_SentID;
        ArrayList<String> lines = new ArrayList<String>();

        //工具里面对lines里面的每一行都进行了trim
        util.readFileToLines(path, lines);

        for( String line : lines )
        {
            String[] lists = line.split("\t");

            Integer id      = Integer.valueOf(lists[0]);
            String  content = lists[1].trim();

            Integer result = Ltp_Xml_Result_SentID_P3.put(content, id);

            if( result != null )
            {
                //System.err.println("[--Error--] Same Sentence: " + content + "\r\n");
            }
        }

    }
}
