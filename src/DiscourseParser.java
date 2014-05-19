import common.Constants;
import entity.recognize.*;
import entity.train.DSAWordDictItem;
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

import java.io.IOException;
import java.util.*;

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

            this.processSentence(dsaSentence, needSegment);
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
        //1: ���Ƚ���Ԥ�������еײ��NLP�����ִʡ����Ա�ע
        this.preProcess(sentence, needSegment);

        //2: ʶ�𵥸����ʺ�ʶ�������ʣ�����...����
        this.findConnWordWithML(sentence);
        this.markConnAsInterOrCross(sentence);
        this.findParallelWord(sentence);

        //4: �����Ӱ��ն���ṹ��ֳɻ���EDU
        //this.findArgumentInLine(sentence);
        boolean tempResult = this.findArgumentWithPhraseParser(sentence);

        //4.1��������־��ӽṹ�Ƿ���Sentence
        if( !tempResult ){ sentence.setIsCorrect(false); return; }

        //5: ȷ��ÿ���������漰��������EDU��������ѡ��ʽ��ϵ
        this.matchConnAndEDUforExpRelation(sentence);
        this.matchParallelConnAndEDUforExpRelation(sentence);

        //6: ���ݻ�ȡ������Ϣ��ʶ����ʽ����ϵ
        this.recognizeExpRelationType(sentence);

        //7: ʶ����ܴ��ڵ���ʽ����ϵ
        this.getCandiateImpRelation(sentence);
        this.recognizeImpRelationType(sentence);
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
        this.markConnAsInterOrCross(sentence);

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

        sentence.setAnsjWordTerms(words);
        sentence.setContent( util.removeAllBlank(sentence.getContent()) );
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

        //b: ʹ��SVMģ���ж�ÿ����ѡ�Ĵ��Ƿ�������
        for( ConnVectorItem item : candidateTerms )
        {
            int label = connSvmMode.predict(item);

            if( label > 0 || item.getOccurInDict() > 100 )
            {
                DSAConnective connective = new DSAConnective( item.getContent(), sentence.getId() );

                connective.setPosTag(item.getPos());
                connective.setPrevPosTag(item.getPrevPos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine(item.getPositionInLine());

                sentence.getConWords().add(connective);
            }
        }

        //���÷ִʺ�Ľ��
        sentence.setSegContent( segmentResult.trim() );
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
     * 3: ����һ�������еĲ������ʣ����磺����...����,
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
    //Segment a sentence into basic edus

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
     * ���ݶ���ṹ����������ȡ��Ӧ�Ķ���ṹ�����Դ�����ȡ����EDU
     * @param sentence
     */
    public boolean findArgumentWithPhraseParser(DSASentence sentence)
    {
        Tree phraseResult = this.phraseParser.parseLine( sentence.getSegContent() );

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
     * ��������ʼ���ݹ�Ĺ���EDU����ÿ������ֻ�ܰ���ֱ�Ӻ��ӽڵ��EDU��һֱ�ݹ���ȥ��
     * �˴λ�ȡ����ֻ�Ǽ򵥵�ÿ��EDU�����νṹ�е�Root��������Ҫ�ٴ���
     **/
    private boolean getEDUsFromRoot(DSAEDU rootEDU)
    {
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
    private void matchConnAndEDUforExpRelation(DSASentence sentence)
    {
        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList<DSAConnective> conns = sentence.getConWords();

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
            String arg2Content = this.getArg2Content(curWord, connEDU);

            //e: ȷ��arg1���������ݣ�����ͱȽϱ����ˡ�
            String arg1Content = this.getArg1Content(curWord, connEDU);

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
        String wContent = connWord.getContent();
        int argConnArg  = Resource.ConnectiveArgNum.get(wContent)[0];
        int connArgArg  = Resource.ConnectiveArgNum.get(wContent)[1];

        String arg1Content;

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
            String arg1Content = segSentContent.substring( curord.getBeginIndex1(), curord.getBeginIndex2() );
            String arg2Content = segSentContent.substring( curord.getBeginIndex2() );

            DSAInterRelation interRelation = new DSAInterRelation();
            interRelation.setArg1Content(arg1Content);
            interRelation.setArg2Content(arg2Content);
            interRelation.setSentID(sentence.getId());
            interRelation.setDsaConnective(curord);
        }
    }

    //-------------------------------3: Sense Recognize---------------------------

    /***ʶ��������ʽ��ϵ����**/
    private void recognizeExpRelationType(DSASentence sentence)
    {
        for( DSAInterRelation curRel:sentence.getRelations() )
        {
            DSAConnective curConn = curRel.getDsaConnective();
            String wContent       = curConn.getContent();

            DSAWordDictItem item  = Resource.allWordsDict.get(wContent);
            double probality      = item.getMostExpProbality();
            String senseNO        = item.getMostExpProbalityRelNO();

            curRel.setRelType(Constants.EXPLICIT);
            curRel.setRelNO( util.convertOldRelIDToNew(senseNO) );
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
    public boolean getCrossExpRelInPara(DSAParagraph para, DSASentence curSentence, DSASentence nextSentence)
    {
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


    public static void main(String[] args) throws Exception
    {
        DiscourseParser discourseParser = new DiscourseParser();
        //discourseParser.parseRawFile("E:\\Program\\Java\\DiscourseParser\\data\\TestSentenceSplit.txt");
        //discourseParser.connectiveRecognize();

        String test     = "�� ���� �� �ܶ� ����, ���� �� ���� �� ��û�� ���� �� ���� �� ���� ��";
        String multiple = "�й� ���� ��Ҫ ���� ���� ��� �� ����˹ ���� �� ��ó ���� Ŀǰ �� �Ȳ� ��չ �� �� ���� �� �� �� ��ɫ ��";
        String simple   = "�ֶ� ���� ���� �� һ �� ���� �Ϻ� �� ���� �ִ��� ���� �� ó�� �� ���� ���� �� ������ ���� �� ��� ���� ���� �� �� ��ǰ �� �� ���� �� �� �� ��� �� �� ���� ��";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        //discourseParser.run(test);
        String twoSentence = "�� �˽� �� ���н� Ŀǰ �Ѿ� ��� �� һ �� ���� �� �� һ �� �� �� �� ��� �� ���� ��ʾ �ܹ� �� ̨�� ���� �� ���н� ������ ������ �� �� ��� �׶� ˹�¸��Ħ ���� �� ���� ��ʽ ���� �� ���� ŵ���� �� ���� ���� ��� ���� ���� ʮ�� �� ��˹��� �� �佱 ��";

        boolean needSegment  = false;
        /**
        DSASentence sentence = new DSASentence(simple);

        discourseParser.findConnWordWithML(sentence);
        discourseParser.findArgumentWithPhraseParser(sentence);

        System.out.println(sentence.getConWords().get(0).getArg2EDU().getContent());
         **/
        DiscourseParser dp = new DiscourseParser();
        dp.parseRawFile(twoSentence, needSegment);
    }
}

