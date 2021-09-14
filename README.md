# 九浅一深NIO

## 关于程序
https://github.com/Tony1024/nio-demo
spi选择不同的selector实现[EpollMultiplexingSingleThread.java]
```bash
# epoll实现
-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
# poll实现
-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider
# 查看系统调用
strace -ff -o [文件前缀] [java执行命令]
```

## 目标

1.带大家理解网络IO、NIO、多路复用器

2.分享一些实战技巧


## 大纲

### IO基础

#### 1.文件

**Linux下，一切皆文件！**

使用 `ll`查看目录下文件

查看第一列显示内容，首个字符代表的意义：**文件类型**

```
- , 常规文件(用lsof查看，可以看到TYPE=REG)
d , 目录文件
b , 块设备文件 如硬盘；支持以block为单位进行随机访问
c , 字符设备文件，如键盘，支持以character为单位进行线性访问
l , symbolic link 符号链接文件，又称软链接文件
p , pipe 管道文件
s , socket
```

#### 2.文件描述符

定义

> 百度百科：文件描述符在形式上是一个非负整数，实际上是一个索引值，指向内核为每一个进程所维护的该进程打开文件的记录表。当程序打开一个现有文件或者创建一个新文件时，内核向进程返回一个文件描述符
>
> 通俗一点来说就是：描述了打开这个文件的具体信息和指针、偏移量(seek)

任何程序都有0 ,1,2三个文件描述符 : [0-标准输入, 1-标准输出, 2-报错输出]

通过以下实验, 便于理解文件描述符

```bash
# 令文件描述符8读取xxx.txt这个文件，注意观察/proc/$$/fd下查看该进程的文件描述符情况
exec 8< xxx.txt
# 让x标准输入来自文件描述符8，read程序有一个特点就是遇到换行符就不读了，再使用lsof -op $$查看
read x 0<& 8
# 理解文件描述符是进程空间私有的
# 新开一个bash，对同一个文件进行读取，进行上述的操作，再次观察文件描述符的偏移量
```

#### 3.PageCache

内核页缓存，也叫内核缓冲区

- 可以没有页缓存，但这样的话，操作系统当中的进程如果想读取数据的时候，只是调用内核的方法，内核调用驱动，驱动直接找硬件，把数据返回到应用程序，这样的话io延迟很大，所以就加了个缓存 pagecache

int 0x80系统调用，中断，是cpu的指令 ，它的值是128: 1000 0000，是放在cpu的寄存器里的，与中断向量表对应，其实128这个值可以找到代表它的一个回调函数，从而发起调用

#### 4.Linux系统小知识

##### 重定向的理解

```bash
# 展示了一堆文件，其实是表示这个ls程序的标准输出是指向了屏幕
ls ./
# 将它的标准输出改一下
ls ./ 1> ~/xxx.txt
# 改变标准输出和标准报错输出的行为
ls ./ /notexist  1> out.txt 2> error.txt
# 可以将cat程序的标准输入来自一个文件，标准输出到一个文件，通过干预他的输入输出，改变了它硬编码的行为
cat 0< xxx.txt 1> cat.out 
```

##### 管道

```bash
#head tail命令的实践
head -1 xxx.txt
tail -1 xxx.txt
# 第n行 
head -n xxx.txt | tail -1
```

从管道引出父子进程的概念

##### 父子进程

```bash
# 查看进程树
pstree [pid] 
```

父子进程实践：

```bash
a=1
echo $a;
{ a=9; echo "1111"; } | cat
```

请问a=?

> 在父进程定义的一个变量，在子进程是无法读取的，但有什么办法可以读取呢？
>
> 用export，这样无论什么进程都可以读到这个变量

我们可以看到有管道，管道会在左边启动一个子进程，右边也启动一个子进程，并且让左边的执行结果作为右边的标准输出

```bash
## 这里会输出的是当前bash的pid ($$优先级比较高)
echo $$ | cat  
## 这里输出的是子进程的pid
echo $BASHPID | cat

{ echo $BASHPID; read x; } | { cat ; echo $BASHPID ; read y; }
```

通过上面的这个指令，学会去观察他们的fd情况，以及父子进程情况，还有管道左边和管道右边的的输入输出关系

#### 5.实用命令介绍

##### 1.lsof

> lsof -p [pid] 可以显示该进程打开了哪些文件
>
> lsof -op [pid] 这个命令可以查看该进程下文件描述符的情况以及偏移量（OFFSET）

##### 2.stat

> stat [文件名]  可以查看该文件的Inode号

##### 3./proc

> 在/proc下可以查看linux内核的变量
>
> 在/proc/{pid}/fd目录可以查看到某个进程的文件描述符情况

##### 4.pcstat

> pcstat [文件名] 可以查看某个文件在PageCache的使用情况

##### 5.nc

> nc [ip] [port] 可以连接某个服务端
> nc -l [ip] [port]

##### 6.exec

> exec [fd文件描述符]< [文件名] 令某个文件描述符读取某个文件

##### 7.strace

用该命令可以查看一个java程序对系统调用的详细信息

> 譬如 strace -ff -o out java Demo
>
> -ff 代表抓取所有线程
>
> -o 追踪每个线程对内核的系统调用
>
> out 表示输出文件以out前缀开头，可以是其他的前缀，随便你怎么定义
>
> java Demo 代表命令，可以是任何执行程序的命令
>
> 用上面的命令可以查看一个java程序对系统调用的详细信息

##### 8.man

可以用man这个指令查看很多linux里的函数以及其详细信息

> 譬如：
>
> man tcp; man 7 ip; man bash; man man; man 2 socket; man 2 bind; man 2 listen; man 2 accept 等等

##### 9.route

> route -n 查看路由表
>
> route add -host 192.168.110.100 gw 192.168.150.1 添加一个路由条目

##### 10.netstat

> netstat -natp 查看网络状态

##### 11.tcpdump

> tcpdump -nn -i eth0 port 9090 查看eth0这个网卡接口下9090端口的抓包情况

##### 12.ulimit

> 查看fd大小: ulimit -n
>
> 更详细的信息: ulimit -a
>
> 显示结果中有一行open files 1024 ，代表什么意思？答：是一个进程可以打开多少个文件描述符（跟线程没关系）
>
> 重新设置进程fd大小为50万：ulimit -SHn 500000
>
> cat /proc/sys/fs/file-max 查看内核级的可以开辟文件描述符的数量
>
> ulimit是用户级别的，不是系统级别的



### 网络IO

#### 1.Socket

IP:PORT

客户端连接服务端，socket四元组  (client_ip, client_port, server_ip, server_port) 只要保证唯一就行

一般情况下，client_port会随机分配，这样就能保证四元组的唯一，所以服务端无需再为客户端再开多一个随机端口号

当一个客户端请求连接服务端9090端口，并且已经达到65535个连接，还能对其他端口号发起连接吗？

可以，只要保证四元组的唯一就可以连接上。那有上限么？只要内存足够大，百万连接都不是问题！

#### 2.TCP连接

> 演示: Server.java
>
> 客户端请求服务端时服务端不accept的抓包情况，在此前提下，并且让客户端向服务端发送数据】

- 当一个客户端向服务端发起连接的时候，调用connect方法

- 服务端接收到客户端发来的请求，在TCP层面，会完成3次握手，在服务端没有调用accpect方法之前，其实连接已经建立了，在内核态已经建立起来

- 此时，使用lsof -p 查看服务端程序的文件描述符情况，没有发现有新的文件描述符

- 利用netstat -natp查看网络状态可以看到对应的连接已经建立，但是发现没有分配pid，并且可以看到recv_queue已经堆积了数据，是客户端之前发过来的

TCP是面向连接的，可靠的传输协议

其实在三次握手完成之后，客户端和服务端已经为这个连接开辟了资源（在内核角度，已经建立起连接）

##### TCP/IP内核数据遗失

> 演示: Server.java
>
> 服务端启动起来，不accept
>
> 客户端connect，然后不断得向服务端发送数据，这时候用netstat -natp发现 Recv-Q 有堆积，而且打到一个值之后就不会变了
>
> 然后放开服务端，开始接受连接和数据，会发现客户端发送的数据存在丢失情况

##### MTU

最大传输单元，单位:字节

ifconfig 可以查看网卡接口的MTU大小

```bash
22:51:51.991290 IP 127.0.0.1.54183 > 127.0.0.1.9090: Flags [S], seq 3223878680, win 65535, options [mss 16344,nop,wscale 6,nop,nop,TS val 1490784895 ecr 0,sackOK,eol], length 0
22:51:51.991855 IP 127.0.0.1.9090 > 127.0.0.1.54183: Flags [S.], seq 1691736985, ack 3223878681, win 65535, options [mss 16344,nop,wscale 6,nop,nop,TS val 1490784895 ecr 1490784895,sackOK,eol], length 0
22:51:51.991874 IP 127.0.0.1.54183 > 127.0.0.1.9090: Flags [.], ack 1, win 6379, options [nop,nop,TS val 1490784896 ecr 1490784895], length 0
22:51:51.991888 IP 127.0.0.1.9090 > 127.0.0.1.54183: Flags [.], ack 1, win 6379, options [nop,nop,TS val 1490784896 ecr 1490784896], length 0
```

在tcpdump抓包时，可以看到mss，mss的意思是数据包除去包头等信息的大小，

另外还有发现一个win（TCP窗口），客户端服务端的win都不一样，他们每次数据包交互的时候这个都会变，会根据各自的情况计算出各自的窗口大小并告诉对方，比方说某一个时刻server端win大小是100，那么client端收到之后就能知道server能接收100个数据包，这样客户端就可以发送100过去，解决了拥塞的情况，假如服务端win大小0，这时候client端就不会发送，客户端就阻塞在那里，避免了服务器拥塞的情况

##### DMA的理解

https://cloud.tencent.com/developer/article/1628161?from=14588



### 3.网络IO演变过程



### 4.多路复用器



## 总结

