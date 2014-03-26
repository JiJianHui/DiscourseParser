resource 资源文件解释如下：

1：expConnectiveWordDict.txt

    包含了所有的显式联词列表(不包含并列联词),而后面的数字则表示了选择连词时候的阈值大小。
    比如：expConnectiveWordDict(2).txt：表示了出现次数>=2的连词组成的词典。

----------------------------------------------------------------------
2: src包说明：

    源文件主要分为七个包。其中
    (1):common:       主要存放基本常用的工具包util.java和常量集合Constants.java
    (2):dataAnalysis: 主要是用于原始数据分析
    (3):entity:       主要是篇章分析时所涉及到的封装好的实体类。

    (4):lab：         主要是用于做实验，比如识别连词。
    (5):ltp:          主要负责完成数据词法、语法以及短语结构分析
    (6):resource：    主要用于完成资源的加载
    (7):train：主要用于训练模型

DiscourseParser.java: 主程序文件
WebServer.java: 服务器文件


