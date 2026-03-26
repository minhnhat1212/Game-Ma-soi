package com.werewolf.server.service;

import com.werewolf.server.entity.PlayerState;
import com.werewolf.server.entity.Room;
import com.werewolf.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho GameEngine
 */
public class GameEngineTest {
    private Room room;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        room = new Room();
        room.setId(1);
        room.setName("Test Room");
        room.setMaxPlayers(8);
        room.setPhaseDurationSeconds(60);
        room.setStatus("WAITING");
        room.setHostUserId(1);

        // Thêm 8 players
        for (int i = 1; i <= 8; i++) {
            PlayerState player = new PlayerState(i, "Player " + i);
            player.setReady(true);
            room.getPlayers().put(i, player);
        }

        new GameEngine(room, roomService);
    }

    @Test
    void testAssignRoles() {
        roomService.assignRoles(room);
        
        int werewolfCount = 0;
        int seerCount = 0;
        int villagerCount = 0;

        for (PlayerState player : room.getPlayers().values()) {
            Role role = player.getRole();
            if (role == Role.WEREWOLF) werewolfCount++;
            else if (role == Role.SEER) seerCount++;
            else if (role == Role.VILLAGER) villagerCount++;
        }

        assertEquals(1, seerCount, "Phải có 1 tiên tri");
        assertTrue(werewolfCount >= 1, "Phải có ít nhất 1 sói");
        assertEquals(8, werewolfCount + seerCount + villagerCount, "Tổng số vai trò phải bằng số người chơi");
    }

    @Test
    void testWinCondition_VillagersWin() {
        // Setup: chỉ còn dân và tiên tri
        for (PlayerState player : room.getPlayers().values()) {
            if (player.getRole() == Role.WEREWOLF) {
                player.setAlive(false);
            }
        }

        // Kiểm tra điều kiện thắng
        int aliveWerewolves = 0;
        int aliveVillagers = 0;
        for (PlayerState player : room.getPlayers().values()) {
            if (player.isAlive()) {
                if (player.getRole() == Role.WEREWOLF) {
                    aliveWerewolves++;
                } else {
                    aliveVillagers++;
                }
            }
        }

        assertEquals(0, aliveWerewolves);
        assertTrue(aliveVillagers > 0);
    }

    @Test
    void testWinCondition_WerewolvesWin() {
        // Setup: số sói >= số dân
        int werewolfCount = 0;
        int villagerCount = 0;
        for (PlayerState player : room.getPlayers().values()) {
            if (player.getRole() == Role.WEREWOLF) {
                werewolfCount++;
            } else {
                villagerCount++;
            }
        }

        // Giết một số dân để sói >= dân
        int killsNeeded = villagerCount - werewolfCount + 1;
        int killed = 0;
        for (PlayerState player : room.getPlayers().values()) {
            if (player.getRole() != Role.WEREWOLF && killed < killsNeeded) {
                player.setAlive(false);
                killed++;
            }
        }

        int aliveWerewolves = 0;
        int aliveVillagers = 0;
        for (PlayerState player : room.getPlayers().values()) {
            if (player.isAlive()) {
                if (player.getRole() == Role.WEREWOLF) {
                    aliveWerewolves++;
                } else {
                    aliveVillagers++;
                }
            }
        }

        assertTrue(aliveWerewolves >= aliveVillagers);
    }
}
