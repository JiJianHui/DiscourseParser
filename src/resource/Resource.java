package resource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import common.Constants;
import common.util;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import org.dom4j.*;

/**
 * ��Ҫ�����Դ�ļ��غ��ͷ�
 * User: Ji JianHui
 * Time: 2014-02-20 10:11
 * Email: jhji@ir.hit.edu.cn
 */
public class Resource
{
    //public static HashSet<String> ExpConnectivesDict;  //ֻ�Ǵ���˹����ʴʱ�
    //public static HashSet<String> ImpConnectivesDict;

    //ԭʼ��ע��Դ
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation;
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p1;   //����ϵ�µı�ע����ķ�װ
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p2;
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p3;


    //����ʶ������������ȡʱ����Ҫ�õ�����Դ
    public static HashMap<String, Integer> ExpConnWordDict;
    public static HashMap<String, Integer> ImpConnWordDict;
    public static HashMap<String, Integer> NotAsDiscourseWordDict;
    public static HashMap<String, Integer> ExpParallelWordDict;          //������ƽ������

    public static HashMap<String, Integer[]> ConnInP2AndP3; //ÿ�������ھ���ϵ�;��ڹ�ϵ�г��ֵĴ���

    public static LinkedHashMap<String, DSAWordDictItem> allWordsDict;  //�������һ�������ʶ�Ӧ�Ĺ��������͵�������Ϣ
    public static LinkedHashMap<String, String> connectiveRelationDict; //��������ʺ͹�ϵ��ŵĶ�Ӧ��ϵ

    public static HashSet<String> Stop_Words = new HashSet<String>(); //ͣ�ôʴʱ�
    public static HashMap<String, Integer[]> ConnectiveArgNum; //��������arg����:arg-conn-arg��conn-arg-arg
    public static HashMap<String, Integer> Ltp_Xml_Result_SentID_P3; //ltp��������ľ��Ӻ�ID�Ķ�Ӧ��ϵ


    //��ϵʶ��ʱ���ȡ����ʱ����Ҫ�õ�������
    public static HashMap<String, Integer> SentimentDic;      //��дʵ�--���� adj 1 1 NN 7 2
    public static ArrayList<String>        SymWordDict;       //ͬ��ʴʵ�--Aa01A02= ���� ���� ȫ����
    public static ArrayList<String>        TitleWordDict;     //����ʴʵ�--Hj01 /n ����_��ס
    public static HashSet<String>          NegationDict;      //�񶨴ʴʵ�-----����

    public static HashMap<String, Integer> ConnTagInSymCiLin; //����������ͬ��ʴ����еı�ǩ�Լ����ֵĴ���
    public static ArrayList<String>        ConnTagInSymCiLinSorted; //Ϊ��ȷ������������ʱ����һ�µ�
    public static ArrayList<String>        AllWordTagsInSymCiLin; //���д���ͬ��ʴ����еı�ǩ����(ǰ��λ��ǩ)--Aa01

    static
    {
        //���ʴʵ��Լ�����ʶ�����
        ExpConnWordDict          = new HashMap<String, Integer>();
        ImpConnWordDict          = new HashMap<String, Integer>();
        NotAsDiscourseWordDict   = new HashMap<String, Integer>();
        ExpParallelWordDict      = new HashMap<String, Integer>();

        ConnInP2AndP3            = new HashMap<String, Integer[]>();

        allWordsDict             = new LinkedHashMap<String, DSAWordDictItem>();
        connectiveRelationDict   = new LinkedHashMap<String, String>();

        ConnectiveArgNum         = new HashMap<String, Integer[]>();
        Ltp_Xml_Result_SentID_P3 = new HashMap<String, Integer>();


        //ԭʼ��ע�������
        Raw_Train_Annotation     = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p1  = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p2  = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p3  = new LinkedHashSet<SenseRecord>();

        //�����ʵ����---������ʽ��ϵ��ʶ��
        SentimentDic             = new HashMap<String, Integer>();
        SymWordDict              = new ArrayList<String>();
        TitleWordDict            = new ArrayList<String>();
        NegationDict             = new HashSet<String>();
        ConnTagInSymCiLin        = new HashMap<String, Integer>();
        ConnTagInSymCiLinSorted  = new ArrayList<String>();
        AllWordTagsInSymCiLin    = new ArrayList<String>();
    }


    /***
     * ͳһ����������Դ��һ�����ʹ�ã���Ϊĳ����ֻ����Ҫһ�������ݡ�
     * @throws DocumentException
     */
    public static void LoadResource() throws DocumentException, IOException
    {
        System.out.println("[--Info--] Loading Resource....");

        LoadExpConnectivesDict();

        LoadWordRelDict();

        LoadRawRecord();

        LoadParallelWordDict();
    }

    /**
     * ������ʽ�������ֵ�.�ʵ���ֻ�����˳��������ʣ���û�а��������ʳ��ֵĴ���
     */
    public static void LoadExpConnectivesDict() throws IOException
    {
        //��ֹ�ظ�����
        if( ExpConnWordDict.size() > 2 ) return;

        String path = "resource/singWord.txt";
        System.out.println("[--Info--] Loading Single Explicit Connective From: " + path);

        //���ص�������
        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(path, lines);

        for( String line : lines )
        {
            String[] lists = line.split("\t");
            if(lists.length > 2)
            {
                int connNum    = Integer.valueOf(lists[1]);
                int notConnNum = Integer.valueOf(lists[2]);

                //filter the word as needed
                if(connNum == 0) continue;

                ExpConnWordDict.put(lists[0], connNum);
                NotAsDiscourseWordDict.put(lists[0], notConnNum);
            }
        }
    }

    /**
     * ���ز������ʴʱ�
     */
    public static void LoadParallelWordDict() throws IOException
    {
        if( ExpParallelWordDict.size() > 2 ) return;

        //���ز�������
        String path = Constants.ExpParallelWord_Dict_Path;
        System.out.println("[--Info--] Loading Parallel Explicit Connective From: " + path);

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

    /**
     * ��ȡ�����ʺ͹�ϵ��ָʾ�ļ����ļ���ʽΪ(10)
     * ʵ����    2    1    1    1    1    [X]5-1-1    1    [Y]5-1-1    1
     * ÿ�μ��ص��� ������ + �����ͳ����Ϣ
     */
    public static void LoadWordRelDict() throws IOException
    {
        if( allWordsDict.size() > 2 ) return;

        String path = Constants.Connective_Relation_Path;
        System.out.println("[--Info--] Loading Word and Sense Dict From: " + path);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "GBK"));

        String line;
        while( (line = reader.readLine()) != null )
        {
            DSAWordDictItem dsaWord = new DSAWordDictItem(line, true);

            allWordsDict.put( dsaWord.getContent(), dsaWord );
        }
        reader.close();
    }


    /**
     * Ϊ���ж�һ�������������˾��ڹ�ϵ���������˾���ϵ����Ҫ�����ھ���ϵ�;��ڹ�ϵ�еķֲ���
     * ��Դ�ļ�connDistributionInP2P3.txt�����˶�Ӧ������.
     * �洢��ʽΪ�������ʡ���\t������p2�г��ִ�������\t������p3�г��ִ�����
     **/
    public static void LoadConnInP2AndP3() throws IOException, DocumentException
    {
        if( ConnInP2AndP3.size() > 2 ) return;

        String fPath = "resource/connDistributionInP2P3.txt";
        System.out.println("[--Info--] Loading ExpConnectives's Occur's Time In P2 and P3 From " + fPath);

        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(fPath, lines);

        for(String line:lines)
        {
            String[] lists = line.split("\t");
            int wordInP2 = Integer.valueOf(lists[1]);
            int wordInP3 = Integer.valueOf(lists[2]);
            ConnInP2AndP3.put(lists[0], new Integer[]{wordInP2,wordInP3} );
        }
    }

    /**
     * �����°汾�µ�ѵ�����ϣ���ѵ�����ϼ���Ϊ����record
     * @throws DocumentException
     */
    public static void LoadRawRecord() throws DocumentException
    {
        if( Raw_Train_Annotation_p1.size() > 2 ) return;
        System.out.println("[--Info--] Loading Raw Record From: " + Constants.Train_Data_Dir);

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
                try{
                Element sentNode = (Element) ite.next();

                if( sentNode.getName().equals("Sense") )
                {
                    String type  = sentNode.attribute("type").getText();
                    String relNO = sentNode.attribute("RelNO").getText();

                    String source  = sentNode.element("Source").getText();
                    String conWord = sentNode.element("Connectives").element("Content").getText();
                    String wSpan   = sentNode.element("Connectives").element("Span").getText().replace("...", ";");

                    String arg1    = sentNode.element("Arg1").element("Content").getText();
                    String arg2    = sentNode.element("Arg2").element("Content").getText();
                    String annot   = sentNode.element("Annotation").getText();

                    if( conWord.equalsIgnoreCase("null") ) continue;
                    if( wSpan.equalsIgnoreCase("null") ) continue;

                    int[] arg1Position = new int[2];
                    int[] arg2Position = new int[2];

                    String[] lists  = annot.split(" ");
                    arg1Position[0] = Integer.valueOf(lists[0]);
                    arg1Position[1] = Integer.valueOf(lists[1]);

                    arg2Position[0] = Integer.valueOf(lists[0]);
                    arg2Position[1] = Integer.valueOf(lists[1]);

                    //ȥ�����ȹ�������䡣
                    //if( fPath.endsWith(Constants.P3_Ending) && source.length() > Constants.Max_Sentence_Length )
                        //continue;

                    //ȥ��source������˹���ǩ {implicit = ����}
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

                    record.setText( source );
                    record.setConnective(conWord);
                    record.setArg1(arg1);
                    record.setArg2(arg2);
                    record.setAnnotation(annot);
                    record.setfPath(fPath);

                    int lineBeg = util.getLineBeg(annot);
                    int connBeg = Integer.valueOf( wSpan.split(";")[0] );

                    if( connBeg < lineBeg ) record.setText( conWord + record.getText() );

                    record.setConnBeginIndex( connBeg - lineBeg > 0 ? connBeg - lineBeg : 0 );

                    results.add(record);
                }
                }catch(Exception e){ e.printStackTrace();continue; }
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
            Raw_Train_Annotation.addAll(results);
        }
    }


    /**
     * ����ͣ�ôʴʱ�
     * @throws IOException
     */
    public static void LoadStopWords() throws IOException
    {
        if( Stop_Words.size() > 2 ) return;

        System.out.println("[--Info--] Loading Stop Words From: " + Constants.Stop_Word_Path_cn);
        String path = Constants.Stop_Word_Path_cn;

        ArrayList<String> words = new ArrayList<String>();

        util.readFileToLines(path, words);

        for(String word:words)
        {
            Stop_Words.add(word.trim());
        }
    }

    /**
     * ����ltp��������ľ��Ӻͷ�������Ķ�Ӧ��,�ļ��Ĵ����ʽΪ��ID \t ��������
     */
    public static void LoadLtpXMLResultSentID() throws IOException
    {
        System.out.println("[--Info--] Loading LTP XML Result SentID From: " + Constants.Stop_Word_Path_cn);

        String path = Constants.Ltp_XML_Result_P3_SentID;
        ArrayList<String> lines = new ArrayList<String>();

        //���������lines�����ÿһ�ж�������trim
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


    /**
     * �������ʺ�argumentλ������.ÿ�����ʺ�����arg��λ�������������arg-conn-arg��conn-arg-arg
     * ÿһ�ж������˸����ʳ��ֵĲ�ͬ���͵Ĵ���
     **/
    public static void LoadConnectiveArgs() throws IOException
    {
        String fPath = "data/connArgArg.txt";
        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(fPath,lines);

        for(String line:lines)
        {
            String[] lists = line.split("\t");
            String connWord = lists[0];
            int argConnArg = Integer.valueOf(lists[1]);
            int connArgArg = Integer.valueOf(lists[2]);

            ConnectiveArgNum.put(connWord, new Integer[]{argConnArg, connArgArg});
        }
    }

    /**-----------------------------------------��ϵʶ�����-------------------------------------------------**/
    /***
     * ������дʵ�,ֻ���شʺͼ���������ǩ.ÿ�еĸ�ʽΪ��
     * ����  adj  1  1	NN	7	2 //�����Ʊ���ָ�
     * ����ֻ��Ҫ��һ�����ƺ͵�7�еļ�������
     **/
    public static void LoadSentimentDict() throws IOException
    {
        if( SentimentDic.size() > 10 ) return;

        String fPath = "resource/dictionary/sentimentDic.txt";
        System.out.println("[--Info--] Loading Sentiment Dictionary From: " + fPath);

        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(fPath, lines);

        for(String line:lines)
        {
            String[] lists = line.trim().split("\t");

            if(lists.length > 6)
            {
                SentimentDic.put( lists[0], Integer.valueOf(lists[6].trim()) );
            }
        }
    }

    /**
     * ����ͬ��ʴ��֣�ÿһ�еĸ�ʽ���£�
     * Aa01A02= ���� ���� ȫ����  //���տո�ָ�
     * ֱ�ӽ������ݼ��ص��ڴ���
     * **/
    public static void LoadSymWordDict() throws IOException
    {
        if( SymWordDict.size() > 10 ) return;

        String fPath = "resource/dictionary/symwords.txt";
        System.out.println("[--Info--] Loading SymWords Dictionary From: " + fPath);

        util.readFileToLines(fPath, SymWordDict);
    }

    /**
     * ���ش���ʴʵ䣬ͬ��ʴ���ÿ���ʶ��и�����ǩ, ������ʴʵ��б����˸����Ĵ����
     * Hj01 \n ����_��ס
     **/
    public static void LoadTitleWordDict() throws IOException
    {
        if( TitleWordDict.size() > 10 ) return;

        String fPath = "resource/dictionary/Title_third.txt";
        System.out.println("[--Info--] Loading Third_Title Word Dict From: " + fPath);

        util.readFileToLines(fPath, TitleWordDict);
    }

    /**
     * ���ط񶨴ʴʵ䣬ÿһ�д�����һ���񶨴�
     * �� \n ���� \n �о�����
     */
    public static void LoadNegationDict() throws IOException
    {
        if( NegationDict.size() > 10 ) return;

        String fPath = "resource/dictionary/negations.txt";
        System.out.println("[--Info--] Loading Negation Word Dictionary From: " + fPath);

        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(fPath, lines);

        for(String line:lines)
        {
            if(line.trim().length() != 0 ) NegationDict.add( line.trim() );
        }
    }

    /**
     * ���س���������ͬ��ʴ����еı�ǩ���
     */
    /**
    public static void LoadConnCatgInSymCiLin() throws IOException
    {
        if( ConnTagInSymCiLin.size() > 10 ) return;

        String fPath = "/resource/dictionary/ConnWordCagInSymCiLin.txt";
        System.out.println("[--Info--] Loading ConnWord Tag In SymWord CiLin From: " + fPath);

        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(fPath, lines);

        for(String line:lines)
        {
            String[] lists = line.split("\t");
            ConnTagInSymCiLin.put( lists[0], Integer.valueOf(lists[1].trim()) );
        }
    }
    **/

    /**
     * ������ͬ��ʴ����г��ֵ����б�ǩ�������Ϳ���Ϊÿ�����ҵ�һ����ǩ����Ҫ��Ϊ�˼�������ϡ���ԡ�
     * ͬ��ʴ����еĸ�ʽΪ��Aa01B02= Ⱥ�� ���� ���� ���� ���� ���� ǧ��
     * ���ǳ�ȡ���Ǵ���������ʵ�ͬ��ʴ��ֵı�ǩ������������ǩ(Aa01B02)�е�ǰ��λ��Aa01. ����ͬ��ʵ�����Ϊ1428
     */
    public static void LoadAllWordTagsInSymCiLin() throws IOException
    {
        if( AllWordTagsInSymCiLin.size() > 2 ) return;

        System.out.println("[--Info--] Loading All Word Tags In SymWord CiLin");

        Resource.LoadSymWordDict();

        for(String line : Resource.SymWordDict){

            line = line.trim().substring(0, 4);
            if( !AllWordTagsInSymCiLin.contains(line) ) {
                AllWordTagsInSymCiLin.add(line);
            }
        }
    }

    /***
     * ��ȡ����������ͬ��ʴ����еı�ǩ����.
     * ��Ҫ�Ǹ������ʴʵ�ȥѰ��ͬ��ʴ����еı�ǩ���ϣ������ñ�ǩ���ֵ��ܴ������浽�ļ��С�
     * ������ʴʵ䷢���˱仯��Ҫ�ٴ�ʹ�øú������������ʶ�Ӧ��ͬ��ʱ�ǩ���ϡ�
     ***/
    public static void LoadConnWordTagInSymCiLin() throws IOException
    {
        if( ConnTagInSymCiLin.size() > 2 ) return;

        System.out.println("[--Info--] Loading ConnWord Tag In SymWord CiLin" );

        Resource.LoadExpConnectivesDict();
        Resource.LoadSymWordDict();

        //�ж�ÿ�����ʵı�ǩ������
        for(Map.Entry<String, Integer> entry:Resource.ExpConnWordDict.entrySet())
        {
            String curConn = entry.getKey();
            int connNum    = entry.getValue();

            if(connNum < 3) continue;

            String curConnCag = util.getWordTagInSym(curConn);
            if( curConnCag == null ) continue;

            int num = 0;
            if( ConnTagInSymCiLin.containsKey(curConnCag) ) num = ConnTagInSymCiLin.get(curConnCag);

            ConnTagInSymCiLin.put(curConnCag, num+connNum);
        }

        //�����ʱ�ǩ���а����������������ڽ���ת��Ϊsvm������ʱ��
        // ���ʱ�ǩ����������ʱ��Ϳ��԰���������ʵ��е�λ��Ϊ����
        ArrayList<Map.Entry<String,Integer>> tags = util.sortHashMapByKey(ConnTagInSymCiLin, true);

        for(Map.Entry<String, Integer> entry:tags){
            ConnTagInSymCiLinSorted.add(entry.getKey());
        }
    }

    public static void main(String[] args) throws IOException, DocumentException
    {
        Resource.LoadRawRecord();
    }
 }
