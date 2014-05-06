package common;

import org.ansj.dic.LearnTool;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import resource.Resource;

import java.io.IOException;
import java.util.*;

/**
 * 主要用于编写临时测试文件
 * User: Ji JianHui
 * Time: 2014-03-28 15:36
 * Email: jhji@ir.hit.edu.cn
 */
public class Test
{

    public static void main(String[] args) throws IOException
    {
        //Test.mergeFile();
        //Test.calendarAndDate();
        //Test.testParallelWordInLine();
        //Test.testAnsjPosTag();
        //Test.testCalendar();
        //Test.getConnCagInSymCiLin();

        //Test.testHashMapSort();
        //Test.testChineseWord();
        Test.testAnsjSegmentWord();

    }

    public static void testChineseWord()
    {
        String test  = "Hi你好吗【】";
        char[] chars = test.toCharArray();
        byte[] bytes = test.getBytes();

        System.out.println(test);

        for(char ch:chars)
        {
            System.out.println(ch);
        }
    }

    public static void testHashMapSort()
    {
        HashMap<String, Integer> data = new HashMap<String, Integer>();

        data.put("a", 4); data.put("b", 3);data.put("c", 1); data.put("d", 2);

        List<Map.Entry<String, Integer>> infoIds = new ArrayList<Map.Entry<String, Integer>>(data.entrySet());

        //排序前
        System.out.println("Before Sorting\n");
        for (int i = 0; i < infoIds.size(); i++) {
            String id = infoIds.get(i).toString();
            System.out.println(id);
        }

        //按照key排序后 false:代表按照降序排列
        System.out.println("Sorting By Key\n");
        infoIds = util.sortHashMapByKey(data,false);
        for (int i = 0; i < infoIds.size(); i++) {
            String id = infoIds.get(i).toString();
            System.out.println(id);
        }

        //按照value排序后
        System.out.println("Sorting By Value\n");
        infoIds = util.sortHashMap(data,false);
        for (int i = 0; i < infoIds.size(); i++) {
            String id = infoIds.get(i).toString();
            System.out.println(id);
        }
    }

    public static void testCalendar()
    {
        Calendar now = Calendar.getInstance();

        System.out.println("Month: " + (now.get(Calendar.MONTH) + 1));
        System.out.println("Day: " + (now.get(Calendar.DATE)));
        System.out.println("Hour: " + now.get(Calendar.HOUR_OF_DAY) );
        System.out.println("Minute: " + now.get(Calendar.MINUTE));

    }

    public static void testAnsjPosTag()
    {
        String[] strs = {"对", "非", "ansj", "的", "分词", "结果", "进行", "词性", "标注"} ;
        List<String> lists = Arrays.asList(strs) ;
        List<Term> recognition = NatureRecognition.recognition(lists, 0) ;

        System.out.println(recognition);
    }

    public static void testAnsjSegmentWord()
    {
        String content   = "浦东开发开放是一项振兴上海，建设现代化经济、贸易、金融中心的跨世纪工程，因此大量出现的是以前不曾遇到过的新情况、新问题。";
        LearnTool learn  = new LearnTool();
        List<Term> parse = NlpAnalysis.parse(content, learn);
        System.out.println(parse);
    }

    public static void calendarAndDate()
    {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -15);

        System.out.println(now);
    }

    public static void testParallelWordInLine()
    {
        String parallWord = "虽然;不过";
        String sentence   = "虽然 影响 了 交通 不过 看到 这 群 可爱 的 游行 队伍 来往 的 人 车 也 都 相当 的 体谅 。";
        boolean result    = util.isParallelWordInSentence(parallWord, sentence);

        System.out.println( result );
    }

    public static void mergeFile() throws IOException
    {
        String fPathA = "data/temp/wordAndNotWord.txt";
        String fPathB = "data/temp/word.txt";
        //a:作为连词和不作为连词出现的次数，b作为连词出现个数
        ArrayList<String> aLines = new ArrayList<String>();
        ArrayList<String> bLines = new ArrayList<String>();

        util.readFileToLines(fPathA,aLines);
        util.readFileToLines(fPathB,bLines);

        HashMap<String, Integer> aWords = new HashMap<String, Integer>();
        HashMap<String, Integer> bWords = new HashMap<String, Integer>();

        HashMap<String, Integer[]> wordDicts = new HashMap<String, Integer[]>();

        for(String line:bLines)
        {
            String[] lists = line.split("\t");
            bWords.put(lists[0], Integer.valueOf(lists[1]));
        }

        for(String line:aLines)
        {
            String[] lists = line.split("\t");

            int aConnNum = Integer.valueOf(lists[1]);
            String word = lists[0];

            if( bWords.containsKey(word) )
            {
                if(bWords.get(word) < aConnNum) bWords.put(word, aConnNum);
            }

            aWords.put(lists[0], Integer.valueOf( lists[2] ) );
        }


        for(Map.Entry<String, Integer> entry:bWords.entrySet())
        {
            int connNum = entry.getValue();
            String word = entry.getKey();
            int notConnNum = 0;
            if( aWords.containsKey(word) ) notConnNum = aWords.get(word);

            wordDicts.put(word, new Integer[]{connNum,notConnNum});
        }

        ArrayList<String> lines = new ArrayList<String>();

        for(Map.Entry<String, Integer[]> entry:wordDicts.entrySet())
        {
            String line = entry.getKey() + "\t" + entry.getValue()[0] + "\t" + entry.getValue()[1];
            lines.add(line);
        }

        util.writeLinesToFile("data/temp/singWord.txt", lines);

    }

    /***获取常见连词在同义词词林中的标签。***/
    public static void getConnCagInSymCiLin() throws IOException
    {
        Resource.LoadExpConnectivesDict();
        Resource.LoadSymWordDict();

        HashMap<String, Integer> connCagInCiLin = new HashMap<>();

        //判断每个连词的标签并报错
        for(Map.Entry<String, Integer> entry:Resource.ExpConnWordDict.entrySet())
        {
            String curConn = entry.getKey();

            int connNum    = entry.getValue();
            if(connNum < 3) continue;

            String curConnCag = getWordTagInSym(curConn);

            if( curConnCag == null ) continue;

            int num = 0;
            if( connCagInCiLin.containsKey(curConnCag) ) num = connCagInCiLin.get(curConnCag);

            connCagInCiLin.put(curConnCag, num+connNum);
        }

        //将结果保存到文件中：标签 \t 出现次数
        String fPath = "resource/dictionary/ConnWordCagInSymCiLin.txt";
        ArrayList<String> lines = new ArrayList<String>();

        for(Map.Entry<String, Integer> entry:connCagInCiLin.entrySet())
        {
            String line = entry.getKey() + "\t" + entry.getValue();
            lines.add(line);
        }
        util.writeLinesToFile(fPath, lines);
    }

    public static String getWordTagInSym(String word)
    {
        String result = null;

        for(String line:Resource.SymWordDict)
        {
            boolean  find  = false;
            String[] lists = line.split(" ");

            for(String curWord:lists)
            {
                if(curWord.equalsIgnoreCase(word))
                {
                    find   = true;
                    result = line.substring(0,4);
                    break;
                }
            }

            if( find == true ) break;
        }

        return result;
    }
}
