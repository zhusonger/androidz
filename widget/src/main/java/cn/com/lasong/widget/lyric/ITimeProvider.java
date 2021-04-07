package cn.com.lasong.widget.lyric;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-05
 * Description: 歌词时间提供器
 */
public interface ITimeProvider {

    long getCurrentPosition();

    long getPollingInterval();
}
