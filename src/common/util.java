package common;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import resource.Resource;

/**
 * 常用函数工具包集合，里面只存放工具函数
 * User: Ji JianHui
 * Time: 2014-02-19 14:48
 * Email: jhji@ir.hit.edu.cn
 */
public class util
{
    /**将整个文件读取为字符串**/
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
     * 将一个字符串进行分割为句子的集合。注意此时的原始句子中还存在着一些非法字符需要自行处理：包括：换行符，非法符号等。
     * @param fileContent
     * @return
     */
    public static ArrayList<String> filtStringToSentence( String fileContent )
    {
        int    index = 0;

        //按照句子分隔符来分割句子
        Matcher  matcher   = Constants.Sentence_Seperators_Pattern.matcher(fileContent);
        String[] sentences = Constants.Sentence_Seperators_Pattern.split(fileContent);

         //将句子结束符连接到相应的句子后，生成粗糙的分割结果
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

        //------------------------分割粒度大小判断-------------------------------
        //判断每个句子是否满足句子分割的大小粒度,防止一些非法的短句
        ArrayList<String> correctSentences = new ArrayList<String>();

        StringBuffer buffer = new StringBuffer( Constants.MAX_Buffer_Size );

        for(index = 0; index < sentences.length; index++)
        {
            String curSentence = sentences[index].trim();

            //如果当前分割的小于分割粒度，进行缓存.如果缓存的内容够了，添加到结果中
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
                //满足分割粒度的情况下，如果缓存中有内容的话
                //首先应该将内容写入到最终的结果中再添加写的句子，以保证原文顺序
                int tempIndex = 0;
                String previous = "", last = "";

                if( buffer.length() != 0 )
                {
                    //为了解决Bug："新的一年，新的开始。。。大家要打起精神，努力工作。"
                    // 以及Bug："新的一年，新的开始。不过！大家要打起精神，努力工作。"
                    //针对缓存中的内容需要判断该缓存是纯粹的上一句的标点符号还是下一句的内容。
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



    /**将一个XML文件转换为DOM对象，用于测试**/
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

    /**将一个XML文件转换为DOM对象，用于测试**/
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

    /**将一个XML文件转换为DOM对象，用于测试**/
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
     * 获取指定文件夹下的所有文件，并将结果添加到files列表中。
     * @param dir：标注文件路径
     * @param files: 需要更新的文件列表，是在原有的基础上修改。这样递归就起作用啦.
     * @param ending:指定的文件后缀。必须指定，若设置为null,则用来获取所有的文件。
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
     * 将一个文件读成按行存储的vector
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
     * 将一个文件读成按行存储的vector
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
     * 判断两个关系编号是否是同一关系。但是因为关系是一种层级结构，因此需要规定判断的精度大小
     * 两个关系编号只有同时达到规定的精度的层次上相同才算是同一种关系
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

    /**获取关系的编号，传入的是1-1时序在先关系,返回的是1-1**/
    public static String getRelNO(String relName)
    {
        int    index = 0;
        String relID = null;

        relName = relName.trim();
        index   = relName.lastIndexOf('-') + 1; //当为0承接时,正好为0

        relID   = relName.substring(0, index+1);

        return relID;
    }


    /***
     * 将旧关系编号转换为新关系编号
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
     * 对HashMap进行排序，按照value值进行排序
     * asc为true代表按照升序进行排列，asc为false代表按照降序排列
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
     * 对输入的Hashmap按照key进行排序
     * asc为true代表按照升序排列，asc为false代表按照降序排列
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
        //测试句子分割
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
     * 使用正则表达式判断一个并列连词是否出现在一个句子中
     * @param parallelWord: 使用;分割两个词
     * @param sentence：句子内容
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

            //如果已经超出了界限
            if( index > sentWords.length - 1 ) break;
        }

        //如果能够到达最后一个词，则为真
        if( end > words.length - 1 ) result = true;

        return result;
    }

    /**去除字符串中所包含的空格（包括:空格(全角，半角)、制表符、换页符等）*/
    public static String removeAllBlank(String s)
    {
        String result = "";
        if(null!=s && !"".equals(s))
        {
            result = s.replaceAll("[　*| *|&nbsp;*|//s*]*", "");
        }
        return result;
    }

    /*** 去除字符串中头部和尾部所包含的空格（包括:空格(全角，半角)、制表符、换页符等）*/
    public static String trim(String s)
    {
        String result = "";
        if(null!=s && !"".equals(s))
        {
            result = s.replaceAll("^[　*| *|&nbsp;*|//s*]*", "").replaceAll("[　*| *|&nbsp;*|//s*]*$", "");
        }
        return result;
    }


    /**
     * 将一个范围内的数字缩放到另外一个范围内
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
     * 获取一行标注文本的开始位置
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

    /**获取新的关系编号下，该关系的索引位置**/
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
     * 获取一个词在同义词词林中的标签。Aa01A03= 人手 人员 人口 人丁 口 食指--我们获取的是前四列
     * @param word：一个词
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

    /**在max范围内获取length个不重复的随机数。返回的是一个Boolean数组，如果下表index对应的值为true，就代表产生了他**/
    public static boolean[] getRandomArrays(int max, int length)
    {
        boolean[] exist = new boolean[max]; //判断是否出现过
        Random r = new Random(max);
        int tempValue = 0;

        for(int i = 0; i < length; i++)
        {
            do{ tempValue = r.nextInt(max); }while( exist[tempValue] );

            exist[tempValue] = true;
        }

        return exist;
    }

    /**将一个集合随机分成训练集和测试集合。**/
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
