package com.emily.captcha.click.service;

import com.emily.captcha.CaptchaProperties;
import com.emily.captcha.click.model.ClickCaptcha;
import com.emily.captcha.click.model.ClickPoint;
import com.emily.captcha.click.store.CaptchaSession;
import com.emily.captcha.click.store.CaptchaSessionStoreService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 文字点选验证码核心服务
 * <p>
 * 完全基于 Java AWT 原生绘制，不依赖任何第三方验证码库。
 */
public class ClickCaptchaService {

    /**
     * 常用汉字池（选取笔画清晰、辨识度高的汉字）
     */
    private static final char[] CHAR_POOL = {
            '春', '夏', '秋', '冬', '风', '花', '雪', '月',
            '山', '水', '云', '雨', '日', '星', '天', '地',
            '龙', '虎', '鸟', '鱼', '马', '牛', '羊', '鸡',
            '红', '蓝', '绿', '黄', '白', '黑', '金', '银',
            '东', '西', '南', '北', '中', '大', '小', '多',
            '长', '高', '深', '远', '快', '慢', '新', '旧',
            '松', '竹', '梅', '兰', '菊', '荷', '桃', '柳',
            '剑', '琴', '棋', '书', '画', '诗', '酒', '茶'
    };

    /**
     * 验证码配置属性
     */
    private final CaptchaProperties properties;
    private final CaptchaSessionStoreService captchaStoreService;

    public ClickCaptchaService(CaptchaProperties properties, CaptchaSessionStoreService captchaStoreService) {
        this.properties = properties;
        this.captchaStoreService = captchaStoreService;
    }

    /**
     * 生成一个新的文字点选验证码
     *
     * @return Captcha 包含 captchaId、Base64 图片、提示文字
     */
    public ClickCaptcha generate() {
        int charCount = properties.getCharCount();
        int targetCount = properties.getTargetCount();
        int width = properties.getWidth();
        int height = properties.getHeight();

        // 1. 从汉字池中随机选取 charCount 个不重复汉字
        List<Character> chars = pickRandomChars(charCount);

        // 2. 为每个汉字计算不重叠的绘制坐标
        List<int[]> positions = computePositions(charCount, width, height);

        // 3. 随机选取 targetCount 个汉字作为按顺序点击的目标
        List<Integer> targetIndices = pickTargetIndices(charCount, targetCount);

        // 4. 绘制图片
        BufferedImage image = drawImage(chars, positions, width, height);

        // 5. 构建 Captcha 响应
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        List<String> targetChars = new ArrayList<>();
        List<ClickPoint> targetPoints = new ArrayList<>();
        for (int idx : targetIndices) {
            targetChars.add(String.valueOf(chars.get(idx)));
            int[] pos = positions.get(idx);
            targetPoints.add(new ClickPoint(pos[0], pos[1]));
        }

        // 6. 存入内存（带过期时间戳）
        long expireAt = System.currentTimeMillis() + properties.getExpiryTime().toMillis();
        captchaStoreService.put(captchaId, new CaptchaSession(targetChars, targetPoints, expireAt));

        // 7. 图片编码为 Base64
        String base64 = encodeBase64(image);

        // 8. 组装提示文字
        String promptText = "请依次点击: " + String.join(" ", targetChars);

        ClickCaptcha captcha = new ClickCaptcha();
        captcha.setCaptchaId(captchaId);
        captcha.setImage(base64);
        captcha.setPromptText(promptText);
        captcha.setTargetChars(targetChars);
        return captcha;
    }

    /**
     * 校验用户的点击序列是否正确
     *
     * @param captchaId 验证码 ID
     * @param clicks    用户点击坐标列表（按点击顺序）
     * @return true=验证通过，false=验证失败
     */
    public boolean verify(String captchaId, List<ClickPoint> clicks) {
        if (captchaId == null || clicks == null) {
            return false;
        }
        //1. 从会话存储中移除验证码会话数据
        CaptchaSession session = captchaStoreService.remove(captchaId);
        if (session == null) {
            return false;
        }
        //2. 检查验证码是否已过期
        if (System.currentTimeMillis() > session.getExpireAt()) {
            return false;
        }
        //3. 检查点击目标数量是否一致
        List<ClickPoint> targets = session.getTargetPoints();
        if (clicks.size() != targets.size()) {
            return false;
        }
        //4. 检查每个点击目标的坐标是否在容差范围内
        int tolerance = properties.getTolerance();
        for (int i = 0; i < targets.size(); i++) {
            ClickPoint expected = targets.get(i);
            ClickPoint actual = clicks.get(i);
            double distance = Math.sqrt(
                    Math.pow(expected.getX() - actual.getX(), 2)
                            + Math.pow(expected.getY() - actual.getY(), 2));
            if (distance > tolerance) {
                return false;
            }
        }
        return true;
    }

    /**
     * 手动使某个验证码失效
     */
    public void invalidate(String captchaId) {
        if (captchaId != null) {
            captchaStoreService.remove(captchaId);
        }
    }

    // ------------------------------------------------------------------ internals

    /**
     * 从汉字池中随机选取 n 个不重复汉字
     */
    private List<Character> pickRandomChars(int n) {
        List<Character> pool = new ArrayList<>(CHAR_POOL.length);
        for (char c : CHAR_POOL) {
            pool.add(c);
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return pool.subList(0, n);
    }

    /**
     * 将图片区域划分为 charCount 个网格，在每个网格内随机取一个绘制坐标，保证不重叠。
     * 返回 List of int[]{centerX, centerY}
     */
    private List<int[]> computePositions(int charCount, int width, int height) {
        int fontSizeMax = properties.getFontSizeMax();
        // 左右边距
        int padTop = fontSizeMax + 8; // 顶部留出空间（后续可用于绘制提示区域，但提示不在图片内）
        int padBottom = fontSizeMax / 2;

        int usableWidth = width - 2 * fontSizeMax;
        int usableHeight = height - padTop - padBottom;

        // 单列宽度
        int cellWidth = usableWidth / charCount;

        Random rnd = ThreadLocalRandom.current();
        List<int[]> positions = new ArrayList<>();

        for (int i = 0; i < charCount; i++) {
            int cellLeft = fontSizeMax + i * cellWidth;
            int cx = cellLeft + rnd.nextInt(Math.max(1, cellWidth - fontSizeMax)) + fontSizeMax / 2;
            int cy = padTop + rnd.nextInt(Math.max(1, usableHeight - fontSizeMax)) + fontSizeMax / 2;
            positions.add(new int[]{cx, cy});
        }
        return positions;
    }

    /**
     * 随机选取 targetCount 个目标索引（不重复，保持顺序）
     */
    private List<Integer> pickTargetIndices(int charCount, int targetCount) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < charCount; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, ThreadLocalRandom.current());
        List<Integer> picked = indices.subList(0, targetCount);
        // 按原始出现顺序排列，方便用户按提示顺序点击
        Collections.sort(picked);
        return picked;
    }

    /**
     * 使用 Java AWT 绘制验证码图片
     */
    private BufferedImage drawImage(List<Character> chars, List<int[]> positions,
                                    int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        // 开启抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 绘制浅色随机背景
        drawBackground(g2d, width, height);

        // 绘制干扰线
        drawNoiseLines(g2d, width, height);

        // 绘制干扰点
        drawNoisePoints(g2d, width, height);

        // 绘制汉字
        Random rnd = ThreadLocalRandom.current();
        int fontSizeMin = properties.getFontSizeMin();
        int fontSizeMax = properties.getFontSizeMax();

        for (int i = 0; i < chars.size(); i++) {
            int fontSize = fontSizeMin + rnd.nextInt(Math.max(1, fontSizeMax - fontSizeMin + 1));
            // 随机轻微旋转（-15° ~ +15°）
            double angle = (rnd.nextDouble() - 0.5) * Math.toRadians(30);

            Font font = new Font("Microsoft YaHei", Font.BOLD, fontSize);
            g2d.setFont(font);

            // 随机深色
            g2d.setColor(new Color(
                    20 + rnd.nextInt(100),
                    20 + rnd.nextInt(100),
                    20 + rnd.nextInt(100)));

            int[] pos = positions.get(i);
            g2d.rotate(angle, pos[0], pos[1]);
            g2d.drawString(String.valueOf(chars.get(i)), pos[0] - fontSize / 2, pos[1] + fontSize / 3);
            g2d.rotate(-angle, pos[0], pos[1]); // 恢复旋转
        }

        g2d.dispose();
        return img;
    }

    /**
     * 绘制浅色随机背景块
     */
    private void drawBackground(Graphics2D g2d, int width, int height) {
        Random rnd = ThreadLocalRandom.current();
        // 整体底色
        g2d.setColor(new Color(230 + rnd.nextInt(20), 230 + rnd.nextInt(20), 230 + rnd.nextInt(20)));
        g2d.fillRect(0, 0, width, height);

        // 随机浅色块
        for (int i = 0; i < 8; i++) {
            g2d.setColor(new Color(
                    200 + rnd.nextInt(55),
                    200 + rnd.nextInt(55),
                    200 + rnd.nextInt(55),
                    80));
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            int w = 30 + rnd.nextInt(60);
            int h = 20 + rnd.nextInt(40);
            g2d.fillRoundRect(x, y, w, h, 8, 8);
        }
    }

    /**
     * 绘制干扰线
     */
    private void drawNoiseLines(Graphics2D g2d, int width, int height) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = 0; i < properties.getNoiseLineCount(); i++) {
            g2d.setColor(new Color(rnd.nextInt(200), rnd.nextInt(200), rnd.nextInt(200), 120));
            g2d.setStroke(new BasicStroke(1 + rnd.nextFloat()));
            g2d.drawLine(rnd.nextInt(width), rnd.nextInt(height),
                    rnd.nextInt(width), rnd.nextInt(height));
        }
    }

    /**
     * 绘制干扰点
     */
    private void drawNoisePoints(Graphics2D g2d, int width, int height) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = 0; i < properties.getNoisePointCount(); i++) {
            g2d.setColor(new Color(rnd.nextInt(220), rnd.nextInt(220), rnd.nextInt(220)));
            int size = 1 + rnd.nextInt(3);
            g2d.fillOval(rnd.nextInt(width), rnd.nextInt(height), size, size);
        }
    }

    /**
     * 将 BufferedImage 编码为 Base64 字符串（含 data URI 前缀）
     */
    private String encodeBase64(BufferedImage image) {
        try (ByteArrayOutputStream bas = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bas);
            String b64 = Base64.getEncoder().encodeToString(bas.toByteArray());
            return "data:image/png;base64," + b64;
        } catch (Exception e) {
            throw new RuntimeException("验证码图片编码失败", e);
        }
    }
}
