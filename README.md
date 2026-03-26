# Game Ma Sói (Werewolf) - Client-Server Application

## Tổng quan

Ứng dụng game Ma Sói dạng client-server được xây dựng bằng:
- **Java JDK 17**
- **Maven** (quản lý dependencies)
- **MySQL 8** (lưu trữ dữ liệu)
- **JavaFX** (Client UI)
- **TCP Socket** (giao tiếp client-server)
- **Jackson** (JSON serialization)
- **BCrypt** (hash mật khẩu)

## Kiến trúc tổng thể

```
werewolf-parent/
├── werewolf-shared/      # Common DTOs, Enums
├── werewolf-server/      # Server application
└── werewolf-client/      # Client application (JavaFX)
```

### Luồng dữ liệu

```
Client <--TCP Socket--> Server <--JDBC--> MySQL
         (JSON)              (SQL)
```

## Cài đặt và chạy

### 1. Yêu cầu hệ thống

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- IDE (IntelliJ IDEA / Eclipse)

### 2. Tạo Database

```bash
# Đăng nhập MySQL
mysql -u root -p

# Chạy script tạo database
mysql -u root -p < database/schema.sql
```

Hoặc import file `database/schema.sql` vào MySQL Workbench.

### 3. Cấu hình Database

Sửa file `werewolf-server/src/main/resources/database.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/werewolf_game?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=your_password_here
```

### 4. Build project

```bash
# Từ thư mục gốc
mvn clean install
```

### 5. Chạy Server

```bash
cd werewolf-server
mvn exec:java -Dexec.mainClass="com.werewolf.server.ServerMain"
```

Hoặc chạy trực tiếp class `ServerMain` từ IDE.

Server sẽ chạy trên port **8888**.

### 6. Chạy Client

```bash
cd werewolf-client
mvn exec:java -Dexec.mainClass="com.werewolf.client.ClientMain"
```

Hoặc chạy trực tiếp class `ClientMain` từ IDE.

## Thiết kế Database

### Các bảng chính

1. **users**: Thông tin người dùng
2. **rooms**: Thông tin phòng chơi
3. **room_members**: Người chơi trong phòng
4. **matches**: Lịch sử trận đấu
5. **match_players**: Vai trò người chơi trong trận
6. **chat_messages**: Tin nhắn chat (optional)
7. **event_logs**: Log sự kiện game (cho replay)

Xem chi tiết trong `database/schema.sql`.

## Message Protocol

### Client → Server

- `LOGIN_REQUEST`: Đăng nhập
- `REGISTER_REQUEST`: Đăng ký
- `CREATE_ROOM_REQUEST`: Tạo phòng
- `JOIN_ROOM_REQUEST`: Vào phòng
- `LEAVE_ROOM_REQUEST`: Rời phòng
- `READY_REQUEST`: Ready/Unready
- `START_GAME_REQUEST`: Bắt đầu game (host only)
- `GAME_ACTION_REQUEST`: Hành động trong game (kill/vote/seer_check)
- `CHAT_REQUEST`: Gửi tin nhắn
- `GET_ROOM_LIST_REQUEST`: Lấy danh sách phòng

### Server → Client

- `LOGIN_RESPONSE`: Kết quả đăng nhập
- `ERROR_RESPONSE`: Lỗi
- `ROOM_UPDATE`: Cập nhật thông tin phòng
- `GAME_STATE_UPDATE`: Cập nhật trạng thái game
- `CHAT_MESSAGE`: Tin nhắn chat
- `SYSTEM_MESSAGE`: Tin nhắn hệ thống
- `ROOM_LIST_RESPONSE`: Danh sách phòng

Tất cả message đều dùng JSON format, xem chi tiết trong package `com.werewolf.shared.dto`.

## State Machine của Game

### Các trạng thái (GamePhase)

1. **WAITING**: Chờ người chơi vào phòng và ready
2. **STARTING**: Game đang khởi động (3 giây)
3. **NIGHT_WOLF**: Đêm - Sói chọn giết người
4. **NIGHT_SEER**: Đêm - Tiên tri soi người
5. **DAY_ANNOUNCE**: Ngày - Công bố người chết (5 giây)
6. **DAY_CHAT**: Ngày - Thảo luận
7. **DAY_VOTE**: Ngày - Bỏ phiếu treo cổ
8. **ENDED**: Kết thúc

### Điều kiện chuyển trạng thái

- **WAITING → STARTING**: Host click Start, tất cả đã ready, >= 8 người
- **STARTING → NIGHT_WOLF**: Sau 3 giây
- **NIGHT_WOLF → NIGHT_SEER**: Hết giờ hoặc tất cả sói đã vote
- **NIGHT_SEER → DAY_ANNOUNCE**: Hết giờ hoặc tiên tri đã soi
- **DAY_ANNOUNCE → DAY_CHAT**: Sau 5 giây
- **DAY_CHAT → DAY_VOTE**: Hết giờ
- **DAY_VOTE → NIGHT_WOLF**: Sau khi vote xong (nếu chưa kết thúc)
- **DAY_VOTE → ENDED**: Khi có đội thắng

### Điều kiện thắng thua

- **Dân thắng**: Hết sói (số sói còn sống = 0)
- **Sói thắng**: Số sói >= số dân còn sống

## Cơ chế đồng bộ & Concurrency

### Giải pháp

1. **ReentrantLock** trong `GameEngine`: Mỗi room có một lock để đảm bảo thread-safe
2. **ConcurrentHashMap**: Sử dụng cho các collection được truy cập từ nhiều thread
3. **ScheduledExecutorService**: Quản lý timer cho các pha game
4. **ExecutorService**: Thread pool cho xử lý client connections

### Tránh race condition

- Tất cả thao tác trong `GameEngine` đều được bảo vệ bởi `lock`
- Vote được xử lý tuần tự trong critical section
- Game state chỉ được cập nhật bởi một thread tại một thời điểm

## Test Plan

### Unit Tests

- `GameEngineTest`: Test logic chia vai, điều kiện thắng thua
- Test vote mechanism
- Test role assignment

### Integration Tests (Optional)

- Test kết nối database
- Test end-to-end flow: đăng ký → tạo phòng → chơi game

Chạy test:
```bash
mvn test
```

## Demo Flow

### Kịch bản demo 8 người

1. **Tạo tài khoản**: 8 người đăng ký/đăng nhập
2. **Tạo phòng**: Host tạo phòng với 8 slots
3. **Vào phòng**: 7 người còn lại vào phòng
4. **Ready**: Tất cả click Ready
5. **Start game**: Host click Start
6. **Đêm**: 
   - Sói chọn giết người
   - Tiên tri soi người
7. **Ngày**: 
   - Công bố người chết
   - Thảo luận
   - Vote treo cổ
8. **Lặp lại** đêm/ngày cho đến khi có đội thắng
9. **Kết thúc**: Hiển thị đội thắng
10. **Xem lịch sử**: Query từ bảng `matches`

## Cấu trúc Code

### Server

```
werewolf-server/
├── config/          # DatabaseConfig
├── entity/          # User, Room, PlayerState
├── repository/      # UserRepository, RoomRepository, RoomMemberRepository
├── service/         # AuthService, RoomService, GameEngine
├── network/          # ClientHandler, ServerMain
└── resources/       # database.properties
```

### Client

```
werewolf-client/
├── network/         # NetworkClient
└── ui/              # JavaFX controllers (TODO)
```

### Shared

```
werewolf-shared/
├── enums/           # Role, GamePhase, ErrorCode, etc.
└── dto/             # Message classes
```

## Tính năng đã triển khai

### MVP (Bắt buộc)

- ✅ Đăng ký, đăng nhập, đăng xuất
- ✅ Tạo phòng, vào phòng, rời phòng
- ✅ Ready/Unready
- ✅ Start game (host only)
- ✅ Chia vai trò ngẫu nhiên (Dân, Sói, Tiên Tri)
- ✅ Luồng game: Đêm → Ngày → Vote
- ✅ Logic thắng thua
- ✅ Chat cơ bản
- ✅ Lưu database (users, rooms, room_members)

### Tính năng nâng cao (Optional)

- ⏳ Thêm vai: Bảo vệ, Phù thủy
- ⏳ Dead chat
- ⏳ Reconnect
- ⏳ Bảng xếp hạng
- ⏳ Replay từ EventLog

## Lưu ý

1. **JavaFX UI**: Hiện tại chỉ có skeleton, cần implement UI hoặc dùng console mode
2. **Broadcast**: Chat và room updates chưa được broadcast đến tất cả clients (cần implement)
3. **Match History**: Chưa lưu match vào DB sau khi kết thúc (TODO trong GameEngine.endGame)
4. **Error Handling**: Cần cải thiện error handling và validation

## Tác giả

Đồ án môn học - Game Ma Sói Client-Server

## License

MIT
