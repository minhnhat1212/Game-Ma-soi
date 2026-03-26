package com.werewolf.server.service;

import com.werewolf.server.entity.User;
import com.werewolf.server.repository.UserRepository;
import com.werewolf.shared.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service xử lý authentication
 */
public class AuthService {
    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    /**
     * Đăng ký user mới
     */
    public User register(String username, String password, String displayName) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("Username đã tồn tại");
        }

        // Hash password bằng BCrypt
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        // Tạo user mới
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setOnline(false);

        return userRepository.create(user);
    }

    /**
     * Đăng nhập
     */
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu");
        }

        // Kiểm tra password
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu");
        }

        // Cập nhật trạng thái online
        user.setOnline(true);
        userRepository.updateOnlineStatus(user.getId(), true);

        return user;
    }

    /**
     * Đăng xuất
     */
    public void logout(int userId) {
        userRepository.updateOnlineStatus(userId, false);
    }

    /**
     * Convert User entity sang UserDTO
     */
    public UserDTO toDTO(User user) {
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getAvatar(),
            user.isOnline(),
            user.getLevel(),
            calculateRank(user.getLevel()),
            user.getExperience(),
            user.getCoins(),
            user.getGamesPlayed(),
            user.getGamesWon()
        );
    }

    /**
     * Refresh progression hiện tại từ DB.
     */
    public UserDTO getUserDTOById(int userId) {
        User user = userRepository.findById(userId);
        if (user == null) return null;
        return toDTO(user);
    }

    private String calculateRank(int level) {
        if (level >= 20) return "Legend";
        if (level >= 15) return "Master";
        if (level >= 10) return "Diamond";
        if (level >= 7) return "Gold";
        if (level >= 4) return "Silver";
        return "Bronze";
    }
}
