package jiaqi;

import common.Constants;
import common.util;
import entity.train.SenseRecord;
import org.dom4j.DocumentException;
import resource.Resource;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Li jiaqi
 * Time: 2014-09-28 12:32
 * Email: jhji@ir.hit.edu.cn
 */
public class Jacky {

    public void computePostionPRF() throws IOException {
        ArrayList<String> stringArrayListRight  = new ArrayList<String>();
        util.readFileToLines(".\\result1.txt",stringArrayListRight);

        ArrayList<String> stringArrayListTest  = new ArrayList<String>();
        util.readFileToLines(".\\result2.txt",stringArrayListTest);

        int a = 0,b = 0,c = 0;
        int sumOfTestSS = 0,sumOfTestPS = 0;
        double p,r,f;

        for(int i = 0; i < 5790 ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str2.equals("SS"))   sumOfTestSS++;
            else sumOfTestPS++;

            if( str1.equals("SS") && str2.equals("SS"))  a++;

            if(str1.equals("SS") && str2.equals("PS"))  c++;

        }

        p = (double) a / sumOfTestSS;
        r = (double) a / (a + c);
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: SS ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);


        System.out.println("SumOfTestPS is " + sumOfTestPS);

        a = 0;
        c = 0;
        for(int i = 0; i < 5790 ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str1.equals("PS") && str2.equals("PS"))  a++;

            if(str1.equals("PS") && str2.equals("SS"))  c++;
        }

        p = (double) a / sumOfTestPS;
        r = (double) a / (a + c);
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: PS ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);

    }

    public void computeArgExtPRF() throws IOException {

        ArrayList<String> stringArrayListRight  = new ArrayList<String>();
        util.readFileToLines(".\\result3.txt",stringArrayListRight);

        ArrayList<String> stringArrayListTest  = new ArrayList<String>();
        util.readFileToLines(".\\result.txt",stringArrayListTest);

        int a = 0,b = 0,c = 0;
        int sumOfTestArg1 = 0,sumOfTestArg2 = 0,sumofTestNone = 0;
        double p,r,f;

        int size = stringArrayListRight.size();

        for(int i = 0; i < size ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str2.equals("arg1"))   sumOfTestArg1++;
            else if(str2.equals("arg2")) sumOfTestArg2++;
            else sumofTestNone++;

            if( str1.equals("arg1") && str2.equals("arg1"))  a++;

            if(str1.equals("arg1") && !str2.equals("arg1"))  c++;

        }

        p = (double) a / sumOfTestArg1;
        r = (double) a / (a + c);
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: arg1 ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);


//        System.out.println("SumOfTestArg1 is " + sumOfTestArg1);

        a = 0;
        c = 0;
        for(int i = 0; i < size ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str1.equals("arg2") && str2.equals("arg2"))  a++;

            if(str1.equals("arg2") && !str2.equals("arg2"))  c++;
        }

        p = (double) a / sumOfTestArg2;
        r = (double) a / (a + c);
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: arg2 ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);

        a = 0;
        c = 0;
        for(int i = 0; i < size ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str1.equals("None") && str2.equals("None"))  a++;

            if(str1.equals("None") && !str2.equals("None"))  c++;
        }

        p = (double) a / sumofTestNone;
        r = (double) a / (a + c);
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: None ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);


    }

    public static void main(String[] args) throws DocumentException, IOException {

//        Resource.LoadRawRecord();
//        System.out.println(Resource.Raw_Train_Annotation.size());
//
//        ArrayList results = new ArrayList();
//        for(SenseRecord sRecord:Resource.Raw_Train_Annotation){
//            if(sRecord.getType().equals(Constants.IMPLICIT)){
//                //System.out.println(sRecord.getRelNO()+sRecord.getArg1()+sRecord.getArg2());
//                String str = sRecord.getRelNO()+sRecord.getArg1()+" | "+sRecord.getArg2();
//                results.add(str);
//
//            }
//        }
//        util.writeLinesToFile("D:\\output.txt",results);

        Jacky jacky = new Jacky();
//        jacky.computePostionPRF();;
        jacky.computeArgExtPRF();



    }

}


