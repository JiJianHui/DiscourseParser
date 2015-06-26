import common.util;
import entity.train.WordVector;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Li Jiaqi
 * Time: 2015-06-26 09:45
 * Email: jqli@ir.hit.edu.cn
 */
public class ImaginationModel {

    ArrayList<String> rawCorpusList = new ArrayList<String>();
    public static HashMap<String, WordVector> wordVectors;

    public double cosSimi(WordVector wordVectorOne, WordVector wordVectorTwo)
    {
        double dCosSimilarity = 0;
        double dChild = 0; //
        double dSquaresSumOfOne = 0 ,dSquaresSumOfTwo = 0;

        for(int i = 0; i < 50; i++)
        {
            dChild += wordVectorOne.wVector[i] * wordVectorTwo.wVector[i];
            dSquaresSumOfOne += Math.sqrt(wordVectorOne.wVector[i] * wordVectorOne.wVector[i]) ;
            dSquaresSumOfTwo += Math.sqrt(wordVectorTwo.wVector[i] * wordVectorTwo.wVector[i]);
        }

        dCosSimilarity = dChild / (dSquaresSumOfOne * dSquaresSumOfTwo);

        return dCosSimilarity;
    }

// Load Corpus, load tripple vector
    public void loadData() throws IOException
    {

        util.readFileToLines("ImageModel/corpus",rawCorpusList);

        String fPath = "ImageModel/corpus";  //词向量文件地址
        System.out.println("[--Info--] Loading Word Vector File from [" + fPath + "]" );

        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLinesWithEncoding(fPath, lines,"UTF-8");

        for(String line:lines)
        {
            WordVector curWordVector = new WordVector(line);

            wordVectors.put(curWordVector.wName, curWordVector);
        }
        lines.clear();

    }



    public static void main(String args[]) throws IOException, DocumentException
    {
        // Raw Corpus.





        DiscourseParser dp = new DiscourseParser();







//        System.out.println("Hello world");
    }
}
