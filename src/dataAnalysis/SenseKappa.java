package dataAnalysis;

import common.Constants;
import common.util;

import java.io.IOException;
import java.util.*;

/**
 * ���������ע�ľ���ϵ��һ���ԡ�����ͬ�ı�ע��Ա�ڱ�עͬһ�����ϵ�ʱ�򣬴�ұ�ע�����һ���ԡ�
 * User: Ji JianHui
 * Time: 2014-02-26 09:46
 * Email: jhji@ir.hit.edu.cn
 */
public class SenseKappa
{
    //������ע100ƪ��ԭʼ����λ��
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
     * ����������֮���ע��ϵ��һ���ԣ����õ���Cohen kappa���㷽����
     * type��Ҫָ�����ֹ�ϵ��һ�µ���������ڵڼ���һ�²���һ�¡�
     * @param type: ���������ں����������Ϊ��һ������;
     * @param precission: ������ϵ�����ʲô����������һ�ֹ�ϵ
     * @return
     */
    public void countKappa(int type, int precission) throws IOException
    {
        //���ݲ�ͬ�ľ��ӱ߽����ͬ����������ע�߽�һ�µı�ע�еļ���
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //���㲻ͬ���͵ľ��ӱ߽������
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //��ȡ������ע���У������ӱ߽�һ�µı�ע������ϡ�
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //���������˶�����arguments����
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

        //���ȼ���ֻ�ж����Ĵ����ϵ��Kappaֵ�����ӱ߽�Ҫ����ȫһ������
        //���������ͳ�ƽ����ʼ����Kappaֵ
        int N = sameRecords.size(); //���ݲ�ͬ�ı߽�һ�����������㲻ͬ�ı�ע��������.

        //����pra��ֵ
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //���վ��ȱ�����guo�г��ֵ�relNO�ʹ���
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //����pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //�޸�pre��������
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

        //����pre����żȻһ�µĸ���
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
     * ���㲻ͬ��ϵ��ע��Kappaֵ
     * @param relation  ��ϵ������: 1��2��3��4
     * @param type ͬ��
     * @param precission ͬ��
     * @throws IOException
     */
    public void countKappaRelation(int relation,int type, int precission) throws IOException
    {
        //���ݲ�ͬ�ľ��ӱ߽����ͬ����������ע�߽�һ�µı�ע�еļ���
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //���㲻ͬ���͵ľ��ӱ߽������
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //��ȡ������ע���У������ӱ߽�һ�µı�ע������ϡ�
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //���������˶�����arguments����
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

        //���ȼ���ֻ�ж����Ĵ����ϵ��Kappaֵ�����ӱ߽�Ҫ����ȫһ������
        //���������ͳ�ƽ����ʼ����Kappaֵ
        int N = sameRecords.size(); //���ݲ�ͬ�ı߽�һ�����������㲻ͬ�ı�ע��������.

        //����pra��ֵ
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //���վ��ȱ�����guo�г��ֵ�relNO�ʹ���
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //����pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //�޸�pre��������
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

        //����pre����żȻһ�µĸ���
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
     * �ֱ����P1��P2��P3 Kappaֵ
     * @param pNumber P1��P2����P3���ֱ���1,2,3��ʾ
     * @param type
     * @param precission
     * @throws IOException
     */
    public void countKappaP(int pNumber,int type, int precission) throws IOException
    {
        //���ݲ�ͬ�ľ��ӱ߽����ͬ����������ע�߽�һ�µı�ע�еļ���
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //���㲻ͬ���͵ľ��ӱ߽������
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //��ȡ������ע���У������ӱ߽�һ�µı�ע������ϡ�
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //���������˶�����arguments����
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

        //���ȼ���ֻ�ж����Ĵ����ϵ��Kappaֵ�����ӱ߽�Ҫ����ȫһ������
        //���������ͳ�ƽ����ʼ����Kappaֵ
        int N = sameRecords.size(); //���ݲ�ͬ�ı߽�һ�����������㲻ͬ�ı�ע��������.

        //����pra��ֵ
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //���վ��ȱ�����guo�г��ֵ�relNO�ʹ���
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //����pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //�޸�pre��������
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

        //����pre����żȻһ�µĸ���
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
     * �������ڻ��߲����������Kappaֵ
     * @param ajacent true�������ڣ�false��������
     * @param type
     * @param precission
     * @throws IOException
     */
    public void countKappaAdjacentOrNot(Boolean ajacent,int type, int precission) throws IOException
    {
        //���ݲ�ͬ�ľ��ӱ߽����ͬ����������ע�߽�һ�µı�ע�еļ���
        ArrayList<String[]> sameRecords = new ArrayList<String[]>();

        //���㲻ͬ���͵ľ��ӱ߽������
        int[] sameArguments = new int[4];
        for(int index = 0; index < sameArguments.length; index++) sameArguments[index] = 0;

        int guoArguments = 0, liuArguments = 0;

        //��ȡ������ע���У������ӱ߽�һ�µı�ע������ϡ�
        for(String guoPath: this.guoFiles)
        {
            //System.out.println( guoPath );

            String liuPath = guoPath.replaceAll("part1__msguo__Result", "part3__hliu__Result");

            ArrayList<String> guoLines = new ArrayList<String>();
            ArrayList<String> liuLines = new ArrayList<String>();

            util.readFileToLines(guoPath, guoLines);
            util.readFileToLines(liuPath, liuLines);

            //���������˶�����arguments����
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

        //���ȼ���ֻ�ж����Ĵ����ϵ��Kappaֵ�����ӱ߽�Ҫ����ȫһ������
        //���������ͳ�ƽ����ʼ����Kappaֵ
        int N = sameRecords.size(); //���ݲ�ͬ�ı߽�һ�����������㲻ͬ�ı�ע��������.

        //����pra��ֵ
        double pra = 0.0;

        Map<String, Integer> guoRels = new LinkedHashMap<String, Integer>(); //���վ��ȱ�����guo�г��ֵ�relNO�ʹ���
        Map<String, Integer> liuRels = new LinkedHashMap<String, Integer>();

        for( String[] pairs : sameRecords )
        {
            RawRecord guoRecord = new RawRecord( pairs[0] );
            RawRecord liuRecord = new RawRecord( pairs[1] );

            String guoRelNO = guoRecord.getRelNO();
            String liuRelNO = liuRecord.getRelNO();

            //����pra
            if( util.isSameRelationNO( guoRelNO, liuRelNO, precission) )
            {
                pra = pra + 1;
            }

            //�޸�pre��������
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

        //����pre����żȻһ�µĸ���
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
        //�趨������������ͬһ�����ӱ߽�����͡�
        // 0����ȫ�޽���   1���߽���ȫ��ͬ
        // 2: �߽���ȫǶ�� 3���߽粿�ֽ���
        int type = 1;

        //�趨�ж�������ϵ��ͬһ�ֹ�ϵ�ľ�ȷ�ȴ�С.
        //���磺1**��1-1**���ڵ�һ�㼶����ͬһ���ϵ�����ǿ��ǵ��ڶ��㼶�Ͳ�����
        //���磺1-1-2��1-1-3�ڵڶ��㼶����ͬһ���ϵ�������ڵ����㼶���������ϵ
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
