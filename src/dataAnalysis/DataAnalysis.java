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

        for( Map.Entry<String, Integer> entry:Resource.ExpConnWordDict.entrySet() )
        {
            String wContent = entry.getKey();

            if( !Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                System.out.println("Error: " + wContent + " not appeared.");
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
        analysis.checkData();
    }
}
