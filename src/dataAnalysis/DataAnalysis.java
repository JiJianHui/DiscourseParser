package dataAnalysis;

import common.Constants;
import common.util;
import entity.DSAWordDictItem;
import entity.SenseRecord;
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
    private Integer senseKappa; //����sense�ı�עһ���ԡ�

    public DataAnalysis()
    {

    }

    //����ÿ�����ʵ�argλ�ã����磺arg1 - conn - arg2 | conn - arg2 -- arg1
    public void countConnArgType() throws DocumentException, IOException
    {
        Resource.LoadRawRecord();

        //��һ�б�ʾ����Arg-Conn-Arg���ڶ��б�ʾ����Conn-Arg-Arg
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

            //���������argConnArg
            if( connBegin > arg1End - 2 )
            {
                connArg.put( wContent, new Integer[]{argConnArg + 1, connArgArg} );
            }
            else
            {
                connArg.put( wContent, new Integer[]{argConnArg, connArgArg + 1} );
            }
        }

        //��������浽�ļ��У�ÿ�з�Ϊ���У�connWord�� ArgConnArg, ConnArgArg
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

    /***�������������ʽ�����ܹ��ﵽ��׼ȷ��,�˴μ������ڶ���relType�Ϲ���������ϼ��㡣**/
    public void countRecognizeSenseBaseonExpWord() throws DocumentException
    {
        Resource.LoadRawRecord();
        Resource.LoadWordRelDict();

        //���Ĵ����ϵ����һά�ϳ����ã�ʣ�µ�һά����һ����
        long[] recCorrect = new long[]{0, 0, 0, 0, 0}; //��ȷʶ��ĸ���
        long[] allAnnoNum = new long[]{0, 0, 0, 0, 0}; //��ע�����и���
        long[] recAllNum  = new long[]{0, 0, 0, 0, 0}; //ʶ��������ܸ�����

        //ͳ��ÿ���ϵʶ��ĸ���
        for( SenseRecord curRecord:Resource.Raw_Train_Annotation )
        {
            if( curRecord.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            //��ע�Ĺ�ϵ���
            String annoType = curRecord.getRelNO().trim();
            if( annoType.length() > 1 ) annoType = annoType.substring(0,1);
            int annoTypeValue = Integer.valueOf(annoType);

            String annoWord = curRecord.getConnective().trim();

            //����ʹ������ʶ��Ĺ�ϵ���
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

            //�ù�ϵ��ע���ܸ���
            allAnnoNum[annoTypeValue]++;

            //�ù�ϵʶ����ȷ�ĸ���
            if(annoTypeValue == mlTypeValue)
            {
                recCorrect[annoTypeValue]++;
            }

            //ĳ����ϵʶ����ܸ���
            recAllNum[mlTypeValue]++;
        }

        //����ÿ���ϵ��prfֵ
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

    public static void main(String[] args) throws IOException, DocumentException
    {
        DataAnalysis analysis = new DataAnalysis();
        //analysis.countConnArgType();
        analysis.countRecognizeSenseBaseonExpWord();
    }
}
