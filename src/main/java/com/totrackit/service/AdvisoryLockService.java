package com.totrackit.service;

import io.micronaut.data.connection.jdbc.advice.DelegatingDataSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL advisory locks for cross-replica coordination of scheduled scans.
 *
 * The session-level lock is held on a dedicated pooled connection for the
 * duration of the action (the action itself runs on other connections), and is
 * released explicitly; if the process dies mid-cycle, closing the connection
 * releases it server-side.
 */
@Singleton
public class AdvisoryLockService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryLockService.class);

    private final DataSource dataSource;

    @Inject
    public AdvisoryLockService(DataSource dataSource) {
        // Unwrap the transaction-aware proxy: the lock must live on its own
        // dedicated connection, outside any transaction scope.
        this.dataSource = DelegatingDataSource.unwrapDataSource(dataSource);
    }

    /**
     * Runs the action only if the advisory lock is free, so at most one
     * replica executes a given scan cycle at a time.
     *
     * @param key the cluster-wide lock identifier
     * @param action the work to run while holding the lock
     * @return true if the lock was acquired and the action ran
     */
    public boolean runExclusive(long key, Runnable action) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryLock(connection, key)) {
                return false;
            }
            try {
                action.run();
            } finally {
                unlock(connection, key);
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Advisory lock acquisition failed", e);
        }
    }

    private boolean tryLock(Connection connection, long key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            statement.setLong(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private void unlock(Connection connection, long key) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, key);
            statement.execute();
        } catch (SQLException e) {
            // The lock still dies with the connection when it is closed below.
            LOG.warn("Failed to release advisory lock {}", key, e);
        }
    }
}
