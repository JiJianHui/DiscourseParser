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
        String[] trainArgs = {"-c", "8192", "-g", "8.0", "./data/svm/libsvmTrainData_scale.txt"};
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

    private static int atoi(String s){ return Integer.parseInt(s); }
    private static double atof(String s) { return Double.valueOf(s).doubleValue(); }

	public static void main(String[] args) throws IOException
    {

        /**
        String[] trainArgs = {"-c", "8192", "-g", "8.0", "./data/svm/libsvmTrainData_scale.txt"};
        String modelFile = svm_train.main(trainArgs);
        System.out.println( "Model File: " + modelFile );
        **/

        //Test for cross validation
        //String[] crossValidationTrainArgs = {"-v", "5", "./data/svm/libsvmTrainData_scale.txt"};// 10 fold cross validation
        //modelFile = svm_train.main(crossValidationTrainArgs);
        //System.out.print("Cross validation is done! The modelFile is " + modelFile);

        ////directory of test file, model file, result file
        /**
		String[] testArgs = {"./data/svm/libsvmTestData_scale.txt", modelFile, "./data/svm/libsvmResult.txt"};
		Double accuracy = svm_predict.main(testArgs);
         **/

//        svm_model model = svm.svm_load_model("./libsvmTrainData_scale.txt.model");
//
//        String line = "1.0 1:0.3333333333333333 2:0.0718232044198895 3:0.4744897959183674 4:0.835820895522388 61:1.0 101:1.0 197:1.0";
//
//        String[] lists = line.split(" ");
//        svm_node[] nodes = new svm_node[lists.length - 1];
//
//        for(int index = 1; index < nodes.length; index++)
//        {
//            String[] temp = lists[index].split(":");
//
//            nodes[index-1] = new svm_node();
//            nodes[index-1].index = Integer.valueOf( temp[0] );
//            nodes[index-1].value = Double.valueOf( temp[1] );
//        }
//
//        System.out.println( svm.svm_predict(model, nodes) );
//
////		System.out.println("SVM Classification is done! The accuracy is " + accuracy);

            /**
            //����ģ���ļ�
            svm_model model = svm.svm_load_model("./libsvmTrainData_scale.txt.model");

            String line = "1.0 1:0.3333333333333333 2:0.07231920199501247 3:0.9915254237288136 4:0.2515991471215352 57:1.0 101:1.0 197:1.0 ";

            StringTokenizer st = new StringTokenizer(line," /t/n/r/f:");

            double target = atof(st.nextToken());
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];

            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }

            double predict_value[] = new double[m];

            //���ʹ�������ģ��Ԥ��ı�ǩ
            System.out.println(svm.svm_predict(model,x));
             **/

        LibSVMTest test = new LibSVMTest();

        //test.scaleTrainData();
        //test.scaleTestData();
        test.trainModel();

	}
}
