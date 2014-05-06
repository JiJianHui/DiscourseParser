package common;

import org.ansj.dic.LearnTool;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import resource.Resource;

import java.io.IOException;
import java.util.*;

/**
 * ��Ҫ���ڱ�д��ʱ�����ļ�
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
        String test  = "Hi����𡾡�";
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

        //����ǰ
        System.out.println("Before Sorting\n");
        for (int i = 0; i < infoIds.size(); i++) {
            String id = infoIds.get(i).toString();
            System.out.println(id);
        }

        //����key����� false:�����ս�������
        System.out.println("Sorting By Key\n");
        infoIds = util.sortHashMapByKey(data,false);
        for (int i = 0; i < infoIds.size(); i++) {
            String id = infoIds.get(i).toString();
            System.out.println(id);
        }

        //����value�����
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
        String[] strs = {"��", "��", "ansj", "��", "�ִ�", "���", "����", "����", "��ע"} ;
        List<String> lists = Arrays.asList(strs) ;
        List<Term> recognition = NatureRecognition.recognition(lists, 0) ;

        System.out.println(recognition);
    }

    public static void testAnsjSegmentWord()
    {
        String content   = "�ֶ�����������һ�������Ϻ��������ִ������á�ó�ס��������ĵĿ����͹��̣���˴������ֵ�����ǰ������������������������⡣";
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
        String parallWord = "��Ȼ;����";
        String sentence   = "��Ȼ Ӱ�� �� ��ͨ ���� ���� �� Ⱥ �ɰ� �� ���� ���� ���� �� �� �� Ҳ �� �൱ �� ���� ��";
        boolean result    = util.isParallelWordInSentence(parallWord, sentence);

        System.out.println( result );
    }

    public static void mergeFile() throws IOException
    {
        String fPathA = "data/temp/wordAndNotWord.txt";
        String fPathB = "data/temp/word.txt";
        //a:��Ϊ���ʺͲ���Ϊ���ʳ��ֵĴ�����b��Ϊ���ʳ��ָ���
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

    /***��ȡ����������ͬ��ʴ����еı�ǩ��***/
    public static void getConnCagInSymCiLin() throws IOException
    {
        Resource.LoadExpConnectivesDict();
        Resource.LoadSymWordDict();

        HashMap<String, Integer> connCagInCiLin = new HashMap<>();

        //�ж�ÿ�����ʵı�ǩ������
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

        //��������浽�ļ��У���ǩ \t ���ִ���
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
