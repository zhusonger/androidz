package cn.com.lasong.widget.adapterview.itemdecoration;

import android.view.LayoutInflater;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/5/10
 * Description:
 */
public interface StickHeaderProvider {
    /**
     * 创建/更新 当前位置对应的头部
     * @param cache     缓存的对应分组的吸顶头部
     * @param inflater  解析布局
     * @param parent    RecyclerView
     * @param position  当前显示的第一个布局之前的每个布局索引位置
     * @return  返回更新完后的吸顶头部, 空就表示该position不是吸顶头部
     */
    RecyclerView.ViewHolder createOrUpdateHeader(RecyclerView.ViewHolder cache, LayoutInflater inflater, RecyclerView parent, int position);

    /**
     * 判断是否是吸顶头部
     * @param position
     * @return
     */
    boolean isStickHeader(int position);
}
