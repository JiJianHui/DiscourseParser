package train.word;

import common.Constants;
import common.util;
import entity.train.SenseRecord;
import org.dom4j.DocumentException;
import resource.Resource;

import java.io.IOException;
import java.util.ArrayList;

/**
 * ʶ��һ�������Ǿ������ʻ��Ǿ�����ʡ�����ʹ�û���ѧϰ���ж�һ��
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-30 15:56
 * Email: jhji@ir.hit.edu.cn
 */
public class InterOrCrossWord
{
    public void getFeature() throws DocumentException, IOException
    {
        Resource.LoadRawRecord();
        Resource.LoadExpConnectivesDict();

        ArrayList<String> crossFeatures = new ArrayList<String>();

        for(SenseRecord record:Resource.Raw_Train_Annotation_p2)
        {
            if( record.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            String wContent = record.getConnective().trim();
            if( !Resource.allWordsDict.containsKey(wContent) ) continue;

            String feature = "1 " + this.getVerbFeature(record);
            crossFeatures.add(feature);
        }

        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            if( record.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            String wContent = record.getConnective().trim();
            if( !Resource.allWordsDict.containsKey(wContent) ) continue;

            String feature = "0 " + this.getVerbFeature(record);
            crossFeatures.add(feature);
        }

        int max = crossFeatures.size();
        int trainLength = max * 4 / 5;

        ArrayList<String> trainDatas = new ArrayList<String>();
        ArrayList<String> testDatas  = new ArrayList<String>();

        util.randomSplitList(crossFeatures, trainLength,trainDatas,testDatas);

        //���������ȡtrainLength����������Ϊtrain
        String trainPath = "data/word/cross_inter_train.txt";
        String testPath = "data/word/cross_inter_test.txt";

        util.writeLinesToFile(trainPath,trainDatas);
        util.writeLinesToFile(testPath, testDatas);
    }

    public String getVerbFeature(SenseRecord record)
    {
        String wContent = record.getConnective().trim();

        //��ȡ����
        //1: ��ȡ��������p2��p3�зֱ���ֵĴ���
        int numInP2 = 0, numInP3 = 0;
        if( Resource.ConnInP2AndP3.containsKey(wContent) )
        {
            numInP2 = Resource.ConnInP2AndP3.get(wContent)[0];
            numInP3 = Resource.ConnInP2AndP3.get(wContent)[1];
        }

        //2����ȡ����������Arg������
        int argConnArg = 0, connArgArg = 0;
        if(Resource.ConnectiveArgNum.containsKey(wContent))
        {
            argConnArg = Resource.ConnectiveArgNum.get(wContent)[0];
            connArgArg = Resource.ConnectiveArgNum.get(wContent)[1];
        }

        //3: ����λ�ھ����м��λ��, �Ƿ�λ�ھ���
        int position = (int) record.getConnBeginIndex();

        System.out.println(record.getfPath());
        System.out.println(record.getConnective());
        //4���Ƿ�λ�ڵ�һ���̾���
        int inSentenceHead = 0;
        String temp = record.getText().substring(0, position);
        if( temp.contains("��") || temp.contains(",") ) inSentenceHead = 1;

        String line = " 1:" + numInP2 + " 2:" + numInP3;
        line = line + " 3:" + argConnArg + " 4:" + connArgArg;
        line = line + " 5:" + inSentenceHead;

        return  line;
    }

    public static void main(String[] args) throws IOException, DocumentException
    {
        InterOrCrossWord temp = new InterOrCrossWord();
        temp.getFeature();
    }
}
