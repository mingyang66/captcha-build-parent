package com.emily.captcha.slider.service;

import com.emily.captcha.CaptchaProperties;
import com.emily.captcha.slider.model.SliderCaptcha;
import com.emily.captcha.slider.store.SliderCaptchaSession;
import com.emily.captcha.slider.store.SliderStoreService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 滑动解锁验证码核心服务
 * <p>
 * 完全基于 Java AWT 原生绘制，不依赖任何第三方验证码库。
 * 生成带缺口的背景图和对应的拼图滑块图，用户拖动滑块到缺口位置完成验证。
 */
public class SliderCaptchaService {

    /**
     * 拼图块尺寸（正方形边长）
     */
    private static final int PIECE_SIZE = 44;

    /**
     * 拼图块凸起/凹陷的半径
     */
    private static final int KNOB_RADIUS = 6;

    /**
     * 验证码配置属性
     */
    private final CaptchaProperties properties;

    /**
     * 会话存储服务
     */
    private final SliderStoreService sliderStoreService;

    public SliderCaptchaService(CaptchaProperties properties, SliderStoreService sliderStoreService) {
        this.properties = properties;
        this.sliderStoreService = sliderStoreService;
    }

    /**
     * 生成一个新的滑动验证码
     *
     * @return SliderCaptcha 包含 captchaId、背景图Base64、滑块图Base64、Y坐标
     */
    public SliderCaptcha generate() {
        int width = properties.getSlider().getWidth();
        int height = properties.getSlider().getHeight();

        // 1. 绘制背景图
        BufferedImage backgroundImage = drawBackground(width, height);

        // 2. 计算拼图块的随机位置（留出左右边距，确保拼图块完整显示）
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int minX = PIECE_SIZE + KNOB_RADIUS + 10;
        int maxX = width - PIECE_SIZE - KNOB_RADIUS - 10;
        int minY = KNOB_RADIUS + 10;
        int maxY = height - PIECE_SIZE - KNOB_RADIUS - 10;

        int targetX = minX + rnd.nextInt(Math.max(1, maxX - minX));
        int targetY = minY + rnd.nextInt(Math.max(1, maxY - minY));

        // 3. 创建拼图块形状
        Shape pieceShape = createPieceShape(0, 0, PIECE_SIZE, KNOB_RADIUS);

        // 4. 从背景图中抠出拼图块区域，生成滑块图
        BufferedImage sliderImage = extractPiece(backgroundImage, pieceShape, targetX, targetY);

        // 5. 在背景图上绘制缺口（半透明遮罩 + 边框）
        drawCutout(backgroundImage, pieceShape, targetX, targetY);

        // 6. 编码为Base64
        String backgroundBase64 = encodeBase64(backgroundImage);
        String sliderBase64 = encodeBase64(sliderImage);

        // 7. 存入会话存储
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + properties.getSlider().getExpiryTime().toMillis();
        sliderStoreService.put(captchaId, new SliderCaptchaSession(targetX, expireAt));

        // 8. 组装返回结果
        SliderCaptcha captcha = new SliderCaptcha();
        captcha.setCaptchaId(captchaId);
        captcha.setBackgroundImage(backgroundBase64);
        captcha.setSliderImage(sliderBase64);
        captcha.setY(targetY);
        return captcha;
    }

    /**
     * 校验用户滑动位置是否正确
     *
     * @param captchaId 验证码ID
     * @param userX     用户滑动停止时的X坐标
     * @return true=验证通过，false=验证失败
     */
    public boolean verify(String captchaId, int userX) {
        if (captchaId == null) {
            return false;
        }
        // 1. 移除并获取会话
        SliderCaptchaSession session = sliderStoreService.remove(captchaId);
        if (session == null) {
            return false;
        }
        // 2. 检查是否过期
        if (System.currentTimeMillis() > session.expireAt()) {
            return false;
        }
        // 3. 检查X坐标是否在容差范围内
        int tolerance = properties.getSlider().getTolerance();
        return Math.abs(session.targetX() - userX) <= tolerance;
    }

    /**
     * 手动使某个验证码失效
     */
    public void invalidate(String captchaId) {
        if (captchaId != null) {
            sliderStoreService.remove(captchaId);
        }
    }

    // ------------------------------------------------------------------ internals

    /**
     * 绘制随机背景图
     */
    private BufferedImage drawBackground(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 渐变底色
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(180 + rnd.nextInt(60), 180 + rnd.nextInt(60), 200 + rnd.nextInt(55)),
                width, height, new Color(160 + rnd.nextInt(60), 190 + rnd.nextInt(50), 210 + rnd.nextInt(45))
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // 随机色块装饰
        for (int i = 0; i < 12; i++) {
            g2d.setColor(new Color(
                    rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256), 40 + rnd.nextInt(40)
            ));
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            int w = 20 + rnd.nextInt(80);
            int h = 20 + rnd.nextInt(60);
            g2d.fillRoundRect(x, y, w, h, 10, 10);
        }

        // 干扰线
        for (int i = 0; i < properties.getSlider().getNoiseLineCount(); i++) {
            g2d.setColor(new Color(rnd.nextInt(200), rnd.nextInt(200), rnd.nextInt(200), 100));
            g2d.setStroke(new BasicStroke(1 + rnd.nextFloat()));
            g2d.drawLine(rnd.nextInt(width), rnd.nextInt(height), rnd.nextInt(width), rnd.nextInt(height));
        }

        // 干扰点
        for (int i = 0; i < properties.getSlider().getNoisePointCount(); i++) {
            g2d.setColor(new Color(rnd.nextInt(220), rnd.nextInt(220), rnd.nextInt(220)));
            int size = 1 + rnd.nextInt(3);
            g2d.fillOval(rnd.nextInt(width), rnd.nextInt(height), size, size);
        }

        g2d.dispose();
        return img;
    }

    /**
     * 创建拼图块形状（带右侧凸起和底部凹陷的经典jigsaw形状）
     *
     * @param x      起始X偏移
     * @param y      起始Y偏移
     * @param size   正方形边长
     * @param radius 凸起/凹陷半径
     * @return Shape 拼图块形状
     */
    private Shape createPieceShape(int x, int y, int size, int radius) {
        // 基础矩形
        Area area = new Area(new Rectangle2D.Double(x, y, size, size));

        // 右侧凸起圆
        double rightCircleX = x + size - radius;
        double rightCircleY = y + (size - radius * 2) / 2.0;
        Area rightKnob = new Area(new Ellipse2D.Double(rightCircleX, rightCircleY, radius * 2, radius * 2));
        area.add(rightKnob);

        // 底部凹陷圆（减去）
        double bottomCircleX = x + (size - radius * 2) / 2.0;
        double bottomCircleY = y + size - radius;
        Area bottomNotch = new Area(new Ellipse2D.Double(bottomCircleX, bottomCircleY, radius * 2, radius * 2));
        area.subtract(bottomNotch);

        return area;
    }

    /**
     * 从背景图中提取拼图块区域的图像
     */
    private BufferedImage extractPiece(BufferedImage background, Shape shape, int offsetX, int offsetY) {
        // 拼图块实际占据的区域大小（含凸起）
        int pieceWidth = PIECE_SIZE + KNOB_RADIUS * 2;
        int pieceHeight = PIECE_SIZE + KNOB_RADIUS * 2;

        BufferedImage pieceImg = new BufferedImage(pieceWidth, pieceHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = pieceImg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 将形状平移到拼图块图像的坐标系中
        g2d.translate(-offsetX, -offsetY);
        g2d.setClip(shape);
        g2d.translate(offsetX, offsetY);

        // 绘制背景图中对应区域
        g2d.drawImage(background, 0, 0, null);

        g2d.dispose();

        // 重新绘制以应用裁剪（上面的clip方式在某些情况下不完整，改用Shape填充方式）
        BufferedImage result = new BufferedImage(pieceWidth, pieceHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 将shape平移到result坐标系
        Shape translatedShape = createPieceShape(0, 0, PIECE_SIZE, KNOB_RADIUS);
        g2.setClip(translatedShape);

        // 从背景图中截取对应区域绘制到result
        g2.drawImage(background,
                -offsetX, -offsetY,
                null);

        // 绘制拼图块边框
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(translatedShape);

        g2.dispose();
        return result;
    }

    /**
     * 在背景图上绘制缺口效果
     */
    private void drawCutout(BufferedImage background, Shape shape, int offsetX, int offsetY) {
        Graphics2D g2d = background.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 将形状平移到正确位置
        g2d.translate(offsetX, offsetY);

        // 绘制半透明遮罩表示缺口
        g2d.setClip(shape);
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(-KNOB_RADIUS, -KNOB_RADIUS, PIECE_SIZE + KNOB_RADIUS * 4, PIECE_SIZE + KNOB_RADIUS * 4);

        // 绘制缺口边框
        g2d.setClip(null);
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(2f));
        g2d.draw(shape);

        g2d.dispose();
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
