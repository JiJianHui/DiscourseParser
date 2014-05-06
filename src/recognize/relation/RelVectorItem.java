package recognize.relation;

import common.Constants;
import common.util;
import resource.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * 用于保存机器学习中的每条实例
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

    public int arg1Sentiment; //arg1的极性---0:中性  1:褒义  2:贬义
    public int arg2Sentiment;

    public String arg1HeadWordTag;  //句首词在同义词词林中的标签
    public String arg2HeadWordTag;

    public String arg1CueWordTag = null;   //是否存在在同义词词林中和连词位于同一个类别的词
    public String arg2CueWordTag = null;

    public String arg1RootWordTag = null;    //每个文本都有个root最主要的词，这是一个特征/或者说是谓语动词
    public String arg2RootWordTag = null;

    public ArrayList<String> arg1VerbsTags = new ArrayList<String>(); //每个文本段中的所有动词
    public ArrayList<String> arg2VerbsTags = new ArrayList<String>();


    /**将该特征向量转换为libsvm需要的格式。用于训练svm识别模型。
     * 特征格式说明：
     * 【label:关系编号 1：arg1极性 2:arg2极性 3：arg1句首词 4:arg2句首词 5:arg1关键词 6:arg2关键词
     * 7:arg1 root词  8:arg2 root词   9:arg1动词集合 10:arg2动词集合】
     * 关于动词：采取判断在同义词标签中的索引作为特征索引。
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
     * 为了将一个item转换为libsvm需要的实数型向量，我们需要首先统计下同义词词林中常见的标签集合，以便进行索引.
     * 我们获取的是同一类词的前四位标签。
     *
     * 注意此方法只有在同义词词典发生变化的时候调用，来重新生成标签集合。
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
