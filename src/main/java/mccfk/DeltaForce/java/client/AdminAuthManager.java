package mccfk.DeltaForce.java.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 管理员密码验证管理器
 * 首次以MC_CaoFangKuai启动时弹窗要求输入密码，SHA-256哈希后加密存储在 client/.MCCFK_ADMIN/ 目录下
 * 后续启动时解密读取哈希与代码中的硬编码哈希对比，匹配则跳过弹窗，不匹配则弹窗后崩溃
 * 写入后文件设为只读，目录及文件设为隐藏
 */
public final class AdminAuthManager {

    /**
     * 正确密码的SHA-256哈希，分两片异或混淆存储
     * 反编译后不可直接读取，需运行时HASH_PART_A ^ HASH_PART_B拼接
     */
    private static final byte[] HASH_PART_A = {
        (byte)0x27, (byte)0xFF, (byte)0x04, (byte)0x12, (byte)0x6D, (byte)0x39, (byte)0x22, (byte)0xD0,
        (byte)0x82, (byte)0xBE, (byte)0x76, (byte)0x0C, (byte)0x83, (byte)0x14, (byte)0xDA, (byte)0x05,
        (byte)0x8F, (byte)0x19, (byte)0x72, (byte)0x53, (byte)0x0C, (byte)0xD6, (byte)0x38, (byte)0x47,
        (byte)0xC3, (byte)0xD8, (byte)0x6D, (byte)0x86, (byte)0xC9, (byte)0x4D, (byte)0x69, (byte)0xDD,
    };

    private static final byte[] HASH_PART_B = {
        (byte)0xE4, (byte)0x2E, (byte)0x0C, (byte)0x78, (byte)0xA7, (byte)0x94, (byte)0x47, (byte)0x17,
        (byte)0xD7, (byte)0xD9, (byte)0x16, (byte)0x55, (byte)0xF7, (byte)0xE5, (byte)0xB6, (byte)0xB8,
        (byte)0x88, (byte)0x76, (byte)0xB0, (byte)0xDA, (byte)0x39, (byte)0x55, (byte)0xC2, (byte)0x1B,
        (byte)0xE8, (byte)0x1F, (byte)0xED, (byte)0xEF, (byte)0xFA, (byte)0x73, (byte)0x5D, (byte)0x25,
    };

    private static final String CORRECT_HASH;
    static {
        byte[] combined = new byte[32];
        for (int i = 0; i < 32; i++) {
            combined[i] = (byte)(HASH_PART_A[i] ^ HASH_PART_B[i]);
        }
        CORRECT_HASH = HexFormat.of().formatHex(combined);
    }

    /** 哈希存储目录 */
    private static final Path AUTH_DIR = Paths.get(".MCCFK_ADMIN");

    /** 哈希存储文件 */
    private static final Path HASH_FILE = AUTH_DIR.resolve("admin-auth.dat");

    /**
     * XOR加密密钥，分两片异或混淆存储
     * 反编译后不可直接读取，需运行时XK_PART_A ^ XK_PART_B拼接
     */
    private static final byte[] XK_PART_A = {
        (byte)0x3D, (byte)0x36, (byte)0xF3, (byte)0x6E, (byte)0x74, (byte)0xA3, (byte)0x0C, (byte)0x2E,
        (byte)0x15, (byte)0x5E, (byte)0x50, (byte)0xA3, (byte)0xB7, (byte)0x65, (byte)0x54, (byte)0xF8,
        (byte)0xAC, (byte)0xBE, (byte)0xAD, (byte)0x26, (byte)0x76, (byte)0xC8, (byte)0x62, (byte)0xDD,
        (byte)0x97,
    };

    private static final byte[] XK_PART_B = {
        (byte)0x79, (byte)0x50, (byte)0xAC, (byte)0x23, (byte)0x37, (byte)0xE0, (byte)0x4A, (byte)0x65,
        (byte)0x4A, (byte)0x1F, (byte)0x25, (byte)0xD7, (byte)0xDF, (byte)0x3A, (byte)0x0C, (byte)0x97,
        (byte)0xDE, (byte)0xE1, (byte)0xFE, (byte)0x47, (byte)0x1A, (byte)0xBC, (byte)0x3D, (byte)0xAB,
        (byte)0xA5,
    };

    private static final byte[] XOR_KEY;
    static {
        int len = XK_PART_A.length;
        XOR_KEY = new byte[len];
        for (int i = 0; i < len; i++) {
            XOR_KEY[i] = (byte)(XK_PART_A[i] ^ XK_PART_B[i]);
        }
    }

    /** 缓存验证结果，整个会话期间只校验一次 */
    private static Boolean cachedVerified;

    /** 当前已验证的玩家名称（用于检测In-Game Account Switcher切换账户） */
    private static String verifiedPlayerName = null;

    public AdminAuthManager() {
    }

    /**
     * 获取正确密码的SHA-256哈希（供网络包使用）
     */
    public static String getCorrectHash() {
        return CORRECT_HASH;
    }

    /**
     * 重置验证状态（检测到账户切换时调用，强制重新验证）
     */
    public static void resetVerification() {
        cachedVerified = null;
        verifiedPlayerName = null;
    }

    /**
     * 检查当前客户端是否已通过管理员验证
     * @param currentPlayerName 当前玩家名称（用于检测账户切换）
     * @return true=已验证通过且账户未变化
     */
    public static boolean isVerified(String currentPlayerName) {
        if (cachedVerified == null || !cachedVerified) {
            return false;
        }
        // 检测账户切换：已验证的玩家名与当前不一致则视为未验证
        if (verifiedPlayerName != null && !verifiedPlayerName.equals(currentPlayerName)) {
            return false;
        }
        return cachedVerified;
    }

    /**
     * 无参版本（兼容旧调用），不做玩家名校验
     */
    public static boolean isVerified() {
        return cachedVerified != null && cachedVerified;
    }

    /**
     * 尝试验证密码
     * @param input 用户输入的明文密码
     * @param playerName 当前验证的玩家名称
     * @return true=密码正确且已保存哈希
     */
    public static boolean tryPassword(String input, String playerName) {
        String inputHash = sha256(input);
        if (CORRECT_HASH.equals(inputHash)) {
            saveHash(inputHash);
            cachedVerified = true;
            verifiedPlayerName = playerName;
            return true;
        }
        return false;
    }

    /** 保存哈希到文件：加密后Base64写入，设置隐藏+只读 */
    private static void saveHash(String hash) {
        try {
            // 确保目录存在且隐藏
            Files.createDirectories(AUTH_DIR);
            try {
                Files.setAttribute(AUTH_DIR, "dos:hidden", true);
            } catch (Exception ignored) {
            }

            // 加密哈希后Base64编码写入
            String encrypted = encrypt(hash);
            Files.writeString(HASH_FILE, encrypted,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 设置文件隐藏+只读（Windows）
            try {
                Files.setAttribute(HASH_FILE, "dos:hidden", true);
                Files.setAttribute(HASH_FILE, "dos:readonly", true);
            } catch (Exception ignored) {
            }
        } catch (IOException e) {
            System.err.println("[AdminAuth] 保存验证哈希失败: " + e.getMessage());
        }
    }

    /**
     * 从磁盘加载并验证保存的管理员哈希
     * 游戏启动时调用，验证通过则设置cachedVerified=true，跳过弹窗
     */
    public static void verifyFromDisk() {
        try {
            if (Files.exists(HASH_FILE)) {
                String encryptedBase64 = Files.readString(HASH_FILE);
                String storedHash = decrypt(encryptedBase64);
                if (CORRECT_HASH.equals(storedHash)) {
                    cachedVerified = true;
                }
            }
        } catch (IOException e) {
            System.err.println("[AdminAuth] 读取验证哈希失败: " + e.getMessage());
        }
    }

    /**
     * 加密哈希字符串：SHA-256哈希字节 → XOR混肴 → Base64编码
     * 密文不再以原始十六进制哈希形式存储于磁盘
     */
    private static String encrypt(String hashHex) {
        byte[] hashBytes = HexFormat.of().parseHex(hashHex);
        byte[] encrypted = xorTransform(hashBytes);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密：Base64解码 → XOR逆变换 → 还原十六进制哈希字符串
     */
    private static String decrypt(String encryptedBase64) {
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
        byte[] hashBytes = xorTransform(encrypted);
        return HexFormat.of().formatHex(hashBytes);
    }

    /** XOR变换（加密/解密对称） */
    private static byte[] xorTransform(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return result;
    }

    /**
     * 计算字符串的 SHA-256 哈希（小写十六进制）
     */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }
}