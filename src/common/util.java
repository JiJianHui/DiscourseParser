package common;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import resource.Resource;

/**
 * ���ú������߰����ϣ�����ֻ��Ź��ߺ���
 * User: Ji JianHui
 * Time: 2014-02-19 14:48
 * Email: jhji@ir.hit.edu.cn
 */
public class util
{
    /**�������ļ���ȡΪ�ַ���**/
    public static String readFileToString(String fPath)
    {
        File file       = new File(fPath);
        Long filelength = file.length();

        byte[] filecontent = new byte[filelength.intValue()];

        try
        {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return new String(filecontent);
    }

    /**
     * ��һ���ַ������зָ�Ϊ���ӵļ��ϡ�ע���ʱ��ԭʼ�����л�������һЩ�Ƿ��ַ���Ҫ���д������������з����Ƿ����ŵȡ�
     * @param fileContent
     * @return
     */
    public static ArrayList<String> filtStringToSentence( String fileContent )
    {
        int    index = 0;

        //���վ��ӷָ������ָ����
        Matcher  matcher   = Constants.Sentence_Seperators_Pattern.matcher(fileContent);
        String[] sentences = Constants.Sentence_Seperators_Pattern.split(fileContent);

         //�����ӽ��������ӵ���Ӧ�ľ��Ӻ����ɴֲڵķָ���
        if( sentences.length > 0 )
        {
            index = 0;
            while(index < sentences.length)
            {
                if(matcher.find())
                {
                    sentences[index] += matcher.group();
                }
                index++;
            }
        }

        //------------------------�ָ����ȴ�С�ж�-------------------------------
        //�ж�ÿ�������Ƿ�������ӷָ�Ĵ�С����,��ֹһЩ�Ƿ��Ķ̾�
        ArrayList<String> correctSentences = new ArrayList<String>();

        StringBuffer buffer = new StringBuffer( Constants.MAX_Buffer_Size );

        for(index = 0; index < sentences.length; index++)
        {
            String curSentence = sentences[index].trim();

            //�����ǰ�ָ��С�ڷָ����ȣ����л���.�����������ݹ��ˣ���ӵ������
            if( curSentence.length() < Constants.MIN_Sentence_Length )
            {
                buffer.append( curSentence );

                if( buffer.length() > Constants.MIN_Sentence_Length )
                {
                    correctSentences.add( buffer.toString() );
                    buffer.delete( 0, buffer.length() );
                }
            }
            else
            {
                //����ָ����ȵ�����£���������������ݵĻ�
                //����Ӧ�ý�����д�뵽���յĽ���������д�ľ��ӣ��Ա�֤ԭ��˳��
                int tempIndex = 0;
                String previous = "", last = "";

                if( buffer.length() != 0 )
                {
                    //Ϊ�˽��Bug��"�µ�һ�꣬�µĿ�ʼ���������Ҫ������Ŭ��������"
                    // �Լ�Bug��"�µ�һ�꣬�µĿ�ʼ�����������Ҫ������Ŭ��������"
                    //��Ի����е�������Ҫ�жϸû����Ǵ������һ��ı����Ż�����һ������ݡ�
                    String bufferStr = buffer.toString();
                    tempIndex = findSeperatorIndex(bufferStr);

                    previous = correctSentences.get( correctSentences.size() - 1 );
                    previous += bufferStr.substring( 0, tempIndex );
                    last = bufferStr.substring(tempIndex);

                    correctSentences.remove( correctSentences.size() - 1 );
                    correctSentences.add( previous );
                }
                correctSentences.add( last + curSentence );
                buffer.delete( 0, buffer.length() );
            }
        }

        return correctSentences;
    }

    private static int findSeperatorIndex(String str)
    {
        int index = 0;

        for(index = 0; index < str.length(); index++)
        {
            Matcher matcher = Constants.Chinese_Word_Pattern.matcher( str.substring(index,index+1) );

            if( matcher.find() ) break;
        }

        return index;
    }



    /**��һ��XML�ļ�ת��ΪDOM�������ڲ���**/
    public static Document parseXMLStringToDOM(String content)
    {
        Document domObj = null;
        try
        {
            domObj = DocumentHelper.parseText(content);
        }
        catch (DocumentException e)
        {
            e.printStackTrace();
        }

        return domObj;
    }

    /**��һ��XML�ļ�ת��ΪDOM�������ڲ���**/
    public static Document parseXMLFileToDOM(String fileName)
    {
        Document domobj  = null;
        SAXReader reader = new SAXReader();
        try
        {
            domobj = reader.read(fileName);
            //domobj = reader.read(new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8")));
        }
        catch (DocumentException e)
        {
            e.printStackTrace();
        }
        return domobj;
    }

    /**��һ��XML�ļ�ת��ΪDOM�������ڲ���**/
    public static Document parseXMLFileToDOM(String fileName, String encoding)
    {
        Document domobj  = null;
        SAXReader reader = new SAXReader();
        try
        {
            //domobj = reader.read(fileName);
            domobj = reader.read(new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encoding)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return domobj;
    }


    /**
     * ��ȡָ���ļ����µ������ļ������������ӵ�files�б��С�
     * @param dir����ע�ļ�·��
     * @param files: ��Ҫ���µ��ļ��б�����ԭ�еĻ������޸ġ������ݹ����������.
     * @param ending:ָ�����ļ���׺������ָ����������Ϊnull,��������ȡ���е��ļ���
     * @return
     */
    public static void getFiles(String dir, ArrayList<String> files, String ending)
    {
        File   temp   = null;
        File   parent = new File(dir);
        File[] lists  = parent.listFiles();

        for(int i = 0; i < lists.length; i++)
        {
            temp = lists[i];

            if( temp.isFile() )
            {
                if( temp.getName().endsWith(ending) )
                {
                    files.add( temp.getAbsolutePath() );
                }
            }
            else if( temp.isDirectory() )
            {
                util.getFiles( temp.getAbsolutePath(), files, ending );
            }
        }
    }

    /**
     * ��һ���ļ����ɰ��д洢��vector
     * @param lines
     * @throws IOException
     */
    public static void readFileToLines(String fPath, ArrayList<String> lines) throws IOException
    {
        String line = null;
        BufferedReader br = new BufferedReader( new FileReader(fPath) );

        while( ( line = br.readLine() ) != null )
        {
            line = line.trim();
            lines.add(line);
        }

        br.close();
    }

    /**
     * ��һ���ļ����ɰ��д洢��vector
     * @param lines
     * @throws IOException
     */
    public static void readFileToLines(String fPath, ArrayList<String> lines, int maxNum) throws IOException
    {
        String line = null;
        BufferedReader br = new BufferedReader( new FileReader(fPath) );

        int index = 0;

        while( ( line = br.readLine() ) != null )
        {
            line = line.trim();
            lines.add(line);

            index = index + 1;
            if( index > maxNum - 1 ) break;
        }

        br.close();
    }


    /**
     * �ж�������ϵ����Ƿ���ͬһ��ϵ��������Ϊ��ϵ��һ�ֲ㼶�ṹ�������Ҫ�涨�жϵľ��ȴ�С
     * ������ϵ���ֻ��ͬʱ�ﵽ�涨�ľ��ȵĲ������ͬ������ͬһ�ֹ�ϵ
     * @param relA
     * @param relB
     * @return
     */
    public static boolean isSameRelationNO(String relA, String relB, int precison)
    {
        String[] aLists = relA.split("-");
        String[] bLists = relB.split("-");

        int aLen = aLists.length;
        int bLen = bLists.length;

        if(aLen < precison || bLen < precison) return false;

        boolean isSame = true;

        for(int index = 0; index < precison; index++)
        {
            if( !aLists[index].equalsIgnoreCase(bLists[index]) )
            {
                isSame = false;
                break;
            }
        }

        return isSame;
    }

    /**��ȡ��ϵ�ı�ţ��������1-1ʱ�����ȹ�ϵ,���ص���1-1**/
    public static String getRelNO(String relName)
    {
        int    index = 0;
        String relID = null;

        relName = relName.trim();
        index   = relName.lastIndexOf('-') + 1; //��Ϊ0�н�ʱ,����Ϊ0

        relID   = relName.substring(0, index+1);

        return relID;
    }


    /***
     * ���ɹ�ϵ���ת��Ϊ�¹�ϵ���
     * @param oldRelID
     * @return
     */
    public static String convertOldRelIDToNew(String oldRelID)
    {
        String newRelID = null;

        int index = 0;
        boolean find    = false;

        for(index = 0; index < Constants.oldRelNO.length; index++)
        {
            if(Constants.oldRelNO[index].equalsIgnoreCase(oldRelID))
            {
                find = true;
                break;
            }
        }

        if( find )
        {
            newRelID = Constants.relNO_oldTonew[index];
        }

        return newRelID;
    }

    /**
     * ��HashMap�������򣬰���valueֵ��������
     * ascΪtrue����������������У�ascΪfalse�����ս�������
     * @return
     */
    public static ArrayList<Map.Entry<String,Integer>>  sortHashMap(HashMap<String, Integer> data, final boolean asc)
    {
        ArrayList<Map.Entry<String,Integer>> results = new ArrayList<Map.Entry<String,Integer>>(data.entrySet());

        Collections.sort( results, new Comparator<Map.Entry<String, Integer>>()
            {
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2)
                {
                    if( asc )
                        return ( o1.getValue() - o2.getValue() );
                    else
                        return ( o2.getValue() - o1.getValue() );
                }
            }
        );

        return results;
    }

    /***
     * �������Hashmap����key��������
     * ascΪtrue�������������У�ascΪfalse�����ս�������
     * **/
    public static ArrayList<Map.Entry<String,Integer>> sortHashMapByKey(HashMap<String, Integer> data, final boolean asc)
    {
        ArrayList<Map.Entry<String,Integer>> results = new ArrayList<Map.Entry<String,Integer>>(data.entrySet());

        Collections.sort( results, new Comparator<Map.Entry<String, Integer>>()
        {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2)
            {
                if( asc )
                    return ( o1.getKey().compareTo(o2.getKey()) );
                else
                    return ( o2.getKey().compareTo(o1.getKey()) );
            }
        }
        );

        return results;
    }

    public static void writeLinesToFile(String fPath, ArrayList<String> lines) throws IOException
    {
        FileWriter fw = new FileWriter( new File(fPath) );

        for(String line : lines)
        {
            fw.write(line + "\r\n");
        }

        fw.close();
    }

    public static void main(String argus[])
    {
        //���Ծ��ӷָ�
        String fPath = "E:\\Program\\Java\\DiscourseParser\\data\\TestSentenceSplit.txt";
        String fContent = util.readFileToString(fPath);

        ArrayList<String> sentences = util.filtStringToSentence(fContent);

        for(int index = 0; index < sentences.size(); index++)
        {
            System.out.println( "-----------------------------" );
            System.out.println( sentences.get(index) );
        }
    }


    /**
     * ʹ��������ʽ�ж�һ�����������Ƿ������һ��������
     * @param parallelWord: ʹ��;�ָ�������
     * @param sentence����������
     * @return
     */
    public static boolean isParallelWordInSentence(String parallelWord, String sentence)
    {
        boolean  result    = false;
        String[] words     = parallelWord.split(";");
        String[] sentWords = sentence.split(" ");

        int index = 0, end = 0;

        for( end = 0; end < words.length; end++ )
        {
            String word = words[end];

            for(; index < sentWords.length; index++)
            {
                if( word.equalsIgnoreCase(sentWords[index]) ) break;
            }

            //����Ѿ������˽���
            if( index > sentWords.length - 1 ) break;
        }

        //����ܹ��������һ���ʣ���Ϊ��
        if( end > words.length - 1 ) result = true;

        return result;
    }

    /**ȥ���ַ������������Ŀո񣨰���:�ո�(ȫ�ǣ����)���Ʊ������ҳ���ȣ�*/
    public static String removeAllBlank(String s)
    {
        String result = "";
        if(null!=s && !"".equals(s))
        {
            result = s.replaceAll("[��*| *|&nbsp;*|//s*]*", "");
        }
        return result;
    }

    /*** ȥ���ַ�����ͷ����β���������Ŀո񣨰���:�ո�(ȫ�ǣ����)���Ʊ������ҳ���ȣ�*/
    public static String trim(String s)
    {
        String result = "";
        if(null!=s && !"".equals(s))
        {
            result = s.replaceAll("^[��*| *|&nbsp;*|//s*]*", "").replaceAll("[��*| *|&nbsp;*|//s*]*$", "");
        }
        return result;
    }


    /**
     * ��һ����Χ�ڵ��������ŵ�����һ����Χ��
     */
    public static double scale(double value, double minValue, double maxValue, double lower, double upper)
    {
        double result = value;

        if(value == minValue)
            result = lower;
        else if(value == maxValue)
            result = upper;
        else
            result = lower + (upper-lower) *(value-minValue)/(maxValue - minValue);

        return result;
    }


    /**
     * ��ȡһ�б�ע�ı��Ŀ�ʼλ��
     * @param line
     * @return
     */
    public static int getLineBeg(String line)
    {
        String[] lists = line.split(" ");

        int beg1 = Integer.valueOf(lists[0]);
        int beg2 = Integer.valueOf(lists[2]);

        if(beg1 < beg2)
            return beg1;
        else
            return beg2;
    }

    /**��ȡ�µĹ�ϵ����£��ù�ϵ������λ��**/
    public static int getRelIDIndex(String relID)
    {
        int     index = -1;
        boolean find  = false;

        for(index = 0; index < Constants.relNo.length; index++)
        {
            if(Constants.relNo[index].equalsIgnoreCase(relID))
            {
                find = true;
                break;
            }
        }

        if( find )
        {
            return index;
        }

        return -1;
    }

    /***
     * ��ȡһ������ͬ��ʴ����еı�ǩ��Aa01A03= ���� ��Ա �˿� �˶� �� ʳָ--���ǻ�ȡ����ǰ����
     * @param word��һ����
     * @return
     */
    public static String getWordTagInSym(String word) throws IOException
    {
        String result = null;

        if( word == null ) return result;
        if( Resource.SymWordDict.size() < 1 ) Resource.LoadSymWordDict();

        for(String line:Resource.SymWordDict)
        {
            boolean  find  = false;

            if(line.indexOf(word) == -1 ) continue;

            String[] lists = line.split(" ");

            for(String curWord:lists)
            {
                if(curWord.equalsIgnoreCase(word)){
                    find   = true;
                    result = line.substring(0,4);
                    break;
                }
            }

            if( find == true ) break;
        }

        return result;
    }

    /**��max��Χ�ڻ�ȡlength�����ظ�������������ص���һ��Boolean���飬����±�index��Ӧ��ֵΪtrue���ʹ����������**/
    public static boolean[] getRandomArrays(int max, int length)
    {
        boolean[] exist = new boolean[max]; //�ж��Ƿ���ֹ�
        Random r = new Random(max);
        int tempValue = 0;

        for(int i = 0; i < length; i++)
        {
            do{ tempValue = r.nextInt(max); }while( exist[tempValue] );

            exist[tempValue] = true;
        }

        return exist;
    }

    /**��һ����������ֳ�ѵ�����Ͳ��Լ��ϡ�**/
    public static void randomSplitList(ArrayList<String> lines, int trainLength,
                                       ArrayList<String> trainDatas, ArrayList<String> testDatas)
    {
        int max = lines.size();
        boolean[] randomValues = getRandomArrays(max, trainLength);

        for(int index = 0; index < max; index++)
        {
            if( randomValues[index] )
                trainDatas.add( lines.get(index) );
            else
                testDatas.add( lines.get(index) );
        }
    }
}
