package mccfk.DeltaForce.java.client.screen;

import java.nio.charset.Charset;

/** Approximate initials for GB2312 level-one characters; other characters remain searchable verbatim. */
public final class PinyinHelper {
    private static final Charset GBK = Charset.forName("GBK");
    private static final int[] BOUNDARIES = {
            -20319, -20317, -19746, -19219, -18711, -18527, -18240, -17923,
            -17418, -16475, -16213, -15641, -15166, -14923, -14915, -14631,
            -14150, -14091, -13319, -12839, -12557, -11848, -11056
    };
    private static final String INITIALS = "abcdefghjklmnopqrstwxyz";
    private PinyinHelper() {}

    public static char getPinyinInitial(char character) {
        if (character < 128 && Character.isLetterOrDigit(character)) return Character.toLowerCase(character);
        byte[] bytes = String.valueOf(character).getBytes(GBK);
        if (bytes.length != 2) return '?';
        int code = ((bytes[0] & 255) << 8) + (bytes[1] & 255) - 65536;
        if (code < BOUNDARIES[0] || code > -10247) return '?';
        for (int i = BOUNDARIES.length - 1; i >= 0; i--) {
            if (code >= BOUNDARIES[i]) return INITIALS.charAt(i);
        }
        return '?';
    }

    public static String getPinyinInitials(String text) {
        return getFullPinyinInitials(text);
    }

    public static String getFullPinyinInitials(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char initial = getPinyinInitial(text.charAt(i));
            if (initial != '?') result.append(initial);
        }
        return result.toString();
    }
}
