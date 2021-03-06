package train.relation;

import common.Constants;
import common.util;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import entity.train.SenseRecord;
import entity.train.WordVector;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import syntax.PhraseParser;
import org.dom4j.DocumentException;
import resource.Resource;
import train.word.ConnVectorItem;

import java.io.IOException;
import java.util.*;

/**
 * 我们从标注的语料中抽取隐式关系出现的特征，然后使用svm进行学习模型。
 * 最终使用该模型进行对两个句子之间的隐式关系进行识别。
 *
 * 首先是抽取特征：抽取的特征包括了7类：
 * 句对句首内容特征（Arg）、句首内容词对特征（ArgPair）、关键词特征（cueWord）、句法特征(parse)、
 * 句子极性特征(po)、句子主要动词特征(verb)和 句子谓词词对特征(verbPair)。
 *
 * 输入：标注好的语料和情感词典、否定词词典、同义词词林
 * 输出：训练svm模型需要的数据
 *
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-23 16:32
 * Email: jhji@ir.hit.edu.cn
 */
public class ImpRelFeatureExtract
{
    private PhraseParser stanfordParser;

    public ImpRelFeatureExtract()throws DocumentException, IOException
    {
        if( Constants.SenseVersion == Constants.NewSenseVersion ) Resource.LoadRawRecord();
        if( Constants.SenseVersion == Constants.OldSenseVersion ) Resource.LoadOldRawRecord();

        Resource.LoadSentimentDict();
        Resource.LoadNegationDict();

        Resource.LoadTitleWordDict();

        Resource.LoadSymWordDict();
        Resource.LoadConnWordTagInSymCiLin();
        Resource.LoadAllWordTagsInSymCiLin();

        stanfordParser = new PhraseParser();
    }

    public ImpRelFeatureExtract(boolean wordVector)
    {

    }

    /**为了训练隐式关系识别的模型，抽取对应的特征数据。**/
    public void run() throws IOException
    {
        //获取原始标注语料中的隐式关系的标注实例
        ArrayList<RelVectorItem> items = new ArrayList<RelVectorItem>();

        for(SenseRecord curRecord:Resource.Raw_Train_Annotation)
        {
            String relType       = curRecord.getType();
            String arg1Content   = curRecord.getArg1();
            String arg2Content   = curRecord.getArg2();

            //if(curRecord.getRelNO().equalsIgnoreCase(Constants.DefaultRelNO)) continue;
            if(relType.equalsIgnoreCase(Constants.EXPLICIT)) continue;
            if( arg1Content.length() > 400 || arg2Content.length() > 400 ) continue;

            try
            {
                //新建一个隐式关系训练实例
                RelVectorItem item = this.getFeatureLine(arg1Content, arg2Content);

                item.relNO         = curRecord.getRelNO();
                item.arg1Content   = arg1Content;
                item.arg2Content   = arg2Content;

                if( Constants.SenseVersion == Constants.OldSenseVersion )
                {
                    //1: 过滤掉隐式关系中的1时序和3条件因为太少了
                    //if( item.relNO.startsWith("1") || item.relNO.startsWith("3") ) continue;
                    //2: 只考虑隐式句间关系1师兄和3条件, 计算两者的分类准确率
                    //if( !item.relNO.startsWith("1") && !item.relNO.startsWith("3") ) continue;
                }

                items.add(item);
            }
            catch (OutOfMemoryError e){e.printStackTrace();}
        }

        //将items转换为libsvm需要的格式，主要是将同义词标签进行转换

        //将数据拆分为训练集和测试集并进行保存
        String trainPath = "data/relation/impRelTrainData.txt";
        String testPath  = "data/relation/impRelTestData.txt";

        if( Constants.SenseVersion == Constants.OldSenseVersion )
        {
            trainPath = "data/relation/oldImpRelTrainData.txt";
            testPath  = "data/relation/oldImpRelTestData.txt";
        }

        int allNum   = items.size();
        int trainNum = allNum / 5 * 4;

        boolean[] exist =  util.getRandomArrays(allNum,trainNum);

        ArrayList<String> trainLines = new ArrayList<String>( );
        ArrayList<String> testLines  = new ArrayList<String>( );

        for( int index = 0; index < allNum; index++ )
        {
            RelVectorItem item  = items.get(index);
            String line = item.toLineForLibsvm();

            if( exist[index] ){trainLines.add(line);}
            else{testLines.add(line);}
        }

        util.writeLinesToFile(trainPath, trainLines);
        util.writeLinesToFile(testPath, testLines);
    }

    /**
     * 判断一段文字的极性，注意改文字必须是分词后的文字。
     * 将该文字段按照空格分隔，然后判断每个词的极性。并考虑其中的否定词个数。
     * 最终输出是该段文字的极性。
     *
     * @param arg：分词之后的文字段
     * @return 该段文字的极性. 0代表中性，1代表正极性  2代表负极性
     */
    public int getSentiment(String arg)
    {
        String[] lists = arg.split(" ");
        int positive = 0, negative = 0, negWordNum = 0;//否定词

        for(String curWord:lists)
        {
            if(Resource.SentimentDic.get(curWord) != null )
            {
                //每碰见一个Positive的, 该句子的正极性提高一分
                int num = Resource.SentimentDic.get(curWord);
                if( num == 1 ) positive++;
                else if( num == 2 ) negative++;
            }
            //获取否定词个数
            if( Resource.NegationDict.contains(curWord) ) negWordNum++;
        }

        int result = 0; //默认是0中性

        //偶数个否定为正，奇数个否定为肯定...并修改极性标签
        if( positive > negative )
        {
            if( negWordNum % 2 == 0 )
                result = 1;
            else
                result = 2;
        }
        if( positive < negative )
        {
            if( negWordNum % 2 == 0 )
                result = 2;
            else
                result = 1;
        }

        return result;
    }

    /**
     * 获取一段文字的句首词的标签和内容，主要用于获取所有的标签
     * Aa01A03= 人手 人员 人口 人丁 口 食指
     * 我们获取的是Aa01中的前四个，没有的情况为null
     * @return
     */
    public String getHeadWordTag(String arg) throws IOException
    {
        String headWord = arg.split(" ")[0];
        String result   = util.getWordTagInSym(headWord);

        return result;
    }


    /**
     * 获取一段文字中是否出现了在同义词词林中和常见连词位于同一类别下的词。这种词成为关键词
     * 输入是一段文字，输出是该段文字中出现的关键词在同义词词林中的标签
     * @return
     */
    public String getCueWordTag(String arg) throws IOException
    {
        int maxNum = 0;//连词标签出现的次数

        String  result = null;
        String[] lists = arg.split(" ");

        //针对arg中的每个词判断一下是否存在同类别的词，同时针对多个连词Tag，获取次数最多的那个
        for(String curWord:lists)
        {
            String curTag = util.getWordTagInSym(curWord);

            if( Resource.ConnTagInSymCiLin.containsKey(curTag) )
            {
                int num = Resource.ConnTagInSymCiLin.get(curTag);
                if( num > maxNum ){
                    result = curTag;
                    maxNum = num;
                }
            }
        }

        return result;
    }


    /**
     * 抽取该文本段中的主要sbv谓词和所有出现的动词，并将他们进行同义词泛化
     * @param item
     */
    public void getVerbFeature(RelVectorItem item) throws IOException
    {
        Tree arg1Parse = stanfordParser.parseLine(item.arg1Content);
        Tree arg2Parse = stanfordParser.parseLine(item.arg2Content);

        List<TypedDependency> dep1 = stanfordParser.parseDependthyUseTree(arg1Parse);
        List<TypedDependency> dep2 = stanfordParser.parseDependthyUseTree(arg2Parse);

        //获取root词, 获取动词集合
        String arg1RootWord = null, arg2RootWord = null;
        ArrayList<String> arg1Verbs = new ArrayList<String>();
        ArrayList<String> arg2Verbs = new ArrayList<String>();

        for(TypedDependency curDep:dep1)
        {
            String relation = curDep.reln().getShortName();
            if( relation.equalsIgnoreCase("root") ){//根动词
                arg1RootWord = curDep.dep().getLeaves().get(0).nodeString();
            }
            if( relation.equalsIgnoreCase("nsubj") ){ //名词主语
                arg1RootWord = curDep.gov().getLeaves().get(0).nodeString();
            }
            if(relation.equalsIgnoreCase("dobj")){ //直接宾语
                arg1Verbs.add(curDep.gov().getLeaves().get(0).nodeString());
            }
        }
        for(TypedDependency curDep:dep2)
        {
            String relation = curDep.reln().getShortName();
            if( relation.equalsIgnoreCase("root")){//根动词
                arg2RootWord = curDep.dep().getLeaves().get(0).nodeString();
            }
            if( relation.equalsIgnoreCase("nsubj") ){//名词主语
                arg2RootWord = curDep.gov().getLeaves().get(0).nodeString();
            }
            if(relation.equalsIgnoreCase("dobj")){ //直接宾语
                arg2Verbs.add(curDep.gov().getLeaves().get(0).nodeString());
            }
        }

        item.arg1RootWordTag = util.getWordTagInSym(arg1RootWord);
        item.arg2RootWordTag = util.getWordTagInSym(arg2RootWord);

        for(String verb:arg1Verbs){
            item.arg1VerbsTags.add(util.getWordTagInSym(verb));
        }
        for(String verb:arg2Verbs){
            item.arg2VerbsTags.add(util.getWordTagInSym(verb));
        }
    }

    /**从两个文本段中提取出隐式关系识别需要的特征。主要是用于预测的时候**/
    public RelVectorItem getFeatureLine(String arg1Content, String arg2Content) throws IOException
    {
        RelVectorItem item   = new RelVectorItem();
        item.arg1Content     = arg1Content;
        item.arg2Content     = arg2Content;

        //1: 抽取两个句子的极性特征：0:中性，1：褒义 2：贬义
        item.arg1Sentiment   = this.getSentiment(arg1Content);
        item.arg2Sentiment   = this.getSentiment(arg2Content);

        //2: 获取句首词特征：主要将第一个词进行同义词转换
        item.arg1HeadWordTag = this.getHeadWordTag(arg1Content);
        item.arg2HeadWordTag = this.getHeadWordTag(arg2Content);

        //3: 获取关键词特征，判断在同义词词林中是否存在和常见关联词同类的词
        item.arg1CueWordTag  = this.getCueWordTag(arg1Content);
        item.arg2CueWordTag  = this.getCueWordTag(arg2Content);

        //4：获取文本段中的谓词特征，主要是使用依存分析来完成
        //this.getVerbFeature(item);

        return item;
    }
    /***
     * 获取常见连词在同义词词林中的标签集合，注意，这个方法是一次性使用。
     * 主要是根据连词词典去寻找同义词词林中的标签集合，并将该标签出现的总次数保存到文件中。
     * 如果连词词典发生了变化需要再次使用该函数来生成连词对应的同义词标签集合。
     ***/
    /**
     public void getConnCagInSymCiLin() throws IOException
    {
        Resource.LoadExpConnectivesDict();
        Resource.LoadSymWordDict();

        HashMap<String, Integer> connCagInCiLin = new HashMap<>();

        //判断每个连词的标签并报错
        for(Map.Entry<String, DSAWordItem> entry:Resource.allWordsDict.entrySet())
        {
            String curConn = entry.getKey();

            int connNum    = entry.getValue().getExpNum();
            if(connNum < 3) continue;

            String curConnCag = util.getWordTagInSym(curConn);

            if( curConnCag == null ) continue;

            int num = 0;
            if( connCagInCiLin.containsKey(curConnCag) ) num = connCagInCiLin.get(curConnCag);

            connCagInCiLin.put(curConnCag, num+connNum);
        }

        //将结果保存到文件中：标签 \t 出现次数
        String fPath = "resource/dictionary/ConnWordCagInSymCiLin.txt";
        ArrayList<String> lines = new ArrayList<String>();

        for(Map.Entry<String, Integer> entry:connCagInCiLin.entrySet())
        {
            String line = entry.getKey() + "\t" + entry.getValue();
            lines.add(line);
        }
        util.writeLinesToFile(fPath, lines);
    }

     **/

    /*****
     * 使用词向量来识别隐式句间关系
     */
    public void regImpRelWithWordVector() throws IOException, DocumentException
    {
        Resource.LoadOldRawRecord();
        Resource.LoadWordVector();
        Resource.LoadStopWords();
        Resource.LoadWordRelDict();

        ArrayList<String> lines = new ArrayList<String>();

        for( SenseRecord curRecord : Resource.Raw_Train_Annotation )
        {
            String relType       = curRecord.getType();
            String relNO         = curRecord.getRelNO();

            String arg1Content   = curRecord.getArg1();
            String arg2Content   = curRecord.getArg2();

            if( relType.equalsIgnoreCase(Constants.EXPLICIT) ) continue;
            if(Constants.SenseVersion == Constants.OldSenseVersion)
            {
                if(relNO.startsWith("1") || relNO.startsWith("3")) continue;
            }

            if( arg1Content.length() > 400 || arg2Content.length() > 400 ) continue;

            //进行分词来获取各个词的语义角色
            List<Term> arg1Words = this.getPosTag(arg1Content);
            List<Term> arg2Words = this.getPosTag(arg2Content);

            //计算arg1和arg2的词向量
            String[] arg1Lists = arg1Content.split(" ");
            String[] arg2Lists = arg2Content.split(" ");

            WordVector arg1Vector = new WordVector();
            WordVector arg2Vector = new WordVector();

            for(String curWord:arg1Lists)
            {
                WordVector curWordVector = Resource.wordVectors.get(curWord);

                if(  curWordVector == null ) continue;
                if( Resource.Stop_Words.contains(curWord) )
                {
                    continue;
                }
//                {
//                    if( !Resource.allWordsDict.containsKey(curWord) ) continue;
//                }

                int weight = 1;
                for( Term curTerm:arg1Words )
                {
                    if( curTerm.getName().equals(curWord) )
                    {
                        String pos = curTerm.getNatrue().natureStr;
                        if( pos.startsWith("a") ) weight = 3;  //形容词
                        if( pos.startsWith("u") ) weight = 1;  //语气词
                        if( pos.startsWith("n") ) weight = 2;   //名词
                        if( pos.startsWith("v") ) weight = 4;   //动词
                    }
                }

                for(int i = 0; i < weight; i++) arg1Vector.addOtheVector(curWordVector);
            }

            for(String curWord:arg2Lists)
            {
                WordVector curWordVector = Resource.wordVectors.get(curWord);

                if(  curWordVector == null ) continue;
                if( Resource.Stop_Words.contains(curWord) )
                {
                    continue;
                }
//                {
//                    if( !Resource.allWordsDict.containsKey(curWord) ) continue;
//                }

                int weight = 1;
                for( Term curTerm : arg2Words )
                {
                    if( curTerm.getName().equals(curWord) ){
                        String pos = curTerm.getNatrue().natureStr;
                        if( pos.startsWith("a") ) weight = 3;  //形容词
                        if( pos.startsWith("u") ) weight = 2;  //语气词
                        if( pos.startsWith("n") ) weight = 2;   //名词
                        if( pos.startsWith("v") ) weight = 4;   //动词
                    }
                }

                for(int i = 0; i < weight; i++) arg2Vector.addOtheVector(curWordVector);
            }

            //arg1Vector.minusOtherVector(arg2Vector);

            //生成特征文件行
            String line = String.valueOf( relNO.charAt(0) );

            for( int index = 0; index < 50; index++ )
            {
                //line = line + " " + (index+1) + ":" + ( arg1Vector.wVector[index] / arg1Words.size() );
                line = line + " " + (index+1) + ":" + ( arg1Vector.wVector[index]  );
            }
            for( int index = 0; index < 50; index++ )
            {
                //line = line + " " + (index + 51) + ":" + ( arg2Vector.wVector[index] / arg2Words.size() );
                line = line + " " + (index + 51) + ":" + ( arg2Vector.wVector[index]  );
            }

            lines.add(line);
        }

        //将items转换为libsvm需要的格式，主要是将同义词标签进行转换

        //将数据拆分为训练集和测试集并进行保存
        String trainPath = "data/relation/impRelTrainData.wordVector.txt";
        String testPath  = "data/relation/impRelTestData.wordVector.txt";

        if( Constants.SenseVersion == Constants.OldSenseVersion )
        {
            trainPath = "data/relation/oldImpRelTrainData.wordVector.txt";
            testPath  = "data/relation/oldImpRelTestData.wordVector.txt";
            System.out.println(trainPath);
        }
        else
        {
            trainPath = "data/relation/impRelTrainData.wordVector.txt";
            testPath  = "data/relation/impRelTestData.wordVector.txt";
            System.out.println(trainPath);
        }

        int allNum   = lines.size();
        int trainNum = allNum / 5 * 4;

        boolean[] exist =  util.getRandomArrays(allNum,trainNum);

        ArrayList<String> trainLines = new ArrayList<String>( );
        ArrayList<String> testLines  = new ArrayList<String>( );

        for( int index = 0; index < allNum; index++)
        {
            String line = lines.get(index);

            if( exist[index] ) trainLines.add(line);
            else testLines.add(line);
        }

        util.writeLinesToFile(trainPath, trainLines);
        util.writeLinesToFile(testPath, testLines);
    }

    private List<Term> getPosTag( String arg)
    {
        //不需要分词, 已经分好词的结果，那么我们需要词性标注的结果
        List<String> lists = Arrays.asList( arg.split(" ") );

        List<Term> words = NatureRecognition.recognition(lists, 0) ;

        return  words;
    }

    public static void main(String[] args) throws IOException, DocumentException
    {
        ImpRelFeatureExtract relFeatureExtract = new ImpRelFeatureExtract(true);

        //relFeatureExtract.run();
        relFeatureExtract.regImpRelWithWordVector();
    }
}




