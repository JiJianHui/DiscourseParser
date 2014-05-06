
1: data/connDistributionInP2P3.txt
为了判断一个连词是句间连词还是句内连词，我们需要知道该连词在句间关系和句内关系中的分布。此文件就存储了连词在p2和p3中出现的次数。
存储格式为：【word】 【\t】 【numInP2】 【\t】 【numInP3】
与此文件有关的另一个文件为data\connArgArg文件


2: data/connArgArg.txt
每个连词连接两个EDU都有不同的类型：arg-conn-arg 和 conn-arg-arg 两种类型。这个文件保存了一个连词是每种类型的次数.
存储格式为: 【word】 【\t】 【arg-conn-arg Num】 【\t】 【conn-arg-arg Num】