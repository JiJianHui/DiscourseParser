
1: data/connDistributionInP2P3.txt
为了判断一个连词是句间连词还是句内连词，我们需要知道该连词在句间关系和句内关系中的分布。此文件就存储了连词在p2和p3中出现的次数。
存储格式为：【word】 【\t】 【numInP2】 【\t】 【numInP3】
与此文件有关的另一个文件为data\connArgArg文件


2: data/connArgArg.txt
每个连词连接两个EDU都有不同的类型：arg-conn-arg 和 conn-arg-arg 两种类型。这个文件保存了一个连词是每种类型的次数.
存储格式为: 【word】 【\t】 【arg-conn-arg Num】 【\t】 【conn-arg-arg Num】

3:data/wordAndNotWord.txt
在抽取连词特征来训练连词识别模型时候，我们针对标注的连词进行了抽取，并得到了中间文件，一个连词作为连词的数目和不作为连词的数目
存储格式为： 【word】 【\t】 【as conn Num】 【\t】 【not as conn Num】

4：data/relation/*.*
这个文件夹是在训练隐式关系识别模型的时候抽取到的特征数据：包括了训练和测试数据以及结果文件

5:data/word/*.*
这个文件夹是在训练连词识别模型的时候抽取到的连词特征数据。包括了训练和测试以及结果文件。