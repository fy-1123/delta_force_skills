package mccfk.DeltaForce.java.client.screen;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 拼音首字母辅助工具
 * 使用GBK编码的区位特性来获取汉字的拼音首字母
 * GBK一级汉字(0xB0-0xD7)按拼音排序，可通过首字节确定拼音首字母
 */
public class PinyinHelper {

    private static final Charset GBK = Charset.forName("GBK");

    // GBK首字节 -> 拼音首字母 映射表
    // 0xB0-0xD7 是GB2312一级汉字，按拼音排序
    private static final char[] GBK_FIRST_LETTER = new char[256];

    static {
        for (int i = 0; i < 256; i++) {
            GBK_FIRST_LETTER[i] = '?';
        }
        // 一级汉字区 (GBK/GB2312 Level 1, 按拼音排序)
        setRange(0xB0, 0xB0, 'A'); // 啊-澳
        setRange(0xB1, 0xB1, 'B'); // 芭-贬
        setRange(0xB2, 0xB2, 'C'); // 擦-错
        setRange(0xB3, 0xB3, 'D'); // 搭-垛
        setRange(0xB4, 0xB4, 'E'); // 蛾-恶
        setRange(0xB5, 0xB5, 'F'); // 发-复
        setRange(0xB6, 0xB6, 'G'); // 噶-过
        setRange(0xB7, 0xB7, 'H'); // 哈-祸
        // I 不存在于拼音首字母中
        setRange(0xB8, 0xB8, 'J'); // 击-竣
        setRange(0xB9, 0xB9, 'K'); // 喀-括
        setRange(0xBA, 0xBA, 'L'); // 拉-略
        setRange(0xBB, 0xBB, 'M'); // 妈-谬
        setRange(0xBC, 0xBC, 'N'); // 拿-懦
        setRange(0xBD, 0xBD, 'O'); // 哦-沤
        setRange(0xBE, 0xBE, 'P'); // 啪-曝
        setRange(0xBF, 0xBF, 'Q'); // 期-逡
        setRange(0xC0, 0xC0, 'R'); // 然-弱
        setRange(0xC1, 0xC1, 'S'); // 撒-琐
        setRange(0xC2, 0xC2, 'T'); // 塌-箨
        // U 不存在于拼音首字母中
        // V 不存在于拼音首字母中
        setRange(0xC3, 0xC3, 'W'); // 挖-渥
        setRange(0xC4, 0xC4, 'X'); // 昔-蕈
        setRange(0xC5, 0xC5, 'Y'); // 压-蕴
        setRange(0xC6, 0xD7, 'Z'); // 匝-座 (及其他)
    }

    private static void setRange(int start, int end, char letter) {
        for (int i = start; i <= end; i++) {
            GBK_FIRST_LETTER[i] = letter;
        }
    }

    /**
     * 获取单个汉字的拼音首字母
     */
    public static char getPinyinInitial(char c) {
        if (c >= 'a' && c <= 'z') return c;
        if (c >= 'A' && c <= 'Z') return (char) (c - 'A' + 'a');
        if (c >= '0' && c <= '9') return c;

        // 尝试GBK编码
        try {
            byte[] gbkBytes = String.valueOf(c).getBytes(GBK);
            if (gbkBytes.length == 2) {
                int firstByte = gbkBytes[0] & 0xFF;
                char letter = GBK_FIRST_LETTER[firstByte];
                if (letter != '?') {
                    return (char) (letter - 'A' + 'a');
                }
            }
        } catch (Exception ignored) {
        }

        return '?';
    }

    /**
     * 获取字符串的拼音首字母串
     * 例如："启用" -> "qy"
     */
    public static String getPinyinInitials(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                char init = getPinyinInitial(c);
                if (init != '?') sb.append(init);
            } else if (Character.isLetter(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /**
     * 获取字符串的完整拼音首字母（每个汉字取首字母，保留英文）
     */
    public static String getFullPinyinInitials(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                char init = getPinyinInitial(c);
                if (init != '?') sb.append(init);
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}