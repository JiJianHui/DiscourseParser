package common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * 静态变量存放地点，存放程序中使用到的静态变量，以及固定不变的量。
 * User: Ji JianHui
 * Time: 2014-02-19 14:53
 * Email: jhji@ir.hit.edu.cn
 */
public class Constants
{
    public static Integer EPSION                  = 3;         //距离判定准确粒度
    public static Integer MAX_Buffer_Size         = 1024;      //程序中的缓冲区的大小
    public static Integer MIN_Sentence_Length     = 5;         //最小的句子识别单元
    public static String  Parallel_Word_Seperator = ";";       //并列连词的分隔符

    public static Integer Max_Sentence_Length     = 150;


    //-----------------------------句子分割相关-------------------------------------------
    public static ArrayList<String> Sentence_Seperators;    //保存了所有的句子分隔符。
    static{
        Sentence_Seperators = new ArrayList<String>();
        Sentence_Seperators.add("。");   Sentence_Seperators.add("？");
        Sentence_Seperators.add("！");   Sentence_Seperators.add(".");
        Sentence_Seperators.add("?");    Sentence_Seperators.add("!");
        //Sentence_Seperators.add("…");
    }

    //句子识别时候的分隔符的正则表达式
    public static String   Sentence_Seperators_Regx = "[。？！.?!…]";

    //中文识别正则表达式
    public static Pattern  Chinese_Word_Pattern = Pattern.compile("[\\u4e00-\\u9fa5]");

    //句子边界识别正则表达式
    public static Pattern  Sentence_Seperators_Pattern = Pattern.compile(Sentence_Seperators_Regx);


    //一个句子拆分为不同的argument时候的分隔符
    public static Pattern Sentence_Element_Pattern = Pattern.compile("[,;，；]");


    //------------------------原始语料相关-------------------------------------
    public static String P2_Ending = ".p2";
    public static String P3_Ending = ".p3";
    public static String P1_Ending = ".p1";

    //最原始的标注语料集合，主要用于基础的数据分析
    public static String Raw_Annotation_Data = "F:\\Corpus Data\\check";

    //转换格式后的语料路径
    //public static String Train_Data_Dir = "F:\\Distribution Data\\Distribution Data HIT\\Test";
    public static String Train_Data_Dir = "F:\\Distribution Data\\Distribution Data HIT\\Corpus Data\\XML";


    //中文停用词词表,注意此处的停用词词表中也把中文的连词当做了停用词
    public static String Stop_Word_Path_cn = "/resource/dictionary/Stopwords_cn_1208.txt";

    //ltp分析结果是独立保存的，sentID保存了每个句子对应的编号。这样就可以找到对应的处理结果
    public static String Ltp_XML_Result_P3        = "data/p3";
    public static String Ltp_XML_Result_P3_SentID = "data/p3/sentID.txt";


    //------------------------机器学习相关-------------------------------------
    public static Integer Labl_Not_ConnWord  = 0;
    public static Integer Labl_is_ConnWord  = 1;

    public static Integer ConnRecoganize_ML   = 1;
    public static Integer ConnRecoganize_Rule = 0;

    public static Integer Label_Inter_ConnWord = 1;
    public static Integer Label_Cross_ConnWord = 2;

    //抽取样本时需要过滤掉的词性:人名+地名+机构名 + 位置名 + 代词 + 外文次 + 标点共七种
    //public static String[] Ignore_PosTags = {"nh", "ni", "nl", "ns", "r", "ws", "wp"};
    public static HashSet<String> Ignore_PosTags = new HashSet<String>();
    static
    {
        Ignore_PosTags.add("nh");  Ignore_PosTags.add("ni");
        Ignore_PosTags.add("nl");  Ignore_PosTags.add("ns");
        Ignore_PosTags.add("nd");  Ignore_PosTags.add("nz");

        Ignore_PosTags.add("r");

        Ignore_PosTags.add("q");   Ignore_PosTags.add("a");

        Ignore_PosTags.add("ws");  Ignore_PosTags.add("wp");
    }

    //特征定义：词性特征,共28种
    public static int posTagsNum   = 28;
    public static String[] posTags = {"a", "b", "c", "d", "e", "g", "h",
                                      "i", "j", "k", "m","n", "nd", "nh",
                                      "ni", "nl", "ns", "nt", "nz", "o", "p",
                                      "q", "r", "u", "v", "wp", "ws", "x"};


    public static String[] ansjPosTags = {
            "n", "nr", "nr1", "nr2", "nrj", "nrf", "ns", "nsf", "nt", "nz", "nl", "ng", "nw", //名词
            "t", "tg",
            "s", "f",
            "v", "vd", "vn", "vshi", "vyou", "vf", "vx", "vi", "vl", "vg",
            "a", "ad", "an", "ag", "al",
            "b", "bl",
            "z",
            "r", "rr", "rz", "rzt", "rzs", "rzv", "ry", "ryt", "rys", "ryv", "rg",
            "m", "mq",
            "q", "qv", "qt",
            "d",
            "p", "pba", "pbei",
            "c", "cc",
            "u", "uzhe", "ule", "uguo", "ude1", "ude2", "ude3", "usuo", "udeng", "uyy", "udh", "uls", "uzhi", "ulian",
            "e", "y", "o", "h", "k",
            "x", "xx", "xu",
            "w", "wkz", "wky", "wyz", "wyy", "wj", "ww", "wt", "wd", "wf", "wn", "wm", "ws", "wp", "wb", "wh"
    };

    public static int ansjPosTagsNum = 95;

    //特征定义：依存特征，共14种
    public static int relateTagsNum   = 14;
    public static String[] relateTags = {"SBV", "VOB", "IOB", "FOB", "DBL", "ATT", "ADV",
                                          "CMP", "COO", "POB", "LAD", "RAD", "IS", "HED"};





    //--------------------------关系类型相关-----------------------------------
    //语义关系
    public static Integer ImpRelationType = 0;
    public static Integer ExpRelationType = 1;

    public static String EXPLICIT = "Explicit";
    public static String IMPLICIT = "Implicit";

    public static String DefaultRelNO = "4-1";  //承接类型

    //关系体系的版本号。0：代表旧关系体系 1：代表新关系体系
    public static int OldSenseVersion = 0;
    public static int NewSenseVersion = 1;
    public static int SenseVersion = OldSenseVersion;
//    public static int SenseVersion = NewSenseVersion;

    //新的关系体系的关系编号
    public static String[] relNo =
    {
            "1",
            "1-1",
            "1-2", "1-2-1", "1-2-2",

            "2",
            "2-1", "2-1-1", "2-1-2",
            "2-2", "2-2-1", "2-2-2",
            "2-3", "2-3-1", "2-3-2",
            "2-4", "2-4-1", "2-4-1-1", "2-4-1-2", "2-4-2", "2-4-2-1", "2-4-2-2",
            "2-5", "2-5-1", "2-5-1-1", "2-5-1-2", "2-5-2", "2-5-2-1", "2-5-2-2",
            "2-6", "2-6-1", "2-6-2",

            "3",
            "3-1", "3-1-1", "3-1-2",
            "3-2",
            "3-3", "3-3-1", "3-3-2",
            "3-4",

            "4",
            "4-1",
            "4-2",
            "4-3", "4-3-1", "4-3-2", "4-3-2-1", "4-3-2-2", "4-3-3", "4-3-3-1", "4-3-3-2",
            "4-4",
            "4-5",
            "4-6", "4-6-1", "4-6-2", "4-6-3",
            "4-7"
    };


    public static String[] relName =
    {
        "时序",
        "同步",
        "异步", "先序", "后序",

        "因果",
        "直接因果", "原因在先", "结果在先",
        "间接因果", "证据在先", "推论在先",
        "目的", "目的在先", "目的在后",
        "直接条件", "必要条件", "必要条件在先", "必要条件在后", "充分条件", "充分条件在先", "充分条件在后",
        "形式条件", "相关条件", "相关条件在先", "相关条件在后", "隐含推断", "隐含推断在先", "隐含推断在后",
        "任意条件", "任意条件在先", "任意条件在后",

        "比较",
        "直接对比", "正向对比", "反向对比",
        "间接对比",
        "让步", "让步在先", "让步在后",
        "形式让步",

        "扩展",
        "承接",
        "递进",
        "细化", "解释说明", "实例", "实例在先", "实例在后", "例外", "例外在先", "例外在后",
        "泛化",
        "平行",
        "选择", "相容选择", "互斥选择", "确定选择",
        "列表"
    };

    /**旧版本关系编号对应的新版本关系编号，与relNO一一对应修改**/
    public static String[] relNO_oldTonew =
    {
        "4-1",

        "1", "1-1", "1-2", "1-2-1", "1-2-2",

        "2", "2-1", "2-1-1", "2-1-2", "2-2", "2-2-1", "2-2-2", "2-3", "2-3-1", "2-3-2",

        "2-4", "2-4", "2-4-1", "2-4-1-1", "2-4-1-2", "2-4-2", "2-4-2-1", "2-4-2-2",
        "2-6", "2-6-1", "2-6-2", "2-5-1", "2-5-1-1", "2-5-1-2",

        "3", "3-1", "3-1-1", "3-1-2", "3-2", "3-3", "3-3-1", "3-3-2",

        "4", "4-3", "4-3-1", "4-3-2", "4-3-2-1", "4-3-2-2", "4-3-3", "4-3-3-1",
        "4-3-3-2", "4-4", "4-2",

        "4-5", "4-5", "4-6", "4-6-1", "4-6-2"
    };

    /**旧版本的关系编号**/
    public static String[] oldRelNO =
    {
        "0",
        "1","1-1","1-2","1-2-1","1-2-2",

        "2","2-1","2-1-1","2-1-2","2-2","2-2-1","2-2-2","2-3","2-3-1","2-3-2",

        "3","3-1","3-1-1","3-1-1-1","3-1-1-2","3-1-2","3-1-2-1","3-1-2-2",
        "3-1-3","3-1-3-1","3-1-3-2","3-2","3-2-1","3-2-2",

        "4","4-1","4-1-1","4-1-2","4-2","4-3","4-3-1","4-3-2",

        "5","5-1","5-1-1","5-1-2","5-1-2-1","5-1-2-2","5-1-3","5-1-3-1",
        "5-1-3-2","5-2","5-3",

        "6","6-1","6-2","6-2-1","6-2-2"
    };
}
