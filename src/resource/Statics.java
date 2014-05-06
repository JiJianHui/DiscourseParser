package resource;

import common.Constants;
import common.util;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.*;

/**
 * 新的关系体系下以及新的存储格式下的数据分析工作代码。
 * User: Ji JianHui
 * Time: 2014-03-04 09:16
 * Email: jhji@ir.hit.edu.cn
 */
public class Statics
{
    //在新的关系体系下统计词表和关系的资源文件
    public void generateWordRelDict() throws IOException, DocumentException
    {
        Resource.LoadRawRecord();

        //总体的连词和关系的对应表
        HashMap<String, DSAWordDictItem> connWords  = new HashMap<String, DSAWordDictItem>();

        HashMap<String, DSAWordDictItem> p1WordsRel = new HashMap<String, DSAWordDictItem>();
        HashMap<String, DSAWordDictItem> p2WordsRel = new HashMap<String, DSAWordDictItem>();
        HashMap<String, DSAWordDictItem> p3WordsRel = new HashMap<String, DSAWordDictItem>();

        //统计连词和关系的对应关系
        this.makeWordDict(Resource.Raw_Train_Annotation_p1, p1WordsRel);
        this.makeWordDict(Resource.Raw_Train_Annotation_p2, p2WordsRel);
        this.makeWordDict(Resource.Raw_Train_Annotation_p3, p3WordsRel);

        //将连词和关系的对应表按照行的形式写入到文件
        String p1Path  = "./data/p1WordRels.txt";
        String p2Path  = "./data/p2WordRels.txt";
        String p3Path  = "./data/p3WordRels.txt";
        String allPath = "./data/allWordRels.txt";

        this.saveWordsRelDict(p1Path, p1WordsRel);
        this.saveWordsRelDict(p2Path, p2WordsRel);
        this.saveWordsRelDict(p3Path, p3WordsRel);

        //逐个合并,合并到p3的结果中
        Iterator ite = p3WordsRel.entrySet().iterator();

        while( ite.hasNext() )
        {
            Map.Entry entry = (Map.Entry)ite.next();

            String word = (String) entry.getKey();
            DSAWordDictItem item = (DSAWordDictItem) entry.getValue();

            if( p2WordsRel.containsKey(word) )
            {
                item.updateWithItem( p2WordsRel.get(word) );
                p2WordsRel.remove(word);
            }
            if( p1WordsRel.containsKey(word) )
            {
                item.updateWithItem( p1WordsRel.get(word) );
                p1WordsRel.remove(word);
            }
        }

        //处理剩余的p2条目
        ite = p2WordsRel.entrySet().iterator();

        while( ite.hasNext() )
        {
            Map.Entry entry = (Map.Entry)ite.next();

            String word = (String) entry.getKey();
            DSAWordDictItem item = (DSAWordDictItem) entry.getValue();

            if( p1WordsRel.containsKey(word) )
            {
                item.updateWithItem( p1WordsRel.get(word) );
                p1WordsRel.remove(word);
            }

            p3WordsRel.put(word, item);
        }

        //处理剩余的p3条目
        ite = p1WordsRel.entrySet().iterator();
        while( ite.hasNext() )
        {
            Map.Entry entry = (Map.Entry)ite.next();

            String word = (String) entry.getKey();
            DSAWordDictItem item = (DSAWordDictItem) entry.getValue();

            p3WordsRel.put(word, item);

        }

        this.saveWordsRelDict(allPath, p3WordsRel);

}


    /**
     * 根据新版本的标注语料来生成连词和关系的对应表。
     * @param records
     * @param wordsRel
     */
    private void makeWordDict(LinkedHashSet<SenseRecord> records, HashMap<String, DSAWordDictItem> wordsRel)
    {

        for(SenseRecord record:records)
        {
            String type   = record.getType();
            String relNO  = record.getRelNO();
            String word   = record.getConnective();

            int num = 1;

            if( !wordsRel.containsKey(word) )
            {
                DSAWordDictItem item = new DSAWordDictItem(word);
                wordsRel.put(word, item);
            }

            if( type.equalsIgnoreCase(Constants.EXPLICIT) )
            {
                wordsRel.get(word).addNewExpRel(relNO, num);
            }
            else
            {
                wordsRel.get(word).addNewImpRel(relNO, num);
            }
        }
    }


    /**
     * 将词表文件写入到文件中
     * @param fPath
     * @param wordRels
     * @throws IOException
     */
    private void saveWordsRelDict(String fPath,  HashMap<String, DSAWordDictItem> wordRels) throws IOException
    {
        ArrayList<String> lines = new ArrayList<String>();

       Iterator iterator = wordRels.entrySet().iterator();
        while( iterator.hasNext() )
        {
            Map.Entry entry = (Map.Entry) iterator.next();

            DSAWordDictItem wordItem = (DSAWordDictItem) entry.getValue();

            String line = wordItem.toLine();

            lines.add(line);
        }

        util.writeLinesToFile(fPath, lines);
    }
}
