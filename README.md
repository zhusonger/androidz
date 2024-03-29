# AndroidZ
AndroidZ开源项目, 简化平时开发时的重复性工作

由于jcenter的停用, 迁移到jitpack

需要引入jitpack存储库

```
allprojects {
    repositories {
        // 其他
        ...
        maven { url "https://jitpack.io" }
    }
}
```

## Base

基础库

https://github.com/zhusonger/zbase

```
implementation 'com.github.zhusonger:zbase:master-SNAPSHOT'
```

## Widget

控件库

https://github.com/zhusonger/zwidget

```
implementation 'com.github.zhusonger:zwidget:master-SNAPSHOT'
```

## Media

媒体库

https://github.com/zhusonger/zmedia

```
implementation 'com.github.zhusonger:zmedia:master-SNAPSHOT'
```

## Plugin

插件库

https://github.com/zhusonger/androidz/tree/master/plugin

```
// 根目录build.gradle
buildscript {
    repositories {
        // 其他
        ...
        maven { url "https://jitpack.io" }
    }
    dependencies {
        // 1.添加classpath
        classpath 'com.github.zhusonger.androidz:plugin:master-SNAPSHOT'
    }
}

// module的build.gradle
apply plugin: 'cn.com.lasong.inject'
// 统计耗时
apply plugin: 'cn.com.lasong.time'
```