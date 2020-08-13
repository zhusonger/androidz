#!/bin/bash
echo "开始git clone任务"
echo "开始Base项目"
rm -rf base
mkdir base
git clone git@github.com:zhusonger/androidz_base.git base
echo "完成Base项目"
echo "开始Widget项目"
rm -rf widget
mkdir widget
git clone git@github.com:zhusonger/androidz_widget.git widget
echo "完成Widget项目"
echo "开始Media项目"
rm -rf media
mkdir media
git clone git@github.com:zhusonger/androidz_media.git media
echo "完成Media项目"
rm -rf plugin
mkdir plugin
git clone git@github.com:zhusonger/androidz_plugin.git plugin
echo "完成Plugin项目"
echo "完成git clone任务"
echo "添加jecenter参数"
echo bintrayUser="zhusong" >> local.properties
echo groupId="cn.com.lasong" >> local.properties
echo bintrayKey="XXXFromApi" >> local.properties
echo "添加完成"
