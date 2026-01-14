# Core Algo (EVRPTW / ALNS Solver)

Đây là engine chính xử lý bài toán **EVRPTW** dùng thuật toán **ALNS (Adaptive Large Neighborhood Search)**. Code bằng **Java 21**, build bằng **Maven**.

## Build

Chạy lệnh này để đóng gói thành file JAR executable (bỏ qua test cho nhanh):

```bash
mvn package -q -DskipTests
```

File sau khi build sẽ nằm ở: `target/EVRPTW_ALNS-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Usage

Chạy tool qua command line (CLI).

**Basic command:**

```bash
java -jar target/EVRPTW_ALNS-1.0-SNAPSHOT-jar-with-dependencies.jar <instance_file_path> [options]
```

**Ví dụ chạy thử:**

```bash
# Chạy 1000 iteration, timeout 60s, có xuất ảnh plot lộ trình, chế độ đổi pin (swap)
java -jar target/EVRPTW_ALNS-1.0-SNAPSHOT-jar-with-dependencies.jar ./src/main/resources/data/r201_21.txt -i 1000 -t 60 -p -cm BATTERY_SWAP
```

## Options & Parameters

Full list mấy cái flag để config thuật toán:

| Option | Short | Default | Mô tả |
| :--- | :---: | :---: | :--- |
| `--iterations <n>` | `-i` | `5000` | Số vòng lặp (iterations) của ALNS. Chỉnh càng to chạy càng lâu nhưng output càng ngon. |
| `--time <seconds>` | `-t` | `0` | Time limit (giây). Để `0` là chạy hết iteration mới dừng. |
| `--output-dir <path>` | `-o` | `solutions` | Folder ném file kết quả ra. |
| `--plot` | `-p` | `false` | Có vẽ biểu đồ (lưu dạng PNG) hay không. Rất tiện để visualize route. |
| `--charging-mode <mode>` | `-cm` | `FULL_RECHARGE` | Chế độ sạc. Hỗ trợ: `FULL_RECHARGE` (sạc đầy) hoặc `BATTERY_SWAP` (đổi pin). |
| `--swap-time <minutes>` | `-st` | `2.0` | Thời gian swap pin (tính bằng phút). Chỉ có tác dụng khi mode là `BATTERY_SWAP`. |
| `--no-verify` | | `false` | Bỏ qua bước verify kết quả (nếu không cần check lại tính hợp lệ). |
| `--verifier <path>` | | `null` | Đường dẫn file JAR verifier bên ngoài (nếu muốn dùng tool check riêng). |
| `--log-level <level>` | | `INFO` | Level log in ra console: `DEBUG`, `INFO`, `WARNING`, `ERROR`. |
| `--help` | `-h` | | Hiện bảng help này. |