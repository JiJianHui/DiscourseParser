package ltp;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Berkeley Parserʹ��.
 * User: Ji JianHui
 * Time: 2014-04-01 16:21
 * Email: jhji@ir.hit.edu.cn
 */
public class PhraseParser
{
    private Tree phraseTree;    //�䷨���������

    private LexicalizedParser stanfordParser;   //�䷨����ģ��
    private TreebankLanguagePack tlp;
    private GrammaticalStructureFactory gsf;

    private String rawLine; //������������

    public PhraseParser()
    {
        String[] options = { "-maxLength", "500","-MAX_ITEMS","500000"};
        String   grammar = "edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz";
        stanfordParser   = LexicalizedParser.loadModel(grammar, options);

        tlp = this.stanfordParser.getOp().langpack();
        gsf = tlp.grammaticalStructureFactory();

    }

    /***
     * ����Ĳ�����һ�зִʺõľ��ӡ����ص��Ƕ���ṹ������������ص���һ��������Tree��
     * @return
     */
    public Tree parseLine(String line)
    {
        //String line = "��Ȼ Ӱ�� �� ��ͨ  ���� ���� �� Ⱥ �ɰ� �� ���� ���� ���� �� �� �� Ҳ �� �൱ �� ���� ��";
        //String[] sent = { "���", "��ƿ", "��", "Ư��", "��" };
        String[] sent = line.split(" ");

        List<HasWord> sentence = new ArrayList<HasWord>();

        for (String word : sent)
        {
            sentence.add(new Word(word));
        }

        Tree parse = this.stanfordParser.parse(sentence);
        //parse.pennPrint();
        //System.out.println();

       // System.out.println( parse.taggedYield() );

        return parse;

    }

    /**Stanford���������������ȡ,���շ��ص��Ǹö������������*/
    public List<TypedDependency> parseLineDependthy(String line)
    {
        Tree parse = this.parseLine(line);

        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

        System.out.println(tdl);
        System.out.println();

        return tdl;
    }

    public List<TypedDependency> parseDependthyUseTree(Tree stanfordTree)
    {
        GrammaticalStructure gs = gsf.newGrammaticalStructure(stanfordTree);
        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

        return tdl;
    }

    public static void main(String[] args)
    {
        String line = "�� ���� �� �� �� �� �� ��";

        PhraseParser stanfordParser = new PhraseParser();

        //Tree  result = stanfordParser.parseLine(line);
        //result.pennPrint();

        stanfordParser.parseLineDependthy(line);
    }
}
