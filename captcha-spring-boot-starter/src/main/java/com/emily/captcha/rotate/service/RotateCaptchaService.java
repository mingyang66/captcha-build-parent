package com.emily.captcha.rotate.service;

import com.emily.captcha.CaptchaProperties;
import com.emily.captcha.rotate.model.RotateCaptcha;
import com.emily.captcha.rotate.store.RotateCaptchaSession;
import com.emily.captcha.rotate.store.RotateStoreService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 旋转验证码核心服务
 * <p>
 * 完全基于 Java AWT 原生绘制，不依赖任何第三方验证码库。
 * 生成一张被随机旋转的圆形图片，用户需要将其旋转到正确方向完成验证。
 */
public class RotateCaptchaService {

    /**
     * 验证码配置属性
     */
    private final CaptchaProperties properties;

    /**
     * 会话存储服务
     */
    private final RotateStoreService storeService;

    public RotateCaptchaService(CaptchaProperties properties, RotateStoreService storeService) {
        this.properties = properties;
        this.storeService = storeService;
    }

    /**
     * 生成一个新的旋转验证码
     *
     * @return RotateCaptcha 包含 captchaId、被旋转后的圆形图片Base64
     */
    public RotateCaptcha generate() {
        int size = properties.getRotate().getSize();

        // 1. 绘制正向圆形图片（带方向标识）
        BufferedImage originalImage = drawCircleImage(size);

        // 2. 随机旋转角度（避开0度附近和360度附近，确保有足够难度）
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int minAngle = properties.getRotate().getMinAngle();
        int maxAngle = properties.getRotate().getMaxAngle();
        int targetAngle = minAngle + rnd.nextInt(Math.max(1, maxAngle - minAngle + 1));

        // 3. 对图片应用旋转
        BufferedImage rotatedImage = rotateImage(originalImage, targetAngle);

        // 4. 编码为Base64
        String imageBase64 = encodeBase64(rotatedImage);

        // 5. 存入会话存储
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + properties.getRotate().getExpiryTime().toMillis();
        storeService.put(captchaId, new RotateCaptchaSession(targetAngle, expireAt));

        // 6. 组装返回结果
        RotateCaptcha captcha = new RotateCaptcha();
        captcha.setCaptchaId(captchaId);
        captcha.setImage(imageBase64);
        return captcha;
    }

    /**
     * 校验用户旋转角度是否正确
     * <p>
     * 用户在前端将图片从被旋转的状态还原到正位，
     * 即用户旋转角度 = (360 - targetAngle) % 360。
     * 但更通用的方式是：用户提交的旋转角度与目标角度的互补值之差在容差内。
     * <p>
     * 简化逻辑：前端提交用户旋转的角度 userAngle，
     * 服务端判断 (userAngle + targetAngle) % 360 是否接近 0（或360）。
     *
     * @param captchaId 验证码ID
     * @param userAngle 用户旋转的角度（度，顺时针）
     * @return true=验证通过，false=验证失败
     */
    public boolean verify(String captchaId, int userAngle) {
        if (captchaId == null) {
            return false;
        }
        // 1. 移除并获取会话
        RotateCaptchaSession session = storeService.remove(captchaId);
        if (session == null) {
            return false;
        }
        // 2. 检查是否过期
        if (System.currentTimeMillis() > session.expireAt()) {
            return false;
        }
        // 3. 计算角度差：用户需要将图片旋转 (360 - targetAngle) 度来还原
        //    所以 userAngle 应该约等于 (360 - targetAngle) % 360
        int expectedUserAngle = (360 - session.targetAngle()) % 360;
        int diff = Math.abs(userAngle - expectedUserAngle);
        // 处理跨越0/360边界的情况
        if (diff > 180) {
            diff = 360 - diff;
        }
        int tolerance = properties.getRotate().getTolerance();
        return diff <= tolerance;
    }

    /**
     * 手动使某个验证码失效
     */
    public void invalidate(String captchaId) {
        if (captchaId != null) {
            storeService.remove(captchaId);
        }
    }

    // ------------------------------------------------------------------ internals

    /**
     * 绘制带方向标识的圆形图片
     * <p>
     * 使用不对称的方向性图案（风景+箭头+刻度盘），让用户能明确判断正方向。
     * 绘制在更大的画布上再裁剪为圆形，避免旋转后出现黑边/锯齿。
     */
    private BufferedImage drawCircleImage(int size) {
        // 使用2倍尺寸绘制，最终缩放回目标尺寸，提升抗锯齿质量
        int canvasSize = size * 2;
        BufferedImage img = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int cx = canvasSize / 2;
        int cy = canvasSize / 2;
        int radius = canvasSize / 2 - 4;

        // 裁剪为圆形
        g2d.setClip(new Ellipse2D.Double(4, 4, canvasSize - 8, canvasSize - 8));

        // 渐变背景（更丰富的色彩）
        Color c1 = new Color(60 + rnd.nextInt(80), 120 + rnd.nextInt(80), 180 + rnd.nextInt(75));
        Color c2 = new Color(160 + rnd.nextInt(60), 80 + rnd.nextInt(80), 180 + rnd.nextInt(60));
        GradientPaint gradient = new GradientPaint(0, 0, c1, canvasSize, canvasSize, c2);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, canvasSize, canvasSize);

        // 绘制装饰性同心圆环（增加方向感）
        for (int i = 3; i >= 1; i--) {
            int ringR = radius * i / 4;
            g2d.setColor(new Color(255, 255, 255, 20 + rnd.nextInt(20)));
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(new Ellipse2D.Double(cx - ringR, cy - ringR, ringR * 2, ringR * 2));
        }

        // 绘制装饰色块（更多、更大）
        for (int i = 0; i < 10; i++) {
            g2d.setColor(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256), 30 + rnd.nextInt(40)));
            int x = rnd.nextInt(canvasSize);
            int y = rnd.nextInt(canvasSize);
            int w = 20 + rnd.nextInt(60);
            int h = 20 + rnd.nextInt(60);
            g2d.fillRoundRect(x, y, w, h, 10, 10);
        }

        // 干扰线
        for (int i = 0; i < properties.getRotate().getNoiseLineCount() * 2; i++) {
            g2d.setColor(new Color(rnd.nextInt(200), rnd.nextInt(200), rnd.nextInt(200), 80));
            g2d.setStroke(new BasicStroke(1 + rnd.nextFloat() * 2));
            g2d.drawLine(rnd.nextInt(canvasSize), rnd.nextInt(canvasSize),
                    rnd.nextInt(canvasSize), rnd.nextInt(canvasSize));
        }

        // 干扰点
        for (int i = 0; i < properties.getRotate().getNoisePointCount() * 2; i++) {
            g2d.setColor(new Color(rnd.nextInt(220), rnd.nextInt(220), rnd.nextInt(220)));
            int dotSize = 2 + rnd.nextInt(4);
            g2d.fillOval(rnd.nextInt(canvasSize), rnd.nextInt(canvasSize), dotSize, dotSize);
        }

        // 绘制方向刻度盘（12个刻度标记，上方加粗突出）
        drawDial(g2d, cx, cy, radius);

        // 绘制向上的箭头作为主方向标识
        drawArrow(g2d, cx, cy, radius);

        // 取消裁剪，绘制圆形边框
        g2d.setClip(null);
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.setStroke(new BasicStroke(4f));
        g2d.draw(new Ellipse2D.Double(4, 4, canvasSize - 8, canvasSize - 8));

        g2d.dispose();

        // 缩放到目标尺寸（高质量缩放）
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D rg = result.createGraphics();
        rg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        rg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rg.drawImage(img, 0, 0, size, size, null);
        rg.dispose();
        return result;
    }

    /**
     * 绘制方向刻度盘
     * <p>
     * 12个等分刻度，其中"上"方刻度加粗加长，帮助用户判断方向。
     */
    private void drawDial(Graphics2D g2d, int cx, int cy, int radius) {
        int outerR = radius - 8;
        int innerRNormal = radius - 24;
        int innerRMain = radius - 36;

        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 - 90); // -90使0度朝上
            boolean isMain = (i == 0); // 上方为主刻度

            int innerR = isMain ? innerRMain : innerRNormal;
            float strokeW = isMain ? 4f : 1.5f;
            int alpha = isMain ? 220 : 100;

            g2d.setColor(new Color(255, 255, 255, alpha));
            g2d.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int x1 = cx + (int) (innerR * Math.cos(angle));
            int y1 = cy + (int) (innerR * Math.sin(angle));
            int x2 = cx + (int) (outerR * Math.cos(angle));
            int y2 = cy + (int) (outerR * Math.sin(angle));
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 在图片中心绘制一个向上的箭头（更醒目）
     */
    private void drawArrow(Graphics2D g2d, int cx, int cy, int radius) {
        int arrowLen = radius * 3 / 5;
        int arrowWidth = Math.max(4, radius / 8);
        int headSize = radius / 4;

        // 箭头阴影（增加立体感）
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.setStroke(new BasicStroke(arrowWidth + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(cx + 2, cy + arrowLen / 2 + 2, cx + 2, cy - arrowLen / 2 + 2);

        // 箭头杆
        g2d.setColor(new Color(255, 255, 255, 240));
        g2d.setStroke(new BasicStroke(arrowWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(cx, cy + arrowLen / 2, cx, cy - arrowLen / 2);

        // 箭头头部（三角形，带阴影）
        int tipY = cy - arrowLen / 2 - headSize;
        int baseY = cy - arrowLen / 2 + headSize / 2;

        // 阴影
        g2d.setColor(new Color(0, 0, 0, 60));
        int[] sxPoints = {cx + 2, cx - headSize + 2, cx + headSize + 2};
        int[] syPoints = {tipY + 2, baseY + 2, baseY + 2};
        g2d.fillPolygon(sxPoints, syPoints, 3);

        // 实体
        g2d.setColor(new Color(255, 255, 255, 240));
        int[] xPoints = {cx, cx - headSize, cx + headSize};
        int[] yPoints = {tipY, baseY, baseY};
        g2d.fillPolygon(xPoints, yPoints, 3);

        // "上" 文字标注（带阴影）
        int fontSize = Math.max(14, radius / 4);
        g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, fontSize));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "\u4E0A";
        int textWidth = fm.stringWidth(text);
        int textX = cx - textWidth / 2;
        int textY = cy + arrowLen / 2 + fm.getHeight();

        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.drawString(text, textX + 1, textY + 1);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.drawString(text, textX, textY);
    }

    /**
     * 将图片旋转指定角度
     */
    private BufferedImage rotateImage(BufferedImage source, int angleDeg) {
        int size = source.getWidth();
        BufferedImage rotated = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        AffineTransform transform = new AffineTransform();
        transform.translate(size / 2.0, size / 2.0);
        transform.rotate(Math.toRadians(angleDeg));
        transform.translate(-size / 2.0, -size / 2.0);

        g2d.drawImage(source, transform, null);
        g2d.dispose();
        return rotated;
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
