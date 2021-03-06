package syntax;

import edu.stanford.nlp.ling.HasWord;
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
        /***factored包含词汇化信息，PCFG是更快更小的模板，
         * xinhua据说是根据大陆的《新华日报》训练的语料，chinese同时包含香港和台湾的语料，
         * xinhuaFactoredSegmenting.ser.gz可以对未分词的句子进行句法解析。
         ***/
        String[] options = { "-maxLength", "500","-MAX_ITEMS","600000"};
        String   grammar = "edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz";
        stanfordParser   = LexicalizedParser.loadModel(grammar, options);

        tlp = this.stanfordParser.getOp().langpack();
        gsf = tlp.grammaticalStructureFactory();

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

        return parse;

    }

    /**Stanford的依存分析特征抽取,最终返回的是该短语的依存特征*/
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
        String line = "他 出发 了 ， 他 打 我 。";

        PhraseParser stanfordParser = new PhraseParser();

        //Tree  result = stanfordParser.parseLine(line);
        //result.pennPrint();

        stanfordParser.parseLineDependthy(line);
    }
}
