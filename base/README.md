# Base
基础工具类库

# 引入

```
implementation 'com.github.zhusonger.androidz:base:1.0.0'
```

## 1.0.0
* 添加日志ILog/弹窗TN便捷工具类
* 解压Zip文件工具类 **ZLibUtils**
* 设备分辨率转换类 **DeviceUtils**
* Activity&Application基类
* 应用管理类 **记录** Activity生命周期
* 添加文件Uri解析绝对路径工具
* BaseActivity添加权限申请方法
* 添加Buffer缓冲任务, 针对连续事件只处理最后的事件
* 使用androidx.activity实现权限申请
* 调整AppManager
    * 修复LifecycleObserver需求Android7.0的问题
    * 添加PERCaller接口, 用来在泛型工具类中统一处理权限请求
    * 修复key相同导致权限请求回调失败的情况
    * 更新androidx 的 fragment & activity版本