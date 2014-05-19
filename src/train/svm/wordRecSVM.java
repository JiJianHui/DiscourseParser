package train.svm;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

import common.Constants;
import common.util;
import libsvm.*;
import train.word.ConnVectorItem;

/***libsvm�����࣬��Ҫ������ͳһ����ʶ��ʱ���libsvm����**/
public class wordRecSVM
{
    private  svm_model svmModel;

    //ֻ��Ҫ��ǰ6��Ԫ�ؽ������ţ�����Ĳ���Ҫ
    public String scaleRulePath  = "data/word/scaleRule.txt";
    public String trainDataPath  = "data/word/wordTrainData.txt";
    public String testDataPath  = "data/word/wordTestData.txt";

    public String scaleTrainDataPath;
    public String scaleTestDataPath;
    public String modelFilePath;

    private double[] featureMin = new double[6];
    private double[] featureMax = new double[6];

    public wordRecSVM() throws IOException
    {
        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(scaleRulePath, lines, 8);

        for(int index = 0; index < 6; index++)
        {
            String[] lists = lines.get(index + 2).split(" ");
            featureMin[index] = Double.valueOf(lists[1]);
            featureMax[index] = Double.valueOf(lists[2]);
        }
    }

    public wordRecSVM(boolean test)
    {

    }

    /*** ����ѵ�����ݺͲ������ݣ���Ҫ��Ҫ����console������ض���������ļ���*/
    private void scaleData() throws IOException
    {
        //String fPath            = "./data/svm/libsvmTrainData_scale.txt";
        String[] trainScaleArgs = {"-l", "0", "-u", "1","-s", scaleRulePath ,trainDataPath};
        String[] testScaleArgs  = {"-l", "0", "-u", "1","-r",scaleRulePath, testDataPath};

        this.scaleTrainDataPath = trainDataPath.substring(0, trainDataPath.length()-4) + "_scale.txt";
        this.scaleTestDataPath  = testDataPath.substring(0, testDataPath.length()-4) + "_scale.txt";

        //����ض���, ����ԭ�����
        PrintStream out = System.out;

        //����ѵ���ļ�
        PrintStream trainOut = new PrintStream( scaleTrainDataPath );
        System.setOut( trainOut );
        svm_scale.main( trainScaleArgs );
        trainOut.close();

        //���Ų����ļ�
        PrintStream testOut = new PrintStream( scaleTestDataPath );
        System.setOut( testOut );
        svm_scale.main( testScaleArgs );
        testOut.close();

        //�ָ������
        System.setOut(out);
    }

    /**ʹ��ѵ��������ѵ��svmģ�ͣ����ص���ģ�͵ĵ�ַ**/
    public String trainModel() throws IOException
    {
        //��������
        this.scaleData();

        //ѵ��ģ��
        String[] trainArgs = {"-c", "2.0", "-g", "0.5","-w1","2","-w0","1", scaleTrainDataPath};
        this.modelFilePath = svm_train.main(trainArgs);

        //����ģ��׼ȷ��
        String[] testArgs = {this.scaleTestDataPath, this.modelFilePath, "./data/word/wordRecResult.txt"};
        Double   accuracy = svm_predict.main(testArgs);

        //����PRF
        this.countPRF();

        return this.modelFilePath;
    }


    public void loadModel() throws IOException
    {
        if(this.svmModel != null ) return;
        System.out.println("[--Info--] Loading Connective SVM Model From ./wordTrainData_scale.txt.model" );
        this.svmModel = svm.svm_load_model("./wordTrainData_scale.txt.model");
    }

    /**Ԥ��һ�����������Ƿ�������**/
    public int predict(ConnVectorItem item) throws IOException
    {
        String line = item.toLineForLibSvmWithAnsj();
        //int label = this.predictInFile(line);
        int label   = this.predict(line);

        return label;
    }

    public int predict(String line)
    {
        StringTokenizer st = new StringTokenizer(line," /t/n/r/f:");

        int m         = st.countTokens()/2;
        svm_node[] x  = new svm_node[m];
        double target = atof(st.nextToken());

        for(int j = 0; j < m; j++)
        {
            x[j] = new svm_node();
            x[j].index = atoi(st.nextToken());
            x[j].value = atof(st.nextToken());

            //��������
            if(j < 6)
            {
                double min = this.featureMin[j];
                double max = this.featureMax[j];
                double lower = 0.0, upper = 1.0;
                x[j].value = util.scale(x[j].value, min,max,lower,upper);
            }
        }

        Double label = svm.svm_predict(this.svmModel, x);

        return label.intValue();
    }


    public void countPRF() throws IOException
    {
        String testFile = this.scaleTestDataPath;
        String result   = "./data/word/wordRecResult.txt";

        ArrayList<String> testLines   = new ArrayList<String>();
        ArrayList<String> resultLines = new ArrayList<String>();

        util.readFileToLines(testFile, testLines);
        util.readFileToLines(result, resultLines);

        double all = 0, correct = 0, rec = 0, recCorrect = 0;
        for(int index = 0; index < testLines.size(); index++)
        {
            String[] lists  = testLines.get(index).split(" ");
            int testLabel   = Double.valueOf(lists[0]).intValue();
            int resultLabel = Double.valueOf(resultLines.get(index).trim()).intValue();

            if( testLabel == Constants.Labl_is_ConnWord ) rec++;
            if( testLabel == Constants.Labl_is_ConnWord && testLabel == resultLabel ) recCorrect++;

            if( resultLabel == Constants.Labl_is_ConnWord ) correct++;

            all++;
        }

        double p = recCorrect / rec;
        double r = recCorrect / correct;
        double f = 2 * p * r / (p + r);

        System.out.println("P: " + p + "\t R: " + r + "\t F: " + f);
    }

    private static int atoi(String s){ return Integer.parseInt(s); }
    private static double atof(String s) { return Double.valueOf(s).doubleValue(); }

	public static void main(String[] args) throws IOException
    {
        wordRecSVM test = new wordRecSVM(true);
        test.trainModel();

        //test.scaleTrainData();
        //test.scaleTestData("./data/svm/libsvmTestData.txt");

        //String modelFile = test.trainModel();

        //�����ٻ��ʺ�׼ȷ��
        //test.testModel(modelFile);
        //test.countPRF();

        /**
        //����ʶ��Ч��
        test.loadModel();

        String testLine = "1 1:2 2:57.0 3:0.8367521367521367 4:117.0 61:1 181:1 244:1";

        int label = test.predict(testLine);

        System.out.println(label);
         **/
	}
}
