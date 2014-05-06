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
 * 主要完成资源的加载和释放
 * User: Ji JianHui
 * Time: 2014-02-20 10:11
 * Email: jhji@ir.hit.edu.cn
 */
public class Resource
{
    //public static HashSet<String> ExpConnectivesDict;  //只是存放了关联词词表
    //public static HashSet<String> ImpConnectivesDict;

    //原始标注资源
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation;
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p1;   //新体系下的标注结果的封装
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p2;
    public static LinkedHashSet<SenseRecord> Raw_Train_Annotation_p3;


    //连词识别：连词特征抽取时候需要用到的资源
    public static HashMap<String, Integer> ExpConnWordDict;
    public static HashMap<String, Integer> ImpConnWordDict;
    public static HashMap<String, Integer> NotAsDiscourseWordDict;
    public static HashMap<String, Integer> ExpParallelWordDict;          //保存了平行连词

    public static HashMap<String, Integer[]> ConnInP2AndP3; //每个连词在句间关系和句内关系中出现的次数

    public static LinkedHashMap<String, DSAWordDictItem> allWordsDict;  //存放了与一个关联词对应的关联词类型的整个信息
    public static LinkedHashMap<String, String> connectiveRelationDict; //存放了连词和关系标号的对应关系

    public static HashSet<String> Stop_Words = new HashSet<String>(); //停用词词表
    public static HashMap<String, Integer[]> ConnectiveArgNum; //连词连接arg类型:arg-conn-arg和conn-arg-arg
    public static HashMap<String, Integer> Ltp_Xml_Result_SentID_P3; //ltp分析结果的句子和ID的对应关系


    //关系识别时候抽取特征时候需要用到的数据
    public static HashMap<String, Integer> SentimentDic;      //情感词典--脏乱 adj 1 1 NN 7 2
    public static ArrayList<String>        SymWordDict;       //同义词词典--Aa01A02= 人类 生人 全人类
    public static ArrayList<String>        TitleWordDict;     //代表词词典--Hj01 /n 生活_居住
    public static HashSet<String>          NegationDict;      //否定词词典-----否则

    public static HashMap<String, Integer> ConnTagInSymCiLin; //常见连词在同义词词林中的标签以及出现的次数
    public static ArrayList<String>        ConnTagInSymCiLinSorted; //为了确保特征索引的时候是一致的
    public static ArrayList<String>        AllWordTagsInSymCiLin; //所有词在同义词词林中的标签集合(前四位标签)--Aa01

    static
    {
        //连词词典以及连词识别相关
        ExpConnWordDict          = new HashMap<String, Integer>();
        ImpConnWordDict          = new HashMap<String, Integer>();
        NotAsDiscourseWordDict   = new HashMap<String, Integer>();
        ExpParallelWordDict      = new HashMap<String, Integer>();

        ConnInP2AndP3            = new HashMap<String, Integer[]>();

        allWordsDict             = new LinkedHashMap<String, DSAWordDictItem>();
        connectiveRelationDict   = new LinkedHashMap<String, String>();

        ConnectiveArgNum         = new HashMap<String, Integer[]>();
        Ltp_Xml_Result_SentID_P3 = new HashMap<String, Integer>();


        //原始标注语料相关
        Raw_Train_Annotation     = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p1  = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p2  = new LinkedHashSet<SenseRecord>();
        Raw_Train_Annotation_p3  = new LinkedHashSet<SenseRecord>();

        //常见词典相关---用于隐式关系的识别
        SentimentDic             = new HashMap<String, Integer>();
        SymWordDict              = new ArrayList<String>();
        TitleWordDict            = new ArrayList<String>();
        NegationDict             = new HashSet<String>();
        ConnTagInSymCiLin        = new HashMap<String, Integer>();
        ConnTagInSymCiLinSorted  = new ArrayList<String>();
        AllWordTagsInSymCiLin    = new ArrayList<String>();
    }


    /***
     * 统一加载所有资源，一般很少使用，因为某部分只是需要一部分数据。
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
     * 加载显式关联词字典.词典中只包含了常见的连词，并没有包含该连词出现的次数
     */
    public static void LoadExpConnectivesDict() throws IOException
    {
        //防止重复加载
        if( ExpConnWordDict.size() > 2 ) return;

        String path = "resource/singWord.txt";
        System.out.println("[--Info--] Loading Single Explicit Connective From: " + path);

        //加载单个连词
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
     * 加载并列连词词表
     */
    public static void LoadParallelWordDict() throws IOException
    {
        if( ExpParallelWordDict.size() > 2 ) return;

        //加载并列连词
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
     * 读取关联词和关系的指示文件，文件格式为(10)
     * 实际上    2    1    1    1    1    [X]5-1-1    1    [Y]5-1-1    1
     * 每次加载的是 关联词 + 后面的统计信息
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
     * 为了判断一个连词是连接了句内关系还是连接了句间关系，需要连词在句间关系和句内关系中的分布。
     * 资源文件connDistributionInP2P3.txt保存了对应的数据.
     * 存储格式为：【连词】【\t】【在p2中出现次数】【\t】【在p3中出现次数】
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
     * 加载新版本下的训练语料，将训练语料加载为各个record
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

                    //去除长度过长的语句。
                    //if( fPath.endsWith(Constants.P3_Ending) && source.length() > Constants.Max_Sentence_Length )
                        //continue;

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
     * 加载停用词词表
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
     * 加载ltp分析结果的句子和分析结果的对应表,文件的存放形式为：ID \t 句子内容
     */
    public static void LoadLtpXMLResultSentID() throws IOException
    {
        System.out.println("[--Info--] Loading LTP XML Result SentID From: " + Constants.Stop_Word_Path_cn);

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


    /**
     * 加载连词和argument位置数据.每个连词和两个arg的位置有两种情况：arg-conn-arg和conn-arg-arg
     * 每一行都代表了该连词出现的不同类型的次数
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

    /**-----------------------------------------关系识别相关-------------------------------------------------**/
    /***
     * 加载情感词典,只加载词和极性两个标签.每行的格式为：
     * 脏乱  adj  1  1	NN	7	2 //按照制表符分割
     * 我们只需要第一列名称和第7列的极性数据
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
     * 加载同义词词林，每一行的格式如下：
     * Aa01A02= 人类 生人 全人类  //按照空格分割
     * 直接将该数据加载到内存中
     * **/
    public static void LoadSymWordDict() throws IOException
    {
        if( SymWordDict.size() > 10 ) return;

        String fPath = "resource/dictionary/symwords.txt";
        System.out.println("[--Info--] Loading SymWords Dictionary From: " + fPath);

        util.readFileToLines(fPath, SymWordDict);
    }

    /**
     * 加载代表词词典，同义词词林每个词都有个类别标签, 而代表词词典中保存了该类别的代表词
     * Hj01 \n 生活_居住
     **/
    public static void LoadTitleWordDict() throws IOException
    {
        if( TitleWordDict.size() > 10 ) return;

        String fPath = "resource/dictionary/Title_third.txt";
        System.out.println("[--Info--] Loading Third_Title Word Dict From: " + fPath);

        util.readFileToLines(fPath, TitleWordDict);
    }

    /**
     * 加载否定词词典，每一行代表了一个否定词
     * 非 \n 否则 \n 感觉不到
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
     * 加载常见连词在同义词词林中的标签组合
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
     * 加载在同义词词林中出现的所有标签，这样就可以为每个词找到一个标签。主要是为了减少数据稀疏性。
     * 同义词词林中的格式为：Aa01B02= 群众 大众 公众 民众 万众 众生 千夫
     * 我们抽取的是代表了这个词的同义词词林的标签，即：整个标签(Aa01B02)中的前四位：Aa01. 最终同义词的种类为1428
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
     * 获取常见连词在同义词词林中的标签集合.
     * 主要是根据连词词典去寻找同义词词林中的标签集合，并将该标签出现的总次数保存到文件中。
     * 如果连词词典发生了变化需要再次使用该函数来生成连词对应的同义词标签集合。
     ***/
    public static void LoadConnWordTagInSymCiLin() throws IOException
    {
        if( ConnTagInSymCiLin.size() > 2 ) return;

        System.out.println("[--Info--] Loading ConnWord Tag In SymWord CiLin" );

        Resource.LoadExpConnectivesDict();
        Resource.LoadSymWordDict();

        //判断每个连词的标签并报错
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

        //对连词标签进行按照名称排序，这样在进行转换为svm特征的时候，
        // 连词标签进行索引的时候就可以按照在这个词典中的位置为索引
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
