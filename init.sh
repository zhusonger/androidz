#!/bin/bash
echo "开始git clone任务"

echo "开始Base项目"
rm -rf base
mkdir base
ret=`git clone git@github.com:zhusonger/zbase.git base`
cd base
cp -f build.gradle.bak build.gradle
cd ..
echo "完成Base项目"

echo "开始Widget项目"
rm -rf widget
mkdir widget
ret=`git clone git@github.com:zhusonger/zwidget.git widget`
cd widget
cp -f build.gradle.bak build.gradle
cd ..
echo "完成Widget项目"

echo "开始Media项目"
rm -rf media
mkdir media
ret=`git clone git@github.com:zhusonger/zmedia.git media`
cd media
cp -f build.gradle.bak build.gradle
cd ..
echo "完成Media项目"

echo "完成git clone任务"
echo "添加完成"
