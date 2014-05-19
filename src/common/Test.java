package common;

import entity.train.DSAWordDictItem;
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
        //Test.testAnsjSegmentWord();
        //Test.testHashSetPlusHashSet();
        //Test.testRemoveAllBlankAndPron();
        //Test.testSameWordsNum();
        //Test.testTreeEyee();
        //Test.testJavaFormatOut();
        //Test.checkChar();
        Test.checkFile();
    }

    public static void checkChar()
    {
        String str = "����Automan Dytang ,����������";

        for(int i = 0 ;i<str.length();i++)
        {
            char ch = str.charAt(i);
            if( (ch >= '\u4e00' && ch <= '\u9fa5') ||(ch >= '\uf900' && ch <='\ufa2d') )
            {
                System.out.println(ch + "������");
            }
            else
            {
                System.out.println(ch + "��������");
            }
        }
    }

    public static void testJavaFormatOut()
    {
        double accuray = 15.8125364;
        System.out.format("accury: %.2f", accuray);
    }

    public static void testTreeEyee()
    {
        Integer one = null;
        Integer two = 2;
        Integer three = one==null?0:one;
        Integer four  = two==null?0:two;

        System.out.println(three);
        System.out.println(four);
    }
    public static void testSameWordsNum()
    {
        String sour = "��Ϊ ��ҵ �� ƾ�� dfdf �� �� �� ��ͳ �� �ʲ� �� ���� ʹ�� ��ҵ �� �ż� ������ ��";
        String dest = "�� ��Ϊ ��ҵ �� ƾ�� �� �� �� ��ͳ �� �ʲ� �� ���� �� ʹ�� ��ҵ �� �ż� ������ ��   Ȼ�� �� ֵ�� ע�� �� �� �� ��ҵ �ɹ� �� �Ѷ� ȴ ������ �� ��";
        int num = util.countSameCharatersNum(util.removeAllBlankAndProun(sour),util.removeAllBlankAndProun(dest));
        System.out.println("same char num: " + num);
    }

    public static void testRemoveAllBlankAndPron()
    {
        String source = "�� ��Ϊ ��ҵ �� ƾ�� �� �� �� ��ͳ �� �ʲ� �� ���� �� ʹ�� ��ҵ �� �ż� ������ �͡�   Ȼ�� �� ֵ��. ? �� ע�� �� �� ��";
        String result = util.removeAllBlankAndProun(source);
        System.out.println(source);
        System.out.println(result);
    }

    /**��������HashSet��ӵõ������HashSet**/
    public static void testHashSetPlusHashSet()
    {
        LinkedHashSet<String> a = new LinkedHashSet<String>();
        LinkedHashSet<String> b = new LinkedHashSet<String>();

        a.add("a0");a.add("a1");a.add("a2");
        b.add("b0");b.add("b1");b.add("b2");

        LinkedHashSet<String> c = new LinkedHashSet<String>();
        c.addAll(a);c.addAll(b);

        for(String cur:c) System.out.println(cur);
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
        for(Map.Entry<String, DSAWordDictItem> entry:Resource.allWordsDict.entrySet())
        {
            String curConn = entry.getKey();

            int connNum    = entry.getValue().getExpNum();
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

    public static void checkFile() throws IOException {
        String onlyWordPath = "resource/onlyWord.txt";
        String p3WordPath = "resource/p3Word(Filtered).txt";

        ArrayList<String> p3Lines   = new ArrayList<String>();
        ArrayList<String> onlyLines = new ArrayList<String>();

        util.readFileToLines(p3WordPath,p3Lines);
        util.readFileToLines(onlyWordPath, onlyLines);

        HashSet<String> onlyWords = new HashSet<String>();
        for(String line:onlyLines)
        {
            String[] lists = line.split("\t");
            onlyWords.add(lists[0]);
        }

        ArrayList<String> filterLines = new ArrayList<String>();

        for(String line:p3Lines)
        {
            String[] lists = line.split("\t");
            String word = lists[0];

            if( onlyWords.contains(word) ) filterLines.add(line);
        }

        String fPath = "resource/allWordInP3.txt";

        util.writeLinesToFile(fPath, filterLines);
    }
}
