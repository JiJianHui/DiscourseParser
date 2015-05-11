/**
 * 本文件用于计算分类模块的PRF值，以及其它小实验室等
 */

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

    //李家琦 部分实验代码

    /**
     * 计算论元位置分类的PRF
     * @throws IOException
     */
    public void computePostionPRF() throws IOException {
        ArrayList<String> stringArrayListRight  = new ArrayList<String>();
        util.readFileToLines(".\\arg_pos_result.txt",stringArrayListRight);

        ArrayList<String> stringArrayListTest  = new ArrayList<String>();
        util.readFileToLines(".\\result.txt",stringArrayListTest);

        int a = 0,c = 0;
        int sumOfTestSS = 0,sumOfTestPS = 0,sumOfRightSS = 0, sumOfRightPS = 0;
        double p,r,f;

        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str2.equals("SS"))   sumOfTestSS++;
            else sumOfTestPS++;

            if(str1.equals("SS")) sumOfRightSS++;
            else                  sumOfRightPS++;

            if( str2.equals("SS") && str1.equals("SS"))  a++;

            if( str2.equals("SS") && str1.equals("PS"))  c++;

        }

        p = (double) a / sumOfTestSS;
        r = (double) a / sumOfRightSS;
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: SS ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);

//        System.out.println("SumOfTestPS is " + sumOfTestPS);

        a = 0;
        c = 0;
        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str2.equals("PS") && str1.equals("PS"))  a++;

            if(str2.equals("PS") && str1.equals("SS"))  c++;
        }

        p = (double) a / sumOfTestPS;
        r = (double) a / sumOfRightPS;
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: PS ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);

    }

    /**
     * 计算句法树内部结点分类的PRF
     * @throws IOException
     */
    public void computeArgExtPRF() throws IOException {

        ArrayList<String> stringArrayListRight  = new ArrayList<String>();
        util.readFileToLines(".\\arg_extResult.txt",stringArrayListRight);

        ArrayList<String> stringArrayListTest  = new ArrayList<String>();
        util.readFileToLines(".\\result.txt",stringArrayListTest);

        int a = 0,b = 0,c = 0;
        int sumOfTestArg1 = 0,sumOfTestArg2 = 0,sumofTestNone = 0;
        int sumOfRightArg1 = 0, sumOfRightArg2 = 0, sumOfRightNone = 0;
        double p,r,f;

        int size = stringArrayListRight.size();

        for(int i = 0; i < size ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if( str2.equals("arg1") )   sumOfTestArg1++;
            else if(str2.equals("arg2")) sumOfTestArg2++;
            else sumofTestNone++;

            if( str1.equals("arg1") )   sumOfRightArg1++;
            else if(str2.equals("arg2")) sumOfRightArg2++;
            else sumOfRightNone++;

            if( str2.equals("arg1") && str1.equals("arg1"))  a++;

            if( str2.equals("arg1") && !str1.equals("arg1"))  c++;

        }

        p = (double) a / sumOfTestArg1;
        r = (double) a / sumOfRightArg1;
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

            if( str2.equals("arg2") && str1.equals("arg2"))  a++;

            if( str2.equals("arg2") && !str1.equals("arg2") )  c++;
        }

        p = (double) a / sumOfTestArg2;
        r = (double) a / sumOfRightArg2;
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

            if( str2.equals("None") && str1.equals("None")  )  a++;

            if( str2.equals("None") && !str1.equals("None") )  c++;
        }

        p = (double) a / sumofTestNone;
        r = (double) a / sumOfRightNone;
        f = (double) (2 * p * r) / (p + r) ;

        System.out.println("For argument postion classification: None ");
        System.out.print("P is ");System.out.println(p);
        System.out.print("R is ");System.out.println(r);
        System.out.print("F is ");System.out.println(f);


    }

    /**
     * 计算一个目录里面以某扩展名结尾的文件数
     * @param dir
     * @param endWith
     * @return
     */
    public int countFileNumbersOfDirectory(String dir,String endWith)
    {
        ArrayList<String> fileList = new ArrayList<String>();
        util.getFiles(dir,fileList,endWith);

        return fileList.size();
    }


    public static int countFileWords(String dir,String endWith)
    {
        ArrayList<String> fileList = new ArrayList<String>();
        util.getFiles(dir,fileList,endWith);


        int numberOfFile = 0;
        for(String file:fileList)
        {
            String content = util.readFileToString(file);
            String contenWithoutSpace = content.replaceAll(" ","");
            numberOfFile += contenWithoutSpace.length();
        }

        return numberOfFile;
    }

    /**
     *
     * @throws IOException
     */
    public void computeConnetiveRecognition() throws IOException {
        ArrayList<String> stringArrayListRight  = new ArrayList<String>();
        util.readFileToLines(".\\isConnectiveResult.txt",stringArrayListRight);

        ArrayList<String> stringArrayListTest  = new ArrayList<String>();
        util.readFileToLines(".\\result.txt",stringArrayListTest);

        int a = 0,b = 0,c = 0,d = 0;
        int sumOfTestYes = 0,sumOfTestNo = 0,sumOfRightYes = 0, sumOfRightNo = 0;
        double p,r,f,acc,err;

        for(int i = 0; i < stringArrayListRight.size() ; i++)
//        for(int i = 0; i < 3000 ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if(str2.equals("1"))   sumOfTestYes++;
            else sumOfTestNo++;

            if(str1.equals("1")) sumOfRightYes++;
            else                  sumOfRightNo++;

            if( str2.equals("1") && str1.equals("1"))   a++;
            if( str2.equals("1") && str1.equals("0"))   b++;
            if( str2.equals("0") && str1.equals("1"))   c++;
            if( str2.equals("0") && str1.equals("0"))   d++;;

        }

//        p = (double) a / sumOfTestYes;
//        r = (double) a / sumOfRightYes;
        p = (double)a / (a + b);
        r = (double)a / (a + c);
        f = (double) (2 * p * r) / (p + r) ;
        acc = (double)(a + d) / (a + b + c + d);
        err = (double)(b + c) / (a + b + c + d);

        System.out.println("For argument postion classification: Yes ");
        System.out.println("P is " + p);
        System.out.println("R is " + r);
        System.out.println("F is " + f);
        System.out.println("Accuracy is " + acc);
        System.out.println("Error is " + err);
//        System.out.println("SumOfTestPS is " + sumOfTestPS);

        a = 0;b = 0;c = 0;d = 0;
        for(int i = 0; i < stringArrayListRight.size() ; i++)
//        for(int i = 0; i < 3000 ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            if( str2.equals("0") && str1.equals("0"))   a++;
            if( str2.equals("0") && str1.equals("1"))   b++;
            if( str2.equals("1") && str1.equals("0"))   c++;
            if( str2.equals("1") && str1.equals("1"))   d++;;
        }

//        p = (double) b / sumOfTestNo;
//        r = (double) b / sumOfRightNo;
        p = (double)a / (a + b);
        r = (double)a / (a + c);
        f = (double) (2 * p * r) / (p + r) ;
        acc = (double)(a + d) / (a + b + c + d);
        err = (double)(b + c) / (a + b + c + d);

        System.out.println("For argument postion classification: No ");
        System.out.println("P is " + p);
        System.out.println("R is " + r);
        System.out.println("F is " + f);
        System.out.println("Accuracy is " + acc);
        System.out.println("Error is " + err);

        System.out.println("The total number of Postive sample  is " + sumOfRightYes);
        System.out.println("The total number of Negative sample is " + sumOfRightNo);
        System.out.println("The total number of Postive sample in test  is " + sumOfTestYes);
        System.out.println("The total number of Negative sample in test is " + sumOfTestNo);
    }

    /**
     * 计算显式关系类型识别的PRF
     * @throws IOException
     */
    public void computPRFOfExplicit() throws IOException
    {

        ArrayList<String> stringArrayListRight  = new ArrayList<String>();
        util.readFileToLines(".\\result2.txt",stringArrayListRight);

        ArrayList<String> stringArrayListTest  = new ArrayList<String>();
        util.readFileToLines(".\\result.txt",stringArrayListTest);


        int sumOfTestYes = 0,sumOfTestNo = 0,sumOfRightYes = 0, sumOfRightNo = 0;

        int nRightOfLevelOne = 0,nRightOfLevelTwo = 0;

        int nErrorOfLevelOne = 0,nErrorOfLevelTwo = 0;
        //微平均
        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            String arrayOfRight[] = str1.split("-");
            String arrayOfTest[] = str2.split("-");

            if(arrayOfTest[0].equalsIgnoreCase(arrayOfRight[0]))
            {
                nRightOfLevelOne++;
                if(arrayOfTest[1].equalsIgnoreCase(arrayOfRight[1]))
                {
                    nRightOfLevelTwo++;
                }
                else
                {
                    nErrorOfLevelTwo++;
                }
            }
            else
            {
                nErrorOfLevelOne ++;
            }

        }

        double dAccracyOfLevelOne = (double) nRightOfLevelOne / stringArrayListTest.size();
        double dAccracyOfLevelOTwo = (double) nRightOfLevelTwo / stringArrayListTest.size();
        System.out.println("The recall of Level 1 is " + dAccracyOfLevelOne);
        System.out.println("The recall of Level 2 is " + dAccracyOfLevelOTwo);


        System.out.println("________________________________~我是华丽丽的分割线~________________________________________");

        double p,r,f,acc,err;
        int a = 0,b = 0,c = 0,d = 0;


        //第1大类
        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            String arrayOfRight[] = str1.split("-");
            String arrayOfTest[] = str2.split("-");

            if(arrayOfTest[0].equalsIgnoreCase("1") && arrayOfRight[0].equalsIgnoreCase("1"))   a++;
            if(arrayOfTest[0].equalsIgnoreCase("1") && !arrayOfRight[0].equalsIgnoreCase("1"))  b++;
            if(!arrayOfTest[0].equalsIgnoreCase("1") && arrayOfRight[0].equalsIgnoreCase("1"))  c++;
            if(!arrayOfTest[0].equalsIgnoreCase("1") && !arrayOfRight[0].equalsIgnoreCase("1")) d++;
        }

        p = (double) a / (a + b);
        r = (double) a / (a + c);
        f = 2 * p * r / (p + r);
        System.out.println("The precision of type 1 is " + p);
        System.out.println("The recall of type 1 is " + r);
        System.out.println("The F-score of type 1 is " + f);


        //第2大类
        System.out.println("________________________________~我是华丽丽的分割线~________________________________________");
        a = 0;b = 0;c = 0;d = 0;

        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            String arrayOfRight[] = str1.split("-");
            String arrayOfTest[] = str2.split("-");

            if(arrayOfTest[0].equalsIgnoreCase("2") && arrayOfRight[0].equalsIgnoreCase("2"))   a++;
            if(arrayOfTest[0].equalsIgnoreCase("2") && !arrayOfRight[0].equalsIgnoreCase("2"))  b++;
            if(!arrayOfTest[0].equalsIgnoreCase("2") && arrayOfRight[0].equalsIgnoreCase("2"))  c++;
            if(!arrayOfTest[0].equalsIgnoreCase("2") && !arrayOfRight[0].equalsIgnoreCase("2")) d++;
        }

        p = (double) a / (a + b);
        r = (double) a / (a + c);
        f = 2 * p * r / (p + r);
        System.out.println("The precision of type 2 is " + p);
        System.out.println("The recall of type 2 is " + r);
        System.out.println("The F-score of type 2 is " + f);


        //第3大类
        System.out.println("________________________________~我是华丽丽的分割线~________________________________________");
        a = 0;b = 0;c = 0;d = 0;

        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            String arrayOfRight[] = str1.split("-");
            String arrayOfTest[] = str2.split("-");

            if(arrayOfTest[0].equalsIgnoreCase("3") && arrayOfRight[0].equalsIgnoreCase("3"))   a++;
            if(arrayOfTest[0].equalsIgnoreCase("3") && !arrayOfRight[0].equalsIgnoreCase("3"))  b++;
            if(!arrayOfTest[0].equalsIgnoreCase("3") && arrayOfRight[0].equalsIgnoreCase("3"))  c++;
            if(!arrayOfTest[0].equalsIgnoreCase("3") && !arrayOfRight[0].equalsIgnoreCase("3")) d++;
        }

        p = (double) a / (a + b);
        r = (double) a / (a + c);
        f = 2 * p * r / (p + r);
        System.out.println("The precision of type 3 is " + p);
        System.out.println("The recall of type 3 is " + r);
        System.out.println("The F-score of type 3 is " + f);


        //第4大类
        System.out.println("________________________________~我是华丽丽的分割线~________________________________________");
        a = 0;b = 0;c = 0;d = 0;

        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            String arrayOfRight[] = str1.split("-");
            String arrayOfTest[] = str2.split("-");

            if(arrayOfTest[0].equalsIgnoreCase("4") && arrayOfRight[0].equalsIgnoreCase("4"))   a++;
            if(arrayOfTest[0].equalsIgnoreCase("4") && !arrayOfRight[0].equalsIgnoreCase("4"))  b++;
            if(!arrayOfTest[0].equalsIgnoreCase("4") && arrayOfRight[0].equalsIgnoreCase("4"))  c++;
            if(!arrayOfTest[0].equalsIgnoreCase("4") && !arrayOfRight[0].equalsIgnoreCase("4")) d++;
        }

        p = (double) a / (a + b);
        r = (double) a / (a + c);
        f = 2 * p * r / (p + r);
        System.out.println("The precision of type 4 is " + p);
        System.out.println("The recall of type 4 is " + r);
        System.out.println("The F-score of type 4 is " + f);

    }


    public void computePositionPRF() throws  IOException
    {
        ArrayList<String> stringArrayListRight  = new ArrayList<String>();
        util.readFileToLines(".\\positionResult.txt",stringArrayListRight);

        ArrayList<String> stringArrayListTest  = new ArrayList<String>();
        util.readFileToLines(".\\positionForTest.txt",stringArrayListTest);

        int sumOfTestYes = 0,sumOfTestNo = 0,sumOfRightYes = 0, sumOfRightNo = 0;


//        System.out.println("________________________________~我是华丽丽的分割线~________________________________________");

        double p,r,f,acc,err;
        int a = 0,b = 0,c = 0,d = 0;

        //SS
        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            String arrayOfRight[] = str1.split("-");
            String arrayOfTest[] = str2.split("-");

            if(arrayOfTest[0].equalsIgnoreCase("true") && arrayOfRight[0].equalsIgnoreCase("true"))   a++;
            if(arrayOfTest[0].equalsIgnoreCase("true") && !arrayOfRight[0].equalsIgnoreCase("true"))  b++;
            if(!arrayOfTest[0].equalsIgnoreCase("true") && arrayOfRight[0].equalsIgnoreCase("true"))  c++;
            if(!arrayOfTest[0].equalsIgnoreCase("true") && !arrayOfRight[0].equalsIgnoreCase("true")) d++;
        }

        p = (double) a / (a + b);
        r = (double) a / (a + c);
        f = 2 * p * r / (p + r);
        System.out.println("The precision of type 1 is " + p);
        System.out.println("The recall of type 1 is " + r);
        System.out.println("The F-score of type 1 is " + f);

        //PS
        for(int i = 0; i < stringArrayListRight.size() ; i++)
        {
            String str1,str2;
            str1= stringArrayListRight.get(i);
            str2 = stringArrayListTest.get(i);

            String arrayOfRight[] = str1.split("-");
            String arrayOfTest[] = str2.split("-");

            if(arrayOfTest[0].equalsIgnoreCase("false") && arrayOfRight[0].equalsIgnoreCase("false"))   a++;
            if(arrayOfTest[0].equalsIgnoreCase("false") && !arrayOfRight[0].equalsIgnoreCase("false"))  b++;
            if(!arrayOfTest[0].equalsIgnoreCase("false") && arrayOfRight[0].equalsIgnoreCase("false"))  c++;
            if(!arrayOfTest[0].equalsIgnoreCase("false") && !arrayOfRight[0].equalsIgnoreCase("false")) d++;
        }

        p = (double) a / (a + b);
        r = (double) a / (a + c);
        f = 2 * p * r / (p + r);
        System.out.println("The precision of type 1 is " + p);
        System.out.println("The recall of type 1 is " + r);
        System.out.println("The F-score of type 1 is " + f);


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


        //实验相关

        //计算论元位置分类的PRF
//        jacky.computePostionPRF();;

        //句法树内部结点分类
//        jacky.computeArgExtPRF();

        //计算关联词识别PRF
        jacky.computeConnetiveRecognition();

        //计算显式关系类型识别PRF
//        jacky.computPRFOfExplicit();


        //统计语料中各类文件的字数
//        String dir1 = "F:\\Corpus Data\\Corpus_pubGuoOnly";
//        String dir2 = "F:\\Distribution Data\\Distribution Data HIT\\Corpus Data\\XML";
//
//        System.out.println("The total number of corpus is " + jacky.countFileNumbersOfDirectory(dir1,".txt"));
//        System.out.println("The total word number of corpus is "  + jacky.countFileWords(dir1,".txt"));
//
//        System.out.println("The total number of corpus is " + jacky.countFileNumbersOfDirectory(dir2,".txt"));
//        System.out.println("The total word number of corpus is "  + jacky.countFileWords(dir2,".txt"));
//
//        String dir3 = "F:\\Result__2.0";
//        System.out.println("The total number of corpus is " + jacky.countFileNumbersOfDirectory(dir3,".p2"));
//        System.out.println("The total word number of corpus is "  + jacky.countFileWords(dir3,".p2"));

    }

}


