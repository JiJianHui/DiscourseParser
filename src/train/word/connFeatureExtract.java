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
        ArrayList<ConnVectorItem> items = new ArrayList<ConnVectorItem>();
        ArrayList<ConnVectorItem> filterItems = new ArrayList<ConnVectorItem>();

        //1：获取特征向量组
        //this.getLabeledTrainData( this.items );
        this.getLabeledTrainDataWithAnsj(items);

        //2：对抽取到的特征进行处理，过滤
        this.filterItem(items, filterItems);

        //3：将特征转换为libsvm要求的格式
        this.convertToLibsvmData(filterItems);

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

            boolean isSameWithPrev = prev != null && util.isTheSameRecord(record, prev);
            boolean isSameWithNext = next != null && util.isTheSameRecord(record, next);

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
                Integer asConnNum    = Resource.AsDiscourseWordDict.get( item.getContent() );
                Integer notAsConnNum = Resource.NotAsDiscourseWordDict.get( item.getContent() );

                double ambugity = ( notAsConnNum == null )? 0 : notAsConnNum*1.0 / (notAsConnNum+asConnNum);

                if( ambugity < 0.00006 && asConnNum > 100 ) item.setLabel( Constants.Labl_is_ConnWord );

                datas.add(item);
            }
        }
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
        ArrayList<String> viewLines  = new ArrayList<String>();

        //将数据拆随机分为训练数据和测试数据
        int originNum   = items.size(), trainNum  = originNum / 5 * 4;
        boolean[] exist =  util.getRandomArrays(originNum,trainNum);

        int trainCorrectNum = 0, testCorrectNum = 0, allCorrectNum = 0;
        for(int index = 0; index < originNum; index++)
        {
            ConnVectorItem item  = items.get(index);

            int    type = item.getLabel();
            String line = item.toLineForLibSvmWithAnsj();

            if( type == Constants.Labl_is_ConnWord ) allCorrectNum++;

            if( exist[index] )
            {
                trainLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) trainCorrectNum++;
            }
            else
            {
                testLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) testCorrectNum++;
            }

            //debug 输出item的实际内容
            String viewLine = item.toLineForView();
            viewLines.add(viewLine);
        }

        util.writeLinesToFile("data/word/wordItems.txt", viewLines);
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

    //------------------------------Process Inter or Cross Word----------------

    /**仅仅使用连词在p2和p3中的分布来判断一个连词属于句内还是句间连词**/
    public void predictJustUseDict() throws IOException,DocumentException
    {
        Resource.LoadRawRecord();
        Resource.LoadWordRelDict();
        Resource.LoadConnInP2AndP3();

        ArrayList<SenseRecord> records = new ArrayList<SenseRecord>();
        records.addAll( Resource.Raw_Train_Annotation_p3 );
        records.addAll( Resource.Raw_Train_Annotation_p2 );

        int interNum = 0, interRec = 0, interCorrect = 0;  //第一类识别结果
        int crossNum = 0, crossRec = 0, crossCorrect = 0;  //第二类识别结果

        for( SenseRecord record : records )
        {
            String conn   = record.getConnective();
            int posInLine = record.getConnBeginIndex();
            String sent   = (record.getConnArgIndex() == 1) ? record.getArg1():record.getArg2();

            if( record.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;
            if( !Resource.allWordsDict.containsKey(conn) ) continue;
            if( conn.contains(";") ) continue;

            //自动识别结果
            int result = markConnAsInterOrCross(conn, posInLine, sent);
            if( result == Constants.Label_Cross_ConnWord ) crossRec++;
            if( result == Constants.Label_Inter_ConnWord ) interRec++;

            //修改识别准确数据
            if( record.getfPath().endsWith(Constants.P2_Ending) )
            {
                crossNum++;
                if( result == Constants.Label_Cross_ConnWord ) crossCorrect++;
            }
            else
            {
                interNum++;
                if( result == Constants.Label_Inter_ConnWord ) interCorrect++;
            }
        }
        //计算每一类的prf
        double pCross = crossCorrect * 1.0 / crossRec;
        double rCross = crossCorrect * 1.0 / crossNum;
        double fCross = 2 * pCross * rCross / (pCross + rCross);

        double pInter = interCorrect * 1.0 / interRec;
        double rInter = interCorrect * 1.0 / interNum;
        double fInter = 2 * pInter * rInter / (pInter + rInter);

        System.out.println("Cross: allNum: " + crossNum + " recNum: " + crossRec + " recCorrect: " + crossCorrect);
        System.out.println("Inter: allNum: " + interNum + " recNum: " + interRec + " recCorrect: " + interCorrect);
        System.out.format("Corss: P: %.2f R: %.2f F: %.2f\n", pCross, rCross, fCross);
        System.out.format("Inter: P: %.2f R: %.2f F: %.2f\n", pInter, rInter, fInter);
    }

    /**判断一个连词是句间连词还是句内连词.输入是一个连词内容，返回的结果为：
     * 0：不是连词  1：句内连词  2：句间连词**/
    public int markConnAsInterOrCross(String conn, int positionInLine, String sentence)
    {
        if( !Resource.allWordsDict.containsKey(conn) ) return Constants.Labl_Not_ConnWord;
        if( conn.contains(";") ) return Constants.Label_Inter_ConnWord;

        //1: 获取该连词在p2和p3中分别出现的次数
        int numInP2 = 0, numInP3 = 0;
        if( Resource.ConnInP2AndP3.containsKey(conn) )
        {
            numInP2 = Resource.ConnInP2AndP3.get(conn)[0];
            numInP3 = Resource.ConnInP2AndP3.get(conn)[1];
        }

        //4：是否位于第一个短句内
        boolean inSentenceHead = true;
        String temp = sentence.substring(0, positionInLine);

        if( temp.contains("，") || temp.contains(",") ) inSentenceHead = false;

        //紧紧依靠字典等特征来判断一个连词的句间和句内属性
        if( numInP2 > numInP3 && inSentenceHead ) return Constants.Label_Cross_ConnWord;

        return Constants.Label_Inter_ConnWord;
    }

    public static void main(String[] args) throws IOException, DocumentException
    {
        connFeatureExtract recognize = new connFeatureExtract();

        //1: 抽取特征文件进行训练--->.\Data\libsvmTrainData.txt
        recognize.extractFeatures();

        //recognize.saveNotLabeledWords();

        //2: 计算句内连词和句间连词识别的效果
        //recognize.predictJustUseDict();
    }

}
