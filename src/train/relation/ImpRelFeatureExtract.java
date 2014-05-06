package train.relation;

import common.Constants;
import common.util;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import entity.SenseRecord;
import ltp.PhraseParser;
import org.dom4j.DocumentException;
import resource.Resource;

import java.io.IOException;
import java.util.*;

/**
 * ���Ǵӱ�ע�������г�ȡ��ʽ��ϵ���ֵ�������Ȼ��ʹ��svm����ѧϰģ�͡�
 * ����ʹ�ø�ģ�ͽ��ж���������֮�����ʽ��ϵ����ʶ��
 *
 * �����ǳ�ȡ��������ȡ������������7�ࣺ
 * ��Ծ�������������Arg�����������ݴʶ�������ArgPair�����ؼ���������cueWord�����䷨����(parse)��
 * ���Ӽ�������(po)��������Ҫ��������(verb)�� ����ν�ʴʶ�����(verbPair)��
 *
 * ���룺��ע�õ����Ϻ���дʵ䡢�񶨴ʴʵ䡢ͬ��ʴ���
 * �����ѵ��svmģ����Ҫ������
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
        Resource.LoadRawRecord();

        Resource.LoadSentimentDict();
        Resource.LoadNegationDict();

        Resource.LoadTitleWordDict();

        Resource.LoadSymWordDict();
        Resource.LoadConnWordTagInSymCiLin();
        Resource.LoadAllWordTagsInSymCiLin();

        stanfordParser = new PhraseParser();
    }

    /**Ϊ��ѵ����ʽ��ϵʶ���ģ�ͣ���ȡ��Ӧ���������ݡ�**/
    public void run() throws IOException
    {
        //��ȡԭʼ��ע�����е���ʽ��ϵ�ı�עʵ��
        ArrayList<RelVectorItem> items = new ArrayList<RelVectorItem>();
        for(SenseRecord curRecord:Resource.Raw_Train_Annotation)
        {
            String relType       = curRecord.getType();
            String arg1Content   = curRecord.getArg1();
            String arg2Content   = curRecord.getArg2();

            if(relType.equalsIgnoreCase(Constants.EXPLICIT)) continue;
            //if(curRecord.getRelNO().equalsIgnoreCase(Constants.DefaultRelNO)) continue;
            if( arg1Content.length() > 400 || arg2Content.length() > 400 ) continue;

            try
            {
                //�½�һ����ʽ��ϵѵ��ʵ��
                RelVectorItem item = this.getFeatureLine(arg1Content, arg2Content);

                item.relNO         = curRecord.getRelNO();
                item.arg1Content   = arg1Content;
                item.arg2Content   = arg2Content;

                items.add(item);
            }
            catch (OutOfMemoryError e){
                continue;
            }
        }

        //��itemsת��Ϊlibsvm��Ҫ�ĸ�ʽ����Ҫ�ǽ�ͬ��ʱ�ǩ����ת��
        ArrayList<String> lines = new ArrayList<String>();

        for(RelVectorItem item:items){
            lines.add( item.toLineForLibsvm() );
        }

        //�����ݲ��Ϊѵ�����Ͳ��Լ������б���
        String trainPath = "data/relation/impRelTrainData.txt";
        String testPath  = "data/relation/impRelTestData.txt";

        int allNum   = items.size();
        int trainNum = allNum / 5 * 4;

        ArrayList<String> trainLines = new ArrayList<String>( lines.subList(0,trainNum) );
        ArrayList<String> testLines  = new ArrayList<String>( lines.subList(trainNum, allNum) );

        util.writeLinesToFile(trainPath, trainLines);
        util.writeLinesToFile(testPath, testLines);
    }

    /**
     * �ж�һ�����ֵļ��ԣ�ע������ֱ����Ƿִʺ�����֡�
     * �������ֶΰ��տո�ָ���Ȼ���ж�ÿ���ʵļ��ԡ����������еķ񶨴ʸ�����
     * ��������Ǹö����ֵļ��ԡ�
     *
     * @param arg���ִ�֮������ֶ�
     * @return �ö����ֵļ���. 0�������ԣ�1����������  2��������
     */
    public int getSentiment(String arg)
    {
        String[] lists = arg.split(" ");
        int positive = 0, negative = 0, negWordNum = 0;//�񶨴�

        for(String curWord:lists)
        {
            if(Resource.SentimentDic.get(curWord) != null )
            {
                //ÿ����һ��Positive��, �þ��ӵ����������һ��
                int num = Resource.SentimentDic.get(curWord);
                if( num == 1 ) positive++;
                else if( num == 2 ) negative++;
            }
            //��ȡ�񶨴ʸ���
            if( Resource.NegationDict.contains(curWord) ) negWordNum++;
        }

        int result = 0; //Ĭ����0����

        //ż������Ϊ������������Ϊ�϶�...���޸ļ��Ա�ǩ
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
     * ��ȡһ�����ֵľ��״ʵı�ǩ�����ݣ���Ҫ���ڻ�ȡ���еı�ǩ
     * Aa01A03= ���� ��Ա �˿� �˶� �� ʳָ
     * ���ǻ�ȡ����Aa01�е�ǰ�ĸ���û�е����Ϊnull
     * @return
     */
    public String getHeadWordTag(String arg) throws IOException
    {
        String headWord = arg.split(" ")[0];
        String result   = util.getWordTagInSym(headWord);

        return result;
    }


    /**
     * ��ȡһ���������Ƿ��������ͬ��ʴ����кͳ�������λ��ͬһ����µĴʡ����ִʳ�Ϊ�ؼ���
     * ������һ�����֣�����Ǹö������г��ֵĹؼ�����ͬ��ʴ����еı�ǩ
     * @return
     */
    public String getCueWordTag(String arg) throws IOException
    {
        int maxNum = 0;//���ʱ�ǩ���ֵĴ���

        String  result = null;
        String[] lists = arg.split(" ");

        //���arg�е�ÿ�����ж�һ���Ƿ����ͬ���Ĵʣ�ͬʱ��Զ������Tag����ȡ���������Ǹ�
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
     * ��ȡ���ı����е���Ҫsbvν�ʺ����г��ֵĶ��ʣ��������ǽ���ͬ��ʷ���
     * @param item
     */
    public void getVerbFeature(RelVectorItem item) throws IOException
    {
        Tree arg1Parse = stanfordParser.parseLine(item.arg1Content);
        Tree arg2Parse = stanfordParser.parseLine(item.arg2Content);

        List<TypedDependency> dep1 = stanfordParser.parseDependthyUseTree(arg1Parse);
        List<TypedDependency> dep2 = stanfordParser.parseDependthyUseTree(arg2Parse);

        //��ȡroot��, ��ȡ���ʼ���
        String arg1RootWord = null, arg2RootWord = null;
        ArrayList<String> arg1Verbs = new ArrayList<String>();
        ArrayList<String> arg2Verbs = new ArrayList<String>();

        for(TypedDependency curDep:dep1)
        {
            String relation = curDep.reln().getShortName();
            if( relation.equalsIgnoreCase("root") ){//������
                arg1RootWord = curDep.dep().getLeaves().get(0).nodeString();
            }
            if( relation.equalsIgnoreCase("nsubj") ){ //��������
                arg1RootWord = curDep.gov().getLeaves().get(0).nodeString();
            }
            if(relation.equalsIgnoreCase("dobj")){ //ֱ�ӱ���
                arg1Verbs.add(curDep.gov().getLeaves().get(0).nodeString());
            }
        }
        for(TypedDependency curDep:dep2)
        {
            String relation = curDep.reln().getShortName();
            if( relation.equalsIgnoreCase("root")){//������
                arg2RootWord = curDep.dep().getLeaves().get(0).nodeString();
            }
            if( relation.equalsIgnoreCase("nsubj") ){//��������
                arg2RootWord = curDep.gov().getLeaves().get(0).nodeString();
            }
            if(relation.equalsIgnoreCase("dobj")){ //ֱ�ӱ���
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

    /**�������ı�������ȡ����ʽ��ϵʶ����Ҫ��������**/
    public RelVectorItem getFeatureLine(String arg1Content, String arg2Content) throws IOException
    {
        RelVectorItem item   = new RelVectorItem();
        item.arg1Content     = arg1Content;
        item.arg2Content     = arg2Content;

        //1: ��ȡ�������ӵļ���������0:���ԣ�1������ 2������
        item.arg1Sentiment   = this.getSentiment(arg1Content);
        item.arg2Sentiment   = this.getSentiment(arg2Content);

        //2: ��ȡ���״���������Ҫ����һ���ʽ���ͬ���ת��
        item.arg1HeadWordTag = this.getHeadWordTag(arg1Content);
        item.arg2HeadWordTag = this.getHeadWordTag(arg2Content);

        //3: ��ȡ�ؼ����������ж���ͬ��ʴ������Ƿ���ںͳ���������ͬ��Ĵ�
        item.arg1CueWordTag  = this.getCueWordTag(arg1Content);
        item.arg2CueWordTag  = this.getCueWordTag(arg2Content);

        //4����ȡ�ı����е�ν����������Ҫ��ʹ��������������
        //this.getVerbFeature(item);

        return item;
    }
    /***
     * ��ȡ����������ͬ��ʴ����еı�ǩ���ϣ�ע�⣬���������һ����ʹ�á�
     * ��Ҫ�Ǹ������ʴʵ�ȥѰ��ͬ��ʴ����еı�ǩ���ϣ������ñ�ǩ���ֵ��ܴ������浽�ļ��С�
     * ������ʴʵ䷢���˱仯��Ҫ�ٴ�ʹ�øú������������ʶ�Ӧ��ͬ��ʱ�ǩ���ϡ�
     ***/
    /**
     public void getConnCagInSymCiLin() throws IOException
    {
        Resource.LoadExpConnectivesDict();
        Resource.LoadSymWordDict();

        HashMap<String, Integer> connCagInCiLin = new HashMap<>();

        //�ж�ÿ�����ʵı�ǩ������
        for(Map.Entry<String, Integer> entry:Resource.ExpConnWordDict.entrySet())
        {
            String curConn = entry.getKey();

            int connNum    = entry.getValue();
            if(connNum < 3) continue;

            String curConnCag = util.getWordTagInSym(curConn);

            if( curConnCag == null ) continue;

            int num = 0;
            if( connCagInCiLin.containsKey(curConnCag) ) num = connCagInCiLin.get(curConnCag);

            connCagInCiLin.put(curConnCag, num+connNum);
        }

        //��������浽�ļ��У���ǩ \t ���ִ���
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

    public static void main(String[] args) throws IOException, DocumentException
    {
        ImpRelFeatureExtract relFeatureExtract = new ImpRelFeatureExtract();

        relFeatureExtract.run();
    }
}




