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
 * Berkeley Parser使用.
 * User: Ji JianHui
 * Time: 2014-04-01 16:21
 * Email: jhji@ir.hit.edu.cn
 */
public class PhraseParser
{
    private Tree phraseTree;    //句法分析结果树

    private LexicalizedParser stanfordParser;   //句法分析模型
    private TreebankLanguagePack tlp;
    private GrammaticalStructureFactory gsf;

    private String rawLine; //待分析的语料

    public PhraseParser()
    {
        String[] options = { "-maxLength", "500"};
        String   grammar = "edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz";
        stanfordParser   = LexicalizedParser.loadModel(grammar, options);

        tlp = this.stanfordParser.getOp().langpack();
        gsf = tlp.grammaticalStructureFactory();

        System.out.println("[--Info--] Init Phrase Parser from chineseFactored.ser.gz");
    }

    /***
     * 传入的参数是一行分词好的句子。返回的是短语结构分析结果。返回的是一个树形类Tree。
     * @return
     */
    public Tree parseLine(String line)
    {
        //String line = "虽然 影响 了 交通  不过 看到 这 群 可爱 的 游行 队伍 来往 的 人 车 也 都 相当 的 体谅 。";
        //String[] sent = { "这个", "花瓶", "真", "漂亮", "。" };
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

        //GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        //List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
        //System.out.println(tdl);
        //System.out.println();

        return parse;

    }

    public static void main(String[] args)
    {
        String line = "他 打 我 。";

        PhraseParser stanfordParser = new PhraseParser();
        Tree  result = stanfordParser.parseLine(line);

        result.pennPrint();
    }
}
