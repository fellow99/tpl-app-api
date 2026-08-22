package org.fellow99.tpl.appapi.util;

import cn.hutool.core.util.RandomUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class CaptchaUtils {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    private static final int CHAR_COUNT = 4;

    public static CaptchaResult generate() {
        String code = RandomUtil.randomString(CHARS, CHAR_COUNT);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Interference lines
        g.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 5; i++) {
            int x1 = RandomUtil.randomInt(0, WIDTH);
            int y1 = RandomUtil.randomInt(0, HEIGHT);
            int x2 = RandomUtil.randomInt(0, WIDTH);
            int y2 = RandomUtil.randomInt(0, HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // Draw characters
        int charWidth = WIDTH / CHAR_COUNT;
        for (int i = 0; i < CHAR_COUNT; i++) {
            g.setFont(new Font("Arial", Font.BOLD, RandomUtil.randomInt(24, 30)));
            g.setColor(new Color(RandomUtil.randomInt(0, 100), RandomUtil.randomInt(0, 100), RandomUtil.randomInt(0, 100)));
            int x = charWidth * i + RandomUtil.randomInt(5, 15);
            int y = RandomUtil.randomInt(28, 38);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        g.dispose();

        // Encode to base64
        String img;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            img = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }

        return new CaptchaResult(uuid, img, code);
    }

    public static class CaptchaResult {
        private final String uuid;
        private final String img;
        private final String code;

        public CaptchaResult(String uuid, String img, String code) {
            this.uuid = uuid;
            this.img = img;
            this.code = code;
        }

        public String getUuid() { return uuid; }
        public String getImg() { return img; }
        public String getCode() { return code; }
    }
}
