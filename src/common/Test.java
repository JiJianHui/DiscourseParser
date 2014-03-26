package common;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import entity.DSAWordDictItem;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.dom4j.*;

import libsvm.*;



/**
 *������ʱ���Ժ���ʹ��
 * User: Ji JianHui
 * Time: 2014-02-19 15:28
 * Email: jhji@ir.hit.edu.cn
 */
public class Test
{
    public static void main(String[] args) throws Exception {
        /**java������ʽ�Ĳ���
        String candidate = "A Matcher. examines the results of!! applying? pattern.";
        String regex = "[#]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(candidate);
        String val = null;

        //���վ��ӽ������ָ����
        String[] substrs = p.split(candidate);

        //�����ӽ��������ӵ���Ӧ�ľ��Ӻ�
        if(substrs.length > 0)
        {
            int count = 0;
            while(count < substrs.length)
            {
                if(m.find())
                {
                    substrs[count] += m.group();
                }
                count++;
            }
        }

        for(int index = 0; index < substrs.length; index++ )
        {
            System.out.println(substrs[index]);
        }
        **/

        /**
         //��ⷢ��Element.getText() ����""��������null
        String str = "<?xml version=\"1.0\" encoding=\"gb2312\" ?><doc><Sense type=\"Implicit\"><Arg1></Arg1></Sense></doc>\n";
        Document document = DocumentHelper.parseText(str);

        Element rootNode = document.getRootElement();
        Element sentNode  = rootNode.element("Sense");
        Element argNode  = sentNode.element("Arg1");
        String text = argNode.getText();

        if(text==null) System.err.println("Error");
        else System.out.println(text.length());
         **/
//        LinkedHashMap<String, Integer> results = new LinkedHashMap<String, Integer>();
//        results.put("a", 1);
//        results.put("b", 2);
//
//        System.out.println( results.get("a").intValue() );
//
//        int num = results.get("a") + 2;
//        results.put("a", num);
//        System.out.println( results.get("a").intValue() );
//
//        HashMap<String, Integer> aMap = new HashMap<String, Integer>();
//        aMap.put("a", 1);

        //String[] trainArgs = {"UCI-breast-cancer-tra"};
        //String modelFile = svm_train.main(trainArgs);

        /**
        //^.*hello.*$
        String[] words = {"��Ȼ", "����"};
        String regularStr = "^.*";
        for(String word: words) regularStr += word + ".*";
        regularStr += "$";

        String candidate = "��Ȼû�гɹ��������ҵõ��˺ܶ�.";

        Pattern p = Pattern.compile(regularStr);
        Matcher m = p.matcher(candidate);

        if( m.find() ){
            System.out.println(m.group());
        }
         **/

        //���վ��ӷָ������ָ����
        /**
        String fileContent = "��Ȼ�����ﲻͬ�����Ż��£������Ǽ޸�������";
        Matcher  matcher   = Constants.Sentence_Element_Pattern.matcher(fileContent);
        String[] sentences = Constants.Sentence_Element_Pattern.split(fileContent);

        //�����ӽ��������ӵ���Ӧ�ľ��Ӻ����ɴֲڵķָ���
        if( sentences.length > 0 )
        {
            int index = 0;
            while(index < sentences.length)
            {
                if(matcher.find())
                {
                    sentences[index] += matcher.group();
                }
                index++;
            }
        }

        System.out.println(sentences[0]);
        **/


        Test test = new Test();

        test.testIO();
        System.out.println("Ending");


    }

    public void testIO() throws IOException
    {
        PrintStream out = System.out; //����ԭ�����

        //������������ض���
        File f = new File("out.txt");
        f.createNewFile();

        FileOutputStream fileOutputStream = new FileOutputStream(f);
        PrintStream printStream = new PrintStream(fileOutputStream);

        System.setOut(printStream);
        System.out.println("Ĭ�����������̨����һ�䣬��������ļ� out.txt");

        System.setOut(out);
    }

}
