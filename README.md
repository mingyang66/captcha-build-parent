# Captcha Spring Boot Starter

基于 Spring Boot 的图形验证码组件，支持 **文字点选**、**滑动解锁**、**旋转验证** 三种验证码类型。

## 特性

- 零配置开箱即用，Spring Boot AutoConfiguration 自动装配
- 三种验证码类型：文字点选 / 滑动解锁 / 旋转验证
- 纯 Java AWT 绘制，无第三方图片库依赖
- 内存存储 + 定时清理过期数据，可自定义存储实现（如 Redis）
- 完善的配置属性，灵活调整各项参数
- 内置 Sample 示例工程，含完整前端交互页面

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.mingyang66</groupId>
    <artifactId>captcha-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 注入 Service

```java
@RestController
public class CaptchaController {

    private final ClickCaptchaService clickCaptchaService;   // 文字点选
    private final SliderCaptchaService sliderCaptchaService; // 滑动解锁
    private final RotateCaptchaService rotateCaptchaService; // 旋转验证

    public CaptchaController(ClickCaptchaService clickCaptchaService,
                             SliderCaptchaService sliderCaptchaService,
                             RotateCaptchaService rotateCaptchaService) {
        this.clickCaptchaService = clickCaptchaService;
        this.sliderCaptchaService = sliderCaptchaService;
        this.rotateCaptchaService = rotateCaptchaService;
    }
}
```

---

## 一、文字点选验证码

用户按照提示顺序点击图片中的指定汉字，验证点击坐标是否正确。

### API 接口

#### 生成验证码

```
GET /api/captcha/generate
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "captchaId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "image": "data:image/png;base64,iVBORw0KGgo...",
    "promptText": "请依次点击：明,月,光"
  }
}
```

| 字段 | 说明 |
|------|------|
| `captchaId` | 验证码唯一标识，校验时传入 |
| `image` | Base64 编码的图片（含 data URI 前缀），可直接作为 `<img src>` |
| `promptText` | 提示文本，告知用户需要按顺序点击哪些字 |

#### 校验验证码

```
POST /api/captcha/verify
Content-Type: application/json
```

请求体：

```json
{
  "captchaId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "clicks": [
    { "x": 120, "y": 80 },
    { "x": 200, "y": 60 },
    { "x": 260, "y": 90 }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `captchaId` | 生成时返回的验证码 ID |
| `clicks` | 用户点击坐标列表，**必须与提示顺序一致** |

响应示例：

```json
{ "code": 200, "message": "验证通过" }
{ "code": 400, "message": "验证失败，请重新点击" }
```

### 后端使用示例

```java
@GetMapping("/api/captcha/generate")
public Map<String, Object> generate() {
    ClickCaptcha captcha = clickCaptchaService.generate();
    Map<String, Object> data = new HashMap<>();
    data.put("captchaId", captcha.getCaptchaId());
    data.put("image", captcha.getImage());
    data.put("promptText", captcha.getPromptText());
    return Map.of("code", 200, "data", data);
}

@PostMapping("/api/captcha/verify")
public Map<String, Object> verify(@RequestBody VerifyRequest request) {
    boolean success = clickCaptchaService.verify(request.getCaptchaId(), request.getClicks());
    return Map.of("code", success ? 200 : 400,
                  "message", success ? "验证通过" : "验证失败");
}
```

### 配置项

```properties
# 图片尺寸
spring.emily.captcha.click.width=320
spring.emily.captcha.click.height=160
# 字符总数 & 需点击的目标数
spring.emily.captcha.click.char-count=5
spring.emily.captcha.click.target-count=3
# 字体大小范围
spring.emily.captcha.click.font-size-min=22
spring.emily.captcha.click.font-size-max=30
# 干扰元素
spring.emily.captcha.click.noise-line-count=6
spring.emily.captcha.click.noise-point-count=30
# 点击容差（像素）
spring.emily.captcha.click.tolerance=20
# 过期时间
spring.emily.captcha.click.expiry-time=PT120S
```

---

## 二、滑动解锁验证码

用户直接拖拽图片中的拼图块，将其移动到背景图上的缺口位置完成验证。

### API 接口

#### 生成验证码

```
GET /api/captcha/slider/generate
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "captchaId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "backgroundImage": "data:image/png;base64,...",
    "sliderImage": "data:image/png;base64,...",
    "y": 45
  }
}
```

| 字段 | 说明 |
|------|------|
| `captchaId` | 验证码唯一标识 |
| `backgroundImage` | 带缺口的背景图 Base64 |
| `sliderImage` | 拼图块图片 Base64 |
| `y` | 拼图块在图片上的 Y 坐标（自然像素），用于前端定位 |

#### 校验验证码

```
POST /api/captcha/slider/verify
Content-Type: application/json
```

请求体：

```json
{
  "captchaId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "x": 156
}
```

| 字段 | 说明 |
|------|------|
| `captchaId` | 生成时返回的验证码 ID |
| `x` | 拼图块最终停留位置的 X 坐标（**图片自然像素坐标**） |

> **坐标换算提示**：前端显示尺寸可能与图片自然尺寸不同，提交时需换算：
> ```javascript
> const scaleX = bgImg.naturalWidth / bgImg.clientWidth;
> const actualX = Math.round(pieceLeft * scaleX);
> ```

响应示例：

```json
{ "code": 200, "message": "验证通过" }
{ "code": 400, "message": "验证失败，请重新滑动" }
```

### 后端使用示例

```java
@GetMapping("/api/captcha/slider/generate")
public Map<String, Object> sliderGenerate() {
    SliderCaptcha captcha = sliderCaptchaService.generate();
    Map<String, Object> data = new HashMap<>();
    data.put("captchaId", captcha.getCaptchaId());
    data.put("backgroundImage", captcha.getBackgroundImage());
    data.put("sliderImage", captcha.getSliderImage());
    data.put("y", captcha.getY());
    return Map.of("code", 200, "data", data);
}

@PostMapping("/api/captcha/slider/verify")
public Map<String, Object> sliderVerify(@RequestBody SliderVerifyRequest request) {
    boolean success = sliderCaptchaService.verify(request.getCaptchaId(), request.getX());
    return Map.of("code", success ? 200 : 400,
                  "message", success ? "验证通过" : "验证失败");
}
```

### 配置项

```properties
# 背景图尺寸
spring.emily.captcha.slider.width=320
spring.emily.captcha.slider.height=160
# X 坐标容差（像素）
spring.emily.captcha.slider.tolerance=5
# 干扰元素
spring.emily.captcha.slider.noise-line-count=6
spring.emily.captcha.slider.noise-point-count=30
# 过期时间
spring.emily.captcha.slider.expiry-time=PT120S
```

---

## 三、旋转验证码

用户拖拽圆形图片外围的手柄进行旋转，将图片旋转到正确方向完成验证。

### API 接口

#### 生成验证码

```
GET /api/captcha/rotate/generate
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "captchaId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "image": "data:image/png;base64,..."
  }
}
```

| 字段 | 说明 |
|------|------|
| `captchaId` | 验证码唯一标识 |
| `image` | 已随机旋转的圆形图片 Base64，图片内含方向刻度盘和"上"箭头标识 |

#### 校验验证码

```
POST /api/captcha/rotate/verify
Content-Type: application/json
```

请求体：

```json
{
  "captchaId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "angle": 180
}
```

| 字段 | 说明 |
|------|------|
| `captchaId` | 生成时返回的验证码 ID |
| `angle` | 用户旋转的角度（顺时针，0~360） |

> **角度计算提示**：前端通过极坐标计算鼠标相对圆心的角度变化量：
> ```javascript
> function getMouseAngle(e, cx, cy) {
>     const dx = e.clientX - cx;
>     const dy = e.clientY - cy;
>     let angle = Math.atan2(dy, dx) * 180 / Math.PI + 90;
>     if (angle < 0) angle += 360;
>     return angle;
> }
> ```

响应示例：

```json
{ "code": 200, "message": "验证通过" }
{ "code": 400, "message": "验证失败，请重新旋转" }
```

### 后端使用示例

```java
@GetMapping("/api/captcha/rotate/generate")
public Map<String, Object> rotateGenerate() {
    RotateCaptcha captcha = rotateCaptchaService.generate();
    Map<String, Object> data = new HashMap<>();
    data.put("captchaId", captcha.getCaptchaId());
    data.put("image", captcha.getImage());
    return Map.of("code", 200, "data", data);
}

@PostMapping("/api/captcha/rotate/verify")
public Map<String, Object> rotateVerify(@RequestBody RotateVerifyRequest request) {
    boolean success = rotateCaptchaService.verify(request.getCaptchaId(), request.getAngle());
    return Map.of("code", success ? 200 : 400,
                  "message", success ? "验证通过" : "验证失败");
}
```

### 配置项

```properties
# 圆形图片尺寸（像素）
spring.emily.captcha.rotate.size=200
# 随机旋转角度范围
spring.emily.captcha.rotate.min-angle=30
spring.emily.captcha.rotate.max-angle=330
# 角度容差（度）
spring.emily.captcha.rotate.tolerance=10
# 干扰元素
spring.emily.captcha.rotate.noise-line-count=4
spring.emily.captcha.rotate.noise-point-count=20
# 过期时间
spring.emily.captcha.rotate.expiry-time=PT120S
```

---

## 全局配置

```properties
# 总开关（默认 true）
spring.emily.captcha.enabled=true
```

当 `enabled=false` 时，所有验证码 Bean 均不会注册。

## 自定义存储实现

默认使用内存 `ConcurrentHashMap` 存储验证码会话。如需替换为 Redis 等持久化方案，只需声明对应 StoreService 的 Bean：

```java
@Bean
public ClickStoreService clickStoreService() {
    return new RedisClickStoreServiceImpl(redisTemplate);
}

@Bean
public SliderStoreService sliderStoreService() {
    return new RedisSliderStoreServiceImpl(redisTemplate);
}

@Bean
public RotateStoreService rotateStoreService() {
    return new RedisRotateStoreServiceImpl(redisTemplate);
}
```

框架通过 `@ConditionalOnMissingBean` 检测，存在自定义实现时自动跳过默认内存实现。

## 模块结构

```
captcha-buld-parent
├── captcha-spring-boot-starter          # 核心组件
│   └── com.emily.captcha
│       ├── CaptchaAutoConfiguration     # 自动配置
│       ├── CaptchaProperties            # 配置属性
│       ├── click/                       # 文字点选验证码
│       │   ├── model/                   #   ClickCaptcha, ClickPoint
│       │   ├── service/                 #   ClickCaptchaService
│       │   └── store/                   #   ClickStoreService + 默认实现
│       ├── slider/                      # 滑动解锁验证码
│       │   ├── model/                   #   SliderCaptcha
│       │   ├── service/                 #   SliderCaptchaService
│       │   └── store/                   #   SliderStoreService + 默认实现
│       └── rotate/                      # 旋转验证码
│           ├── model/                   #   RotateCaptcha
│           ├── service/                 #   RotateCaptchaService
│           └── store/                   #   RotateStoreService + 默认实现
└── captcha-spring-boot-sample           # 使用示例
    ├── controller/CaptchaController     # REST 接口
    └── resources/static/
        ├── index.html                   # 文字点选前端
        ├── slider.html                  # 滑动解锁前端（拖拽拼图块）
        └── rotate.html                  # 旋转验证前端
```

## 运行示例

```bash
cd captcha-spring-boot-sample
mvn spring-boot:run
```

访问以下页面体验三种验证码：

- 文字点选：http://localhost:8080/index.html
- 滑动解锁：http://localhost:8080/slider.html
- 旋转验证：http://localhost:8080/rotate.html

## 环境要求

- Java 21+
- Spring Boot 4.x
