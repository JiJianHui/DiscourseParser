package recognize.relation;

import common.Constants;
import common.util;
import resource.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * ���ڱ������ѧϰ�е�ÿ��ʵ��
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-25 09:34
 * Email: jhji@ir.hit.edu.cn
 */
public class RelVectorItem
{
    public String relNO;
    public String arg1Content;
    public String arg2Content;

    public int arg1Sentiment; //arg1�ļ���---0:����  1:����  2:����
    public int arg2Sentiment;

    public String arg1HeadWordTag;  //���״���ͬ��ʴ����еı�ǩ
    public String arg2HeadWordTag;

    public String arg1CueWordTag = null;   //�Ƿ������ͬ��ʴ����к�����λ��ͬһ�����Ĵ�
    public String arg2CueWordTag = null;

    public String arg1RootWordTag = null;    //ÿ���ı����и�root����Ҫ�Ĵʣ�����һ������/����˵��ν�ﶯ��
    public String arg2RootWordTag = null;

    public ArrayList<String> arg1VerbsTags = new ArrayList<String>(); //ÿ���ı����е����ж���
    public ArrayList<String> arg2VerbsTags = new ArrayList<String>();


    /**������������ת��Ϊlibsvm��Ҫ�ĸ�ʽ������ѵ��svmʶ��ģ�͡�
     * ������ʽ˵����
     * ��label:��ϵ��� 1��arg1���� 2:arg2���� 3��arg1���״� 4:arg2���״� 5:arg1�ؼ��� 6:arg2�ؼ���
     * 7:arg1 root��  8:arg2 root��   9:arg1���ʼ��� 10:arg2���ʼ��ϡ�
     * ���ڶ��ʣ���ȡ�ж���ͬ��ʱ�ǩ�е�������Ϊ����������
     **/
    public String toLineForLibsvm() throws IOException
    {
        Resource.LoadConnWordTagInSymCiLin();

        int allWordTagNum     = Resource.AllWordTagsInSymCiLin.size();
        int allConnWordTagNum = Resource.ConnTagInSymCiLinSorted.size();

        int arg1HeadWordIndex = 0, arg2HeadWordIndex = 0;
        int arg1CueWordIndex  = 0, arg2CueWordIndex  = 0;

        if(arg1HeadWordTag != null ) {
            arg1HeadWordIndex = Resource.AllWordTagsInSymCiLin.indexOf(arg1HeadWordTag) + 1;
        }
        if(arg2HeadWordTag != null ){
            arg2HeadWordIndex = Resource.AllWordTagsInSymCiLin.indexOf(arg2HeadWordTag) + 1;
        }
        if(arg1CueWordTag != null ){
            arg1CueWordIndex = Resource.ConnTagInSymCiLinSorted.indexOf(arg1CueWordTag) + 1;
        }
        if(arg2CueWordTag != null ){
            arg2CueWordIndex = Resource.ConnTagInSymCiLinSorted.indexOf(arg2CueWordTag) + 1;
        }


        String line = String.valueOf( relNO.charAt(0) );

        if( relNO.equalsIgnoreCase(Constants.DefaultRelNO) ) line = "0";

        line += " 1:" + arg1Content.length();
        line += " 2:" + arg2Content.length();

        line += " " + (3+arg1Sentiment) + ":1";
        line += " " + (6+arg2Sentiment) + ":1";

        line += " " + (9+arg1HeadWordIndex) + ":1";
        line += " " + (9+allWordTagNum+1 + arg2HeadWordIndex)+":1";

        line += " " + (10+allWordTagNum*2+1 + arg1CueWordIndex) + ":1";
        line += " " + (11+allWordTagNum*2+allConnWordTagNum+1 +arg2CueWordIndex) + ":1";

        return line;
    }

    /**
     * Ϊ�˽�һ��itemת��Ϊlibsvm��Ҫ��ʵ����������������Ҫ����ͳ����ͬ��ʴ����г����ı�ǩ���ϣ��Ա��������.
     * ���ǻ�ȡ����ͬһ��ʵ�ǰ��λ��ǩ��
     *
     * ע��˷���ֻ����ͬ��ʴʵ䷢���仯��ʱ����ã����������ɱ�ǩ���ϡ�
     * */
    public static void getAllWordTagInCiLin() throws IOException
    {
        Resource.LoadSymWordDict();

        ArrayList<String> allTags = new ArrayList<String>();

        for(String line : Resource.SymWordDict){
            line = line.trim().substring(0, 4);
            if( !allTags.contains(line) ) allTags.add(line);
        }

        String fPath = "resource/dictionary/allSymWordTags.txt";

        util.writeLinesToFile(fPath, allTags);
    }

    public static void main(String[] args) throws IOException{

        //RelVectorItem.getAllWordTagInCiLin();
    }
}
