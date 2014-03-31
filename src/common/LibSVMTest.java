package common;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

import libsvm.*;
import train.MLVectorItem;

public class LibSVMTest
{
    private  svm_model svmModel;

    //ֻ��Ҫ��ǰ6��Ԫ�ؽ������ţ�����Ĳ���Ҫ
    String scaleRulePath    = "./data/svm/scaleRule.txt";

    private double[] featureMin = new double[4];
    private double[] featureMax = new double[4];

    public LibSVMTest() throws IOException
    {
        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLines(scaleRulePath, lines, 6);

        for(int index = 0; index < 4; index++)
        {
            String[] lists = lines.get(index + 2).split(" ");
            featureMin[index] = Double.valueOf(lists[1]);
            featureMax[index] = Double.valueOf(lists[2]);
        }
    }

    /*** ����ѵ�����ݺͲ������ݣ���Ҫ��Ҫ����console������ض���������ļ���*/
    public void scaleTrainData() throws IOException
    {
        String fPath            = "./data/svm/libsvmTrainData_scale.txt";
        String[] trainScaleArgs = {"-l", "0", "-u", "1","-s", "./data/svm/scaleRule.txt" ,
                                   "./data/svm/libsvmTrainData.txt"};

        //����ض���, ����ԭ�����
        PrintStream out = System.out;
        PrintStream fo  = new PrintStream( fPath );
        System.setOut(fo);

        svm_scale.main( trainScaleArgs );

        fo.close();

        //�ָ������
        System.setOut(out);
    }

    public void scaleTestData() throws IOException
    {
        String   fPath          = "./data/svm/libsvmTestData_scale.txt";
        String[] testScaleArgs  = {"-l", "0", "-u", "1","-r","./data/svm/scaleRule.txt",
                                   "./data/svm/libsvmTestData.txt"};

        //����ض���, ����ԭ�����
        PrintStream out = System.out;
        PrintStream fo  = new PrintStream( fPath );
        System.setOut(fo);

        svm_scale.main( testScaleArgs );

        //�ָ������
        System.setOut(out);
    }

    /**ʹ��ѵ��������ѵ��svmģ�ͣ����ص���ģ�͵ĵ�ַ**/
    public String trainModel() throws IOException
    {
        String[] trainArgs = {"-c", "2048", "-g", "0.0078125","-w1","5","-w0","1", "./data/svm/libsvmTrainData_scale.txt"};
        //String[] trainArgs = {"E:\\Program\\libsvm-3.17\\data\\svmguide1"};
        String modelFile   = svm_train.main(trainArgs);
        return modelFile;
    }

    /***ʹ�ý�����֤��ѵ��svmģ��**/
    public String trainModelWithCrossValidation() throws  IOException
    {
        String[] cvrainArgs = {"-v", "5", "./data/svm/libsvmTrainData_scale.txt"};
        String modelFile = svm_train.main(cvrainArgs);
        return modelFile;
    }

    /**����ģ�͵�׼ȷ��**/
    public double testModel(String modelFile) throws IOException
    {
        String[] testArgs = {"./data/svm/libsvmTestData_scale.txt", modelFile, "./data/svm/libsvmResult.txt"};
        //String[] testArgs = {"E:\\Program\\libsvm-3.17\\data\\svmguide1.t", modelFile, "./data/svm/libsvmResult.txt"};
        Double   accuracy = svm_predict.main(testArgs);

        return  accuracy;
    }

    public void loadModel() throws IOException
    {
        this.svmModel = svm.svm_load_model("./libsvmTrainData_scale.txt.model");
    }

    /**Ԥ��һ�����������Ƿ�������**/
    public Double predict(MLVectorItem item) throws IOException
    {
        String line        = item.toLineForLibSvmWithAnsj();
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

        double label = svm.svm_predict(this.svmModel, x);

        return label;
    }

    public void countPRF() throws IOException {
        String testFile = Constants.Libsvm_Test_Scale_Data_Path;
        String result   = Constants.Libsvm_Result_Path;

        ArrayList<String> testLines   = new ArrayList<String>();
        ArrayList<String> resultLines = new ArrayList<String>();

        util.readFileToLines(testFile, testLines);
        util.readFileToLines(result, resultLines);

        double tp = 0, fp = 0, fn = 0, tn = 0;

        for(int index = 0; index < testLines.size(); index++)
        {
            String testLine = testLines.get(index);
            String[] lists  = testLine.split(" ");

            int testLabel   = Double.valueOf(lists[0]).intValue();
            int resultLabel = Double.valueOf(resultLines.get(index).trim()).intValue();

            if( testLabel == resultLabel )
            {
                if(testLabel == Constants.Labl_is_ConnWord ) tp++;
                else tn++;
            }
            else
            {
                if(testLabel == Constants.Labl_is_ConnWord) fp++;
                else fn++;
            }
        }

        double p = tp / (tp + fp);
        double r = tp / (tp + fn);
        double f = 2 * p * r / (p + r);

        System.out.println("P: " + p + "\t R: " + r + "\t F: " + f);
    }

    private static int atoi(String s){ return Integer.parseInt(s); }
    private static double atof(String s) { return Double.valueOf(s).doubleValue(); }

	public static void main(String[] args) throws IOException
    {
        LibSVMTest test = new LibSVMTest();

        test.scaleTrainData();
        test.scaleTestData();

        String modelFile = test.trainModel();
        test.testModel(modelFile);

        //�����ٻ��ʺ�׼ȷ��
        test.countPRF();

	}
}
