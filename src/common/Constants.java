package common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * ��̬������ŵص㣬��ų�����ʹ�õ��ľ�̬�������Լ��̶����������
 * User: Ji JianHui
 * Time: 2014-02-19 14:53
 * Email: jhji@ir.hit.edu.cn
 */
public class Constants
{
    public static Integer EPSION                  = 3;         //�����ж�׼ȷ����
    public static Integer MAX_Buffer_Size         = 1024;      //�����еĻ������Ĵ�С
    public static Integer MIN_Sentence_Length     = 5;         //��С�ľ���ʶ��Ԫ
    public static String  Parallel_Word_Seperator = ";";       //�������ʵķָ���

    public static Integer Max_Sentence_Length     = 150;


    //-----------------------------���ӷָ����-------------------------------------------
    public static ArrayList<String> Sentence_Seperators;    //���������еľ��ӷָ�����
    static{
        Sentence_Seperators = new ArrayList<String>();
        Sentence_Seperators.add("��");   Sentence_Seperators.add("��");
        Sentence_Seperators.add("��");   Sentence_Seperators.add(".");
        Sentence_Seperators.add("?");    Sentence_Seperators.add("!");
        //Sentence_Seperators.add("��");
    }

    //����ʶ��ʱ��ķָ�����������ʽ
    public static String   Sentence_Seperators_Regx = "[������.?!��]";

    //����ʶ��������ʽ
    public static Pattern  Chinese_Word_Pattern = Pattern.compile("[\\u4e00-\\u9fa5]");

    //���ӱ߽�ʶ��������ʽ
    public static Pattern  Sentence_Seperators_Pattern = Pattern.compile(Sentence_Seperators_Regx);


    //һ�����Ӳ��Ϊ��ͬ��argumentʱ��ķָ���
    public static Pattern Sentence_Element_Pattern = Pattern.compile("[,;����]");


    //------------------------ԭʼ�������-------------------------------------
    public static String P2_Ending = ".p2";
    public static String P3_Ending = ".p3";
    public static String P1_Ending = ".p1";

    //��ԭʼ�ı�ע���ϼ��ϣ���Ҫ���ڻ��������ݷ���
    public static String Raw_Annotation_Data = "F:\\Corpus Data\\check";

    //ת����ʽ�������·��
    //public static String Train_Data_Dir = "F:\\Distribution Data\\Distribution Data HIT\\Test";
    public static String Train_Data_Dir = "F:\\Distribution Data\\Distribution Data HIT\\Corpus Data\\XML";


    //����ͣ�ôʴʱ�,ע��˴���ͣ�ôʴʱ���Ҳ�����ĵ����ʵ�����ͣ�ô�
    public static String Stop_Word_Path_cn = "/resource/dictionary/Stopwords_cn_1208.txt";

    //ltp��������Ƕ�������ģ�sentID������ÿ�����Ӷ�Ӧ�ı�š������Ϳ����ҵ���Ӧ�Ĵ�����
    public static String Ltp_XML_Result_P3        = "data/p3";
    public static String Ltp_XML_Result_P3_SentID = "data/p3/sentID.txt";


    //------------------------����ѧϰ���-------------------------------------
    public static Integer Labl_Not_ConnWord  = 0;
    public static Integer Labl_is_ConnWord  = 1;

    public static Integer ConnRecoganize_ML   = 1;
    public static Integer ConnRecoganize_Rule = 0;

    public static Integer Label_Inter_ConnWord = 1;
    public static Integer Label_Cross_ConnWord = 2;

    //��ȡ����ʱ��Ҫ���˵��Ĵ���:����+����+������ + λ���� + ���� + ���Ĵ� + ��㹲����
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

    //�������壺��������,��28��
    public static int posTagsNum   = 28;
    public static String[] posTags = {"a", "b", "c", "d", "e", "g", "h",
                                      "i", "j", "k", "m","n", "nd", "nh",
                                      "ni", "nl", "ns", "nt", "nz", "o", "p",
                                      "q", "r", "u", "v", "wp", "ws", "x"};


    public static String[] ansjPosTags = {
            "n", "nr", "nr1", "nr2", "nrj", "nrf", "ns", "nsf", "nt", "nz", "nl", "ng", "nw", //����
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

    //�������壺������������14��
    public static int relateTagsNum   = 14;
    public static String[] relateTags = {"SBV", "VOB", "IOB", "FOB", "DBL", "ATT", "ADV",
                                          "CMP", "COO", "POB", "LAD", "RAD", "IS", "HED"};





    //--------------------------��ϵ�������-----------------------------------
    //�����ϵ
    public static Integer ImpRelationType = 0;
    public static Integer ExpRelationType = 1;

    public static String EXPLICIT = "Explicit";
    public static String IMPLICIT = "Implicit";

    public static String DefaultRelNO = "4-1";  //�н�����

    //��ϵ��ϵ�İ汾�š�0������ɹ�ϵ��ϵ 1�������¹�ϵ��ϵ
    public static int OldSenseVersion = 0;
    public static int NewSenseVersion = 1;
    public static int SenseVersion = OldSenseVersion;
//    public static int SenseVersion = NewSenseVersion;

    //�µĹ�ϵ��ϵ�Ĺ�ϵ���
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
        "ʱ��",
        "ͬ��",
        "�첽", "����", "����",

        "���",
        "ֱ�����", "ԭ������", "�������",
        "������", "֤������", "��������",
        "Ŀ��", "Ŀ������", "Ŀ���ں�",
        "ֱ������", "��Ҫ����", "��Ҫ��������", "��Ҫ�����ں�", "�������", "�����������", "��������ں�",
        "��ʽ����", "�������", "�����������", "��������ں�", "�����ƶ�", "�����ƶ�����", "�����ƶ��ں�",
        "��������", "������������", "���������ں�",

        "�Ƚ�",
        "ֱ�ӶԱ�", "����Ա�", "����Ա�",
        "��ӶԱ�",
        "�ò�", "�ò�����", "�ò��ں�",
        "��ʽ�ò�",

        "��չ",
        "�н�",
        "�ݽ�",
        "ϸ��", "����˵��", "ʵ��", "ʵ������", "ʵ���ں�", "����", "��������", "�����ں�",
        "����",
        "ƽ��",
        "ѡ��", "����ѡ��", "����ѡ��", "ȷ��ѡ��",
        "�б�"
    };

    /**�ɰ汾��ϵ��Ŷ�Ӧ���°汾��ϵ��ţ���relNOһһ��Ӧ�޸�**/
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

    /**�ɰ汾�Ĺ�ϵ���**/
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
