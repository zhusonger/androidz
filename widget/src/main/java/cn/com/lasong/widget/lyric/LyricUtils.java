package cn.com.lasong.widget.lyric;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;


/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-04
 * Description: krc歌词文件解码工具类
 */
public class LyricUtils {
    private static final String TAG = "LRC";
    private static final char[] MI_ARRAY = { '@', 'G', 'a', 'w', '^', '2', 't',
            'G', 'Q', '6', '1', '-', 'Î', 'Ò', 'n', 'i' };

    private static final String[] REGULAR_PREDIX = {"id", "ar", "ti", "by", "hash", "al", "sign", "total", "offset"};

    /**
     * 将歌词文件里的二进制内容解密，然后解压缩，处理回正常文本
     * @param filePath 文件路径
     * @return
     */
    public static byte[] parseLyricFileToByte(String filePath){
        try{
            FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            return parseLyricFileToByte(fileInputStream);
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decompress(byte[] data) {
        byte[] output;
        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);
        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        decompresser.end();
        return output;
    }

    public static byte[] parseLyricFileToByte(InputStream in) {
        try{
            byte[] zipBytes = new byte[(int)in.available()];
            //下面这段是网上查到的协议解密代码，经检验无误
            byte[] topBytes = new byte[4];
            in.read(topBytes);
            in.read(zipBytes);
            int len = zipBytes.length;
            for (int i = 0; i < len; i++) {
                int j = i % 16;
                int tmp67_65 = i;
                byte[] tmp67_64 = zipBytes;
                tmp67_64[tmp67_65] = (byte) (tmp67_64[tmp67_65] ^ MI_ARRAY[j]);
            }
            //解压缩。krc格式文件除了加密之外，还使用了ZLIB压缩库中的 DEFLATE压缩算法 进行压缩。
            byte[] output;
            Inflater decompresser = new Inflater();
            decompresser.reset();
            decompresser.setInput(zipBytes);
            ByteArrayOutputStream o = new ByteArrayOutputStream(zipBytes.length);
            try {
                byte[] buf = new byte[1024];
                while (!decompresser.finished()) {
                    int i = decompresser.inflate(buf);
                    o.write(buf, 0, i);
                }
                output = o.toByteArray();
            } catch (Exception e) {
                output = zipBytes;
                e.printStackTrace();
            } finally {
                try {
                    o.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            decompresser.end();
            return output;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * 读取歌词
     * @param filePath
     * @return
     */
    public static List<String> readLyricList(String filePath) {
        byte[] bytes = parseLyricFileToByte(filePath);
        if (null == bytes) {
            return null;
        }

        return readLyricListFromBytes(bytes);
    }
    public static List<String> readLyricList(InputStream in) {
        byte[] bytes = parseLyricFileToByte(in);
        if (null == bytes) {
            return null;
        }
        return readLyricListFromBytes(bytes);
    }
    private static List<String> readLyricListFromBytes(byte[] bytes) {
        BufferedReader br = null;
        List<String> list = new ArrayList<>();
        try {
            br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (Exception e) {
            Log.e(TAG, "readLyc", e);
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    public static Lyric readLyric(InputStream in) {
        List<String> list = readLyricList(in);
        if (null == list || list.size() <= 0) {
            return null;
        }
        Pattern linePattern = Pattern.compile("\\[(\\d+),(\\d+)](.*)");
        Pattern wordPattern = Pattern.compile("<(\\d+),(\\d+),(\\d+)>([^<]*)");
        Lyric lyric = new Lyric();
        for (String line : list) {
            // 歌词信息
            if (line.startsWith("[id:")) {
                lyric.id  = line.replace("[id:", "").replace("]", "");
            } else if (line.startsWith("[ar:")) {
                lyric.ar  = line.replace("[ar:", "").replace("]", "");
            } else if (line.startsWith("[ti:")) {
                lyric.ti  = line.replace("[ti:", "").replace("]", "");
            } else if (line.startsWith("[by:")) {
                lyric.by  = line.replace("[by:", "").replace("]", "");
            } else if (line.startsWith("[hash:")) {
                lyric.hash  = line.replace("[hash:", "").replace("]", "");
            } else if (line.startsWith("[al:")) {
                lyric.al  = line.replace("[al:", "").replace("]", "");
            } else if (line.startsWith("[sign:")) {
                lyric.sign  = line.replace("[sign:", "").replace("]", "");
            } else if (line.startsWith("[total:")) {
                String string  = line.replace("[total:", "").replace("]", "");
                lyric.total = Long.parseLong(string);
            } else if (line.startsWith("[offset:")) {
                String string  = line.replace("[offset:", "").replace("]", "");
                lyric.offset = Long.parseLong(string);
            } else {
                Matcher lineMatcher = linePattern.matcher(line);
                if (lyric.lines == null) {
                    lyric.lines = new ArrayList<>();
                }
                // 歌词
                if (lineMatcher.matches()) {
                    LyricLine lrcLine = new LyricLine();
                    lyric.lines.add(lrcLine);
                    String lineStart = lineMatcher.group(1);
                    String lineDuration = lineMatcher.group(2);
                    String lineWords = lineMatcher.group(3);
                    lrcLine.start = Long.parseLong(lineStart != null ? lineStart : "0");
                    lrcLine.duration = Long.parseLong(lineDuration != null ? lineDuration : "0");
                    if (!TextUtils.isEmpty(lineWords)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        Matcher wordMatcher = wordPattern.matcher(lineWords);
                        while (wordMatcher.find()) {
                            LyricWord lrcWord = new LyricWord();
                            if (lrcLine.words == null) {
                                lrcLine.words = new ArrayList<>();
                            }
                            lrcLine.words.add(lrcWord);
                            String wordStart = wordMatcher.group(1);
                            String wordDuration = wordMatcher.group(2);
                            String wordOffset = wordMatcher.group(3);
                            String word = wordMatcher.group(4);
                            lrcWord.start = Long.parseLong(wordStart != null ? wordStart : "0");
                            lrcWord.duration = Long.parseLong(wordDuration != null ? wordDuration : "0");
                            lrcWord.offset = Long.parseLong(wordOffset != null ? wordOffset : "0");
                            lrcWord.word = word;
                            stringBuilder.append(word);
                        }
                        lrcLine.content = stringBuilder.toString();

                        // 校正某些歌词文件最后一行歌词的持续时间为歌词真实时长
                        // 部分歌词最后一句时长是歌词总时长, 部分是单行歌词时长
                        long wordDuration = lrcLine.duration;
                        if (lrcLine.words != null && lrcLine.words.size() > 0) {
                            int index = lrcLine.words.size() - 1;
                            LyricWord lastWord = lrcLine.words.get(index);
                            wordDuration = Math.min(lastWord.start + lastWord.duration, wordDuration);
                        }
                        lrcLine.duration = wordDuration;
                    }
                }
            }
        }

        return lyric;
    }
}
