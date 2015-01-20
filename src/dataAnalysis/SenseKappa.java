package dataAnalysis;

import common.Constants;
import common.util;

import java.io.IOException;
import java.util.*;

/**
 * 用来计算标注的句间关系的一致性。即不同的标注人员在标注同一段语料的时候，大家标注结果的一致性。
 * User: Ji JianHui
 * Time: 2014-02-26 09:46
 * Email: jhji@ir.hit.edu.cn
 */
public class SenseKappa
{
    //公共标注100篇的原始语料位置
    private static String corpusDir = "F:\\Corpus Data\\check\\public__result";
    //private static String corpusDir = "F:\\Corpus Data\\Test Data\\public__result";
    private ArrayList<String> guoFiles;

    public SenseKappa()
    {
        this.guoFiles = new ArrayList<String>();

        String guoDir = corpusDir + "\\part1__msguo__Result";
        String liuDir = corpusDir + "\\part3__hliu__Result";

//        util.getFiles(guoDir, guoFiles, ".p1");
        util.getFiles(guoDir, guoFiles, ".p2");
//        util.getFiles(guoDir, guoFiles, ".p3");
    }

    /**
     * 计算两个人之间标注关系的一致性，采用的是Cohen kappa计算方法。
     * type需要指定两种关系是一致的情况，即在第几层一致才算一致。
     * @param type: 两个句子在何种情况下认为是一个句子;
     * @param precission: 两个关系编号在什么精度下算是一种关系
     * @return
     */
    public void countKappa(int type, int precission) throws IOException
    {
        //根据不同的句子边界的相同情况来计算标注边界一致的标注行的集合
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //计算不同类型的句子边界个数，
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //获取公共标注的行，即句子边界一致的标注结果集合。
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //计算两个人独立的arguments个数
            for(String line:guoLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;
                //if(record.getExpNum() > 0 )
                    guoArguments++;
            }

            for(String line:liuLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;

                //if(record.getExpNum() > 0 )
                    liuArguments++;
            }

            for(String guoLine:guoLines)
            {
                int isSameType = 0;

                if( guoLine.split(" ").length < 5 ) continue;

                RawRecord guoRecord = new RawRecord(guoLine);
                if( guoRecord.getRelNO().equals("4-1") ) continue;

                for(String liuLine:liuLines)
                {
                    if( liuLine.split(" ").length < 5 ) continue;

                    RawRecord liuRecord = new RawRecord(liuLine);

                    if( liuRecord.getRelNO().equals("4-1") ) continue;

                    isSameType = guoRecord.isSameBoundaryWith(liuRecord);

                    if( isSameType > 0 && isSameType <= type )
                    {
//                        if( guoRecord.getExpNum() > 0 && liuRecord.getExpNum() > 0)
//                        {
                            sameRecords.add( new String[]{guoLine, liuLine} );

                            if(isSameType == type)
                            {
                                sameArguments[isSameType] = sameArguments[isSameType] + 1;
                            }
//                        }

                        break;
                    }
                    //if(guoRecord.getExpNum() > 0 && liuRecord.getExpNum()>0)
                }
            }
        }

        //首先计算只有顶层四大类关系的Kappa值，句子边界要求完全一致类型
        //根据上面的统计结果开始计算Kappa值
        int N = sameRecords.size(); //根据不同的边界一致类型来计算不同的标注句子总数.

        //计算pra的值
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //按照精度保存了guo中出现的relNO和次数
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //计算pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //修改pre所需数据
            guoRelNO = guoRelNO.substring( 0, Math.min(guoRelNO.length(), precission) );
            liuRelNO = liuRelNO.substring( 0, Math.min(liuRelNO.length(), precission) );

            int temp = 0;
            if( guoRels.containsKey(guoRelNO) ) temp = guoRels.get(guoRelNO).intValue();

            guoRels.put( guoRelNO, temp + 1 );

            temp = 0;
            if( liuRels.containsKey(liuRelNO) ) temp = liuRels.get(liuRelNO).intValue();

            liuRels.put( liuRelNO, temp + 1);
        }

        System.out.println("All same annotation num: " + pra);
        pra = pra / N;

        //计算pre：即偶然一致的概率
        double pre = 0.0;
        Iterator ite = guoRels.keySet().iterator();

        while( ite.hasNext() )
        {
            String key = (String)ite.next();
            int guoNum = guoRels.get(key).intValue();

            int liuNum = 0;
            if( liuRels.containsKey(key) ) liuNum = liuRels.get(key).intValue();

            pre = pre + guoNum * liuNum;
        }

        pre = pre /(N * N);

        double kappa = 0.0, expension = 10E-10;
        if( Math.abs(pre - 1) > expension )
        {
            kappa = (pra - pre) / (1 - pre);
        }

        System.out.println("All the same senteces Num: " + N + "\n");

        System.out.println("Pra: "   + pra );
        System.out.println("Pre: "   + pre);
        System.out.println("Kappa: " + kappa);

        for(int index = 0; index < sameArguments.length; index++)
        {
            System.out.println( "Type " + index + " num: " + sameArguments[index] );
        }

        System.out.println("Guo Arguments Num: " + guoArguments);
        System.out.println("Liu Arguments Num: " + liuArguments);
    }


    /**
     * 计算不同关系标注的Kappa值
     * @param relation  关系顶层编号: 1、2、3、4
     * @param type 同上
     * @param precission 同上
     * @throws IOException
     */
    public void countKappaRelation(int relation,int type, int precission) throws IOException
    {
        //根据不同的句子边界的相同情况来计算标注边界一致的标注行的集合
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //计算不同类型的句子边界个数，
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //获取公共标注的行，即句子边界一致的标注结果集合。
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //计算两个人独立的arguments个数
            for(String line:guoLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;
                //if(record.getExpNum() > 0 )
                guoArguments++;
            }

            for(String line:liuLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;

                //if(record.getExpNum() > 0 )
                liuArguments++;
            }

            for(String guoLine:guoLines)
            {
                int isSameType = 0;

                if( guoLine.split(" ").length < 5 ) continue;

                RawRecord guoRecord = new RawRecord(guoLine);
                if( guoRecord.getRelNO().equals("4-1") ) continue;

                for(String liuLine:liuLines)
                {
                    if( liuLine.split(" ").length < 5 ) continue;

                    RawRecord liuRecord = new RawRecord(liuLine);

                    if( liuRecord.getRelNO().equals("4-1") ) continue;

                    isSameType = guoRecord.isSameBoundaryWith(liuRecord);

                    if( isSameType > 0 && isSameType <= type )
                    {
//                        if( guoRecord.getExpNum() > 0 && liuRecord.getExpNum() > 0)
//                        {
                        sameRecords.add( new String[]{guoLine, liuLine} );

                        if(isSameType == type)
                        {
                            sameArguments[isSameType] = sameArguments[isSameType] + 1;
                        }
//                        }

                        break;
                    }
                    //if(guoRecord.getExpNum() > 0 && liuRecord.getExpNum()>0)
                }
            }
        }

        //首先计算只有顶层四大类关系的Kappa值，句子边界要求完全一致类型
        //根据上面的统计结果开始计算Kappa值
        int N = sameRecords.size(); //根据不同的边界一致类型来计算不同的标注句子总数.

        //计算pra的值
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //按照精度保存了guo中出现的relNO和次数
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //计算pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //修改pre所需数据
            guoRelNO = guoRelNO.substring( 0, Math.min(guoRelNO.length(), precission) );
            liuRelNO = liuRelNO.substring( 0, Math.min(liuRelNO.length(), precission) );

            int temp = 0;
            if( guoRels.containsKey(guoRelNO) ) temp = guoRels.get(guoRelNO).intValue();

            guoRels.put( guoRelNO, temp + 1 );

            temp = 0;
            if( liuRels.containsKey(liuRelNO) ) temp = liuRels.get(liuRelNO).intValue();

            liuRels.put( liuRelNO, temp + 1);
        }

        System.out.println("All same annotation num: " + pra);
        pra = pra / N;

        //计算pre：即偶然一致的概率
        double pre = 0.0;
        Iterator ite = guoRels.keySet().iterator();

        while( ite.hasNext() )
        {
            String key = (String)ite.next();
            int guoNum = guoRels.get(key).intValue();

            int liuNum = 0;
            if( liuRels.containsKey(key) ) liuNum = liuRels.get(key).intValue();

            pre = pre + guoNum * liuNum;
        }

        pre = pre /(N * N);

        double kappa = 0.0, expension = 10E-10;
        if( Math.abs(pre - 1) > expension )
        {
            kappa = (pra - pre) / (1 - pre);
        }

        System.out.println("All the same senteces Num: " + N + "\n");

        System.out.println("Pra: "   + pra );
        System.out.println("Pre: "   + pre);
        System.out.println("Kappa: " + kappa);

        for(int index = 0; index < sameArguments.length; index++)
        {
            System.out.println( "Type " + index + " num: " + sameArguments[index] );
        }

        System.out.println("Guo Arguments Num: " + guoArguments);
        System.out.println("Liu Arguments Num: " + liuArguments);
    }


    /**
     * 分别计算P1、P2、P3 Kappa值
     * @param pNumber P1、P2，、P3，分别用1,2,3表示
     * @param type
     * @param precission
     * @throws IOException
     */
    public void countKappaP(int pNumber,int type, int precission) throws IOException
    {
        //根据不同的句子边界的相同情况来计算标注边界一致的标注行的集合
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //计算不同类型的句子边界个数，
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //获取公共标注的行，即句子边界一致的标注结果集合。
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //计算两个人独立的arguments个数
            for(String line:guoLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;
                //if(record.getExpNum() > 0 )
                guoArguments++;
            }

            for(String line:liuLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;

                //if(record.getExpNum() > 0 )
                liuArguments++;
            }

            for(String guoLine:guoLines)
            {
                int isSameType = 0;

                if( guoLine.split(" ").length < 5 ) continue;

                RawRecord guoRecord = new RawRecord(guoLine);
                if( guoRecord.getRelNO().equals("4-1") ) continue;

                for(String liuLine:liuLines)
                {
                    if( liuLine.split(" ").length < 5 ) continue;

                    RawRecord liuRecord = new RawRecord(liuLine);

                    if( liuRecord.getRelNO().equals("4-1") ) continue;

                    isSameType = guoRecord.isSameBoundaryWith(liuRecord);

                    if( isSameType > 0 && isSameType <= type )
                    {
//                        if( guoRecord.getExpNum() > 0 && liuRecord.getExpNum() > 0)
//                        {
                        sameRecords.add( new String[]{guoLine, liuLine} );

                        if(isSameType == type)
                        {
                            sameArguments[isSameType] = sameArguments[isSameType] + 1;
                        }
//                        }

                        break;
                    }
                    //if(guoRecord.getExpNum() > 0 && liuRecord.getExpNum()>0)
                }
            }
        }

        //首先计算只有顶层四大类关系的Kappa值，句子边界要求完全一致类型
        //根据上面的统计结果开始计算Kappa值
        int N = sameRecords.size(); //根据不同的边界一致类型来计算不同的标注句子总数.

        //计算pra的值
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //按照精度保存了guo中出现的relNO和次数
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //计算pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //修改pre所需数据
            guoRelNO = guoRelNO.substring( 0, Math.min(guoRelNO.length(), precission) );
            liuRelNO = liuRelNO.substring( 0, Math.min(liuRelNO.length(), precission) );

            int temp = 0;
            if( guoRels.containsKey(guoRelNO) ) temp = guoRels.get(guoRelNO).intValue();

            guoRels.put( guoRelNO, temp + 1 );

            temp = 0;
            if( liuRels.containsKey(liuRelNO) ) temp = liuRels.get(liuRelNO).intValue();

            liuRels.put( liuRelNO, temp + 1);
        }

        System.out.println("All same annotation num: " + pra);
        pra = pra / N;

        //计算pre：即偶然一致的概率
        double pre = 0.0;
        Iterator ite = guoRels.keySet().iterator();

        while( ite.hasNext() )
        {
            String key = (String)ite.next();
            int guoNum = guoRels.get(key).intValue();

            int liuNum = 0;
            if( liuRels.containsKey(key) ) liuNum = liuRels.get(key).intValue();

            pre = pre + guoNum * liuNum;
        }

        pre = pre /(N * N);

        double kappa = 0.0, expension = 10E-10;
        if( Math.abs(pre - 1) > expension )
        {
            kappa = (pra - pre) / (1 - pre);
        }

        System.out.println("All the same senteces Num: " + N + "\n");

        System.out.println("Pra: "   + pra );
        System.out.println("Pre: "   + pre);
        System.out.println("Kappa: " + kappa);

        for(int index = 0; index < sameArguments.length; index++)
        {
            System.out.println( "Type " + index + " num: " + sameArguments[index] );
        }

        System.out.println("Guo Arguments Num: " + guoArguments);
        System.out.println("Liu Arguments Num: " + liuArguments);
    }


    /**
     * 计算相邻或者不相邻情况的Kappa值
     * @param ajacent true代表相邻，false代表不相邻
     * @param type
     * @param precission
     * @throws IOException
     */
    public void countKappaAdjacentOrNot(Boolean ajacent,int type, int precission) throws IOException
    {
        //根据不同的句子边界的相同情况来计算标注边界一致的标注行的集合
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //计算不同类型的句子边界个数，
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //获取公共标注的行，即句子边界一致的标注结果集合。
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //计算两个人独立的arguments个数
            for(String line:guoLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;
                //if(record.getExpNum() > 0 )
                guoArguments++;
            }

            for(String line:liuLines)
            {
                if( line.split(" ").length < 5 ) continue;

                RawRecord record = new RawRecord(line);
                if( record.getRelNO().equals("4-1") ) continue;

                //if(record.getExpNum() > 0 )
                liuArguments++;
            }

            for(String guoLine:guoLines)
            {
                int isSameType = 0;

                if( guoLine.split(" ").length < 5 ) continue;

                RawRecord guoRecord = new RawRecord(guoLine);
                if( guoRecord.getRelNO().equals("4-1") ) continue;

                for(String liuLine:liuLines)
                {
                    if( liuLine.split(" ").length < 5 ) continue;

                    RawRecord liuRecord = new RawRecord(liuLine);

                    if( liuRecord.getRelNO().equals("4-1") ) continue;

                    isSameType = guoRecord.isSameBoundaryWith(liuRecord);

                    if( isSameType > 0 && isSameType <= type )
                    {
//                        if( guoRecord.getExpNum() > 0 && liuRecord.getExpNum() > 0)
//                        {
                        sameRecords.add( new String[]{guoLine, liuLine} );

                        if(isSameType == type)
                        {
                            sameArguments[isSameType] = sameArguments[isSameType] + 1;
                        }
//                        }

                        break;
                    }
                    //if(guoRecord.getExpNum() > 0 && liuRecord.getExpNum()>0)
                }
            }
        }

        //首先计算只有顶层四大类关系的Kappa值，句子边界要求完全一致类型
        //根据上面的统计结果开始计算Kappa值
        int N = sameRecords.size(); //根据不同的边界一致类型来计算不同的标注句子总数.

        //计算pra的值
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //按照精度保存了guo中出现的relNO和次数
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //计算pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //修改pre所需数据
            guoRelNO = guoRelNO.substring( 0, Math.min(guoRelNO.length(), precission) );
            liuRelNO = liuRelNO.substring( 0, Math.min(liuRelNO.length(), precission) );

            int temp = 0;
            if( guoRels.containsKey(guoRelNO) ) temp = guoRels.get(guoRelNO).intValue();

            guoRels.put( guoRelNO, temp + 1 );

            temp = 0;
            if( liuRels.containsKey(liuRelNO) ) temp = liuRels.get(liuRelNO).intValue();

            liuRels.put( liuRelNO, temp + 1);
        }

        System.out.println("All same annotation num: " + pra);
        pra = pra / N;

        //计算pre：即偶然一致的概率
        double pre = 0.0;
        Iterator ite = guoRels.keySet().iterator();

        while( ite.hasNext() )
        {
            String key = (String)ite.next();
            int guoNum = guoRels.get(key).intValue();

            int liuNum = 0;
            if( liuRels.containsKey(key) ) liuNum = liuRels.get(key).intValue();

            pre = pre + guoNum * liuNum;
        }

        pre = pre /(N * N);

        double kappa = 0.0, expension = 10E-10;
        if( Math.abs(pre - 1) > expension )
        {
            kappa = (pra - pre) / (1 - pre);
        }

        System.out.println("All the same senteces Num: " + N + "\n");

        System.out.println("Pra: "   + pra );
        System.out.println("Pre: "   + pre);
        System.out.println("Kappa: " + kappa);

        for(int index = 0; index < sameArguments.length; index++)
        {
            System.out.println( "Type " + index + " num: " + sameArguments[index] );
        }

        System.out.println("Guo Arguments Num: " + guoArguments);
        System.out.println("Liu Arguments Num: " + liuArguments);
    }

    public void test() throws IOException {
        //设定两个句子是在同一个句子边界的类型。
        // 0：完全无交叉   1：边界完全相同
        // 2: 边界完全嵌套 3：边界部分交叉
        int type = 1;

        //设定判断两个关系是同一种关系的精确度大小.
        //比如：1**和1-1**是在第一层级上是同一类关系，但是考虑到第二层级就不是了
        //比如：1-1-2和1-1-3在第二层级上是同一类关系，但是在第三层级就是两类关系
        int precission = 1;

        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);

        this.countKappa(type, precission);

        System.out.println("----------------------------------------------");
        type = 1; precission = 2;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);


        System.out.println("----------------------------------------------");
        type = 1; precission = 3;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);


        System.out.println("----------------------------------------------");
        type = 2; precission = 1;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);

        System.out.println("----------------------------------------------");
        type = 2; precission = 2;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);

        System.out.println("----------------------------------------------");
        type = 2; precission = 3;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);


        System.out.println("----------------------------------------------");
        type = 3; precission = 1;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);


        System.out.println("----------------------------------------------");
        type = 3; precission = 2;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);


        System.out.println("----------------------------------------------");
        type = 3; precission = 3;
        System.out.println("Same Sentence Boundary: " + type);
        System.out.println("Sense Precission: " + precission);
        this.countKappa(type, precission);


//        System.out.println("----------------------------------------------");
//        type = 0; precission = 1;
//        System.out.println("Same Sentence Boundary: " + type);
//        System.out.println("Sense Precission: " + precission);
//        this.countKappa(type, precission);
//
//
//        System.out.println("----------------------------------------------");
//        type = 0; precission = 2;
//        System.out.println("Same Sentence Boundary: " + type);
//        System.out.println("Sense Precission: " + precission);
//        this.countKappa(type, precission);
//
//        System.out.println("----------------------------------------------");
//        type = 0; precission = 3;
//        System.out.println("Same Sentence Boundary: " + type);
//        System.out.println("Sense Precission: " + precission);
//        this.countKappa(type, precission);
    }

    public static void main(String[] args) throws IOException
    {

        SenseKappa senseKappa = new SenseKappa();

        senseKappa.test();




    }
}
