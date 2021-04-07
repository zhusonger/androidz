package cn.com.lasong.widget.lyric;

import java.util.List;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-04
 * Description: 歌词对象
*/
public class Lyric {
    /*
     * [id:$00000000]
     * [ar:齐豫]  //演唱者
     * [ti:欢颜]  //标题
     * [by:]  //作者
     * [hash:1560dd9bf686a9782213367c5f9e2a57]
     * [al:]    //???
     * [sign:]  //签名
     * [total:226650]   //总时长(ms)
     * [offset:0]       //歌词的整体初始偏移时间
     * [262,1650]<0,300,0>齐<300,350,0>豫 <650,250,0>- <900,401,0>欢<1301,349,0>颜
     */

    public String id;
    public String ar;
    public String ti;
    public String by;
    public String hash;
    public String al;
    public String sign;
    public long total;
    public long offset;//歌词总的偏移值：当为正值的时候，整体向前偏移；当为负值的时候，整体向后偏移。

    public List<LyricLine> lines;//存储各行歌词

    @Override
    public String toString() {
        return "Lyric{" +
                "id='" + id + '\'' +
                ", ar='" + ar + '\'' +
                ", ti='" + ti + '\'' +
                ", by='" + by + '\'' +
                ", hash='" + hash + '\'' +
                ", al='" + al + '\'' +
                ", sign='" + sign + '\'' +
                ", total=" + total +
                ", offset=" + offset +
                ", size=" + (null != lines ? lines.size() : 0) +
                '}';
    }
}
