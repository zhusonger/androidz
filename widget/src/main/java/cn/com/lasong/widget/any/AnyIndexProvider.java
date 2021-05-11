package cn.com.lasong.widget.any;

import android.view.LayoutInflater;
import android.view.View;

import cn.com.lasong.widget.AnyIndexView;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/5/11
 * Description: 任意的索引的数据提供
 */
public interface AnyIndexProvider {

    int indexCount();

    View inflateIndex(LayoutInflater inflater, AnyIndexView parent, int position);
}
