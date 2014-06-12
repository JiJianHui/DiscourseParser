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
    public void countRecognizeSenseBaseonExpWord() throws DocumentException, IOException
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


    /***�������������ʽ�����ܹ��ﵽ��׼ȷ��,�˴μ������ڶ���relType�Ϲ���������ϼ��㡣**/
    public void countRecognizeOldSenseBaseonExpWord() throws DocumentException, IOException
    {
        Resource.LoadOldRawRecord();
        Resource.LoadWordRelDict();

        //���Ĵ����ϵ����һά�ϳ����ã�ʣ�µ�һά����һ����
        long[] recCorrect = new long[]{0, 0, 0, 0, 0, 0, 0, 0}; //��ȷʶ��ĸ���
        long[] allAnnoNum = new long[]{0, 0, 0, 0, 0, 0, 0, 0}; //��ע�����и���
        long[] recAllNum  = new long[]{0, 0, 0, 0, 0, 0, 0, 0}; //ʶ��������ܸ�����

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
            System.out.format(" P: %.2f\t R:%.2f\t F:%.2f\n" , p , r, f );
        }
    }


    /***
     * �ж��ھ���ϵ�;��ڹ�ϵ�е�����ʹ�����������
     * ��p2����ϵ�У���ʾ����ռ�ݵı����Լ���p3���ڹ�ϵ����ʾ����ռ�ݵı�����
     * ��Ҫ��Ϊ��ʶ��һ�����������ھ������ʻ��Ǿ������
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



    /**����ÿ��������p2��p3�еķֲ�����������һ�������ھ���ϵ�;��ڹ�ϵ�зֱ���ֵĴ�����**/
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

        //���������ݺϲ���һ��������:��һ�б�ʾ��p2�г��ֵĴ������ڶ��б�ʾ��p3�г��ֵĴ���
        HashMap<String,Integer[]> finalResult = new HashMap<String,Integer[]>();

        for(Map.Entry<String,Integer> entry:expWordsInP2.entrySet())
        {
            String  conn    = entry.getKey();
            Integer numInP2 = entry.getValue();
            Integer numInP3 = expWordsInP3.get(conn);

            if( numInP3 == null ) numInP3 = 0;

            finalResult.put( conn,new Integer[]{numInP2,numInP3} );
        }
        //����ʣ��ĵ�������
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



    /**�ж��Ƿ�������©���ˣ���Ϊ���ʴʱ��ǹ��˹��ġ�**/
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



    /**���������еĹ�ϵ�Ƿ�����ص�������**/
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

            //�ж��Ƿ���ͬһ��Record,�Լ��Ƿ���ͬһƪ���µ�Record
            if( util.isTheSameRecord(curRecord, prevRecord) ) continue;
            if( !curRecord.getfPath().equals(prevRecord.getfPath()) ) continue;

            //�ж�����Record�Ƿ������ڵ�Record���Ա�ͳ�ƹ�ϵ֮���Ƿ�������б�ע�����⡣
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

                //������ĳһ��ϵ֮�������һ�ֹ�ϵ�ĸ���: ת�ƾ���
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

    /***��������Record��λ�ù�ϵ�� 0������û�й�ϵ��1: ��������һ��Arg����Ƕ��ABBC����
     * 2: ����������Record����ֱ��˳������**/
    private int adjacentTypeWithPrevRecord(SenseRecord cur, SenseRecord prev)
    {
        int result = 0;

        int prevEnd = prev.getArg2End();
        int curBeg  = cur.getArg1Beg(), curEnd = cur.getArg2End();

        //prev��cur��ͬʹ�����м�ľ��ӣ���ôprevEnd��curBegӦ���غ�
        if( prevEnd > curBeg + cur.getArg1().length() / 2 - 3 && prevEnd < curEnd ) result = 1;

        //ֱ��˳���������ͣ���ôcurBegӦ�ñ�prevEnd��һ�����Сһ��
        if( prevEnd <= curBeg && prevEnd > curBeg - 3 ) result = 2;

        return result;
    }

    /**����������ʵ��������ʹ�õ�Ƶ��**/
    public void countConnProbality()
    {

    }


    /***����EDU�зִ����׼ȷ��**/
    public void countEDUAccuray()
    {

    }

    /***�������ʺ���Ϣ�ʵ�,�������CorpusProcess����ĳ�����Ҫԭ������CorpusProcess�Գ�һ�壬������**/
    public void generateWordDict() throws DocumentException
    {
        Resource.LoadRawRecord();


    }

    /**���ݹ��˵Ĵʱ���Ϣ�����˴�ʱ�onlyWord����������ǹ���֮�������������ʣ���p3Word�ǳ������ɵ�ͳ�����ݡ�
     * Ϊ�˷�ֹÿ�ζ�Ҫ�ֶ���p3Word֮����ļ������ֶ����ˣ����������·�����**/
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

    /**����wordAmbiguity.txt������ÿ������ָʾ��ϵ�ĸ��ʡ�ÿ�����ʿ���ָʾ���ֹ�ϵ������������Ҫ�ڴ˻����ϼ���ָʾ��ϵ�Ŀ����Էֲ���
     * ��Ҫ�������ÿ������ָʾ���ֹ�ϵ�ķֲ���**/
    public void countWordAmbiguity() throws IOException
    {
        String fPath = "resource/wordAmbiguity.txt";
        ArrayList<String> lines = new ArrayList<String>();

        util.readFileToLines(fPath, lines);

        double eps = 0.05; //�����������ֵ����Ϊ��0
        int allNum = 0;
        int[] relNums = new int[7]; //index = 1: ָʾһ�ֹ�ϵ�����ʴ���  index = 2 ָʾ���ֹ�ϵ�����ʴ���

        for( String line : lines)
        {
            WordAmbiguity tempWord = new WordAmbiguity(line);

            allNum = allNum + tempWord.number;

            //�������ָʾһ�ֹ�ϵ�����ʸ���,�ܹ�7���ϵ
            //double maxProbality = tempWord.probaliaty[tempWord.maxIndex];
            //if( Math.abs(maxProbality - 1) < 0.05 ) singleNum++;

            int zeroNum = tempWord.getZeroNum(eps);//�����6���㣬��ô�ʹ���ָʾ��һ�ֹ�ϵ
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
        String text = "��";
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
