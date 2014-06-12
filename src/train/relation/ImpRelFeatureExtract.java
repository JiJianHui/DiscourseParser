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

            //if(curRecord.getRelNO().equalsIgnoreCase(Constants.DefaultRelNO)) continue;
            if(relType.equalsIgnoreCase(Constants.EXPLICIT)) continue;
            if( arg1Content.length() > 400 || arg2Content.length() > 400 ) continue;

            try
            {
                //�½�һ����ʽ��ϵѵ��ʵ��
                RelVectorItem item = this.getFeatureLine(arg1Content, arg2Content);

                item.relNO         = curRecord.getRelNO();
                item.arg1Content   = arg1Content;
                item.arg2Content   = arg2Content;

                if( Constants.SenseVersion == Constants.OldSenseVersion )
                {
                    //1: ���˵���ʽ��ϵ�е�1ʱ���3������Ϊ̫����
                    //if( item.relNO.startsWith("1") || item.relNO.startsWith("3") ) continue;
                    //2: ֻ������ʽ����ϵ1ʦ�ֺ�3����, �������ߵķ���׼ȷ��
                    //if( !item.relNO.startsWith("1") && !item.relNO.startsWith("3") ) continue;
                }

                items.add(item);
            }
            catch (OutOfMemoryError e){e.printStackTrace();}
        }

        //��itemsת��Ϊlibsvm��Ҫ�ĸ�ʽ����Ҫ�ǽ�ͬ��ʱ�ǩ����ת��

        //�����ݲ��Ϊѵ�����Ͳ��Լ������б���
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

    /**�������ı�������ȡ����ʽ��ϵʶ����Ҫ����������Ҫ������Ԥ���ʱ��**/
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

    /*****
     * ʹ�ô�������ʶ����ʽ����ϵ
     */
    public void regImpRelWithWordVector() throws IOException, DocumentException
    {
        Resource.LoadOldRawRecord();
        Resource.LoadWordVector();
        Resource.LoadStopWords();

        ArrayList<String> lines = new ArrayList<String>();

        for( SenseRecord curRecord : Resource.Raw_Train_Annotation )
        {
            String relType       = curRecord.getType();
            String relNO         = curRecord.getRelNO();

            String arg1Content   = curRecord.getArg1();
            String arg2Content   = curRecord.getArg2();

            if( relType.equalsIgnoreCase(Constants.EXPLICIT) ) continue;
            if(relNO.startsWith("1") || relNO.startsWith("3")) continue;
            if( arg1Content.length() > 400 || arg2Content.length() > 400 ) continue;

            //���зִ�����ȡ�����ʵ������ɫ
            List<Term> arg1Words = this.getPosTag(arg1Content);
            List<Term> arg2Words = this.getPosTag(arg2Content);

            //����arg1��arg2�Ĵ�����
            String[] arg1Lists = arg1Content.split(" ");
            String[] arg2Lists = arg2Content.split(" ");

            WordVector arg1Vector = new WordVector();
            WordVector arg2Vector = new WordVector();

            for(String curWord:arg1Lists)
            {
                WordVector curWordVector = Resource.wordVectors.get(curWord);

                if(  curWordVector == null ) continue;
                if( Resource.Stop_Words.contains(curWord) ) continue;

                int weight = 1;
                for( Term curTerm:arg1Words )
                {
                    if( curTerm.getName().equals(curWord) )
                    {
                        String pos = curTerm.getNatrue().natureStr;
                        if( pos.startsWith("a") ) weight = 2;  //���ݴ�
                        if( pos.startsWith("u") ) weight = 2;  //������
                        if( pos.startsWith("n") ) weight = 3;   //����
                        if( pos.startsWith("v") ) weight = 4;   //����
                    }
                }

                for(int i = 0; i < weight; i++) arg1Vector.addOtheVector(curWordVector);
            }

            for(String curWord:arg2Lists)
            {
                WordVector curWordVector = Resource.wordVectors.get(curWord);

                if(  curWordVector == null ) continue;
                if( Resource.Stop_Words.contains(curWord) ) continue;

                int weight = 1;
                for( Term curTerm : arg2Words )
                {
                    if( curTerm.getName().equals(curWord) ){
                        String pos = curTerm.getNatrue().natureStr;
                        if( pos.startsWith("a") ) weight = 2;  //���ݴ�
                        if( pos.startsWith("u") ) weight = 2;  //������
                        if( pos.startsWith("n") ) weight = 3;   //����
                        if( pos.startsWith("v") ) weight = 4;   //����
                    }
                }

                for(int i = 0; i < weight; i++) arg2Vector.addOtheVector(curWordVector);
            }

            //arg1Vector.minusOtherVector(arg2Vector);

            //���������ļ���
            String line = String.valueOf( relNO.charAt(0) );

            for( int index = 0; index < 50; index++ )
            {
                line = line + " " + (index+1) + ":" + ( arg1Vector.wVector[index] / arg1Words.size() );
            }
            for( int index = 0; index < 50; index++ )
            {
                line = line + " " + (index + 51) + ":" + ( arg2Vector.wVector[index] / arg2Words.size() );
            }

            lines.add(line);
        }

        //��itemsת��Ϊlibsvm��Ҫ�ĸ�ʽ����Ҫ�ǽ�ͬ��ʱ�ǩ����ת��

        //�����ݲ��Ϊѵ�����Ͳ��Լ������б���
        String trainPath = "data/relation/impRelTrainData.wordVector.txt";
        String testPath  = "data/relation/impRelTestData.wordVector.txt";

        if( Constants.SenseVersion == Constants.OldSenseVersion )
        {
            trainPath = "data/relation/oldImpRelTrainData.wordVector.txt";
            testPath  = "data/relation/oldImpRelTestData.wordVector.txt";
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
        //����Ҫ�ִ�, �Ѿ��ֺôʵĽ������ô������Ҫ���Ա�ע�Ľ��
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




