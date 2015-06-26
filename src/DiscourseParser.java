import common.Constants;
import entity.recognize.*;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import net.didion.jwnl.data.IndexWord;
import opennlp.maxent.DataStream;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import org.ansj.splitWord.analysis.ToAnalysis;
import train.svm.wordRecSVM;
import train.svm.relRecSVM;
import common.util;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.Tree;
import syntax.PhraseParser;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.dom4j.DocumentException;
import train.relation.ImpRelFeatureExtract;
import train.relation.RelVectorItem;
import train.word.ConnVectorItem;
import resource.Resource;

import java.io.*;
import java.util.*;
import  train.maxent.Predict;

/**
 * ƪ�·�����������Ҫ��Ϊ�����Discourse Parser����ı�д�������Ե����Ͻ��б�д����
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2013-12-10 09:02
 * Email: jhji@ir.hit.edu.cn
 */
public class DiscourseParser
{
    private wordRecSVM connSvmMode;      //����ʶ��ģ��
    private relRecSVM relationSVMMode;   //��ʽ��ϵʶ��ģ��

    private PhraseParser phraseParser;  //����ṹ����
    private ImpRelFeatureExtract impRelFeatureExtract;

    public DiscourseParser() throws DocumentException, IOException
    {
        //1: ���شʵ���Ϣ
        Resource.LoadExpConnectivesDict();

        //2: �Լ�������ָʾ�Ĺ�ϵ��Ϣ
        Resource.LoadWordRelDict();

        //3: ���ر�ע�õ�ѵ������
        //Resource.LoadRawRecord();

        //5: ��������Argλ��ͳ������
        Resource.LoadConnectiveArgs();
        Resource.LoadConnInP2AndP3();

        //6: ��������ʶ��svmģ��
        connSvmMode = new wordRecSVM();
        connSvmMode.loadModel();

        //7: ���ض���ṹ����
        this.phraseParser = new PhraseParser();

        //8: ������ʽ��ϵʶ��ģ��
        relationSVMMode = new relRecSVM();
        relationSVMMode.loadModel();

        impRelFeatureExtract = new ImpRelFeatureExtract();

        //9: ���ع�ϵ�б���������˹�ϵ��ź͹�ϵ���ƵĶ�Ӧ��
        Resource.LoadSenseList();
    }

    /**������ʹ��*/
    public DiscourseParser(boolean debug)
    {

    }


    /**
     * ���һƪ���½��з�����������Ҫ��һƪ���½��з־䣬Ȼ���ÿ�����ӽ��д���
     * �������һƪ���µ����ݣ����ص��Ƿ�װ�õ�DSA����
     * fileContent: ��������£�������һ�����ӻ����Ƕ�����ӡ�
     * needSegment���Ƿ���зִʵı�־
     */
    public DSAParagraph parseRawFile(String fileContent, boolean needSegment) throws Exception
    {
        //0: �����¶�ȡ�����зָ�õ������еľ��Ӽ���
        DSAParagraph      paragraph = new DSAParagraph(fileContent);
        ArrayList<String> sentences = util.filtStringToSentence(fileContent);

        //1�����ڹ�ϵ���ж�:������һ�������ڲ�����ʽ����ʽ��ϵ�Ĺ���
        for( int index = 0; index < sentences.size(); index++ )
        {
            DSASentence dsaSentence = new DSASentence( sentences.get(index) );
            dsaSentence.setId(index);

            try{
                this.processSentence(dsaSentence, needSegment);
            }
            catch (NullPointerException e){
                System.out.println(e.getMessage());
            }

            paragraph.sentences.add(dsaSentence);
        }

        //2������ϵ��ʶ��:�����ʽ��ϵʶ��,���ڵ�������һ����ѡ
        for(int index = 0; index < paragraph.sentences.size() - 1; index++)
        {
            DSASentence curSentence  = paragraph.sentences.get(index);
            DSASentence nextSentence = paragraph.sentences.get(index + 1);

            //a: �ж��Ƿ��Ǿ���ϵ�е���ʽ��ϵ
            boolean hasExpRel = getCrossExpRelInPara(paragraph, curSentence, nextSentence);

            if( hasExpRel ) continue;

            //b: ��ȡ��������֮����ܴ��ڵ���ʽ��ϵ
            int  senseType   = getCrossImpInPara(paragraph, curSentence, nextSentence);
        }

        return paragraph;
    }

    /**
     * ����һ����װ�õ�DSASentence���Ǿɰ汾run��������д��
     */
    public void processSentence(DSASentence sentence, boolean needSegment) throws IOException
    {
        int id = 0;
        boolean argPosition = true;     //Ĭ�Ͻ���Ԫλ������ΪSS

        //1: ���Ƚ���Ԥ�������еײ��NLP�����ִʡ����Ա�ע
        this.preProcess(sentence, needSegment);

        //2: ʶ�𵥸����ʺ�ʶ�������ʣ�����...����
        this.findConnWordWithML(sentence);

//         if(!sentence.getConWords().isEmpty())      //Find Connective~
//         {

        argPosition = this.markConnAsInterOrCross(sentence,id);
        //        this.markConnAsInterOrCross(sentence);        ԭ�з���,based on rules
        this.findParallelWord(sentence);

        //4: �����Ӱ��ն���ṹ��ֳɻ���EDU
        //this.findArgumentInLine(sentence);
        boolean tempResult = this.findArgumentWithPhraseParser(sentence);

        //4.1��������־��ӽṹ�Ƿ���Sentence
        if( !tempResult ){ sentence.setIsCorrect(false); return; }

        //��Ԫλ��ΪSS(�־��ϵ)
        for(DSAConnective curWord:sentence.getConWords() )
        {
             Boolean isInterConnetive = curWord.getInterConnective();
             if(isInterConnetive)
             {
                 argLaberler(sentence);      //��ȡ���������䷨���ڲ��ڵ���з��ࣺArg1 Node��Arg2 Node��None
             }
        }

        //5: ȷ��ÿ���������漰��������EDU��������ѡ��ʽ��ϵ
        this.matchConnAndEDUforExpRelation(sentence);
        this.matchParallelConnAndEDUforExpRelation(sentence);
        //6: ���ݻ�ȡ������Ϣ��ʶ����ʽ����ϵ
        this.recognizeExpRelationType(sentence);

//         }
//         else
//         {
             //7: ʶ����ܴ��ڵ���ʽ����ϵ
             this.getCandiateImpRelation(sentence);
             this.recognizeImpRelationType(sentence);
//         }

    }

    /**
     * ����һ�������ڲ��ľ��ڹ�ϵ������Ϊ�ַ������Ƿ���Ҫ�ִ�.
     * ע��÷����Ѿ���ProcessSentence�滻����Ϊ�漰����sentID�����⡣
     ***/
    public DSASentence run(String line, boolean needSegment) throws  IOException
    {
        DSASentence sentence = new DSASentence( line );
        sentence.setId(0);

        //1: ���Ƚ���Ԥ�������еײ��NLP�����ִʡ����Ա�ע
        this.preProcess(sentence, needSegment);

        //2: ʶ�𵥸�����
        this.findConnWordWithML(sentence);
//        this.markConnAsInterOrCross(sentence);        //ѵ��

        //3: ʶ�������ʣ�����...����
        this.findParallelWord(sentence);

        //4: �����Ӱ��ն���ṹ��ֳɻ���EDU
        //this.findArgumentInLine(sentence);
        boolean tempResult = this.findArgumentWithPhraseParser(sentence);

        //4.1��������־��ӽṹ�Ƿ���Sentence
        if( !tempResult ){ sentence.setIsCorrect(false); return sentence; }

        //5: ȷ��ÿ���������漰��������EDU��������ѡ��ʽ��ϵ
        this.matchConnAndEDUforExpRelation(sentence);
        this.matchParallelConnAndEDUforExpRelation(sentence);

        //6: ���ݻ�ȡ������Ϣ��ʶ����ʽ����ϵ
        this.recognizeExpRelationType(sentence);

        //7: ʶ����ܴ��ڵ���ʽ����ϵ
        this.recognizeImpRelationType(sentence);

        return sentence;
    }



    //-------------------------------0: �Ծ��ӽ��еײ㴦��--------------------------

    /**������ľ��ӽ��еײ��NLP������Ҫ�ǰ����˷ִʡ����Ա�ע�ȡ�**/
    private void preProcess(DSASentence sentence, boolean needSegment)
    {
        List<Term> words;
        int beginIndex = 0;
        String segmentResult = "";
        ArrayList<ConnVectorItem> candidateTerms = new ArrayList<ConnVectorItem>();

        if( !needSegment )
        {
            //����Ҫ�ִ�, �Ѿ��ֺôʵĽ������ô������Ҫ���Ա�ע�Ľ��
            List<String> lists = Arrays.asList( sentence.getContent().split(" ") );

            words = NatureRecognition.recognition(lists, 0) ;

            for( int index = 0; index < words.size(); index++ )
            {
                Term curTerm = words.get(index);

                if(index > 0) {
                    curTerm.setFrom( words.get(index - 1) );
                }
                if(index < words.size() - 1){
                    curTerm.setTo( words.get(index+1) );
                }
            }
        }
        else{

            words = ToAnalysis.parse(util.removeAllBlank(sentence.getContent()));
        }

        //a: ��Ծ����е�ÿ���ʽ��г�ȡ����


        sentence.setAnsjWordTerms(words);
        sentence.setContent( util.removeAllBlank(sentence.getContent()) );

        for( Term wordItem : sentence.getAnsjWordTerms() )
        {
            String wContent     = wordItem.getName().trim();
            ConnVectorItem item = new ConnVectorItem(wContent);

            segmentResult += wContent + " ";
        }

        //���÷ִʺ�Ľ��
        sentence.setSegContent( segmentResult.trim() );
    }

    //-------------------------------1: Connective Recognize----------------------

    /**
     * 2: ʶ�𵥸����ʼ��뵽��ѡ������
     * @param sentence
     * @throws IOException
     */
    private void findConnWordWithML(DSASentence sentence) throws IOException
    {
        int beginIndex = 0;
        String segmentResult = "";

        ArrayList<ConnVectorItem> candidateTerms = new ArrayList<ConnVectorItem>();

        //a: ��Ծ����е�ÿ���ʽ��г�ȡ����
        for( Term wordItem : sentence.getAnsjWordTerms() )
        {
            String wContent     = wordItem.getName().trim();
            ConnVectorItem item = new ConnVectorItem(wContent);

            segmentResult += wContent + " ";

            //2: ���˵�������
            if( !Resource.allWordsDict.containsKey(wContent) ) continue;

            //3����ȡ��������
            String wNextPos = "w", wPrevPos = "w";
            Term wNextTerm  = wordItem.getTo(), wPrevTerm  = wordItem.getFrom();

            if( wPrevTerm != null ) wPrevPos = wPrevTerm.getNatrue().natureStr;
            if( wNextTerm != null ) wNextPos = wNextTerm.getNatrue().natureStr;

            item.setPos( wordItem.getNatrue().natureStr );
            item.setPrevPos(wPrevPos);  item.setNextPos(wNextPos);

            //4����ȡ�ô��ھ����еĵ�λ��
            beginIndex = sentence.getContent().indexOf(wContent, beginIndex);
            item.setPositionInLine( beginIndex );

            //6����ȡ�ô������ʴʵ��г��ֵĴ���,�Լ�������
            double occurTime = 0.0, ambiguity = 1.0;

            if( Resource.allWordsDict.containsKey(wContent) )
            {
                DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);
                occurTime = wordDictItem.getExpNum();
                ambiguity = wordDictItem.getMostExpProbality();
            }

            item.setAmbiguity(ambiguity);
            item.setOccurInDict(occurTime);

            //5: ���ñ�ǩ����Ϊ��Ԥ�⣬�����������ñ�ǩ, Ĭ�ϲ�������
            if(occurTime < 3) item.setLabel( Constants.Labl_Not_ConnWord );
            else item.setLabel( Constants.Labl_is_ConnWord );

            //�������ʳ��ִ����Լ�����Ϊ���ʳ��ִ���
            Integer connNum    = Resource.allWordsDict.get(wContent).getExpNum();
            Integer notConnNum = Resource.NotAsDiscourseWordDict.get(wContent);

            if( connNum == null )    connNum = 0;
            if( notConnNum == null ) notConnNum = 0;

            item.setConnNum(connNum);
            item.setNotConnNum(notConnNum);

            candidateTerms.add(item);
        }

        ArrayList<String> features = new ArrayList<String>();

        //b: ʹ��ģ���ж�ÿ����ѡ�Ĵ��Ƿ�������
        for( ConnVectorItem item : candidateTerms )
        {
//            int label = connSvmMode.predict(item);
            String strFeatures =  item.getContent() + " " + item.getLength() + " " + item.getPositionInLine() + " " +
                    item.getAmbiguity() + " " + item.getOccurInDict() + " " + item.getPos() + " " +
                    item.getPrevPos() + " " + item.getNextPos();
            features.add(strFeatures);
            util.writeLinesToFile("isConnective.test.txt",features);

            String result = ClassifyViaMaximumEntrop("isConnectiveModel.txt","isConnective.test.txt");
//            System.out.println("The result is " + result);
            int label  = Integer.valueOf(result);

            if( label > 0 || item.getOccurInDict() > 100 )
            {
                DSAConnective connective = new DSAConnective( item.getContent(), sentence.getId() );
                String strWords[] = segmentResult.split(" ");

                ArrayList<String> stringArrayList = new ArrayList<String>();
                for(int i = 0; i < strWords.length; i++){
                    stringArrayList.add(strWords[i]);
                }
                List<Term>  posOfWords  = util.getSegmentedSentencePosTag(segmentResult);

                int indexOfConnective = stringArrayList.indexOf(item.getContent());
                String prev1 = "",prev2 = "";
                try {
                    prev1 = stringArrayList.get(indexOfConnective - 1);
                    prev2 = stringArrayList.get(indexOfConnective - 2);
                }
                catch (IndexOutOfBoundsException e){
                    System.out.println(e.toString());
                }

                String prev2Pos  = "";
//                prev2Pos = sentence.getAnsjWordTerms().get(indexOfConnective - 1).getRealName();
                Term prev2Term;
                try{
                    prev2Term = sentence.getAnsjWordTerms().get(indexOfConnective - 3).getTo();
                    prev2Pos = prev2Term.getNatrue().natureStr;
                    item.setPrev2Pos(prev2Pos);
                }
                catch (IndexOutOfBoundsException e){
                    System.out.println(e.toString());
                }

                connective.setPrev1(prev1);
                connective.setPrev2(prev2);
                connective.setPosTag(item.getPos());
                connective.setPrevPosTag(item.getPrevPos());
                connective.setPrev2PosTag(item.getPrev2Pos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine(item.getPositionInLine());

                sentence.getConWords().add(connective);
            }
        }
    }

    /**
     * Ϊ������һ�������Ǿ�����ʻ��Ǿ������ʣ���ʶ��������֮����Ҫ�����ж����������Ӿ��ڹ�ϵ�������Ӿ���ϵ��
     * DSAConnective������һ�����ԣ�isInterConnective
     **/
    private void markConnAsInterOrCross(DSASentence sentence)
    {
        for(DSAConnective curWord:sentence.getConWords() )
        {
            String wContent = curWord.getContent();

            //1: ��ȡ��������p2��p3�зֱ���ֵĴ���
            int numInP2 = 0, numInP3 = 0;
            if( Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                numInP2 = Resource.ConnInP2AndP3.get(wContent)[0];
                numInP3 = Resource.ConnInP2AndP3.get(wContent)[1];
            }

            //2����ȡ����������Arg������
            int argConnArg = 0, connArgArg = 0;
            if( Resource.ConnectiveArgNum.containsKey(wContent) )
            {
                argConnArg = Resource.ConnectiveArgNum.get(wContent)[0];
                connArgArg = Resource.ConnectiveArgNum.get(wContent)[1];
            }

            //3: ����λ�ھ����м��λ��, �Ƿ�λ�ھ���
            int position = (int) curWord.getPositionInLine();

            //4���Ƿ�λ�ڵ�һ���̾���
            boolean inSentenceHead = true;
            String temp = sentence.getContent().substring(0, position);
            if( temp.contains("��") || temp.contains(",") ) inSentenceHead = false;

            if( numInP2 > numInP3 && inSentenceHead ) curWord.setInterConnective(false);
        }
    }




    /**
     * Ϊ������һ�������Ǿ�����ʻ��Ǿ������ʣ���ʶ��������֮����Ҫ�����ж����������Ӿ��ڹ�ϵ�������Ӿ���ϵ��
     * DSAConnective������һ�����ԣ�isInterConnective
     * @param sentence
     * @return
     * @throws IOException
     */
    private boolean markConnAsInterOrCross(DSASentence sentence,int id) throws IOException {
        boolean argPosition = true;
        ArrayList results = new ArrayList();

        String wTestFileName = "arg_pos.test.txt" ,wModelFileName = "arg_posModel.txt";     //ģ���ļ����������ļ���

//      a. ��ȡ����
        for(DSAConnective curWord:sentence.getConWords() )
        {
            String wContent = curWord.getContent();

            //1: ��ȡ��������p2��p3�зֱ���ֵĴ���
            int numInP2 = 0, numInP3 = 0;
            if( Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                numInP2 = Resource.ConnInP2AndP3.get(wContent)[0];
                numInP3 = Resource.ConnInP2AndP3.get(wContent)[1];
            }

            //2����ȡ����������Arg������
            int argConnArg = 0, connArgArg = 0;
            if( Resource.ConnectiveArgNum.containsKey(wContent) )
            {
                argConnArg = Resource.ConnectiveArgNum.get(wContent)[0];
                connArgArg = Resource.ConnectiveArgNum.get(wContent)[1];
            }

            //3: ����λ�ھ����м��λ��, �Ƿ�λ�ھ���
            int position = (int) curWord.getPositionInLine();

            //4���Ƿ�λ�ڵ�һ���̾���
            boolean inSentenceHead = true;
            String temp = sentence.getContent().substring(0, position);
            if( temp.contains("��") || temp.contains(",") ) inSentenceHead = false;

//            if( numInP2 > numInP3 || inSentenceHead ) curWord.setInterConnective(false);

            //5����ȡ������������Ԫλ�÷���
//            String str = curWord.getContent() + " " + curWord.getPosTag() + " " + curWord.getPrevPosTag() + " " + curWord.getPositionInLine();
            String str = curWord.getContent() + " " + curWord.getPosTag() + " " + curWord.getPrev1() + " " + curWord.getPrevPosTag() + " "
                    +curWord.getPrev1() + curWord.getContent() + " " +curWord.getPosTag()+curWord.getPrevPosTag() +" "+ curWord.getPrev2()
                    +" "+ curWord.getPrev2PosTag() + " " +curWord.getPosTag() + curWord.getPrev2PosTag() + " "
                    + curWord.getContent() + curWord.getPrev2() +" " + curWord.getPositionInLine()
                    + " " + numInP2 + " " + numInP3 + " " + argConnArg + " " + connArgArg;
            results.add(str);

        }

        util.writeLinesToFile(wTestFileName,results);

        //b. ʹ�������ģ�ͽ��з��࣬ resultΪ������
        String result =  ClassifyViaMaximumEntrop(wModelFileName,wTestFileName);

        if(result.equals("PS")){
            argPosition = false;
        }
        else{
            argPosition = true;
        }

        return  argPosition;
    }

    /**
     *��Ԫλ�÷��࣬�жϹ�������ָʾ�Ĺ�ϵ�Ǿ��ڹ�ϵ���Ǿ���ϵ
     * @param sentence
     * @param id
     * @param train
     * @param pNumber
     * @param wConnective  Connective in Corpus
     * @return true ����
     * @throws IOException
     */
    private boolean markConnAsInterOrCross(DSASentence sentence,int id,Boolean train,int pNumber,String wConnective) throws IOException {
        boolean argPosition = true;
        ArrayList results = new ArrayList();
        ArrayList resultsForTest = new ArrayList();
        int nDividing = pNumber==2?1524:4409;       //ѵ�����ݺͲ������ݵķֽ���

        for(DSAConnective curWord:sentence.getConWords() )
        {
            String wContent = curWord.getContent();

            if(!wContent.equalsIgnoreCase(wConnective)) continue;

            //1: ��ȡ��������p2��p3�зֱ���ֵĴ���
            int numInP2 = 0, numInP3 = 0;
            if( Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                numInP2 = Resource.ConnInP2AndP3.get(wContent)[0];
                numInP3 = Resource.ConnInP2AndP3.get(wContent)[1];
            }

            //2����ȡ����������Arg������
            int argConnArg = 0, connArgArg = 0;
            if( Resource.ConnectiveArgNum.containsKey(wContent) )
            {
                argConnArg = Resource.ConnectiveArgNum.get(wContent)[0];
                connArgArg = Resource.ConnectiveArgNum.get(wContent)[1];
            }

            //3: ����λ�ھ����м��λ��, �Ƿ�λ�ھ���
            int position = (int) curWord.getPositionInLine();

            //4���Ƿ�λ�ڵ�һ���̾���
            boolean inSentenceHead = true;
            String temp = sentence.getContent().substring(0, position);
            if( temp.contains("��") || temp.contains(",") ) inSentenceHead = false;

            if( numInP2 > numInP3 || inSentenceHead ) curWord.setInterConnective(false);

            //5����ȡ������������Ԫλ�÷���

            String str = curWord.getContent() + " " + curWord.getPosTag() + " " + curWord.getPrev1() + " " + curWord.getPrevPosTag() + " "
                    +curWord.getPrev1() + curWord.getContent() + " " +curWord.getPosTag()+curWord.getPrevPosTag() +" "+ curWord.getPrev2()
                    +" "+ curWord.getPrev2PosTag() + " " +curWord.getPosTag() + curWord.getPrev2PosTag() + " "
                    + curWord.getContent() + curWord.getPrev2() +" " + curWord.getPositionInLine()
                    + " " + numInP2 + " " + numInP3 + " " + argConnArg + " " + connArgArg;
//                    +" " + wType;

            if(id < nDividing)
            {
                //����ѵ��
                if(3 == pNumber)        //SS
                {
                    argPosition = true;
                    str += " SS";
                }
                else            //PS
                {
                    argPosition = false;
                    str += " PS";
                }
                results.add(str);
                util.appendMethodB("arg_pos.txt",results);
            }

            else
            {
                //���ڲ���
                String string;
                if(3 == pNumber)        //SS
                {
                    argPosition = true;
                    string = "SS";

                }
                else            //PS
                {
                    argPosition = false;
                    string = "PS";
                }
                results.add(str);
                util.appendMethodB("arg_pos.test",results);
                resultsForTest.add(string);
                util.appendMethodB("arg_pos_result.txt",resultsForTest);
            }
        }

        return  argPosition;
    }

    /**
     * 3: ����һ�������еĸ��Ϲ����ʣ����磺����...����
     * Ŀǰ��ʶ�𷽷�ֻ��ʶ����һ����������Ϊ���ܵĲ������ʡ������ճ��ִ����Ĵ�С���ж�
     * @param sentence
     */
    private void findParallelWord(DSASentence sentence)
    {
        int occurTimes      = -1;
        String sentContent  = sentence.getSegContent();

        DSAConnective parallelConnective = null;

        for(Map.Entry<String, DSAWordDictItem> entry:Resource.allWordsDict.entrySet())
        {
            if( !entry.getValue().isParallelWord() ) continue;

            String parallelWord = entry.getKey();
            Integer numInDict   = entry.getValue().getExpNum();

            //if( numInDict < 2 ) continue;

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

                //2: ������ѡ���������б�. ֻ��ȡ���ִ������Ĳ�������
                if(  numInDict > occurTimes )
                {
                    if( parallelConnective == null )
                        parallelConnective = new DSAConnective(parallelWord, sentence.getId());

                    occurTimes = numInDict;
                    parallelConnective.setContent(parallelWord);
                    parallelConnective.setBeginIndex1(beginIndex1);
                    parallelConnective.setBeginIndex2(beginIndex2);
                    parallelConnective.setParallelWord(true);
                }
            }
        }

        if( parallelConnective != null )
            sentence.getParallelConnectives().add(parallelConnective);
    }

    //-------------------------------2: EDU segment-----------------------------

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
            DSAInterRelation relation = new DSAInterRelation();
            relation.setArg1Content(arg1.getContent());
            relation.setArg2Content(arg2.getContent());
            relation.setDsaConnective(curWord);
            relation.setRelType(Constants.EXPLICIT);
            sentence.getRelations().add(relation);
        }
    }


    /**
     * ���ݶ���ṹ����������ȡ��Ӧ�Ķ���ṹ�����Դ�����ȡ����EDU��ע������ĵ���Ԥ����(�ֺô�)֮���DSASentence
     * @param sentence
     */
    public boolean findArgumentWithPhraseParser(DSASentence sentence) throws IOException {
        Tree phraseResult;
        try{
            phraseResult = this.phraseParser.parseLine( sentence.getSegContent() );
        }
        catch (OutOfMemoryError e){
            return false;
        }

        //1: �ݹ�Ĺ�������EDU����Ҫ�ǻ�ȡ�������ֵ�EDU������
        DSAEDU rootEDU = new DSAEDU(sentence.getContent(), sentence.getContent());
        rootEDU.setRoot(phraseResult.firstChild());

        boolean result = getEDUsFromRoot(rootEDU);

        if( result == false ) return false; //Ϊfalse��ʾ�þ��ӵĶ���ṹ�������ָþ��Ӳ��ǺϷ��ľ��ӡ�

        //2: ����ͷ�������EDU����Ҫ�ǽ�����EDU���Ͻ��з���, �������EDU����
        this.clearDuplictEDUs(rootEDU);

        //3: ��ȡ����EDU��Ϣ���з�װ����Ҫ����ȡ��EDU�����ݵ���Ϣ
        this.getEDUContent(rootEDU);

        //����EDU��Ϣ
        sentence.setRootEDU(rootEDU);

        //4: �������������ӵ�����EDU
        //this.matchConnectiveAndEDU(sentence);

        return  true;
    }

    /**
     * ��Ԫ������ȡ����������ط���䷨���еĽڵ㣺Arg1 node��Arg2 node ��None
     *��ȡ���������ʵ��ַ��������ʵľ䷨���ࣨ�������ʡ��������ʣ����������ֵܵĸ������������ֵܵĸ�����·��P�Լ�����C�����ֵܵĸ����Ƿ����1���ڵ�C�����λ��(���С���)
     * @param sentence
     * @throws IOException
     */
    public void argLaberler(DSASentence sentence) throws IOException {

//        �����Ӱ��ն���ṹ��ֳɻ���EDU

        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList<DSAConnective> conns = sentence.getConWords();
        String arg1Content = "",arg2Content = "";
        boolean  result = false;

//      ��Ǿ䷨���е�ÿһ���ڵ�

        Tree rootTree = rootEDU.getRoot();


        for(DSAConnective connective:conns)     //����ÿ��������
        {

            for( Tree child : rootTree.children() )       //���ʾ䷨����ÿ�����ӽ��
            {
                //�����ǵ��������ĺ��ӽڵ㣺�����ڣ���->Pron->N֮���
                if( child.depth() == child.size() - 1 ) continue;

                String strChild = getRootContent(child);

                DSAEDU curEDU = new DSAEDU();
                curEDU.setRoot(child);
                curEDU.setParentEDU(rootEDU);
                curEDU.setDepth( rootEDU.getDepth() + 1 );

                boolean temp = getEDUsFromRoot( curEDU );

                if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

                result = result || temp;

                //nClassΪ������
                int nClass = InnerNodeClassification(child,sentence);

                System.out.println(strChild + ":" + nClass);

                if(1 == nClass){
                    connective.setArg1Content(strChild);
                }
                else if(2 == nClass){
                    connective.setArg2Content(strChild);
                }
                else continue;
            }
        }

    }

    /**
     * �䷨���ڲ���������ط���ģ�� ѵ������
     * @param tree,senseRecord
     * @return
     * @throws IOException
     */
    private int InnerNodeClassificationTrain(Tree tree,SenseRecord senseRecord,DSASentence sentence,int index,String wConnective) throws IOException
    {
        String text = "",arg1Content = "",arg2Content = "";
;       int type = 0;

        text        = senseRecord.getText();
        arg1Content = senseRecord.getArg1();
        arg2Content = senseRecord.getArg2();

//        DSASentence sentence = new DSASentence(text);
//        sentence.setSegContent(text);
//        sentence.setId(index++);
//
//        this.preProcess(sentence,false);
//        this.findConnWordWithML(sentence);
//        this.markConnAsInterOrCross(sentence,index);
//        this.findParallelWord(sentence);
//        this.findArgumentWithPhraseParser(sentence);

        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList featureList = new ArrayList();
        ArrayList classResult = new ArrayList();
        int leftSiblings = 0 ,rightSiblings = 0;
        ArrayList<DSAConnective> conns = sentence.getConWords();

        for( DSAConnective curWord : conns )
        {
//            if( !curWord.getInterConnective() ) continue;

            String wContent = curWord.getContent();
            ConnVectorItem conn = new ConnVectorItem(wContent);

            if(!wContent.equalsIgnoreCase(wConnective)) continue;

            DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);
            int numChildren;
            DSAEDU parrentEDU;

//            Tree parrentTree = connEDU.getRoot().parent();
//            Tree parrentTree = parrentEDU.getRoot();
//            connEDU.getBeginIndex();
//            int numChildren = parrentTree.numChildren();

            try {
                parrentEDU = connEDU.getParentEDU();
                numChildren = parrentEDU.getRoot().numChildren();
            }
            catch(NullPointerException e){
                continue;
            }


            for(DSAEDU dsaEDU :parrentEDU.getChildrenEDUS() ){
                if(!dsaEDU.equals(connEDU)) {
                    leftSiblings += 1;
                }
                else break;
            }

            rightSiblings = numChildren - leftSiblings - 1;

            if( connEDU == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
                System.out.println(sentence.getContent());
                curWord.setIsConnective(false);
                continue;
            }

            List<Tree> listPath = tree.pathNodeToNode(connEDU.getRoot(),tree);

            int leftSiblingsGreaterThanOne = (leftSiblings>1)?1:0;

            String results = "";
//           results += getRootContent(tree);
//                results += getRootContent(dsaedu.getRoot()) + " ";
//                results = curWord.getContent() + curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings + " "  + listPath.toString() + " " +  leftSiblingsGreaterThanOne ;
//          results = curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings +  " " +  leftSiblingsGreaterThanOne ; // 4 dimention
            results = curWord.getContent() + " " + curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings +  " " +  leftSiblingsGreaterThanOne ;


//            DSASentence dsaSentence1 = new DSASentence(arg1Content);
//            DSASentence dsaSentence2 = new DSASentence(arg2Content);
            String strType ;

            if(arg1Content.contains(getRootContent(tree))){
                strType = "arg1";
                type = 1;
            }
            else if(arg2Content.contains(getRootContent(tree))){
                strType = "arg2";
                type = 2;
            }
            else {
                strType = "None";
                type = 0;
            }

            if(index < 4409)
            {
                results += " " + strType; /* + " " + dsaedu.getRoot().toString() */
                featureList.add(results);
                util.appendMethodB("./arg_ext.txt", featureList);

            }
            else
            {
                featureList.add(results);
                util.appendMethodB("./arg_ext.test", featureList);
                classResult.add(strType);
                util.appendMethodB("./arg_extResult.txt",classResult);
            }

        }

        return type;
    }


    /**
     *�䷨���ڲ��ڵ����:1����ȡ������2������ط���
     * @param tree,sentence
     * @throws IOException
     */
    private int InnerNodeClassification(Tree tree, DSASentence sentence) throws IOException
    {

        int nType = 0;
//      1. ������ȡ
        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList featureList = new ArrayList();

        int leftSiblings = 0 ,rightSiblings = 0;
        ArrayList<DSAConnective> conns = sentence.getConWords();

        String wModelFileName = "arg_extModel.txt" ,wTestFileName = "arg_ext.test.txt";       //ģ���ļ����������ļ���

//        int times = 0;
        for( DSAConnective curWord : conns )
        {

//            System.out.println("Times are " + times);
//            times ++;

            if( !curWord.getInterConnective() ) continue;

            String wContent = curWord.getContent();
            ConnVectorItem conn = new ConnVectorItem(wContent);

            DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);
            DSAEDU parrentEDU;

            connEDU.getBeginIndex();

            int numChildren;
            try {
                parrentEDU = connEDU.getParentEDU();
                numChildren = parrentEDU.getRoot().numChildren();
            }
            catch(NullPointerException e){
                continue;
            }

            for(DSAEDU dsaEDU :parrentEDU.getChildrenEDUS() ){
                if(!dsaEDU.equals(connEDU)) {
                    leftSiblings += 1;
                }
                else break;
            }

            rightSiblings = numChildren - leftSiblings - 1;

            if( connEDU == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
                System.out.println(sentence.getContent());
                curWord.setIsConnective(false);
                continue;
            }

//            List<Tree> listPath = dsaedu.getRoot().pathNodeToNode(connEDU.getRoot(),dsaedu.getRoot());
            List<Tree> listPath = tree.pathNodeToNode(connEDU.getRoot(),tree);

            int leftSiblingsGreaterThanOne = (leftSiblings>1)?1:0;

            String results = "";
//            results = curWord.getContent() + curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings + " "  + listPath.toString() + " " +  leftSiblingsGreaterThanOne ;
            results = curWord.getContent() + " " +  curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings +  " " +  leftSiblingsGreaterThanOne ;
            featureList.add(results);
        }

        util.writeLinesToFile(wTestFileName,featureList);

        String wResult  = ClassifyViaMaximumEntrop(wModelFileName,wTestFileName);    //ʹ�������ģ�ͽ��з���

        String wType = "";
        if(wResult.equals("arg1"))     {
            wType = "arg1";
            nType = 1;
        }
        else if(wResult.equals("arg2")){
            wType = "arg2";
            nType = 2;
        }
        else {
            wType = "None";
            nType = 0;
        }

        return nType;
    }

//   private String getConnectFeatures()
//   {
//       String results = "";
//       int leftSiblings = 0 ,rightSiblings = 0;
//       ArrayList<DSAConnective> conns = sentence.getConWords();
//
//       for( DSAConnective curWord : conns )
//       {
//           if( !curWord.getInterConnective() ) continue;
//
//           String wContent = curWord.getContent();
//           ConnVectorItem conn = new ConnVectorItem(wContent);
//
//           DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);
//           DSAEDU parrentEDU = connEDU.getParentEDU();
//
//           connEDU.getBeginIndex();
//           int numChildren = parrentEDU.getChildrenEDUS().size();
//
//           for(DSAEDU dsaEDU :parrentEDU.getChildrenEDUS() ){
//               if(!dsaEDU.equals(connEDU)) {
//                   leftSiblings += 1;
//               }
//               else break;
//           }
//
//           rightSiblings = numChildren - leftSiblings - 1;
//
//           if( connEDU == null )
//           {
//               System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
//               System.out.println(sentence.getContent());
//               curWord.setIsConnective(false);
//               continue;
//           }
//
//           int leftSiblingsGreaterThanOne = (leftSiblings>1)?1:0;
//
//           results = curWord.getContent() + " " + leftSiblings + " " + rightSiblings + " " +  leftSiblingsGreaterThanOne;
//       }
//
//        return  results;
//   }





    /**
     * ��������ʼ���ݹ�Ĺ���EDU����ÿ������ֻ�ܰ���ֱ�Ӻ��ӽڵ��EDU��һֱ�ݹ���ȥ��
     * �˴λ�ȡ����ֻ�Ǽ򵥵�ÿ��EDU�����νṹ�е�Root��������Ҫ�ٴ���
     **/
    private boolean getEDUsFromRoot(DSAEDU rootEDU ) throws IOException {
        boolean  result = false;

        //�ݹ鴦��������ӽڵ�
        for( Tree child : rootEDU.getRoot().children() )
        {
            //�����ǵ��������ĺ��ӽڵ㣺�����ڣ���->Pron->N֮���
            if( child.depth() == child.size() - 1 ) continue;

            DSAEDU curEDU = new DSAEDU();
            curEDU.setRoot(child);
            curEDU.setParentEDU(rootEDU);
            curEDU.setDepth( rootEDU.getDepth() + 1 );

            boolean temp = getEDUsFromRoot( curEDU );

            if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

            result = result || temp;

            DSASentence sentence;
            try {
                sentence = new DSASentence(curEDU.getRoot().toString());
            }
            catch (NullPointerException e){
                continue;
            }

        }
        //�жϵ�ǰ�ڵ��Ƿ񹹳���һ���������EDU,�ݹ���ֹ����
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("vp") )
        {
            if( rootEDU.getRoot().children().length > 1 ) result = true;
        }
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("np") )
        {
            result = false;
        }

        return result;
    }

    /**
     * ��ȡEDU������ѵ������ģ��
     * @param rootEDU
     * @param senseRecord
     * @return
     * @throws IOException
     */
    private boolean getEDUsFromRoot(DSAEDU rootEDU,SenseRecord senseRecord) throws IOException {
        boolean  result = false;

        //�ݹ鴦��������ӽڵ�
        for( Tree child : rootEDU.getRoot().children() )
        {
            //�����ǵ��������ĺ��ӽڵ㣺�����ڣ���->Pron->N֮���
            if( child.depth() == child.size() - 1 ) continue;

            DSAEDU curEDU = new DSAEDU();
            curEDU.setRoot(child);
            curEDU.setParentEDU(rootEDU);
            curEDU.setDepth( rootEDU.getDepth() + 1 );

            boolean temp = getEDUsFromRoot( curEDU ,senseRecord);

            if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

            result = result || temp;
        }

        //�жϵ�ǰ�ڵ��Ƿ񹹳���һ���������EDU,�ݹ���ֹ����
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("vp") )
        {
            if( rootEDU.getRoot().children().length > 1 ) result = true;
        }
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("np") )
        {
            result = false;
        }

        return result;
    }



    /**
     * �Ի���EDU���������˲�����Ҫ��Ϊ����������EDU����Ҫ�ǽ��������кϲ�����ֻ��һ��EDU��root�������Ϸ���
     */
    private void clearDuplictEDUs(DSAEDU rootEDU)
    {
        /**
        //�Ժ��ӽڵ����edu����
        for(DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            this.clearDuplictEDUs( childEDU );
        }

       //����ýڵ�ֻ��һ������EDU�ڵ㣬��ô���Խ��ú��ӽڵ�EDU���Ϸ�������ȡ���ú��ӽڵ��EDU����
        if( rootEDU.getChildrenEDUS().size() == 1 )
        {
            DSAEDU childEDU = rootEDU.getChildrenEDUS().get(0);
            ArrayList<DSAEDU> temp = null;

            if( childEDU.getChildrenEDUS().size() > 1 ) temp = childEDU.getChildrenEDUS();

            rootEDU.getChildrenEDUS().clear();

            if( temp != null )
            {
                rootEDU.setChildrenEDUS(temp);
                for(DSAEDU curEDU:temp) curEDU.setParentEDU(rootEDU);
            }
        }
         **/

        for(DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            this.clearDuplictEDUs(childEDU);
        }

        //ֻ��Ҷ�ӽڵ��EDU��������
        if(rootEDU.getChildrenEDUS().size() == 1)
        {
            if(rootEDU.getChildrenEDUS().get(0).getChildrenEDUS().size() == 0)
            {
                rootEDU.getChildrenEDUS().clear();
            }
        }
    }


    /**
     * ���ÿ��EDU�ڵ㣬��ȡ�ýڵ���������ַ���ֵ����������EDU�ͷ�EDU�ڵ��ֵ��
     */
    private String getEDUContent(DSAEDU rootEDU)
    {
        //���ȼ�����ڵ��content
        Tree   root    = rootEDU.getRoot();
        String content = getRootContent(root);

        rootEDU.setContent(content);

        //����������ӽڵ��contentֵ
        for( DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            String temp = getEDUContent(childEDU);
        }

        return content;
    }

    /**�ڶ���ṹ���л�ȡ��root�ڵ��µ��ַ���ֵ��**/
    private String getRootContent(Tree root)
    {
        String content = "";

        for( Label curWord : root.yield() ) content += curWord.value() + " ";

        if( content.length() > 1 ) content = content.substring(0, content.length() - 1 );

        return content;
    }

    //-------------------------------2.1: Matchd Edu and connective--------------------

    /***
     * Ѱ����ʽ����������������EDU
     * @param sentence
     */
    private ArrayList<String> matchConnAndEDUforExpRelation(DSASentence sentence)
    {
        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList<DSAConnective> conns = sentence.getConWords();
        ArrayList<String> argList = new ArrayList<String>();

        //���ÿ�����ʶ���Ҫ���д���. ÿ������������һ��EDU���ڡ�
        for( DSAConnective curWord : conns )
        {
            if( !curWord.getInterConnective() ) continue;

            String wContent = curWord.getContent();
            DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);

            if( connEDU == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
                System.out.println(sentence.getContent());
                curWord.setIsConnective(false);
                continue;
            }

            //d: ȷ��arg2���������ݣ���arg2EDU��ʼ�����ҵ���������
            //���䷨�������б��Ϊarg2 Node�Ľڵ���װ����

            //e: ȷ��arg1���������ݣ�����ͱȽϱ����ˡ�                    //���䷨�������б��Ϊarg1 Node�Ľڵ���װ����

//            String arg1Content = "",arg2Content = "";;

            String arg1Content = this.getArg1Content(curWord, connEDU);     //���ڹ���
//            String arg1Content = sentence.getArg1();          //���ڻ���ѧϰ

//            arg1Content = curWord.getArg1Content();     //
            curWord.setArg1Content(arg1Content);
            argList.add(arg1Content);

            String arg2Content = this.getArg2Content(curWord, connEDU);       //���ڹ���
//            String arg2Content = sentence.getArg2();      //���ڻ���ѧϰ

//            arg2Content = curWord.getArg2Content();     //
            curWord.setArg2Content(arg2Content);
            argList.add(arg2Content);

//            if(sentence.getArg1().isEmpty())
//            {
//                sentence.setArg1("��Ԫ 1 �� �յ� ");
//                arg1Content = sentence.getArg1();
//            }
//            if(sentence.getArg2().isEmpty())
//            {
//                sentence.setArg2("��Ԫ 2 �� �յ�");
//                arg2Content = sentence.getArg2();
//            }

            if( arg2Content == null || arg2Content.length() < 2 ){ curWord.setIsConnective(false); continue; }
            if( arg1Content == null || arg1Content.length() < 2 ){ curWord.setInterConnective(false); continue;}

            //f: ���ɺ�ѡ��ϵ, ��ʱ��ϵ��ֻ�����ʺ�argument����û��ʶ������ϵ
            DSAInterRelation interRelation = new DSAInterRelation();

            interRelation.setDsaConnective(curWord);
            interRelation.setArg2Content(arg2Content);
            interRelation.setArg1Content(arg1Content);
            interRelation.setSentID(sentence.getId());

            sentence.getRelations().add(interRelation);
        }

        return argList;
    }


    //�ھ䷨�ṹ�����ҵ������ʽڵ�
    private  Tree getConnectiveInTree(DSAConnective curWord,DSAEDU rootEDU)
    {
        int index = 0;

        Tree curWordNode = null;
        String  wContent = curWord.getContent();

        List<Tree> phraseLeaves = rootEDU.getRoot().getLeaves();

        for( Tree curNode : phraseLeaves )
        {
            String curWordContent = curNode.nodeString();

            if( curWordContent.equalsIgnoreCase(wContent) )
            {
                if( Math.abs( curWord.getPositionInLine() - index ) < 3 )
                {
                    curWordNode = curNode;
                    break;
                }
            }
            index = index + curWordContent.length();
        }

        if( curWordNode == null )
        {
            System.out.print("[--Error--] Not Found " + wContent + "'s Tree Node In ");
            System.out.print(rootEDU.getContent());
            return null;
        }

        return curWordNode;
    }


    /**��ȡһ�������ڶ���ṹ������������EDU�ڵ㡣���շ��ص��Ǹ�������ֱ��������EDU�ڵ�**/
    private DSAEDU getConnectiveBelongEDU(DSAConnective curWord, DSAEDU rootEDU)
    {
        //a: ���Ҹô��ڶ���ṹ�����������ڵĽڵ�
        int index = 0;

        Tree curWordNode = null;
        String  wContent = curWord.getContent();

        List<Tree> phraseLeaves = rootEDU.getRoot().getLeaves();

        for( Tree curNode : phraseLeaves )
        {
            String curWordContent = curNode.nodeString();

            if( curWordContent.equalsIgnoreCase(wContent) )
            {
                if( Math.abs( curWord.getPositionInLine()-index ) < 3 )
                {
                    curWordNode = curNode; break;
                }
            }
            index = index + curWordContent.length();
        }

        if( curWordNode == null )
        {
            System.out.print("[--Error--] Not Found " + wContent + "'s Tree Node In ");
            System.out.print(rootEDU.getContent());
            return null;
        }

        //b: ��connNode����ȥ����������,To-Do�������������� Modified Connective:����ԭ���ǣ�
        while( curWordNode.parent(rootEDU.getRoot()) != null )
        {
            if(curWordNode.parent(rootEDU.getRoot()).children().length == 1)
            {
                curWordNode = curWordNode.parent(rootEDU.getRoot());
            }
            else break;
        }
        curWord.setConnNode(curWordNode);

        //c: ȷ����������������EDU������ֱ�������ĵ�һ��EDU�ڵ���Ϊ���ڵ�����������EDU
        int num = 0, error = 0;
        Tree parentNode = curWordNode.parent( rootEDU.getRoot() );

        while( parentNode != null && !this.isAEDURoot(parentNode, rootEDU) )
        {
            parentNode = parentNode.parent(rootEDU.getRoot());
            if( parentNode.nodeString().equalsIgnoreCase("np") || num++ > 3 )
            {
                error = 1;break;
            }
        }

        //d: ���������û�и������κ�һ����Ч��EDU��
        if( error == 1 || parentNode == null )
        {
            System.out.print("[--Error--] Not Found " + wContent + "'s ParentNode In ");
            System.out.print(rootEDU.getContent());
            return null;
        }

        return this.findTreeEDU(parentNode, rootEDU);
    }

    /***
     * ȷ��һ�����ʵ�Arg2��Χ����ֱ���ϲ�EDU���Һ��ӽڵ㼯��. ���ص���arg2���ַ������ݣ����տո�ָ�
     * @param connWord������
     * @param arg2EDU������������ֱ���ϲ�EDU
     */
    private String getArg2Content(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg2Content = "";
        if( arg2EDU.getChildrenEDUS().size() < 2 )
        {
            connWord.getArg2Nodes().add( arg2EDU.getRoot() );
            connWord.setArg2EDU( arg2EDU );

            arg2Content = this.getEDUContent(arg2EDU);
        }
        else
        {
            //����arg2�Ŀ�ʼ��
            boolean find  = false;
            Tree    root  = arg2EDU.getRoot();

            for( Tree curChild : root.children() )
            {
                //�жϵ�ǰ���ӽڵ��Ƿ��ǿ�ʼ�ڵ�
                if( !find )
                {
                    if( curChild == connWord.getConnNode() ){
                        find = true;
                    }
                }
                else
                {
                    //�ӿ�ʼ�����ұߵĶ�����arg2,ֱ�����ұߵ�EDU,������������ŵ�ѭ�����棬��arg2�����������ʽڵ�
                    connWord.getArg2Nodes().add(curChild);

                    arg2Content += this.getRootContent(curChild) + " ";

                    //��������һ��EDU��ʱ���˳���
                    if( this.isAEDURoot(curChild, arg2EDU) )
                    {
                        DSAEDU temp = this.findTreeEDU(curChild, arg2EDU);
                        connWord.setArg2EDU( temp );
                        break;
                    }
                }
            }
        }
     return  arg2Content;
    }


    /**
     *���䷨�������б��Ϊarg2 Node�Ľڵ���װ����
     * @param connWord
     * @param arg2EDU
     * @return
     */
    private String getArg2ContentNew(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg2Content = "";


        return arg2Content;
    }

    /*** �ж�һ���䷨�������еĽڵ��Ƿ���EDU�ڵ㡣*/
    private boolean isAEDURoot(Tree root, DSAEDU rootEDU)
    {
        boolean result = false;

        /**
        DSAEDU rootEDU = new DSAEDU();
        rootEDU.setRoot(root);

        result = this.getEDUsFromRoot(rootEDU);
         **/

        if( rootEDU.getRoot() == root ) return  true;

        for( DSAEDU childEDU:rootEDU.getChildrenEDUS() )
        {
            boolean temp = this.isAEDURoot(root, childEDU);

            result = result || temp;
        }

        return result;
    }

    /**����һ��Tree root ��λ�ڵ�EDU�ڵ�**/
    private DSAEDU findTreeEDU(Tree root, DSAEDU rootEDU)
    {
        if( rootEDU.getRoot() == root ) return rootEDU;

        for( DSAEDU curEDU:rootEDU.getChildrenEDUS() )
        {
            DSAEDU childResult = this.findTreeEDU(root, curEDU);

            if( childResult != null ) return childResult;
        }

        return null;
    }

    private String getArg1Content(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg1Content = "";
        String wContent = connWord.getContent();
        int argConnArg  = Resource.ConnectiveArgNum.get(wContent)[0];
        int connArgArg  = Resource.ConnectiveArgNum.get(wContent)[1];


        if( argConnArg > connArgArg )
        {
            arg1Content = this.getArgConnArgContent(connWord, arg2EDU);
        }
        else
        {
            arg1Content = this.getConnArgArgContent(connWord, arg2EDU);
        }
        return arg1Content;
    }

    //    ���䷨���б��ΪArg1 Node ���ڲ��ڵ���װ����
    private String getArg1ContentNew(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg1Content = "";

        return arg1Content;
    }


    /**
     * ��������ΪArg-Conn-Arg���������漰������һ��EDU��
     * ���ص���ͬһArg2EDU�ڵ��µ��ֵܽڵ㣬������Arg2���ڵ��е��ֵܽڵ�
     **/
    private String getArgConnArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU     = null;
        String arg1Content = "";

        //���arg2EDU������Ϊһ��EDU����arg1EDUΪarg2EDU���ֵ�EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            DSAEDU parentEDU = arg2EDU.getParentEDU();

            if( parentEDU == null ) return null;

            int parentChildSize = parentEDU.getChildrenEDUS().size();

            //���Ҷ�Ӧ��arg1EDUλ�á�
            for(int index = 1; index < parentChildSize; index++)
            {
                if(parentEDU.getChildrenEDUS().get(index) == arg2EDU)
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index - 1);
                    break;
                }
            }
            //û���ҵ�arg1EDU�����Ϊ��arg2EDUΪparent�ĵ�һ�� || arg2EDU��parentֻ��һ��
            //�����Arg-Conn-Arg���͵�û���ҵ�����϶�����Ϊarg2EDU��parent�ĵ�һ������
            if( arg1EDU == null )
            {
                //��������һ��,��Ϊ����ֲ���EDU�����
                DSAEDU grandPaEDU = parentEDU.getParentEDU();
                if( grandPaEDU != null )
                {
                    int grandPaChildSize = grandPaEDU.getChildrenEDUS().size();
                    for(int index = 1; index < grandPaChildSize; index++)
                    {
                        if( grandPaEDU.getChildrenEDUS().get(index) == arg2EDU ){
                            arg1EDU = parentEDU.getChildrenEDUS().get(index-1);
                        }
                    }
                }

                //��ʱ�������Ϊarg2EDU���Ǹ�����EDU�ˣ���ôֱ�ӷ���null, �����Ǹ��׽ڵ��ֱ���Һ���
                if( arg1EDU == null && arg2EDU.getParentEDU() != null && parentChildSize > 1 )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(1);
                }
            }
        }
        else
        {
            //���arg2EDU�����edu��ֹһ��������Arg-Conn-Arg�����£�arg1EDU�����������
            //1��λ��arg2EDU�е�һ������EDU�С�
            //2: λ��arg2EDU���׽ڵ��е�һ��
            for( int index = 0; index < arg2EDU.getChildrenEDUS().size(); index++)
            {
                DSAEDU curEDU = arg2EDU.getChildrenEDUS().get(index);

                if( curEDU == connWord.getArg2EDU() && index > 0 )
                {
                    arg1EDU = arg2EDU.getChildrenEDUS().get(index -  1);
                    break;
                }
            }
            if( arg1EDU == null )
            {
                //����һ����г�ȡһ��
                if( arg2EDU.getParentEDU() != null )
                {
                    DSAEDU prev = null;
                    for( DSAEDU curEDU : arg2EDU.getParentEDU().getChildrenEDUS() )
                    {
                        if( curEDU == arg2EDU ){
                            arg1EDU = prev;
                            break;
                        }else{
                            prev = curEDU;
                        }
                    }
                    //����ϲ�Ҳû��
                    if( arg1EDU == null ) arg1EDU = arg2EDU.getChildrenEDUS().get(1);
                }
            }
        }

        if( arg1EDU != null )
        {
            connWord.setArg1EDU(arg1EDU);
            arg1Content = this.getEDUContent(arg1EDU);
        }

        return arg1Content;
    }

    /***��ȡ����ΪConnArgArg��arg1�ڵ㡣**/
    private String getConnArgArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU     = null;
        String arg1Content = "";

        //���arg2EDU������Ϊһ��EDU����arg1EDUΪarg2EDU���ֵ�EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            DSAEDU parentEDU    = arg2EDU.getParentEDU();

            if( parentEDU == null ) return null;

            int parentChildSize = parentEDU.getChildrenEDUS().size() - 1;

            //���Ҷ�Ӧ��arg1EDUλ�á�
            for( int index = 0; index < parentChildSize; index++ )
            {
                if( parentEDU.getChildrenEDUS().get(index) == arg2EDU )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index + 1);
                    break;
                }
            }

            //�����Conn-Arg-Arg���͵�û���ҵ�����϶�����Ϊarg2EDU��parent�����һ������
            if( arg1EDU == null )
            {
                //��ʱ�������Ϊarg2EDU���Ǹ�����EDU�ˣ���ôֱ�ӷ���null, �����Ǹ��׽ڵ��ֱ������
                if( arg2EDU.getParentEDU() != null && parentChildSize >= 2 )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(parentChildSize - 2);
                }
            }
        }
        else
        {
            //���arg2EDU�����edu��ֹһ��������Conn-Arg-Arg�����£�arg1EDU�����������
            //1��λ��arg2EDU�е�һ������EDU�С�
            //2: λ��arg2EDU���׽ڵ��е�һ��
            for( int index = 0; index < arg2EDU.getChildrenEDUS().size() - 1; index++)
            {
                DSAEDU curEDU = arg2EDU.getChildrenEDUS().get(index);

                if( curEDU == connWord.getArg2EDU() )
                {
                    arg1EDU = arg2EDU.getChildrenEDUS().get(index + 1);
                    break;
                }
            }

            if( arg1EDU == null )
            {
                //����һ����г�ȡһ��
                if( arg2EDU.getParentEDU() != null )
                {
                    for( int index = 0; index < arg2EDU.getParentEDU().getChildrenEDUS().size() - 1; index++ )
                    {
                        DSAEDU curEDU = arg2EDU.getParentEDU().getChildrenEDUS().get(index);
                        if( curEDU == arg2EDU )
                        {
                            arg1EDU = arg2EDU.getParentEDU().getChildrenEDUS().get(index + 1);
                        }
                    }
                    //����ϲ�Ҳû��. ����ΪcurWord��arg2EDU�Ǹ���arg2EDU�����ұߵĺ���
                    if( arg1EDU == null )
                    {
                        arg1EDU = arg2EDU.getChildrenEDUS().get(arg2EDU.getChildrenEDUS().size() - 2);
                    }
                }
            }
        }

        if( arg1EDU != null )
        {
            connWord.setArg1EDU( arg1EDU );
            arg1Content = this.getEDUContent(arg1EDU);
        }

        return arg1Content;
    }


    /**ʹ�ò�����������ȡEDU��������ѡ�Ĺ�ϵ**/
    private void matchParallelConnAndEDUforExpRelation(DSASentence sentence)
    {
        String segSentContent = sentence.getSegContent();

        for(DSAConnective curord:sentence.getParallelConnectives())
        {
            String wContent = curord.getContent();

            String arg1Content ="",arg2Content="";
            try{
                arg1Content = segSentContent.substring( curord.getBeginIndex1(), curord.getBeginIndex2() );
                arg2Content = segSentContent.substring( curord.getBeginIndex2() );
            }
            catch (StringIndexOutOfBoundsException e){
                System.out.println(e.toString());
            }

            DSAInterRelation interRelation = new DSAInterRelation();
            interRelation.setArg1Content(arg1Content);
            interRelation.setArg2Content(arg2Content);
            interRelation.setSentID(sentence.getId());
            interRelation.setDsaConnective(curord);
        }
    }

    //-------------------------------3: Sense Recognize---------------------------

    /***ʶ��������ʽ��ϵ����**/
    private void recognizeExpRelationType(DSASentence sentence) throws IOException
    {
        for( DSAInterRelation curRel:sentence.getRelations() )
        {
            DSAConnective curConn = curRel.getDsaConnective();
            String wContent       = curConn.getContent();

            String wFeature = "",wPrev = "",wPosOfConn = "";
            ArrayList<String> arrayList = new ArrayList<String>();
            String arrayfWords[] = sentence.getContent().split(" ");
            int indexOfConnective = 0;

//            for(int nIndex = 0 ; nIndex < arrayfWords.length ; nIndex++)
//            {
//                if(arrayfWords[nIndex].equalsIgnoreCase(wContent))
//                {
//                    indexOfConnective = nIndex;
//                }
//            }
//
//            wPosOfConn = sentence.getAnsjWordTerms().get(indexOfConnective).getNatrue().natureStr;
//
//            try {
//                wPrev = arrayfWords[indexOfConnective - 1];
//                if (wPrev.isEmpty())
//                {
//                    wPrev = arrayfWords[indexOfConnective - 3];
//                }
//            }
//            catch (ArrayIndexOutOfBoundsException e)
//            {
//                System.out.println(e.toString());
//            }
//
//            wFeature = wContent + " " + wPosOfConn + " " + wContent + wPrev;
//            arrayList.add(wFeature);
//            util.writeLinesToFile(".\\explicit.test.txt",arrayList);

//           String   senseNO =  ClassifyViaMaximumEntrop("arg_posModel.txt","explicit.test.txt");

            DSAWordDictItem item  = Resource.allWordsDict.get(wContent);
            double probality      = item.getMostExpProbality();
            String senseNO        = item.getMostExpProbalityRelNO();

            curRel.setRelType(Constants.EXPLICIT);

            if( Constants.SenseVersion == Constants.NewSenseVersion )
            {
                curRel.setRelNO( util.convertOldRelIDToNew(senseNO) );
            }
            else
            {
                curRel.setRelNO(senseNO);
            }

            curRel.setProbality(probality);
        }
    }


    /**��ʶ��ֻ���������ɺ�ѡ����ʽ��ϵ**/
    private void getCandiateImpRelation(DSASentence sentence) throws IOException
    {
        if(sentence.getRelations().size() > 0 ) return;

        // �ڻ�����EDU�Ļ����Ͽ�ʼ��������ƥ��
        DSAEDU rootEDU = sentence.getRootEDU();

        //��ȡ�����е�EDU�ԣ���Ϊ��ʽ��ϵ�ĺ�ѡ
        //�Ӹ������¿�ʼѰ�ҵ�һ�����ӽڵ��������1��EDU�ڵ�
        while( rootEDU.getChildrenEDUS().size() == 1 )
        {
            rootEDU = rootEDU.getChildrenEDUS().get(0);
        }

        //�����˳���ԭ���������Ϊֻ��һ�����ӽڵ㣬����û����
        if( rootEDU.getChildrenEDUS().size() > 1 )
        {
            //����˳����Բ�����ѡ
            for(int index = 1; index < rootEDU.getChildrenEDUS().size(); index++)
            {
                DSAEDU curEDU  = rootEDU.getChildrenEDUS().get(index);
                DSAEDU prevEDU = rootEDU.getChildrenEDUS().get(index - 1);

                //�жϺ��ӽڵ�
                this.getImpRelationInSubEDU(sentence, curEDU);
                this.getImpRelationInSubEDU(sentence, prevEDU);

                DSAInterRelation interRelation = new DSAInterRelation();

                interRelation.setSentID(sentence.getId());
                interRelation.setRelType(Constants.IMPLICIT);
                interRelation.setRelNO(Constants.DefaultRelNO);

                interRelation.setArg1Content(prevEDU.getContent());
                interRelation.setArg2Content(curEDU.getContent());

                //�ж��Ƿ��й�ϵ
                RelVectorItem item = impRelFeatureExtract.getFeatureLine(
                interRelation.getArg1Content(), interRelation.getArg2Content() );
                item.relNO = Constants.DefaultRelNO;

                int senseType = this.relationSVMMode.predict( item.toLineForLibsvm() );

                if( senseType == 0 ) continue;

                interRelation.setRelNO( String.valueOf(senseType) );
                //sentence.getImpRelations().add(interRelation);
                sentence.getRelations().add(interRelation);
            }
        }
    }

    private void getImpRelationInSubEDU(DSASentence sentence, DSAEDU rootEDU) throws IOException
    {
        if( rootEDU.getChildrenEDUS().size() < 2 ) return;

        //����˳����Բ�����ѡ
        for(int index = 1; index < rootEDU.getChildrenEDUS().size(); index++)
        {
            DSAEDU curEDU  = rootEDU.getChildrenEDUS().get(index);
            DSAEDU prevEDU = rootEDU.getChildrenEDUS().get(index - 1);

            DSAInterRelation interRelation = new DSAInterRelation();

            interRelation.setSentID(sentence.getId());
            interRelation.setRelType(Constants.IMPLICIT);
            interRelation.setRelNO(Constants.DefaultRelNO);

            interRelation.setArg1Content(prevEDU.getContent());
            interRelation.setArg2Content(curEDU.getContent());

            //�ж��Ƿ��й�ϵ
            RelVectorItem item = impRelFeatureExtract.getFeatureLine(
                    interRelation.getArg1Content(), interRelation.getArg2Content() );
            item.relNO = Constants.DefaultRelNO;

            int senseType = this.relationSVMMode.predict( item.toLineForLibsvm() );

            if( senseType == 0 ) continue;

            interRelation.setRelNO( String.valueOf(senseType) );
            //sentence.getImpRelations().add(interRelation);
            sentence.getRelations().add(interRelation);
        }
    }

    /***ʶ����ܴ��ڵ���ʽ����ϵ**/
    private void recognizeImpRelationType(DSASentence sentence) throws IOException
    {

    }

    private ArrayList<DSAConnective> findConnective(String line) throws Exception
    {
        ArrayList<String> sentences = util.filtStringToSentence(line);

        DSASentence dsaSentence = new DSASentence(line);

        //this.findConnectiveWithRule(dsaSentence);

        ArrayList<DSAConnective> conWords = dsaSentence.getConWords();

        return conWords;
    }

    //-----------------------3.1: Cross Sentence Sense Recognize-----------------

    /**�ж�һ�������е����������Ƿ������ʽ�ľ���ϵ��**/
    public boolean getCrossExpRelInPara(DSAParagraph para, DSASentence curSentence, DSASentence nextSentence) throws IOException {
        boolean hasExpRel = false;

        //1: �����ж��Ƿ����connArgArg���͵Ĺ�ϵ
        for(DSAConnective conn:curSentence.getConWords())
        {
            if( !conn.getInterConnective() && conn.getIsConnective() )
            {
                int argConnArg = Resource.ConnectiveArgNum.get(conn.getContent())[0];
                int connArgArg = Resource.ConnectiveArgNum.get(conn.getContent())[1];

                if( connArgArg > argConnArg )
                {
                    DSACrossRelation crossRelation = new DSACrossRelation();

                    crossRelation.arg1SentID  = curSentence.getId();
                    crossRelation.arg2SentID  = nextSentence.getId();
                    crossRelation.arg1Content = curSentence.getContent();
                    crossRelation.arg2Content = nextSentence.getContent();

                    crossRelation.conn        = conn;
                    crossRelation.relType     = Constants.EXPLICIT;

                    DSAWordDictItem item      = Resource.allWordsDict.get( conn.getContent() );
                    double     probality      = item.getMostExpProbality();
                    String       senseNO      = item.getMostExpProbalityRelNO();

                    crossRelation.relNO       = util.convertOldRelIDToNew(senseNO);
                    crossRelation.probality   = probality;

                    para.crossRelations.add(crossRelation);

                    hasExpRel = true;
                }
            }
        }

        //2: �ж��Ƿ����conn-Arg-Arg���͵Ĺ�ϵ
        for( DSAConnective conn:nextSentence.getConWords() )
        {
            if( !conn.getInterConnective() && conn.getIsConnective() )
            {
                int argConnArg = Resource.ConnectiveArgNum.get(conn.getContent())[0];
                int connArgArg = Resource.ConnectiveArgNum.get(conn.getContent())[1];

                if( connArgArg <= argConnArg )
                {
                    DSACrossRelation crossRelation = new DSACrossRelation();

                    crossRelation.arg1SentID  = curSentence.getId();
                    crossRelation.arg2SentID  = nextSentence.getId();
                    crossRelation.arg1Content = curSentence.getContent();
                    crossRelation.arg2Content = nextSentence.getContent();

                    crossRelation.conn        = conn;
                    crossRelation.relType     = Constants.EXPLICIT;

                    DSAWordDictItem item      = Resource.allWordsDict.get( conn.getContent() );
                    double     probality      = item.getMostExpProbality();
                    String       senseNO      = item.getMostExpProbalityRelNO();

                    crossRelation.relNO       = util.convertOldRelIDToNew(senseNO);
                    crossRelation.probality   = probality;

                    para.crossRelations.add(crossRelation);

                    hasExpRel = true;
                }
            }
        }

        return hasExpRel;
    }


    /**�ж�һ�������е����������Ƿ������ʽ��ϵ**/
    private int getCrossImpInPara(DSAParagraph para, DSASentence curSentence,
                                  DSASentence nextSentence) throws IOException
    {
        String cur  = curSentence.getSegContent();
        String nex = nextSentence.getSegContent();

        RelVectorItem item = impRelFeatureExtract.getFeatureLine(cur, nex);
        item.relNO         = Constants.DefaultRelNO;

        int senseType = this.relationSVMMode.predict( item.toLineForLibsvm() );

        //������ڷ�0��ʽ��ϵ
        if( senseType != 0 )
        {
            DSACrossRelation crossRelation = new DSACrossRelation();

            crossRelation.arg1SentID  = curSentence.getId();
            crossRelation.arg2SentID  = nextSentence.getId();
            crossRelation.arg1Content = curSentence.getContent();
            crossRelation.arg2Content = nextSentence.getContent();

            crossRelation.relType     = Constants.IMPLICIT;
            crossRelation.relNO       = String.valueOf(senseType);
            crossRelation.probality   = 1.0;
            crossRelation.conn        = null;

            //TO-DO: ������������Ϻ�ѡ���ʣ������ĸ����ʿ��Բ���
            para.crossRelations.add(crossRelation);
        }

        return senseType;
    }


     //-----------------------4����������-----------------
    /**����EDU�зִ����׼ȷ��**/
    public void countEDUAccuray() throws DocumentException, IOException {
        Resource.LoadRawRecord();
        this.phraseParser = new PhraseParser();

        int allEDUNum = 0, arg1Correct = 0, arg2Correct = 0;//��ȫ��ͬ�ĸ���
        int[] arg1CorrectInPercent   = new int[10];
        int[] arg2CorrectInPercent   = new int[10];

        DSAEDU rootEDU = null;
        String text, arg1Content, arg2Content, arg1EDU, arg2EDU;

        //�����ȡ200�������׼ȷ�ʣ� ��300��������
        boolean[] exist = util.getRandomArrays(Resource.Raw_Train_Annotation_p3.size(), 500);

        for(int index = 0; index < Resource.Raw_Train_Annotation_p3.size(); index++)
        {
            if( !exist[index] ) continue;

            SenseRecord curRecord = Resource.Raw_Train_Annotation_p3.get(index);

            text        = curRecord.getText();
            arg1Content = curRecord.getArg1();
            arg2Content = curRecord.getArg2();

            if( curRecord.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;
            if( text.length() > Constants.Max_Sentence_Length + 100 ) continue;

            //System.out.println(curRecord.getfPath());
            //System.out.println(text);

            //�鿴���ж���ṹ�����õ�������Argument
            DSASentence dsaSentence = new DSASentence(text);
            dsaSentence.setSegContent(text);

            try
            {
                boolean  eduResult = this.findArgumentWithPhraseParser(dsaSentence);
                if( !eduResult ) continue;
            }
            catch(OutOfMemoryError e)
            {
                continue;
            }

            //��ȡ�Զ������Ľ��
            rootEDU = dsaSentence.getRootEDU();

            while( rootEDU.getChildrenEDUS().size() == 1 ) rootEDU = rootEDU.getChildrenEDUS().get(0);
            if( rootEDU.getChildrenEDUS().size() < 2 ) continue;

            allEDUNum = allEDUNum + 2;

            arg1EDU = rootEDU.getChildrenEDUS().get(0).getContent();
            arg2EDU = rootEDU.getChildrenEDUS().get(1).getContent();

            //�ж��˹���ע���Զ�������EDU�����ƶ�
            int sameNum1 = util.countSameCharatersNum(arg1Content, arg1EDU);
            int sameNum2 = util.countSameCharatersNum(arg2Content, arg2EDU);

            int length1 = arg1Content.length() > arg1EDU.length() ? arg1EDU.length():arg1Content.length();
            int length2 = arg2Content.length() > arg2EDU.length() ? arg2EDU.length():arg2Content.length();

            if( Math.abs(sameNum1 - length1) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg1Correct++;
            if( Math.abs(sameNum2 - length2) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg2Correct++;


            //����7�����ƣ������Ƶ�ռ��3��
            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.4) arg1CorrectInPercent[6]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.3) arg1CorrectInPercent[7]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.3) arg2CorrectInPercent[7]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.2) arg1CorrectInPercent[8]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.2) arg2CorrectInPercent[8]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.1) arg1CorrectInPercent[9]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.1) arg2CorrectInPercent[9]++;

             //����Զ�����������˹���ע���
            System.out.println("-------------****************************----------------------");
            System.out.println(text);
            System.out.println("annote1: " + arg1Content);
            System.out.println("myAuto1: " + arg1EDU);
            System.out.println("annote2: " + arg2Content);
            System.out.println("myAuto1: " + arg2EDU);
        }

        System.out.println("All EDU:" + allEDUNum );
        System.out.println(" arg1Correct:" + arg1Correct + " arg2Correct:" + arg2Correct );

        for(int index = 7; index < 10; index ++)
        {
            System.out.format("Same Percentnge %d0 percent arg1CorrectIn:%d arg2Correct:%d\n", index, arg1CorrectInPercent[index], arg2CorrectInPercent[index]);
        }
    }

    /**
     * ����ʹ�ö�������EDU�����зֵõ���׼ȷ�ʡ�Ŀǰ�������Ǽ�ʹ�ö��ŶԾ��ӽ��зָ�
     * ���ͻ��ڹ����EDU�зֽ��жԱȡ�
     * @throws DocumentException
     */
    public void countEDUAccurayWithComma() throws DocumentException, IOException
    {
        Resource.LoadRawRecord();

        int allEDUNum = 0, arg1Correct = 0, arg2Correct = 0;//��ȫ��ͬ�ĸ���
        int[] arg1CorrectInPercent   = new int[10];
        int[] arg2CorrectInPercent   = new int[10];


        String text, arg1Content, arg2Content, arg1EDU, arg2EDU;

        //�����ȡ200�������׼ȷ�ʣ� ��300��������
        boolean[] exist = util.getRandomArrays(Resource.Raw_Train_Annotation_p3.size(), 500);

        String fPath = "data/edu/eduSplitWithSimpleComma.txt";
        BufferedWriter fw = new BufferedWriter(new FileWriter(new File(fPath)));

        for(int index = 0; index < Resource.Raw_Train_Annotation_p3.size(); index++)
        {
            if( !exist[index] ) continue;

            SenseRecord curRecord = Resource.Raw_Train_Annotation_p3.get(index);

            text        = curRecord.getText();
            arg1Content = curRecord.getArg1();
            arg2Content = curRecord.getArg2();

            if( curRecord.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;
            if( text.length() > Constants.Max_Sentence_Length + 100 ) continue;

            //�Ծ��ӽ��ж��ŷָ������
            allEDUNum = allEDUNum + 2;

            String[] edus =  util.splitSentenceWithComma(text);

            if( edus.length < 2 ) continue;

            arg1EDU = edus[0];
            arg2EDU = edus[1];

            fw.write("---------------************************---------------------------\n");
            fw.write( text + "\nannoted1: " + arg1Content + "\n comma: " + edus[0] + "\n");
            fw.write( "annoted2: " + arg2Content + "\n comma: " + edus[1] + "\n\n");

            //�����Զ�����׼ȷ��
            int sameNum1 = util.countSameCharatersNum(arg1Content, arg1EDU);
            int sameNum2 = util.countSameCharatersNum(arg2Content, arg2EDU);

            int length1 = arg1Content.length() > arg1EDU.length() ? arg1EDU.length():arg1Content.length();
            int length2 = arg2Content.length() > arg2EDU.length() ? arg2EDU.length():arg2Content.length();

            if( Math.abs(sameNum1 - length1) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg1Correct++;
            if( Math.abs(sameNum2 - length2) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg2Correct++;


            //����7�����ƣ������Ƶ�ռ��3��
            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.4) arg1CorrectInPercent[6]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.3) arg1CorrectInPercent[7]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.3) arg2CorrectInPercent[7]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.2) arg1CorrectInPercent[8]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.2) arg2CorrectInPercent[8]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.1) arg1CorrectInPercent[9]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.1) arg2CorrectInPercent[9]++;


        }
        fw.close();

        System.out.println("All EDU:" + allEDUNum );
        System.out.println(" arg1Correct:" + arg1Correct + " arg2Correct:" + arg2Correct );

        for(int index = 7; index < 10; index ++)
        {
            System.out.format("Same Percentnge %d0 percent arg1CorrectIn:%d arg2Correct:%d\n", index, arg1CorrectInPercent[index], arg2CorrectInPercent[index]);
        }

    }

    /**
     * �䷨���ڲ�������, prepare corpus for training and testing.
     * @throws IOException
     */
    private void train( ) throws IOException
    {

        int index = 0;
//      ��Ǿ䷨���е�ÿһ���ڵ�
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            if (senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT)) continue;      //filter implicit relations.

            int nClass = 0;
            String wConnective = senseRecord.getConnective();
            String arg1Content = "",arg2Content = "";
            String text = senseRecord.getText();
            DSASentence sentence = new DSASentence(text);
            sentence.setSegContent(text);
            sentence.setId(index);

            try{
                this.preProcess(sentence,false);
                this.findConnWordWithML(sentence);
                this.markConnAsInterOrCross(sentence,index);
                this.findParallelWord(sentence);

                this.findArgumentWithPhraseParser(sentence);

                DSAEDU rootEDU = sentence.getRootEDU();

                Tree tChildren[] = rootEDU.getRoot().children();

                for( Tree child : rootEDU.getRoot().children() )
                {
                    //�����ǵ��������ĺ��ӽڵ㣺�����ڣ���->Pron->N֮���
                    if( child.depth() == child.size() - 1 ) continue;

                    DSAEDU curEDU = new DSAEDU();
                    curEDU.setRoot(child);
                    curEDU.setParentEDU(rootEDU);
                    curEDU.setDepth( rootEDU.getDepth() + 1 );

                    boolean temp = getEDUsFromRoot( curEDU);
//                    if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

                    nClass = InnerNodeClassificationTrain(child,senseRecord,sentence,index,wConnective);
                    if(1 == nClass)          sentence.setArg1(curEDU.getContent());
                    else if(2 == nClass)     sentence.setArg2(curEDU.getContent());
                    else  continue;

                }
                index++;
                System.out.println("SenseRecord number is :" + index);
            }
            catch ( NullPointerException e){
                System.out.println("NullPointerException!");
            }
        }

    }

    /**
     * ��ȡarg1��arg2��None����ѵ����������Ŀ
     * @throws IOException
     */
    private void trainArgument( ) throws IOException
    {
        String arg1Content = "",arg2Content = "",text;
        int index = 0;

        int nClass = 0;
//      ��Ǿ䷨���е�ÿһ���ڵ�

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            if (senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT)) continue;

            String wConnective = senseRecord.getConnective();

            try{
                text = senseRecord.getText();

                DSASentence sentence = new DSASentence(text);
                sentence.setSegContent(text);
                sentence.setId(index);

                this.preProcess(sentence,false);
                this.findConnWordWithML(sentence);
                this.markConnAsInterOrCross(sentence,index);
                this.findParallelWord(sentence);

                this.findArgumentWithPhraseParser(sentence);

                DSAEDU rootEDU = sentence.getRootEDU();

                Tree tChildren[] = rootEDU.getRoot().children();

                for( Tree child : rootEDU.getRoot().children() )
                {
                    //�����ǵ��������ĺ��ӽڵ㣺�����ڣ���->Pron->N֮���
                    if( child.depth() == child.size() - 1 ) continue;

                    DSAEDU curEDU = new DSAEDU();
                    curEDU.setRoot(child);
                    curEDU.setParentEDU(rootEDU);
                    curEDU.setDepth( rootEDU.getDepth() + 1 );

                    boolean temp = getEDUsFromRoot( curEDU);
//                    if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

                    InnerNodeClassificationTrain(child,senseRecord,sentence,index,wConnective);
                    if(1 == nClass)          sentence.setArg1(curEDU.getContent());
                    else if(2 == nClass)     sentence.setArg2(curEDU.getContent());
                    else  continue;;

                }
                index++;
                System.out.println("SenseRecord number is :" + index);
            }
            catch ( NullPointerException e){
                System.out.println("NullPointerException!");
            }
        }

    }


    /**
     * ��Ԫλ�÷���
     * @throws IOException
     */
    private void trainPosition( ) throws IOException
    {
        String text;
        int index = 0;

//      ��Ǿ䷨���е�ÿһ���ڵ�
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            text        = senseRecord.getText();

            String wConnective = senseRecord.getConnective();

            DSASentence sentence = new DSASentence(text);
            sentence.setSegContent(text);
            sentence.setId(index);

            this.preProcess(sentence,false);
            this.findConnWordWithML(sentence);
            this.markConnAsInterOrCross(sentence,index,true,3,wConnective);
            this.findParallelWord(sentence);

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
        }
    }


    /**
     * ͳ��1������P2������P3������������
     */
    private void countWordNumbers()
    {
        String text,arg1Content,arg2Content;
        long lWordNumberofP1 = 0,lWordNumberofP2 = 0 ,lWordNumberofP3 = 0,lWordNumberofTotal = 0;
        long lRelationNumber = 0;

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p1)
        {

            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofP1 += strContent.length();
        }

        System.out.print("Total word number of P1 is ");
        System.out.println(lWordNumberofP1);

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        {

            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofP2 += strContent.length();
        }

        System.out.print("Total word number of P2 is ");
        System.out.println(lWordNumberofP2);

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofP3 += strContent.length();
        }
        System.out.print("Total word number of P3 is ");
        System.out.println(lWordNumberofP3);

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation)
        {

            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofTotal += strContent.length();
            lRelationNumber ++;
        }

        System.out.print("Total word number is ");
        System.out.println(lWordNumberofTotal);

        System.out.print("Total relation number is ");
        System.out.println(lRelationNumber);

    }

    /**
     * ������Ԫ�Զ��зֵ�׼ȷ��
     * @throws DocumentException
     */
    public void computeArgumentAccuracy() throws Exception {
//        Resource.LoadRawRecord();

        double  dAccuracyOfArg1,dAccuracyOfArg2;

        int nIndex = 0,nNumbers = 0;      //index ΪRecord�ı�ţ���numbers����ѡȡ��Record�ı��
        int nRightOfArg1 = 0, nRightOfArg2 = 0;                 //�õ���ȷ��Ԫ����Ŀ

        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {

            if(record.getType().equals(Constants.IMPLICIT) || (nIndex % 3) != 0) //������ʽ��ϵ�����������������ȡһЩ���������3�ı�����record���������3�ı�������������
            {
                nIndex++;
                continue;
            }
            else
            {
                if(nNumbers < 300)      //ֻ��ȡ300����ʾƪ�¹�ϵ�ļ�¼
                {
                    String content = record.getText();
                    DSASentence sentence = new DSASentence(content);
                    sentence.setId(nNumbers);

                    try{
                        this.processSentence(sentence,false);
                    }
                    catch (NullPointerException e){
                        nIndex++;
                        continue;
                    }

                    String arg1 = sentence.getArg1();
                    String arg2 = sentence.getArg2();

                    if(arg1.equals(record.getArg1()))
                    {
                        nRightOfArg1 ++;
                    }
                    if(arg2.equals(record.getArg2()))
                    {
                        nRightOfArg2++;
                    }

                    nIndex++;
                    nNumbers++;
                }
                else  break;

            }
        }

        System.out.println("The number of right arg1 is " + nRightOfArg1);
        System.out.println("The number of right arg2 is " + nRightOfArg2);
        System.out.println("The number of total arguments is " + nNumbers);

        dAccuracyOfArg1 = (double)nRightOfArg1 / nNumbers;
        dAccuracyOfArg2 = (double)nRightOfArg2 / nNumbers;

        System.out.println("The accuracy of arg1 is " + dAccuracyOfArg1);
        System.out.println("The accuracy of arg2 is " + dAccuracyOfArg2);
    }

    /**
     * �õ�ÿ���������ϵ�arg1��arg2����д���ļ�arg.txt������֮�������Ԫʶ��׼ȷ��
     * @throws IOException
     */
    public void argument() throws IOException {

        int index = 0;

//      ��Ǿ䷨���е�ÿһ���ڵ�

//        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
//            if(index++ < 4366)    continue;
            if(index++ < 6053)    continue;

            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;

            ArrayList<String> argList = new ArrayList<String>();

//            text        = senseRecord.getText();
//            arg1Content = senseRecord.getArg1();
//            arg2Content = senseRecord.getArg2();

            argList.add(senseRecord.getArg1() + "|" + senseRecord.getArg2());
            util.appendMethodB("arg.txt",argList);

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
        }
    }


    public void getArgumentOfTestCorpora() throws  IOException
    {
        int index = 0;

//        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
//            if(index++ < 4366)    continue;
            if(index++ < 6053)    continue;

            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;

            String strArg1="",strArg2="";

            ArrayList<String> argList = new ArrayList<String>();
            boolean argPosition = true;     //Ĭ�Ͻ���Ԫλ������ΪSS

            DSASentence sentence = new DSASentence(senseRecord.getText());
            sentence.setId(index);

            //1: ���Ƚ���Ԥ�������еײ��NLP�����ִʡ����Ա�ע
            this.preProcess(sentence, false);

            //2: ʶ�𵥸����ʺ�ʶ�������ʣ�����...����
            this.findConnWordWithML(sentence);
            argPosition = this.markConnAsInterOrCross(sentence,index);
            this.findParallelWord(sentence);

            //4: �����Ӱ��ն���ṹ��ֳɻ���EDU
            //this.findArgumentInLine(sentence);
            boolean tempResult = this.findArgumentWithPhraseParser(sentence);

            //4.1��������־��ӽṹ�Ƿ���Sentence
            if( !tempResult ){ sentence.setIsCorrect(false); return; }

            //��Ԫλ��ΪSS(�־��ϵ)
            if(argPosition)
            {
                //�ڲ��ڵ���ࣺArg1��Arg2��None
                argLaberler(sentence);      //��ȡ���������䷨���ڲ��ڵ���з��ࣺArg1 Node��Arg2 Node��None
            }

            argList = this.matchConnAndEDUforExpRelation(sentence);

//            if(argList.isEmpty())   continue;

            strArg1 = argList.get(0);
            strArg2 = argList.get(1);

            argList.add(strArg1 + "|" + strArg2);
            util.appendMethodB("argOfTestCorpora.txt",argList);

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
            System.out.println(senseRecord.getText());

        }

    }

    public void comuteRelationAccuracy() throws IOException
    {
        int index = 0,nNumberOfRight = 0, nNumberOfTotal= 0; ;
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
//            if(index++ < 4366)    continue;
//            if(index++ < 6053)    continue;
            index++;
            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;
            if(senseRecord.getText().length() >= 500)    continue;
            if(26 == index || 53 == index)     continue;

//            if(index >= 50) break;
//            if(index < 55)  continue;
            if(index > 200) break;
            if(nNumberOfTotal > 100)   break;      //ֻѡȡ200��


            String strArg1="",strArg2="",strTypeInCorpus = "",strType = "";

            strTypeInCorpus = senseRecord.getRelNO();
            String arrayOfRightTypy[] = strTypeInCorpus.split("-");


            ArrayList<String> argList = new ArrayList<String>();
            boolean argPosition = true;     //Ĭ�Ͻ���Ԫλ������ΪSS

            DSASentence sentence = new DSASentence(senseRecord.getText());
            sentence.setId(index);

            //1: ���Ƚ���Ԥ�������еײ��NLP�����ִʡ����Ա�ע
            this.preProcess(sentence, false);

            //2: ʶ�𵥸����ʺ�ʶ�������ʣ�����...����
            this.findConnWordWithML(sentence);
            this.markConnAsInterOrCross(sentence);
            this.findParallelWord(sentence);

            //4: �����Ӱ��ն���ṹ��ֳɻ���EDU
            //this.findArgumentInLine(sentence);
            boolean tempResult = this.findArgumentWithPhraseParser(sentence);

            //4.1��������־��ӽṹ�Ƿ���Sentence
            if( !tempResult ){ sentence.setIsCorrect(false); continue; }

            ArrayList<DSAConnective> arrayConnective = sentence.getConWords();


            try
            {
                //5: ȷ��ÿ���������漰��������EDU��������ѡ��ʽ��ϵ
                this.matchConnAndEDUforExpRelation(sentence);
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString());
                continue;
            }

            try
            {
                this.matchParallelConnAndEDUforExpRelation(sentence);
            }
            catch (StringIndexOutOfBoundsException e)
            {
                System.out.println(e.toString());
                continue;
            }

            //6: ���ݻ�ȡ������Ϣ��ʶ����ʽ����ϵ
            this.recognizeExpRelationType(sentence);
            //7: ʶ����ܴ��ڵ���ʽ����ϵ
            this.getCandiateImpRelation(sentence);
            this.recognizeImpRelationType(sentence);

            int nNumOfRelations = sentence.getRelations().size();

            if(sentence.getRelations().isEmpty())   continue;

            nNumberOfTotal ++;
            strType = sentence.getRelations().get(0).getRelNO();
            String arrayOfType[] = strType.split("-");

            if (arrayOfType[0].equalsIgnoreCase(arrayOfRightTypy[0]))
            {
                nNumberOfRight++;
            }

//            for(int i = 0; i <nNumOfRelations; i++)
//            {
//                strType = sentence.getRelations().get(i).getRelNO();
//
//                if(strTypeInCorpus.equalsIgnoreCase(strType))
//                {
//                    nNumberOfRight++;
//                }
//            }

            System.out.println("SenseRecord number is :" + index);
        }

        double dAccuracy;
        dAccuracy = (double)nNumberOfRight / nNumberOfTotal;
        System.out.println("The number of right is " + nNumberOfRight);
        System.out.println("The total number is " + nNumberOfTotal);
        System.out.println("The accuracy of explicit relation recognition is " + dAccuracy);

    }

    /**
     * ��ȡ�����б�ע����Ԫλ�÷�����
     * @throws IOException
     */
    public void getConnectiveIsInterOrNot() throws IOException
    {
        int nIndex = 0;
        ArrayList<String> arrayList = new ArrayList<String>();

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {
            if (senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT)) continue;

            String bIsInterConnective = "true";
            arrayList.add(bIsInterConnective);
            nIndex++;
        }

        System.out.println("The record number is " + nIndex);
        util.appendMethodB("positionResult.txt",arrayList);

    }




    /**
     * ��ȡ��Ԫλ�÷�����������֮������PRF
     * @throws IOException
     */
    public void computPRFOfPosition() throws IOException
    {

        int index = 0,nNumberOfTotal = 0;
        ArrayList<String> stringArrayList = new ArrayList<String>();

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        {
            index++;
            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;
            if(senseRecord.getText().length() >= 500)    continue;
//            if(26 == index || 53 == index)     continue;

            DSASentence sentence = new DSASentence(senseRecord.getText());
            sentence.setId(nNumberOfTotal);

            //1: ���Ƚ���Ԥ�������еײ��NLP�����ִʡ����Ա�ע
            this.preProcess(sentence, false);

            //2: ʶ�𵥸����ʺ�ʶ�������ʣ�����...����
            this.findConnWordWithML(sentence);
            this.markConnAsInterOrCross(sentence);
            this.findParallelWord(sentence);

            //4: �����Ӱ��ն���ṹ��ֳɻ���EDU
            //this.findArgumentInLine(sentence);
            boolean tempResult = this.findArgumentWithPhraseParser(sentence);

            //4.1��������־��ӽṹ�Ƿ���Sentence
            if( !tempResult ){ sentence.setIsCorrect(false); continue; }

            ArrayList<DSAConnective> arrayConnective = sentence.getConWords();

            try
            {
                //5: ȷ��ÿ���������漰��������EDU��������ѡ��ʽ��ϵ
                this.matchConnAndEDUforExpRelation(sentence);
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString());
                continue;
            }


            String isInterConnective = "false";
            for(DSAConnective conn:arrayConnective)
            {
                if(!conn.getIsConnective())  continue;

                if(conn.getInterConnective())
                {
                    isInterConnective = "true";
                }
            }

            stringArrayList.add(isInterConnective);

            nNumberOfTotal ++;
            System.out.println("SenseRecord number is :" + nNumberOfTotal);
        }

        util.appendMethodB("positionForTest.txt",stringArrayList);

    }

    /**
     * ��ȡÿһ�����ϵ�����
     * @throws IOException
     */
    public void getSentRecord() throws IOException
    {

        int nIndex = 0;
        ArrayList<String> stringArrayList = new ArrayList<String>();

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            String wIndex = Integer.toString(nIndex);
            String wText = senseRecord.getText();
            String wResult = wIndex + " : " + wText;

            stringArrayList.add(wResult);
            nIndex++;
        }

        util.writeLinesToFile("./SenseRecord.txt",stringArrayList);
    }

    /**
     * ��ȡÿ�����ϣ������Ӧ����Ԫ
     * @throws IOException
     */
    public void getSentRecordAndArgument() throws IOException
    {

        int nIndex = 0;
        ArrayList<String> stringArrayList = new ArrayList<String>();

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            String wIndex = Integer.toString(nIndex);
            String wText = senseRecord.getText();
            String wArg1 = senseRecord.getArg1(), wArg2 = senseRecord.getArg2();


            String wResult = wIndex + " : " + wText + "; Arg1: " + wArg1 + "; Arg2:" + wArg2;

            stringArrayList.add(wResult);
            nIndex++;
        }

        util.writeLinesToFile("./SenseRecordAndArgument.txt",stringArrayList);
    }


    /**
     * ʹ�������ģ�ͽ��з���
     * @param wModelFileName    �����ģ�͵��ļ���
     * @param wTestFileName     �������ݵ��ļ���
     * @return                  ���ط�����
     */
    public String ClassifyViaMaximumEntrop(String wModelFileName, String wTestFileName)
    {
        String wResult="";      //wResultΪ������

        // ��������ط��������з���
        Predict predictor = null;
        String modelFileName = wModelFileName;
        try {
            GISModel m =
                    new SuffixSensitiveGISModelReader(
                            new File(modelFileName)).getModel();
            predictor = new Predict(m);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        try {
            DataStream ds =
                    new PlainTextByLineDataStream(
                            new FileReader(new File(wTestFileName)));

            while (ds.hasNext()) {
                String s = (String)ds.nextToken();
                wResult = predictor.eval(s.substring(0, s.lastIndexOf(' ')));
            }
        }
        catch (Exception e) {
            System.out.println("Unable to read from specified file: "
                    + "arg_pos.test");
            System.out.println();

        }

        return  wResult;
    }

    /**
     *��ȡP2�ļ�����ʽ��ϵ����Ŀ�����Ϊ2032
     */
    public void getExplicitNumbersInP2()
    {
        int index = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p2)
        {
            if (senseRecord.getType().equalsIgnoreCase(Constants.EXPLICIT)) index++;
        }
        System.out.println("The number of explicit relation is " + index);
    }

    /**
     *��ȡP3�ļ�����ʽ��ϵ����Ŀ�����Ϊ5879
     */
    public void getExplicitNumbersInP3()
    {
        int index = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {
            if (senseRecord.getType().equalsIgnoreCase(Constants.EXPLICIT)) index++;
        }
        System.out.println("The number of explicit relation is " + index);
    }

    /**
     * �ֱ��ȡP2�ļ���P3�ļ�����Ԫλ�÷����еĴ���ʵ��
     * @throws IOException
     */
    public void getErrorResultOfArgPos() throws IOException
    {
        int index = 0;
        ArrayList<String> arrayListOfError = new ArrayList<String>();

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {

            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            String text        = senseRecord.getText();
            String wRelNo = senseRecord.getRelNO();
            String arrType[] = wRelNo.split("-");
            String wType = arrType[0];
            String wResult ="";

            wResult = senseRecord.getConnective() + ": " + text;

            DSASentence sentence = new DSASentence(text);
            sentence.setSegContent(text);
            sentence.setId(index);

            this.preProcess(sentence,false);
            this.findConnWordWithML(sentence);
            Boolean bResult = this.markConnAsInterOrCross(sentence,index);
            this.findParallelWord(sentence);

            if (!bResult)
            {
                arrayListOfError.add(wResult);
            }

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
        }

        util.writeLinesToFile("errorInstanceInP3.txt",arrayListOfError);

    }

    /**
     * Count number that the connective is in the head of sentence.
     */
    public void countHeadInP2P3()
    {
        int nIndexOfP2 =0,nIndexOfP3 = 0;
        int nHeadInP2 = 0,nHeadInP3 = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p2)
        {
            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            String sConnective = senseRecord.getConnective();
            String sText[] = senseRecord.getArg2().split(" ");

            if(sText[0].equalsIgnoreCase(sConnective))  nHeadInP2++;

            nIndexOfP2++;
        }

        double dHeadInP2 = (double)nHeadInP2 / nIndexOfP2;
        System.out.println("The explicit number of P2 is :" + nIndexOfP2);
        System.out.println("The number of cases where connective is at the head of sentece: " + nHeadInP2);
        System.out.println("The rate of Head in P2 is : " + dHeadInP2);

        System.out.println("________________________________~���ǻ������ķָ���~________________________________________");

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {
            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            String sConnective = senseRecord.getConnective();
            String sText[] = senseRecord.getText().split(" ");

            if(sText[0].equalsIgnoreCase(sConnective)) nHeadInP3++;

            nIndexOfP3++;
        }

        double dHeadInP3 = (double)nHeadInP3 / nIndexOfP3;
        System.out.println("The explicit number of P3 is :" + nIndexOfP3);
        System.out.println("The number of cases where connective is at the head of sentece: " + nHeadInP3);
        System.out.println("The rate of Head in P3 is : " + dHeadInP3);
    }


    public void countNumbersOfP1()
    {
        int index = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p1)
        {
            index++;
        }

        System.out.println("The instances number of P1 is "+ index);
    }


    public void countCorpus()
    {
        int nExplicitOne = 0, nExplicitTwo = 0, nExplicitThree = 0, nExplicitFour = 0;
        int nImplicitOne = 0, nImplicitTwo = 0, nImplicitThree = 0, nImplicitFour = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation)
        {
            String strTypeArr[] = senseRecord.getRelNO().split("-");
            String strType = strTypeArr[0];

            if (senseRecord.getType().equalsIgnoreCase(Constants.EXPLICIT))
            {
                if (strType.equalsIgnoreCase("1"))       nExplicitOne++;
                else if(strType.equalsIgnoreCase("2"))  nExplicitTwo++;
                else if(strType.equalsIgnoreCase("3"))  nExplicitThree++;
                else  nExplicitFour++;
            }
            else
            {
                if (strType.equalsIgnoreCase("1"))       nImplicitOne++;
                else if(strType.equalsIgnoreCase("2"))  nImplicitTwo++;
                else if(strType.equalsIgnoreCase("3"))  nImplicitThree++;
                else  nImplicitFour++;
            }

        }

        System.out.println("The explicit relations:");
        System.out.println("One: " + nExplicitOne +" Two: " + nExplicitTwo +" Three: " + nExplicitThree + " Four:" + nExplicitFour);

        System.out.println("The implicit relations:");
        System.out.println("One: " + nImplicitOne +" Two: " + nImplicitTwo +" Three: " + nImplicitThree + " Four:" + nImplicitFour);

    }



    public static void main(String[] args) throws Exception
    {
        //DiscourseParser discourseParser = new DiscourseParser();
        //discourseParser.parseRawFile("E:\\Program\\Java\\DiscourseParser\\data\\TestSentenceSplit.txt");
        //discourseParser.connectiveRecognize();

        String test     = "�� ���� �� �ܶ� ����, ���� �� ���� �� ��û�� ���� �� ���� �� ���� ��";
        String multiple = "�й� ���� ��Ҫ ���� ���� ��� �� ����˹ ���� �� ��ó ���� Ŀǰ �� �Ȳ� ��չ �� �� ���� �� �� �� ��ɫ ��";
        String simple   = "�ֶ� ���� ���� �� һ �� ���� �Ϻ� �� ���� �ִ��� ���� �� ó�� �� ���� ���� �� ������ ���� �� ��� ���� ���� �� �� ��ǰ �� �� ���� �� �� �� ��� �� �� ���� ��";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        //discourseParser.run(test);
        String twoSentence = "�� �˽� �� ���н� Ŀǰ �Ѿ� ��� �� һ �� ���� �� �� һ �� �� �� �� ��� �� ���� ��ʾ �ܹ� �� ̨�� ���� �� ���н� ������ ������ �� �� ��� �׶� ˹�¸��Ħ ���� �� ���� ��ʽ ���� �� ���� ŵ���� �� ���� ���� ��� ���� ���� ʮ�� �� ��˹��� �� �佱 ��";

        String line = "�� ���� �� �ܶ� ����, ���� �� ���� �� ��û�� ���� �� ���� �� ���� ��";

        boolean needSegment  = false;

        /**
        DSASentence sentence = new DSASentence(simple);

        discourseParser.findConnWordWithML(sentence);
        discourseParser.findArgumentWithPhraseParser(sentence);

        System.out.println(sentence.getConWords().get(0).getArg2EDU().getContent());
         **/

        //���ض��ļ�����Discourse Parse �����õ���XMLд���ļ�
//        ArrayList<String> stringArrayList = new ArrayList<String>();
//        ArrayList<String> stringArrayResult = new ArrayList<String>();
//
//        if(args[0].isEmpty())
//        {
//            System.out.println("Parameter Error??");
//            return;
//        }
//
//        String strFileName = args[0];
//        String strResultFileName = strFileName + "Result.xml";
//
//        util.readFileToLines(strFileName,stringArrayList);
//
//        int index = 0;
//        for (String str:stringArrayList)
//        {
//            System.out.println(index + str);
//            index++;
//        }
//
//        DiscourseParser dp = new DiscourseParser();
//
//        for (String strContext:stringArrayList)
//        {
//            String strResult = "";
//            DSAParagraph paragraph = dp.parseRawFile(strContext, true);
//            strResult = paragraph.toXML();
//            stringArrayResult.add(strResult);
//        }
//
//        util.appendMethodB(strResultFileName,stringArrayResult);

        //Experiments
        //ʵ�����
        DiscourseParser dp = new DiscourseParser();

//        dp.getExplicitNumbersInP2();


//        dp.parseRawFile(test, needSegment);
//        dp.computeArgumentAccuracy();

//        dp.getSentRecord();
//        dp.getSentRecordAndArgument();

//        dp.getConnectiveIsInterOrNot();

         //��Ԫ���
//        dp.argument();
//        dp.getArgumentOfTestCorpora();

        //��Ԫλ�÷���ʵ��
//        dp.trainPosition();

        //��ȡ��Ԫλ�÷���Ĵ���ʵ��
//        dp.getErrorResultOfArgPos();

//        dp.computPRFOfPosition();

//        dp.computeArgumentAccuracy();


        //�䷨���ڲ�������ʵ��
        dp.train();

        //ͳ������
//        dp.countWordNumbers();
//        dp.countNumbersOfP1();
        //Count numbers of the connective is at the head of sentence.
//        dp.countHeadInP2P3();

//        dp.countCorpus();   //count numbers of four classes in Explicit and Implicit.
//        dp.comuteRelationAccuracy();
//        DSASentence dsaSentence = new DSASentence(test);
//        dp.countEDUAccuray();
//        dp.countEDUAccurayWithComma();


    }
}

