package train.svm;

import common.util;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * ��Ҫ������ʽ��ϵ��ʶ��
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-23 10:05
 * Email: jhji@ir.hit.edu.cn
 */
public class relRecSVM
{
    private svm_model svmModel;

    public String trainDataPath;
    public String testDataPath;

    //ֻ��Ҫ��ǰ2��Ԫ�ؽ������ţ�����Ĳ���Ҫ
    public String scaleTrainDataPath;
    public String scaleTestDataPath;
    public String scaleRulePath;

    public String resultPath;

    public String modelFilePath;

    private double[] featureMin;
    private double[] featureMax;

    public relRecSVM() throws IOException
    {
        this.scaleRulePath = "data/relation/impRelTrainData._scaleRule.txt";
        this.scaleTrainDataPath = "data/relation/impRelTrainData._scale.txt";
        this.scaleTrainDataPath = "data/relation/impRelTestData._scale.txt";
    }

    /*** ����ѵ�����ݺͲ������ݣ���Ҫ��Ҫ����console������ض���������ļ���*/
    private void scaleTrainData() throws IOException
    {
        //String fPath            = "./data/svm/libsvmTrainData_scale.txt";
        String[] trainScaleArgs = {"-l", "0", "-u", "1","-s", scaleRulePath ,trainDataPath};

        String fPath = trainDataPath.substring(0, trainDataPath.length()-3) + "_scale.txt";

        this.scaleTrainDataPath = fPath;

        //����ض���, ����ԭ�����
        PrintStream out = System.out;
        PrintStream fo  = new PrintStream( fPath );
        System.setOut(fo);

        svm_scale.main(trainScaleArgs);

        fo.close();

        //�ָ������
        System.setOut(out);
    }

    private void scaleTestData() throws IOException
    {
        //String   fPath          = "./data/svm/libsvmTestData_scale.txt";
        String[] testScaleArgs  = {"-l", "0", "-u", "1","-r",scaleRulePath, this.testDataPath};

        String fPath = testDataPath.substring(0, testDataPath.length()-3) + "_scale.txt";

        this.scaleTestDataPath = fPath;

        //����ض���, ����ԭ�����
        PrintStream out = System.out;
        PrintStream fo  = new PrintStream( fPath );
        System.setOut(fo);

        svm_scale.main( testScaleArgs );

        //�ָ������
        System.setOut(out);
    }

    /**ʹ��ѵ��������ѵ��svmģ�ͣ����ص���ģ�͵ĵ�ַ**/
    public String trainModel(String trainDataPath, String testDataPath) throws IOException
    {
        this.trainDataPath = trainDataPath;
        this.testDataPath  = testDataPath;

        this.scaleRulePath = trainDataPath.substring(0, trainDataPath.length()-3) + "_scaleRule.txt";

        this.scaleTrainData();
        this.scaleTestData();

        //String[] trainArgs = {"-c", "2048", "-g", "0.0078125","-w1","6","-w0","1", scaleTrainDataPath};
        String[] trainArgs = {"-c", "2.0", "-g", "0.125","-w1","15","-w2","4","-w3","10","-w4","1", scaleTrainDataPath};

        this.modelFilePath = svm_train.main(trainArgs);

        return this.modelFilePath;
    }

    /**����ģ�͵�׼ȷ��**/
    public double testModel(String modelFile) throws IOException
    {
        this.resultPath   = trainDataPath.substring(0, trainDataPath.length()-3);
        this.resultPath   = resultPath + "_result.txt";

        String[] testArgs = {this.scaleTestDataPath, modelFile, this.resultPath};

        Double   accuracy = svm_predict.main(testArgs);

        return  accuracy;
    }

    public void loadModel(String fPath) throws IOException
    {
        System.out.println("[--Info--] Loading SVM Model From " + fPath );
        this.svmModel = svm.svm_load_model(fPath);
    }
    public void loadModel() throws IOException
    {
        if( this.svmModel != null ) return;

        String fPath = "./impRelTrainData._scale.txt.model";
        System.out.println("[--Info--] Loading SVM Model From " + fPath );
        this.svmModel = svm.svm_load_model(fPath);
    }


    private void loadScaleRule() throws IOException
    {
        featureMin = new double[4];
        featureMax = new double[4];

        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(scaleRulePath, lines, 6);

        for(int index = 0; index < 4; index++)
        {
            String[] lists = lines.get(index + 2).split(" ");
            featureMin[index] = Double.valueOf(lists[1]);
            featureMax[index] = Double.valueOf(lists[2]);
        }
    }

    public int predict(String line) throws IOException
    {
        if(this.featureMin == null || this.featureMin.length == 0) this.loadScaleRule();

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
            if(j < 4)
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
        String result   = this.resultPath;
        //String testFile = "data/relation/impRelTestData._scale.txt";
        //String result   = "data/relation/impRelTrainData._result.txt";

        ArrayList<String> testLines   = new ArrayList<String>();
        ArrayList<String> resultLines = new ArrayList<String>();

        util.readFileToLines(testFile, testLines);
        util.readFileToLines(result, resultLines);

        double allNum = 0, recNum = 0, recCorrect = 0;

        for(int index = 0; index < testLines.size(); index++)
        {
            String testLine = testLines.get(index);
            String[] lists  = testLine.split(" ");

            int testLabel   = Double.valueOf(lists[0]).intValue();
            int resultLabel = Double.valueOf(resultLines.get(index).trim()).intValue();

            allNum++;
            if( testLabel != 0 ) recNum++;
            if( testLabel != 0 && testLabel == resultLabel) recCorrect++;

        }

        double p = recCorrect / recNum;
        double r = recNum / allNum ;
        double f = 2 * p * r / (p + r);

        System.out.println("P: " + p + "\t R: " + r + "\t F: " + f);
    }

    private static int atoi(String s){ return Integer.parseInt(s); }
    private static double atof(String s) { return Double.valueOf(s).doubleValue(); }

    public static void main(String[] args) throws IOException
    {
        relRecSVM test = new relRecSVM();

        String trainDataPath = "data/relation/impRelTrainData.txt";
        String testDataPath  = "data/relation/impRelTestData.txt";

        String modelPath     = test.trainModel(trainDataPath, testDataPath);

        test.loadModel(modelPath);
        test.testModel(modelPath);

        test.countPRF();

    }
}