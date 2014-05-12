package train.word;

import common.Constants;
import common.util;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.dom4j.*;
import resource.Resource;

import java.io.IOException;
import java.util.*;

/**
 * 使用机器学习方法来训练一个连词识别模型,,目前针对的是p3句间关系.主要是用来抽取特征生成训练数据TrainData
 * User: Ji JianHui
 * Time: 2014-03-05 19:45
 * Email: jhji@ir.hit.edu.cn
 */
public class connFeatureExtract
{
    private ArrayList<ConnVectorItem> items;
    private ArrayList<ConnVectorItem> filterItems;


    private ArrayList<ArrayList<String>> notLabeledWords = new ArrayList<ArrayList<String>>();

    public connFeatureExtract() throws DocumentException, IOException
    {
        //加载资源
        Resource.LoadRawRecord();
        //Resource.LoadStopWords();
        Resource.LoadExpConnectivesDict();
        Resource.LoadWordRelDict();
        //Resource.LoadLtpXMLResultSentID();
    }

    /**
     * 从新版本的标注语料中抽取连词特征来构建libsvm训练数据。TrainData
     * 为了构建模型只需要运行一次即可。
     */
    public void extractFeatures() throws IOException, DocumentException
    {
        this.items       = new ArrayList<ConnVectorItem>();
        this.filterItems = new ArrayList<ConnVectorItem>();

        String path = Constants.Libsvm_Origin_Data_Path;

        //1：获取特征向量组
        //this.getLabeledTrainData( this.items );
        this.getLabeledTrainDataWithAnsj(this.items);

        //2：对抽取到的特征进行处理，过滤
        this.filterItem( this.items, this.filterItems );

        //3：将特征转换为libsvm要求的格式
        this.convertToLibsvmData( this.filterItems );

    }

    /**
     * 根据标注语料生成连词识别训练数据.此方法生成的是原始的训练数据，并没有按照机器学习的要求生成统一格式的数据。
     * @throws DocumentException
     * @throws IOException
     */
    public void getLabeledTrainDataWithAnsj(ArrayList<ConnVectorItem> datas)
    {
        System.out.println("[--Info--]: Get Labeled Train Instances From Sense Record..." );

        ArrayList<SenseRecord> records= new ArrayList<SenseRecord>();
        records.addAll(Resource.Raw_Train_Annotation_p2);
        records.addAll(Resource.Raw_Train_Annotation_p3);

        int size = records.size();
        for(int index = 0; index < size; index++)
        {
            SenseRecord record = records.get(index);

            //0: 预处理进行简单过滤掉平行连词
            String  connWord    = record.getConnective().trim();
            String  sentContent = record.getText().trim();

            if( connWord.contains(";") ) continue;
            if( record.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            //判断是否是同一个record,防止出错
            SenseRecord prev = index > 0 ? records.get(index - 1) : null;
            SenseRecord next = index < size-1 ? records.get(index + 1) : null;

            boolean isSameWithPrev = prev != null && this.isTheSameRecord(record, prev);
            boolean isSameWithNext = next != null && this.isTheSameRecord(record, next);

            String prevConn = (prev == null) ? null : prev.getConnective();
            String nextConn = (next == null) ? null : next.getConnective();

            //1：分词和词性标注,注意sentContent是分好词的
            List<Term> words = util.getSegmentedSentencePosTag(sentContent);

            //2：获取对应的每个词的特征
            int beginIndex = 0, periodIndex = -1;
            if( record.getfPath().contains(".p2") ) periodIndex = sentContent.indexOf("。");

            for( Term wordItem : words )
            {
                ConnVectorItem item = this.getFeatureLine(wordItem, sentContent);

                if( item == null ) continue;

                //还有两个被注释掉的属性没有设置:连词标志和位置
                if( item.getContent().equalsIgnoreCase(connWord) ){
                    item.setLabel( Constants.Labl_is_ConnWord );
                }

                beginIndex = beginIndex + wordItem.getName().length();
                if( periodIndex != -1 && beginIndex > periodIndex){
                    beginIndex = beginIndex - periodIndex;
                }
                item.setPositionInLine(beginIndex);

                //判断是否是被处理过了的连词
                if( prevConn != null && isSameWithPrev && item.getContent().equals(prevConn) ){
                    item.setLabel( Constants.Labl_is_ConnWord );
                }
                if( nextConn != null && isSameWithNext && item.getContent().equals(nextConn) ){
                    item.setLabel( Constants.Labl_is_ConnWord );
                }
                //判断该连词作为连词和不作为连词的比例
                Integer asConnNum    = Resource.allWordsDict.get(item.getContent()).getExpNum();
                Integer notAsConnNum = Resource.NotAsDiscourseWordDict.get(item.getContent());

                double ambugity = ( notAsConnNum == null )? 0 : notAsConnNum*1.0 / (notAsConnNum+asConnNum);

                if( ambugity < 0.03 && asConnNum > 100 ) item.setLabel( Constants.Labl_is_ConnWord );

                datas.add(item);
            }
        }
    }

    /**判断两个记录是否是同一个记录，主要是根据两个记录的相识度来计算**/
    public boolean isTheSameRecord(SenseRecord sour, SenseRecord dest)
    {
        boolean result  = false;
        String sourText = util.removeAllBlankAndProun( sour.getText() );
        String destText = util.removeAllBlankAndProun( dest.getText() );
        int    sourLen  = sourText.length(), destLen = destText.length();

        //确保dest是最长的那位
        if( destLen < sourLen )
        {
            String temp = destText;
            destText    = sourText;
            sourText    = temp;
        }
        sourLen = sourText.length();

        //判断是否是同一个记录或者是同一个记录的两个记录
        if( destText.equalsIgnoreCase(sourText) ) {
            result = true;
        }else if( destText.contains(sourText) ){
            result = true;
        }else{
            //计算两个文本的相似度
            int sameNum = util.countSameCharatersNum(sourText, destText);
            if( Math.abs(sameNum - sourLen) < 3 ) result = true;
        }

        return result;
    }

    /**抽取一个句子中的某一个特定单词的特征。**/
    public ConnVectorItem getFeatureLine(Term wordItem, String sentContent)
    {
        //1：针对每个词，进行判断和处理
        String wContent = wordItem.getName().trim();

        //2: 过滤掉噪音词以及根据连词出现的次数进行过滤
        if( !Resource.allWordsDict.containsKey(wContent) ) return null;

        ConnVectorItem item = new ConnVectorItem(wContent);

        //3：设置词性特征
        Term wNextTerm  = wordItem.getTo(), wPrevTerm  = wordItem.getFrom();

        item.setPos( wordItem.getNatrue().natureStr );
        item.setPrevPos( wPrevTerm == null ? "w" : wPrevTerm.getNatrue().natureStr );
        item.setNextPos( wNextTerm == null ? "w" : wNextTerm.getNatrue().natureStr );

        //设置作为连词出现次数和不作为连词出现次数
        Integer connNum    = Resource.allWordsDict.get(wContent).getExpNum();
        Integer notConnNum = Resource.NotAsDiscourseWordDict.get(wContent);

        //if( connNum < 5 && notConnNum > 5 * connNum ) return null;
        item.setConnNum( connNum == null ? 0 : connNum);
        item.setNotConnNum( notConnNum == null ? 0 : notConnNum );

        //6：判断该词在连词词典中出现的次数,以及歧义性
        DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);

        item.setOccurInDict( wordDictItem.getExpNum() );
        item.setAmbiguity( wordDictItem.getMostExpProbality() );

        return item;
    }

    /**
     * 根据标注语料生成连词识别训练数据.此方法生成的是原始的训练数据，并没有按照机器学习的要求生成统一格式的数据。
     * @throws DocumentException
     * @throws IOException
     */
    public void getLabeledTrainData(ArrayList<ConnVectorItem> datas) throws DocumentException, IOException
    {
        System.out.println("[--Info--]: Get Labeled Train Instances From Sense Record..." );

        //LTP分词结果处理
        //针对每条记录，加载ltp分析结果,根据ltp来抽取词性以及
        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            //查找对应的ltp分析结果保存的文件位置
            String content   = record.getText().trim();
            Integer resultId = Resource.Ltp_Xml_Result_SentID_P3.get( content );

            if( resultId == null ) continue;

            String xmlPath = Constants.Ltp_XML_Result_P3 + "\\sent" + resultId + ".xml";

            //该记录的类型和其中的连词
            boolean  isExplicit = record.getType().equalsIgnoreCase(Constants.EXPLICIT);
            String   connWord   = record.getConnective();
            //String[] connWords  = record.getConnective().split( Constants.Parallel_Word_Seperator );

            //-------------------------------------------------------
            //分析ltp结果, 将每个词都打包为一个item，样本。
            Document domObj  = util.parseXMLFileToDOM(xmlPath, "gbk");
            Element rootNode = domObj.getRootElement();
            Element noteNode = rootNode.element("note");

            if( noteNode.attribute("pos").getText().equalsIgnoreCase("n") )    continue ;
            if( noteNode.attribute("parser").getText().equalsIgnoreCase("n") ) continue;

            Element paraNode  = rootNode.element("doc").element("para");

            //-----------------------------------------------
            //针对每个词，抽取信息来封装为一个样本
            for(Iterator iterator = paraNode.elementIterator(); iterator.hasNext();)
            {
                Element sentNode    = (Element) iterator.next();
                String  sentContent = sentNode.attribute("cont").getText();

                int        beginIndex = 0;     //为了查找一个词在该句子中的位置，使用beginIndex来防止同名的词
                String       prevPos  = "wp";
                ConnVectorItem prevItem = null; //用于设置上一个词条的nextPos。

                for( Iterator ite = sentNode.elementIterator(); ite.hasNext(); )
                {
                    Element wordNode = (Element)ite.next();
                    String nodeName  = wordNode.getName();

                    //出现的每个词都是样本
                    if( nodeName.equals("word") )
                    {
                        String wContent = wordNode.attribute("cont").getText();
                        String wPosTag  = wordNode.attribute("pos").getText();
                        String wRelate  = wordNode.attribute("relate").getText();

                        ConnVectorItem item = new ConnVectorItem(wContent);

                        //3：设置词性特征
                        item.setPos(wPosTag);
                        item.setRelateTag(wRelate);
                        item.setPrevPos(prevPos);
                        prevPos = wPosTag;

                        //设置上一个item的nextPos以及自身的nextPos
                        if( prevItem != null ) prevItem.setNextPos(wPosTag);
                        prevItem = item;

                        //4：判断该词是正样本还是负样本. 目前多个词组成的连词当做负样本，以后使用模板识别
                        if( isExplicit && connWord.equalsIgnoreCase(wContent) )
                        {
                            item.setLabel( Constants.Labl_is_ConnWord );
                        }

                        //5：判断该词在句子中的的位置
                        beginIndex = sentContent.indexOf(wContent, beginIndex);
                        item.setPositionInLine( beginIndex );

                        //6：判断该词在连词词典中出现的次数,以及歧义性
                        double occurTime = 0.0, ambiguity = 1.0;

                        if( Resource.allWordsDict.containsKey(wContent) )
                        {
                            DSAWordDictItem wordItem = Resource.allWordsDict.get(wContent);

                            occurTime = wordItem.getExpNum();
                            ambiguity = wordItem.getMostExpProbality();
                        }

                        item.setAmbiguity(ambiguity);
                        item.setOccurInDict(occurTime);

                        datas.add(item);
                    }
                }
                //设置最后一个item的nextPosTage
                prevItem.setNextPos("wp");
            }
        }
    }

    public void filterItem(ArrayList<ConnVectorItem> datas, ArrayList<ConnVectorItem> results) throws IOException
    {
        HashMap<String, Integer> asConnWords    = new HashMap<String, Integer>();
        HashMap<String, Integer> notAsConnWords = new HashMap<String, Integer>();

        for(ConnVectorItem item : datas)
        {
            String wContent = item.getContent();

            int num = 0;
            if( item.getLabel() == Constants.Labl_is_ConnWord )
            {
                if( asConnWords.containsKey(wContent) ) num = asConnWords.get(wContent);
                asConnWords.put( wContent, num + 1 );
            }
            else
            {
                if( notAsConnWords.containsKey(wContent) ) num = notAsConnWords.get(wContent);
                notAsConnWords.put( wContent, num + 1 );
            }
            //1：过滤停用词
            //if( Resource.Stop_Words.contains(wContent) && !Resource.ExpConnectivesDict.contains(wContent) )

            //2：过滤掉词性为nh, ni和nl等等的词
            //if( Constants.Ignore_PosTags.contains(wPosTag) ) continue;

            results.add(item);
        }

        ArrayList<Map.Entry<String,Integer>> sortWords = util.sortHashMap(asConnWords, false);

        //计算每个连词作为连词出现和不作为连词出现的结果
        ArrayList<String> lines = new ArrayList<String>();
        for(Map.Entry<String, Integer> entry : sortWords)
        {
            String connWord = entry.getKey();
            String line     = connWord + "\t" + entry.getValue();

            if( notAsConnWords.containsKey(connWord) ){
                line += "\t" + notAsConnWords.get(connWord);
            }else{
                line += "\t0";
            }

            lines.add(line);
        }

        String fPath = "./data/wordAndNotWord.txt";
        util.writeLinesToFile(fPath, lines);
    }

    /**
     * 将原始训练数据转换为libsvm格式并将它写入到文件中。
     * @throws IOException
     */
    public void convertToLibsvmData(ArrayList<ConnVectorItem> items) throws IOException
    {
        ArrayList<String> trainLines = new ArrayList<String>();
        ArrayList<String> testLines  = new ArrayList<String>();

        //将数据拆随机分为训练数据和测试数据
        int originNum   = items.size(), trainNum  = originNum / 5 * 4;
        boolean[] exist =  util.getRandomArrays(originNum,trainNum);

        int trainCorrectNum = 0, testCorrectNum = 0, allCorrectNum = 0;
        for(int index = 0; index < originNum; index++)
        {
            ConnVectorItem item  = items.get(index);
            String line = item.toLineForLibSvmWithAnsj();

            int type = item.getLabel();
            if( type == Constants.Labl_is_ConnWord ) allCorrectNum++;

            if( exist[index] ){
                trainLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) trainCorrectNum++;
            }
            else{
                testLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) testCorrectNum++;
            }
        }
        util.writeLinesToFile("data/word/wordTrainData.txt", trainLines);
        util.writeLinesToFile("data/word/wordTestData.txt", testLines);

        System.out.println("All Num: " + originNum + " Correct: " + allCorrectNum);
        System.out.println("Train Num: " + trainNum + " Correct: " + trainCorrectNum);
        System.out.println("Test Num: " + (originNum - trainNum) + " Correct: " + testCorrectNum);
    }


    /**保存没有作为连词的词表**/
    public void saveNotLabeledWords() throws IOException
    {
        String fPath = "./notLabeledWords.txt";
        ArrayList<String> lines = new ArrayList<String>();

        for(ArrayList<String> curWords:this.notLabeledWords)
        {
            String line = "";
            for(String temp:curWords)
            {
                line = line + temp + "\t";
                break;
            }
            lines.add(line.trim());
        }
        util.writeLinesToFile(fPath, lines);
    }


    public static void main(String[] args) throws IOException, DocumentException
    {
        connFeatureExtract recognize = new connFeatureExtract();

        //1: 抽取特征文件进行训练--->.\Data\libsvmTrainData.txt
        recognize.extractFeatures();

        //recognize.saveNotLabeledWords();
    }

}
