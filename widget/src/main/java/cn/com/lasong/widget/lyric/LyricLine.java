package cn.com.lasong.widget.lyric;

import java.util.List;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-04
 * Description: 歌词行对象
 */
public class LyricLine {
    /*
     * [262,1650]<0,300,0>齐<300,350,0>豫 <650,250,0>- <900,401,0>欢<1301,349,0>颜
     */
    public long start;//行歌词时间偏移
    public long duration;//行歌词总时长, 最后一行可能是整首歌的时长, 在解析时调整为时长
    public String content;//行文本内容
    public List<LyricWord> words;

    @Override
    public String toString() {
        return "LyricLine{" +
                "start=" + start +
                ", duration=" + duration +
                ", content='" + content+"'}";
    }
}
