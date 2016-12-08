# StormTopologyMinitor
Storm Topology monitor and Alert


监控Storm Topology 任务 及时报警 重启任务 
方案 一： 代码中查看源码中的Toplogy 相关类的属性 和demo 取到Storm 每个Topology _Metric 中 Topology Stat 信息       
        连接到 Storm Thrift server 进行代码操作        
        Topology Stat 分别包括 10minutes 3 hour 1 day 中的 emit transfered ack  相关信息监控变化量 和最终的0 停留点 就行 报警 重启功能
方案二 : 利用Storm 提供的一下Rest API 进行 操作  
         具体操作 API 查看文档 该代码只是进行初步的功能实现 和 实际运行的任务不一样
功能都已实现 细节部分需要根据场景细节进行修改
