package dataAnalysis;

import common.Constants;
import common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * ��ԭ��Python����ͳ�Ƴ��������ʺ͹�ϵ�Ķ�Ӧ��ת��Ϊ�¹�ϵ��ϵ�µ���Դ�ļ���
 * ��ȡ�����ʺ͹�ϵ��ָʾ�ļ����ļ���ʽΪ
 * ʵ����	2	1	1	1	1	[X]5-1-1	1	[Y]5-1-1	1
 * User: Ji JianHui
 * Time: 2014-03-03 10:02
 * Email: jhji@ir.hit.edu.cn
 */
public class ConvertOldWordDict
{
    private String oldWordDictPath;
    private String newWordDictPath;

    public ConvertOldWordDict(String oldPath, String newPath)
    {
        this.oldWordDictPath = oldPath;
        this.newWordDictPath = newPath;
    }

    /**
     * ʵ����	2	1	1	1	1	[X]5-1-1	1	[Y]5-1-1	1
     * ת��Ϊ�¹�ϵ��ϵ�µ���Դ�ļ�
     * @throws IOException
     */
    public void convertToNewWordDict() throws IOException
    {
        ArrayList<String> oldLines = new ArrayList<String>();
        util.readFileToLines(this.oldWordDictPath, oldLines);

        ArrayList<String> newLines = new ArrayList<String>();

        //��ÿһ�ж�����ת��
        for(String oldLine:oldLines)
        {
            String[] lists = oldLine.split("\t");

            //���ֲ��������
            String word = lists[0];
            int allNum = Integer.valueOf( lists[1] ).intValue();
            int expNum = Integer.valueOf( lists[2] ).intValue();
            int impNum = Integer.valueOf( lists[3] ).intValue();

            //��������Ҫ�ϲ��Ĺ�ϵ���кϲ�
            HashMap<String,Integer> expRelations = new HashMap<String, Integer>();
            HashMap<String,Integer> impRelations = new HashMap<String, Integer>();

            //ÿ�ζ�����������ϵ�����
            for(int index = 6; index < lists.length - 1; index = index + 2)
            {
                String  type   = lists[index].substring(0,3);
                String  oldRel = lists[index].substring(3);

                Integer num    = Integer.valueOf( lists[index+1] );
                String  newRel = util.convertOldRelIDToNew(oldRel);

                if( type.equalsIgnoreCase("[X]") )
                {
                    if( expRelations.containsKey(newRel) )
                    {
                        num = num + expRelations.get(newRel);
                    }
                    expRelations.put(newRel,num);
                }
                else
                {
                    if( impRelations.containsKey(newRel) )
                    {
                        num = num + impRelations.get(newRel);
                    }
                    impRelations.put(newRel, num);
                }
            }

            ArrayList<Map.Entry<String,Integer>> newExpRels = util.sortHashMap(expRelations,false);
            ArrayList<Map.Entry<String,Integer>> newImpRels = util.sortHashMap(impRelations,false);

            int expKind = newExpRels.size();
            int impKind = newImpRels.size();

            String newLine = word + "\t" + allNum + "\t" + expNum + "\t" + impNum + "\t" + expKind + "\t" + impKind;

            for( Map.Entry<String, Integer> entry : newExpRels )
            {
                newLine = newLine + "\t" + "[X]" + entry.getKey() + "\t" + entry.getValue();
            }
            for( Map.Entry<String, Integer> entry : newImpRels )
            {
                newLine = newLine + "\t" + "[Y]" + entry.getKey() + "\t" + entry.getValue();
            }

            newLines.add( newLine );
        }

        //��ת����ɵĽ�����浽�ļ���
        util.writeLinesToFile(this.newWordDictPath, newLines);
    }

    public static void main(String[] args) throws IOException
    {
        String oldPath = "./resource/p1Word(Filtered).txt";
        String newPath = "./resource/p1Word.txt";
        ConvertOldWordDict convert = new ConvertOldWordDict(oldPath, newPath);

        convert.convertToNewWordDict();
    }
}
