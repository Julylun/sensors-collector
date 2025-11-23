# Sensor Collector App

Ứng dụng Android thu thập dữ liệu cảm biến với Material 3 UI.

## Yêu cầu

- Android SDK 29 trở lên (Android 10+)
- Kotlin
- Material 3

## Tính năng

- Thu thập dữ liệu từ 4 cảm biến:
  - Accelerometer
  - Gyroscope
  - Ambient Light Sensor
  - Proximity Sensor

- Bật/tắt từng cảm biến riêng lẻ
- Ghi dữ liệu với countdown timer
- Lưu dữ liệu dưới dạng JSON theo từng loại (fall, jump, walk, running, idle, shake, other)
- Chia sẻ dữ liệu qua ZIP file
- Xóa tất cả dữ liệu đã lưu

## Cấu trúc dự án

```
sensor-collector-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/sensorcollector/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SensorApplication.kt
│   │   │   ├── ui/
│   │   │   │   ├── MainScreen.kt
│   │   │   │   ├── SaveDialog.kt
│   │   │   │   ├── ViewModel.kt
│   │   │   │   └── components/
│   │   │   ├── sensor/
│   │   │   │   ├── SensorManager.kt
│   │   │   │   └── SensorData.kt
│   │   │   ├── data/
│   │   │   │   ├── SensorRepository.kt
│   │   │   │   └── FileManager.kt
│   │   │   └── utils/
│   │   │       ├── DateTimeFormatter.kt
│   │   │       └── ZipHelper.kt
```

## Build và chạy

### Yêu cầu
- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 11 trở lên
- Android SDK với API level 29-34

### Các bước build

1. **Mở project trong Android Studio**
   - File → Open → Chọn thư mục `sensor-collector-app`

2. **Sync Gradle**
   - Android Studio sẽ tự động sync hoặc click "Sync Now" nếu có thông báo
   - Gradle sẽ tự động download dependencies và tạo wrapper nếu cần

3. **Tạo icon launcher (tùy chọn)**
   - Right-click `app/src/main/res` → New → Image Asset
   - Hoặc sử dụng icon mặc định của Android Studio

4. **Build project**
   - Build → Make Project (Ctrl+F9 / Cmd+F9)
   - Hoặc Build → Build Bundle(s) / APK(s) → Build APK(s)

5. **Chạy trên thiết bị**
   - Kết nối thiết bị Android qua USB (bật USB debugging)
   - Click Run (Shift+F10 / Ctrl+R)
   - **Lưu ý**: Khuyến nghị chạy trên thiết bị thật vì emulator có thể không hỗ trợ đầy đủ cảm biến

## Lưu ý

- Dữ liệu được lưu trong thư mục `app_data/sensor_data/{type}/`
- Tên file: `sensors_phút-giờ_ngày-tháng-năm-n.json`
- Các cảm biến không khả dụng hoặc bị tắt sẽ có giá trị `null` trong JSON

