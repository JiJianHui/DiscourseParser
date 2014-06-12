package dataAnalysis;

/**
 * Created with IntelliJ IDEA.
 * Ϊ�˼���һ������ָʾ������ϵ�͸��ʡ�Ϊ����ĺ���countWordAmbiguity����
 * User: Ji JianHui
 * Time: 2014-05-28 14:58
 * Email: jhji@ir.hit.edu.cn
 */
public class WordAmbiguity
{
    public int      maxIndex   = 0;   //����ָʾ������ϵ�������
    public int      number     = 0;   //���ʳ��ִ���
    public String   content    = null;

    public double[] probaliaty = new double[7];//ָʾÿ����ϵ�ĸ���

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

    //�Թ�ϵ���ʽ��м���. ��Ҫ���뾫�ȣ��ڸþ���֮�µ����ݾͱ�������û��ָʾһ����ϵ
    //ָʾ��ϵ�����ر�С�ĸ�������Ҫ���ò�ͬ����ֵ�Ϳ��Եõ���ͬ�ľ���
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