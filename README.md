# FTP客户端和服务端

1. 内部采用socket通讯实现
2. 实现了上传和下载(get,put)
3. 实现了list指令
4. 实现了监控下载上传流量功能
5. 实现了黑白名单功能
6. 支持修改端口


## 服务器客户端应答:

1. OK	执行成功
2. ERROR	执行失败
3. INVALID	无效的指令



## 客户端命令

1. PUT file	上传文件
2. GET file	下载文件
3. LIST	列出服务器文件

## 服务端命令:
1. BAN ip	把对应IP加入黑名单
2. WHITE ip	把对应IP加入白名单
3. OPEN BAN	开启黑名单,关闭白名单
4. OPEN WHITE	开启白名单,关闭黑名单
5. CLOSE BAN	关闭黑名单
6. CLOSE WHITE	关闭白名单


**PS:给大学党:拿去交实验报告可以,不过记得给个star,谢了**