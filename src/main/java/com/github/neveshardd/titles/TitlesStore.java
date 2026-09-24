package com.github.neveshardd.titles;

import com.github.neveshardd.api.database.DatabaseAPI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public final class TitlesStore {

    private final DatabaseAPI database;

    public TitlesStore(DatabaseAPI database) {
        this.database = database;
        createSchema();
    }

    public Optional<String> loadTitle(UUID uuid) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT title_id FROM titles_players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString("title_id")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao carregar titulo do jogador", e);
        }
    }

    public void saveTitle(UUID uuid, String titleId) {
        String player = uuid.toString();
        String update = "UPDATE titles_players SET title_id = ? WHERE uuid = ?";
        try (Connection connection = database.getConnection()) {
            if (execute(connection, update, titleId, player)) {
                return;
            }
            try {
                execute(connection, "INSERT INTO titles_players (uuid, title_id) VALUES (?, ?)", player, titleId);
            } catch (SQLException duplicate) {
                if (!execute(connection, update, titleId, player)) {
                    throw duplicate;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao salvar titulo do jogador", e);
        }
    }

    private void createSchema() {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS titles_players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        title_id VARCHAR(64) NOT NULL
                    )""");
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao criar schema de titulos", e);
        }
    }

    private boolean execute(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate() > 0;
        }
    }
}
