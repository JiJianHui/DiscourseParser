package common;

import java.util.Random;

/**
 * ����������࣬��Ҫ�������ڳ������������������
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-30 14:40
 * Email: jhji@ir.hit.edu.cn
 */
public class RandomUtil
{
    /**
     * ��ȡN��Χ�ڵ�һ�鲻�ظ��������m����Ҫ�������ڽ�Nԭʼ���ݷָ�Ϊm��ѵ�����ݺ�n���������ݡ�
     * max����ΧN��length���ɵĸ�����
     **/
    public void getRandomNums(int length, int max)
    {
        int[] randomValues = new int[length]; //��ŵ������ֵ
        boolean[] exist    = new boolean[max]; //�ж��Ƿ���ֹ�

        Random r = new Random(max);
        int tempValue = 0;
        for(int i = 0; i < length; i++)
        {
            //����һ�����ظ�������
            do
            {
                tempValue = r.nextInt(max);
            }while( exist[tempValue] );

            exist[tempValue] = true;
            randomValues[i]  = tempValue;
            System.out.println("i: "+ i + " value:" + tempValue);
        }
    }

    /**������ͬ����������������������**/
    public void testSameSeeds()
    {
        Random r1 = new Random(100);
        Random r2 = new Random(100);

        for(int i = 0;i < 5;i++)
        {
            System.out.println("Times: " + i);
            System.out.println("r1: " + r1.nextInt(50000) + " r2: " + r2.nextInt(50000) );
        }
    }

    /**������������������������������ϴ�Ҷ�˵���ɵ��ظ��ʱȽϸ�**/
    public void testRandomFunction()
    {
        int N = 50000;
        int sameNum = 0;
        int[] times = new int[N];
        Random r = new Random();

        for(int i = 0; i < N; i++) times[i] = 0;

        for(int i = 0; i < N; i++)
        {
            int curValue = r.nextInt(50000);
            times[curValue]++;
            if(times[curValue] > 1) sameNum++;
        }

        double sameRate = sameNum*1.0 / N;

        System.out.println("Same: " + sameRate);
    }

    public static void main(String[] args)
    {
        RandomUtil util = new RandomUtil();

        //util.testSameSeeds();
        //System.out.println(Integer.MAX_VALUE);
        //util.testRandomFunction();

        util.getRandomNums(5, 5000);
    }
}
