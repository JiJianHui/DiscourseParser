package common;

import java.util.Random;

/**
 * 随机数工具类，主要是用于在程序中来生成随机数。
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-30 14:40
 * Email: jhji@ir.hit.edu.cn
 */
public class RandomUtil
{
    /**
     * 获取N范围内的一组不重复的随机数m，主要是用于在将N原始数据分割为m个训练数据和n个测试数据。
     * max：范围N，length生成的个数。
     **/
    public void getRandomNums(int length, int max)
    {
        int[] randomValues = new int[length]; //存放的是随机值
        boolean[] exist    = new boolean[max]; //判断是否出现过

        Random r = new Random(max);
        int tempValue = 0;
        for(int i = 0; i < length; i++)
        {
            //产生一个不重复的数字
            do
            {
                tempValue = r.nextInt(max);
            }while( exist[tempValue] );

            exist[tempValue] = true;
            randomValues[i]  = tempValue;
            System.out.println("i: "+ i + " value:" + tempValue);
        }
    }

    /**测试相同种子数的随机数生成情况。**/
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

    /**测试随机数生成器的生成质量。网上大家都说生成的重复率比较高**/
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
