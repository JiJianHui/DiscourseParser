import common.Constants;
import common.LibSVMTest;
import common.util;
import entity.*;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.dom4j.DocumentException;
import resource.Resource;
import train.MLVectorItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ƪ�·�����������Ҫ��Ϊ�����Discourse Parser����ı�д�������Ե����Ͻ��б�д����
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2013-12-10 09:02
 * Email: jhji@ir.hit.edu.cn
 */
public class DiscourseParser
{
    private String text;    //����������

    private ArrayList<DSASentence> sentences;

    private LibSVMTest libsvmMode = null;

    public DiscourseParser() throws DocumentException, IOException
    {
        sentences = new ArrayList<DSASentence>();

        System.out.println("[--Info--]Loading Resource: word and relation dictionary");

        //1: ���شʵ���Ϣ
        Resource.LoadExpConnectivesDict();

        //2: �Լ�������ָʾ�Ĺ�ϵ��Ϣ
        Resource.LoadWordRelDict();

        //3: ���ر�ע�õ�ѵ������
        Resource.LoadRawRecord();

        //4: ���ز������ʴʵ�
        Resource.LoadParallelWordDict();

        //5: ��������ʶ��svmģ��
        libsvmMode = new LibSVMTest();
        libsvmMode.loadModel();
    }


    public void run(String line) throws  IOException
    {
        //1: ���һ�仰ʶ����ڹ�ϵ
        DSASentence sentence = new DSASentence( util.removeAllBlank(line) );

        //2: ʶ�𵥸�����
        this.findConnWordWithML(sentence);

        //3: ʶ�������ʣ�����...����
        this.findParallelWord(sentence);

        //4: ʶ��Argument��Ŀǰ���õķ����Ǽ򵥵Ķ��ŷָ�
        this.findArgumentInLine(sentence);

        //5: ���ݻ�ȡ������Ϣ��ʶ���ϵ���
        this.recognizeExpRelationType(sentence);

        //6: ʶ����ʽ����ϵ
        this.recognizeImpRelationType(sentence);
    }

    /**
     * 2: ʶ�𵥸����ʼ��뵽��ѡ������
     * @param sentence
     * @throws IOException
     */
    private void findConnWordWithML(DSASentence sentence) throws IOException {
        //1�����зִ�
        List<Term> words = ToAnalysis.parse( sentence.getContent().replaceAll(" ", "") );
        sentence.setAnsjWordTerms(words);

        int beginIndex = 0;

        ArrayList<MLVectorItem> candidateTerms = new ArrayList<MLVectorItem>();

        for(Term wordItem : words)
        {
            String wContent   = wordItem.getName().trim();
            MLVectorItem item = new MLVectorItem(wContent);

            //2: ���˵�������
            if( !Resource.ExpConnectivesDict.contains(wContent) ) continue;

            //3����ȡ��������
            String wNextPos = "w";
            String wPrevPos = "w";
            Term wNextTerm  = wordItem.getTo();
            Term wPrevTerm  = wordItem.getFrom();

            if( wPrevTerm != null ) wPrevPos = wPrevTerm.getNatrue().natureStr;
            if( wNextTerm != null ) wNextPos = wNextTerm.getNatrue().natureStr;

            item.setPos( wordItem.getNatrue().natureStr );
            item.setPrevPos(wPrevPos);  item.setNextPos(wNextPos);

            //4����ȡ�ô��ھ����еĵ�λ��
            beginIndex = sentence.getContent().indexOf(wContent, beginIndex);
            item.setPositionInLine( beginIndex );

            //5: ���ñ�ǩ����Ϊ��Ԥ�⣬�����������ñ�ǩ, Ĭ�ϲ�������
            //item.setLabel( Constants.Labl_Not_ConnWord );

            //6����ȡ�ô������ʴʵ��г��ֵĴ���,�Լ�������
            double occurTime = 0.0, ambiguity = 1.0;

            if( Resource.allWordsDict.containsKey(wContent) )
            {
                DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);

                occurTime = wordDictItem.getExpNum();
                ambiguity = 1 - wordDictItem.getMostExpProbality();
            }

            item.setAmbiguity(ambiguity);
            item.setOccurInDict(occurTime);

            candidateTerms.add(item);
        }

        for(MLVectorItem item : candidateTerms)
        {
            double result = libsvmMode.predict(item);

            if( result > 0 )
            {
                DSAConnective connective = new DSAConnective( item.getContent() );
                connective.setPosTag( item.getPos() );
                connective.setPrevPosTag(item.getPrevPos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine( item.getPositionInLine() );
            }
        }
    }


    /**
     * 3: ����һ�������еĲ������ʣ����磺����...����,
     * Ŀǰ��ʶ�𷽷�ֻ��ʶ����һ����������Ϊ���ܵĲ������ʡ������ճ��ִ����Ĵ�С���ж�
     * @param sentence
     */
    private void findParallelWord(DSASentence sentence)
    {
        int occurTimes      = -1;
        String sentContent  = sentence.getContent();
        ParallelConnective parallelConnective = null;

        for(Map.Entry<String, Integer> entry: Resource.ExpParallelWordDict.entrySet())
        {
            String parallelWord = entry.getKey();
            Integer numInDict = entry.getValue();

            //0: �жϴʵ��е�һ�����������Ƿ�������˸þ�����
            if( util.isParallelWordInSentence(parallelWord, sentContent) )
            {
                //1: ����������ʵ��������ֹ�ظ��϶�
                String[] lists = parallelWord.split(";");
                int beginIndex1 = sentContent.indexOf(lists[0]);
                int beginIndex2 = sentContent.indexOf(lists[1]);

                Iterator<DSAConnective> ite = sentence.getConWords().iterator();

                while( ite.hasNext() )
                {
                    DSAConnective curConn = ite.next();
                    if( curConn.getContent().equalsIgnoreCase(lists[0]) )
                    {
                        if( Math.abs(curConn.getPositionInLine() - beginIndex1) < Constants.EPSION )
                        {
                            ite.remove();
                        }
                    }
                    else if( curConn.getContent().equalsIgnoreCase(lists[1]) )
                    {
                        if( Math.abs(curConn.getPositionInLine() - beginIndex2) < Constants.EPSION )
                        {
                            ite.remove();
                        }
                    }
                }

                //2: ������ѡ���������б�
                if(  numInDict > occurTimes )
                {
                    if( parallelConnective == null ) parallelConnective = new ParallelConnective();
                    occurTimes = numInDict;
                    parallelConnective.setContent(parallelWord);
                    parallelConnective.setBeginIndex1(beginIndex1);
                    parallelConnective.setBeginIndex2(beginIndex2);
                }
            }
        }

        if( parallelConnective != null )
            sentence.getParallelConnectives().add(parallelConnective);
    }


    /**
     * ʶ��һ����ϵ�漰����Argument�����Կ��ǵ���ʹ��argument�ı�ע��Ϣ������ѵ����
     * ���絫�ǵ�argument1һ����λ�ڵ��Ǻ��棬��argument2һ��λ��ǰ�档
     * ��ʽ�ıȽϺ��жϣ�ֱ���������
     * Ŀǰ��Ϊ����ǰ��Ķ�����argument
     * @param sentence
     */
    private void findArgumentInLine(DSASentence sentence)
    {
        //0: �����Ӳ��Ϊ�������嵥Ԫ
        sentence.seperateSentenceToEDU();

        //1��ȷ����ʽ���ʵ�argument����Ҫ�������ڣ�����ǰ�滹���ں��档
        ArrayList<DSAConnective> singleConnWords = sentence.getConWords();

        for(DSAConnective curWord:singleConnWords)
        {
            String word   = curWord.getContent();
            int posInLine = (int) curWord.getPositionInLine();

            DSAArgument arg1 = null, arg2 = null;

            int index  = 0;
            boolean isSoloWord = false;
            DSAEDU curEDU = null;

            //ȷ��arg1�������������ʵ�argument
            for( index = 0; index < sentence.getEdus().size(); index++ )
            {
                curEDU = sentence.getEdus().get(index);

                if( curEDU.getContent().indexOf(word)!= -1 )
                {
                    if( posInLine >= curEDU.getBeginIndex() && posInLine < curEDU.getEndIndex() )
                    {
                        //��Ҫ����������Ϊһ��������EDUʱ������
                        if( curEDU.getContent().trim().length() - word.length() <= 1 )
                        {
                            isSoloWord = true;
                            curEDU = sentence.getEdus().get(index == 0 ? 1: index - 1);
                        }

                        arg1 = new DSAArgument( curEDU.getContent(), sentence.getContent() );
                        break;
                    }
                }
            }

            //ȷ��arg2�������ʵ�����һ��argument,,�Լ��ж�EDUλ�ھ��׵����
            if( arg1 != null )
            {
                if( isSoloWord )
                    curEDU = sentence.getEdus().get( index == 0 ? 2: index - 1);
                else
                    curEDU = sentence.getEdus().get( index ==0 ? 1 : index - 1 );
                arg2 = new DSAArgument( curEDU.getContent(), sentence.getContent() );
            }

            //���ɶ�Ӧ�ľ���ϵ,����ϵ�ı�Ż���Ҫ��һ�������ж�
            DSARelation relation = new DSARelation();
            relation.setArg1(arg1);
            relation.setArg2(arg2);
            relation.setDsaConnective(curWord);
            relation.setRelType(Constants.ExpRelationType);

            sentence.getRelations().add(relation);
        }
    }


    /***ʶ��������ʽ��ϵ����**/
    private void recognizeExpRelationType(DSASentence sentence)
    {
        for( DSARelation candiatateRel : sentence.getRelations() )
        {
            if( candiatateRel.getDsaConnective() == null ) continue;

            String connWord = candiatateRel.getDsaConnective().getContent();
            String relNO    = Resource.connectiveRelationDict.get(connWord);

            if( relNO == null ) relNO = Constants.DefaultRelNO;

            candiatateRel.setRelNO( relNO );
        }
    }


    /***ʶ����ܴ��ڵ���ʽ����ϵ**/
    private void recognizeImpRelationType(DSASentence sentence)
    {
        //�ڻ�����EDU�Ļ����Ͽ�ʼ��������ƥ��

    }


    /**
     * ���һƪ���½��з�����
     * @param fPath: ԭʼ���ϵ�·��
     */
    private void parseRawFile(String fPath) throws Exception
    {
        //0: �����¶�ȡ���õ������еľ��Ӽ���
        String fileContent = util.readFileToString(fPath);
        ArrayList<String> sentences = util.filtStringToSentence(fileContent);


        //1�������Ǿ��ڹ�ϵ���ж�
        for( String sentence : sentences )
        {
            DSASentence dsaSentence = new DSASentence( sentence );
            //this.recognizeRelationInSentence( dsaSentence.getContent() );
            //this.findConnectiveWithRule(dsaSentence);

        }

    }


    private ArrayList<DSAConnective> findConnective(String line) throws Exception
    {
        ArrayList<String> sentences = util.filtStringToSentence(line);

        DSASentence dsaSentence = new DSASentence(line);

        //this.findConnectiveWithRule(dsaSentence);

        ArrayList<DSAConnective> conWords = dsaSentence.getConWords();

        return conWords;
    }


    public static void main(String[] args) throws Exception
    {
        DiscourseParser discourseParser = new DiscourseParser();
        //discourseParser.parseRawFile("E:\\Program\\Java\\DiscourseParser\\data\\TestSentenceSplit.txt");
        //discourseParser.connectiveRecognize();

        String test = "�� ���� �� �ܶ� ����, ���� �� ���� �� ��û�� ���� �� ���� �� ���� ��";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        discourseParser.run(test);
    }
}

