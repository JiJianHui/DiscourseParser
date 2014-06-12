package dataAnalysis;

/**
 * Created with IntelliJ IDEA.
 * 为了计算一个连词指示的最大关系和概率。为上面的函数countWordAmbiguity服务
 * User: Ji JianHui
 * Time: 2014-05-28 14:58
 * Email: jhji@ir.hit.edu.cn
 */
public class WordAmbiguity
{
    public int      maxIndex   = 0;   //连词指示最多个关系编号索引
    public int      number     = 0;   //连词出现次数
    public String   content    = null;

    public double[] probaliaty = new double[7];//指示每个关系的概率

    public WordAmbiguity(String line)
    {
        String[] lists = line.split("\t");
        content = lists[0];
        number  = Integer.valueOf(lists[1]);

        double max = 0;

        for(int index = 2; index < lists.length; index = index + 2)
        {
            int relID   = Integer.valueOf(lists[index]);
            double prob = Double.valueOf( lists[index+1] );

            probaliaty[relID] = prob;

            if( prob > max ) maxIndex = index - 2;
        }
    }

    //对关系概率进行计算. 需要输入精度，在该精度之下的数据就被当做了没有指示一个关系
    //指示关系概率特别小的个数，主要采用不同的阈值就可以得到不同的精度
    public int getZeroNum(double eps)
    {
        int zeroNum = 0;

        for(int index  = 0; index < this.probaliaty.length; index ++)
        {
            if( probaliaty[index] <= eps ) zeroNum++;
        }
        return zeroNum;
    }

}