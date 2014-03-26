entity包：

主要用来在Discourse Parser的过程中，封装数据使用。

SenseRecord：原始标注语料的封装，每个标注行都会被封装为一个senseRcord

DSASentence：篇章语义关系分析中的句子，每个句子都会被封装为一个。
DSAConnective：整个篇章关系分析中的连词封装，每个句子中的连词都会被封装。
DSARelation：整个篇章语义关系中的语义关系封装，每个标注的关系都会被封装成一个。

DSAWordDictItem：这个一个连词的总的信息封装，里面封装了一个连词的所有信息。是资源文件。

Recognize：主要是为了方便识别的时候的PRF值的计算。
