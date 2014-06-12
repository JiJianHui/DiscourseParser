package dataAnalysis;

import common.Constants;
import common.util;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import org.dom4j.DocumentException;
import resource.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-02-26 09:40
 * Email: jhji@ir.hit.edu.cn
 */
public class DataAnalysis
{
    private Integer senseKappa; //计算sense的标注一致性。

    public DataAnalysis()
    {

    }

    //计算每个连词的arg位置，比如：arg1 - conn - arg2 | conn - arg2 -- arg1
    public void countConnArgType() throws DocumentException, IOException
    {
        Resource.LoadRawRecord();

        //第一列表示的是Arg-Conn-Arg，第二列表示的是Conn-Arg-Arg
        HashMap<String, Integer[]> connArg = new HashMap<String, Integer[]>();

        for(SenseRecord curRecord : Resource.Raw_Train_Annotation_p3)
        {
            String type = curRecord.getType();
            if( type.equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            String sentence = curRecord.getText();
            String wContent = curRecord.getConnective();

            String arg1     = util.removeAllBlank( curRecord.getArg1() );
            String arg2     = util.removeAllBlank(curRecord.getArg2());

            int arg1Begin   = sentence.indexOf(arg1);
            int arg1End     = arg1Begin + arg1.length();
            int arg2Begin   = sentence.indexOf(arg2);

            int connBegin   = curRecord.getConnBeginIndex();

            int argConnArg = 0, connArgArg = 0;

            if( !connArg.containsKey(wContent) ) connArg.put(wContent, new Integer[]{0,0});

            argConnArg = connArg.get(wContent)[0];
            connArgArg = connArg.get(wContent)[1];

            //如果类型是argConnArg
            if( connBegin > arg1End - 2 )
            {
                connArg.put( wContent, new Integer[]{argConnArg + 1, connArgArg} );
            }
            else
            {
                connArg.put( wContent, new Integer[]{argConnArg, connArgArg + 1} );
            }
        }

        //将结果保存到文件中，每行分为三列：connWord， ArgConnArg, ConnArgArg
        ArrayList<String> lines = new ArrayList<String>();
        for(Map.Entry<String, Integer[]> entry:connArg.entrySet() )
        {
            String line = entry.getKey() + "\t";
            line += entry.getValue()[0] + "\t";
            line += entry.getValue()[1];

            lines.add(line);
        }

        String fPath = "data/connArgArg.txt";
        util.writeLinesToFile(fPath, lines);
    }

    /***计算仅仅依靠显式连词能够达到的准确率,此次计算是在顶层relType上共四类基础上计算。**/
    public void countRecognizeSenseBaseonExpWord() throws DocumentException, IOException
    {
        Resource.LoadRawRecord();
        Resource.LoadWordRelDict();

        //共四大类关系，第一维废除不用，剩下的一维代表一个类
        long[] recCorrect = new long[]{0, 0, 0, 0, 0}; //正确识别的个数
        long[] allAnnoNum = new long[]{0, 0, 0, 0, 0}; //标注的所有个数
        long[] recAllNum  = new long[]{0, 0, 0, 0, 0}; //识别出来的总个数。

        //统计每类关系识别的个数
        for( SenseRecord curRecord:Resource.Raw_Train_Annotation )
        {
            if( curRecord.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            //标注的关系编号
            String annoType = curRecord.getRelNO().trim();
            if( annoType.length() > 1 ) annoType = annoType.substring(0,1);
            int annoTypeValue = Integer.valueOf(annoType);

            String annoWord = curRecord.getConnective().trim();

            //仅仅使用连词识别的关系编号
            DSAWordDictItem item = Resource.allWordsDict.get(annoWord);
            String mlType        = null;

            if( item == null )
            {
                //System.out.println(annoWord);
                mlType = "0";
            }
            else
            {
                mlType = item.getMostExpProbalityRelNO();
                mlType = util.convertOldRelIDToNew(mlType);
                if( mlType.length() > 1 ) mlType = mlType.substring(0, 1);
            }

            int mlTypeValue = Integer.valueOf(mlType);

            //该关系标注的总个数
            allAnnoNum[annoTypeValue]++;

            //该关系识别正确的个数
            if(annoTypeValue == mlTypeValue)
            {
                recCorrect[annoTypeValue]++;
            }

            //某个关系识别的总个数
            recAllNum[mlTypeValue]++;
        }

        //计算每类关系的prf值
        for(int index = 1; index <allAnnoNum.length; index++)
        {
            if( recAllNum[index] == 0 || allAnnoNum[index] == 0 )
            {
                System.out.println("The data set is Zero!");
                continue;
            }

            double p = recCorrect[index] * 1.0 / ( recAllNum[index] * 1.0 );
            double r = recCorrect[index] * 1.0 / ( allAnnoNum[index] * 1.0 );
            double f = p * r * 2 / ( p + r );

            System.out.println("SenseNO: " + index + " All: " + allAnnoNum[index] + " RecAll: " + recAllNum[index] + " RecCorr: " + recCorrect[index]);
            System.out.println(" P: " + p + " R: " + r + " F: " + f + "\r\n");
        }
    }


    /***计算仅仅依靠显式连词能够达到的准确率,此次计算是在顶层relType上共四类基础上计算。**/
    public void countRecognizeOldSenseBaseonExpWord() throws DocumentException, IOException
    {
        Resource.LoadOldRawRecord();
        Resource.LoadWordRelDict();

        //共四大类关系，第一维废除不用，剩下的一维代表一个类
        long[] recCorrect = new long[]{0, 0, 0, 0, 0, 0, 0, 0}; //正确识别的个数
        long[] allAnnoNum = new long[]{0, 0, 0, 0, 0, 0, 0, 0}; //标注的所有个数
        long[] recAllNum  = new long[]{0, 0, 0, 0, 0, 0, 0, 0}; //识别出来的总个数。

        //统计每类关系识别的个数
        for( SenseRecord curRecord:Resource.Raw_Train_Annotation )
        {
            if( curRecord.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            //标注的关系编号
            String annoType = curRecord.getRelNO().trim();
            if( annoType.length() > 1 ) annoType = annoType.substring(0,1);
            int annoTypeValue = Integer.valueOf(annoType);

            String annoWord = curRecord.getConnective().trim();

            //仅仅使用连词识别的关系编号
            String mlType;
            DSAWordDictItem item = Resource.allWordsDict.get(annoWord);

            if( item == null )
            {
                //System.out.println(annoWord);
                //mlType = "0";
                continue;
            }
            else
            {
                mlType = item.getMostExpProbalityRelNO();
                //mlType = util.convertOldRelIDToNew(mlType);
                if( mlType.length() > 1 ) mlType = mlType.substring(0, 1);
            }

            int mlTypeValue = Integer.valueOf(mlType);

            //该关系标注的总个数
            allAnnoNum[annoTypeValue]++;

            //该关系识别正确的个数
            if(annoTypeValue == mlTypeValue)
            {
                recCorrect[annoTypeValue]++;
            }

            //某个关系识别的总个数
            recAllNum[mlTypeValue]++;
        }

        //计算每类关系的prf值
        for(int index = 1; index <allAnnoNum.length; index++)
        {
            if( recAllNum[index] == 0 || allAnnoNum[index] == 0 )
            {
                System.out.println("The data set is Zero!");
                continue;
            }

            double p = recCorrect[index] * 1.0 / ( recAllNum[index] * 1.0 );
            double r = recCorrect[index] * 1.0 / ( allAnnoNum[index] * 1.0 );
            double f = p * r * 2 / ( p + r );

            System.out.println("SenseNO: " + index + " All: " + allAnnoNum[index] + " RecAll: " + recAllNum[index] + " RecCorr: " + recCorrect[index]);
            System.out.format(" P: %.2f\t R:%.2f\t F:%.2f\n" , p , r, f );
        }
    }


    /***
     * 判断在句间关系和句内关系中的连词使用情况。即：
     * 在p2句间关系中，显示连词占据的比例以及在p3句内关系中显示连词占据的比例。
     * 主要是为了识别一个连词是属于句内连词还是句间连词
     */
    public void countExpAndImpDistibutionInFile() throws IOException, DocumentException
    {
        Resource.LoadRawRecord();

        int expInstanceP2 = 0, impInstanceP2 = 0;

        for(SenseRecord record:Resource.Raw_Train_Annotation_p2)
        {
            String relType = record.getType();

            if( relType.equalsIgnoreCase(Constants.EXPLICIT) )
                expInstanceP2++;
            else
                impInstanceP2++;
        }

        int expInstanceP3 = 0, impInstanceP3 = 0;

        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            String relType = record.getType();

            if( relType.equalsIgnoreCase(Constants.EXPLICIT) )
                expInstanceP3++;
            else
                impInstanceP3++;
        }


        System.out.println("P2 Cross Relation: Exp:" + expInstanceP2  + " Imp: " + impInstanceP2);
        System.out.println("P3 Inter Relation: Exp:" + expInstanceP3  + " Imp: " + impInstanceP3);
    }



    /**计算每个连词在p2和p3中的分布，即：计算一个连词在句间关系和句内关系中分别出现的次数。**/
    public void countConnectiveInP2AndP3() throws IOException, DocumentException
    {
        Resource.LoadRawRecord();
        HashMap<String, Integer> expWordsInP3 = new HashMap<String, Integer>();
        HashMap<String, Integer> expWordsInP2 = new HashMap<String, Integer>();

        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            if(record.getType().equalsIgnoreCase(Constants.EXPLICIT))
            {
                int num = 0;
                String conn = record.getConnective();

                if(expWordsInP3.containsKey(conn)) num = expWordsInP3.get(conn);

                expWordsInP3.put(conn, num+1);
            }
        }

        for(SenseRecord record:Resource.Raw_Train_Annotation_p2)
        {
            if(record.getType().equalsIgnoreCase(Constants.EXPLICIT))
            {
                int num = 0;
                String conn = record.getConnective();

                if(expWordsInP2.containsKey(conn)) num = expWordsInP2.get(conn);

                expWordsInP2.put(conn, num+1);
            }
        }

        //将两个数据合并到一个数据中:第一列表示在p2中出现的次数，第二列表示在p3中出现的次数
        HashMap<String,Integer[]> finalResult = new HashMap<String,Integer[]>();

        for(Map.Entry<String,Integer> entry:expWordsInP2.entrySet())
        {
            String  conn    = entry.getKey();
            Integer numInP2 = entry.getValue();
            Integer numInP3 = expWordsInP3.get(conn);

            if( numInP3 == null ) numInP3 = 0;

            finalResult.put( conn,new Integer[]{numInP2,numInP3} );
        }
        //查找剩余的单独连词
        for(Map.Entry<String, Integer> entry:expWordsInP3.entrySet())
        {
            String conn = entry.getKey();

            if( finalResult.containsKey(conn) ) continue;

            finalResult.put( conn, new Integer[]{0, expWordsInP3.get(conn)} );
        }

        ArrayList<String> lines = new ArrayList<String>();

        for(Map.Entry<String, Integer[]> entry:finalResult.entrySet())
        {
            String line = entry.getKey() + "\t" + entry.getValue()[0] + "\t" + entry.getValue()[1];
            lines.add(line);
        }

        String fPath = "resource/connDistributionInP2P3.txt";

        util.writeLinesToFile(fPath, lines);

    }



    /**判断是否有连词漏掉了，因为连词词表是过滤过的。**/
    public void checkData() throws IOException, DocumentException
    {
        Resource.LoadExpConnectivesDict();
        Resource.LoadConnInP2AndP3();

        for( Map.Entry<String, DSAWordDictItem> entry:Resource.allWordsDict.entrySet() )
        {
            String wContent = entry.getKey();

            if( !Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                System.out.println("Error: " + wContent + " not appeared.");
            }
        }

    }



    /**分析语料中的关系是否存在重叠的现象。**/
    public void countRel() throws DocumentException
    {
        Resource.LoadRawRecord();
        int[][] trans = new int[5][5];

        int expAndExp = 0, expAndImp = 0; int impAndExp = 0, impAndImp = 0;
        for( int i = 0; i < 5; i++ ) for( int j = 0; j < 5; j++ ) trans[i][j] = 0;

        System.out.println(Resource.Raw_Train_Annotation_p2.size());

        for( int index = 1; index < Resource.Raw_Train_Annotation_p2.size(); index++ )
        {
            SenseRecord curRecord  = Resource.Raw_Train_Annotation_p2.get(index);
            SenseRecord prevRecord = Resource.Raw_Train_Annotation_p2.get(index - 1);

            //System.out.println(prevRecord.getAnnotation() + "\t" + prevRecord.getRelNO());
            //System.out.println(curRecord.getAnnotation() + "\t" + curRecord.getRelNO());

            //判断是否是同一个Record,以及是否是同一篇文章的Record
            if( util.isTheSameRecord(curRecord, prevRecord) ) continue;
            if( !curRecord.getfPath().equals(prevRecord.getfPath()) ) continue;

            //判断两个Record是否是相邻的Record，以便统计关系之间是否存在序列标注的问题。
            int adjacentType = this.adjacentTypeWithPrevRecord(curRecord, prevRecord);

            if( adjacentType != 0 )
            {
                String curType  = curRecord.getType(),  curRelID  = curRecord.getRelNO();
                String prevType = prevRecord.getType(), prevRelID = prevRecord.getRelNO();

                //count the probality of the next explict
                if( prevType.equalsIgnoreCase(Constants.EXPLICIT) )
                {
                    if( curType.equalsIgnoreCase(Constants.EXPLICIT) ) expAndExp++;
                    else expAndImp++;
                }
                else
                {
                    if( curType.equalsIgnoreCase(Constants.EXPLICIT) ) impAndExp++;
                    else impAndImp++;
                }

                //计算在某一关系之后出现另一种关系的概率: 转移矩阵
                int prevSenseNO = prevRelID.length() == 1 ? Integer.valueOf(prevRelID) : Integer.valueOf(prevRelID.substring(0,1));
                int curSenseNO  = curRelID.length() == 1 ? Integer.valueOf(curRelID) : Integer.valueOf(curRelID.substring(0,1));

                trans[prevSenseNO][curSenseNO] = trans[prevSenseNO][curSenseNO] + 1;

                //debug:
                if( prevSenseNO == 1 && curSenseNO == 1){
                    System.out.println(curRecord.getfPath());
                    System.out.println(curRecord.getAnnotation());
                }
            }
        }

        System.out.println("Exp-Exp: " + expAndExp + " Exp-Imp: " + expAndImp);
        System.out.println("Imp-Exp: " + impAndExp + " Imp-Imp: " + impAndImp);

        for(int i = 1; i < 5; i++ )
        {
            for(int j = 1; j< 5; j++)
            {
                System.out.print(trans[i][j] + "\t");
                //System.out.print(i + "-" + j + ": " + trans[i][j] + " ");
            }
            System.out.print("\n");
        }
    }

    /***返回两个Record的位置关系， 0：代表没有关系。1: 代表共用了一个Arg属于嵌套ABBC类型
     * 2: 代表了两个Record属于直接顺序链接**/
    private int adjacentTypeWithPrevRecord(SenseRecord cur, SenseRecord prev)
    {
        int result = 0;

        int prevEnd = prev.getArg2End();
        int curBeg  = cur.getArg1Beg(), curEnd = cur.getArg2End();

        //prev和cur共同使用了中间的句子，那么prevEnd和curBeg应该重合
        if( prevEnd > curBeg + cur.getArg1().length() / 2 - 3 && prevEnd < curEnd ) result = 1;

        //直接顺序链接类型，那么curBeg应该比prevEnd大一点或者小一点
        if( prevEnd <= curBeg && prevEnd > curBeg - 3 ) result = 2;

        return result;
    }

    /**计算连词在实际语料中使用的频率**/
    public void countConnProbality()
    {

    }


    /***计算EDU切分代码的准确率**/
    public void countEDUAccuray()
    {

    }

    /***生成连词和信息词典,用来替代CorpusProcess里面的程序，主要原因在于CorpusProcess自成一体，不方便**/
    public void generateWordDict() throws DocumentException
    {
        Resource.LoadRawRecord();


    }

    /**根据过滤的词表信息来过滤大词表，onlyWord里面包含的是过滤之后保留下来的连词，而p3Word是程序生成的统计数据。
     * 为了防止每次都要手动对p3Word之类的文件进行手动过滤，开发了如下方法。**/
    public void checkFile() throws IOException
    {
        String onlyWordPath = "resource/onlyWord.txt";
        String allWordPath   = "resource/wordAmbiguity.txt";

        ArrayList<String> p3Lines   = new ArrayList<String>();
        ArrayList<String> onlyLines = new ArrayList<String>();

        util.readFileToLines(allWordPath,p3Lines);
        util.readFileToLines(onlyWordPath, onlyLines);

        HashSet<String> onlyWords = new HashSet<String>();
        for(String line:onlyLines)
        {
            String[] lists = line.split("\t");
            onlyWords.add(lists[0]);
        }

        ArrayList<String> filterLines = new ArrayList<String>();

        for(String line:p3Lines)
        {
            String[] lists = line.split("\t");
            String word = lists[0];

            if( onlyWords.contains(word) ) filterLines.add(line);
        }

        //String fPath = "resource/allWords.txt";

        util.writeLinesToFile(allWordPath, filterLines);
    }

    /**根据wordAmbiguity.txt来计算每个连词指示关系的概率。每个连词可能指示多种关系，现在我们需要在此基础上计算指示关系的可能性分布。
     * 主要计算的是每个连词指示多种关系的分布。**/
    public void countWordAmbiguity() throws IOException
    {
        String fPath = "resource/wordAmbiguity.txt";
        ArrayList<String> lines = new ArrayList<String>();

        util.readFileToLines(fPath, lines);

        double eps = 0.05; //低于这个概率值就认为是0
        int allNum = 0;
        int[] relNums = new int[7]; //index = 1: 指示一种关系的连词次数  index = 2 指示两种关系的连词次数

        for( String line : lines)
        {
            WordAmbiguity tempWord = new WordAmbiguity(line);

            allNum = allNum + tempWord.number;

            //计算仅仅指示一种关系的连词个数,总共7类关系
            //double maxProbality = tempWord.probaliaty[tempWord.maxIndex];
            //if( Math.abs(maxProbality - 1) < 0.05 ) singleNum++;

            int zeroNum = tempWord.getZeroNum(eps);//如果有6个零，那么就代表指示了一种关系
            relNums[7 - zeroNum] = relNums[7 - zeroNum] + tempWord.number;
        }

        System.out.println("All num is " + allNum);
        for(int index = 0; index < relNums.length; index++)
        {
            System.out.format("Point to %d rels's num is %d \n", index, relNums[index]);
        }
    }


    public void findString() throws DocumentException
    {
        String text = "而";
        Resource.LoadOldRawRecord();

        for( SenseRecord curRecord:Resource.Raw_Train_Annotation_p3 )
        {
            String conn = curRecord.getConnective();

            if(curRecord.getRelNO().startsWith("3") )
            {
                System.out.println(curRecord.getText());
                System.out.println(curRecord.getRelNO());
            }

        }
    }

    public static void main(String[] args) throws IOException, DocumentException
    {
        DataAnalysis analysis = new DataAnalysis();
        //analysis.countConnArgType();
        //analysis.countRecognizeSenseBaseonExpWord();
        //analysis.countConnectiveInP2AndP3();
        //analysis.countExpAndImpDistibutionInFile();
        //analysis.checkData();
        //analysis.countRel();
        //analysis.checkFile();
        //analysis.countWordAmbiguity();
        //analysis.countRecognizeOldSenseBaseonExpWord();
        analysis.findString();
    }
}
