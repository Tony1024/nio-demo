# nio-demo

```bash
# epoll实现
-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
# poll实现
-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider
# 查看系统调用
strace -ff -o [文件前缀] [java执行命令]
```