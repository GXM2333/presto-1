/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.plugin.jdbc;

import io.prestosql.spi.connector.AggregateFunction;
import io.prestosql.spi.connector.ColumnHandle;
import io.prestosql.spi.connector.ColumnMetadata;
import io.prestosql.spi.connector.ConnectorSession;
import io.prestosql.spi.connector.ConnectorSplitSource;
import io.prestosql.spi.connector.ConnectorTableMetadata;
import io.prestosql.spi.connector.SchemaTableName;
import io.prestosql.spi.connector.SystemTable;
import io.prestosql.spi.predicate.TupleDomain;
import io.prestosql.spi.statistics.TableStatistics;
import io.prestosql.spi.type.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface JdbcClient
{
    default boolean schemaExists(JdbcIdentity identity, String schema)
    {
        return getSchemaNames(identity).contains(schema);
    }

    Set<String> getSchemaNames(JdbcIdentity identity);

    List<SchemaTableName> getTableNames(JdbcIdentity identity, Optional<String> schema);

    Optional<JdbcTableHandle> getTableHandle(JdbcIdentity identity, SchemaTableName schemaTableName);

    List<JdbcColumnHandle> getColumns(ConnectorSession session, JdbcTableHandle tableHandle);

    Optional<ColumnMapping> toPrestoType(ConnectorSession session, Connection connection, JdbcTypeHandle typeHandle);

    /**
     * Bulk variant of {@link #toPrestoType(ConnectorSession, Connection, JdbcTypeHandle)}.
     */
    List<ColumnMapping> getColumnMappings(ConnectorSession session, List<JdbcTypeHandle> typeHandles);

    WriteMapping toWriteMapping(ConnectorSession session, Type type);

    default boolean supportsGroupingSets()
    {
        return true;
    }

    default Optional<JdbcExpression> implementAggregation(ConnectorSession session, AggregateFunction aggregate, Map<String, ColumnHandle> assignments)
    {
        return Optional.empty();
    }

    ConnectorSplitSource getSplits(ConnectorSession session, JdbcTableHandle tableHandle);

    Connection getConnection(JdbcIdentity identity, JdbcSplit split)
            throws SQLException;

    default void abortReadConnection(Connection connection)
            throws SQLException
    {
        // most drivers do not need this
    }

    PreparedStatement buildSql(ConnectorSession session, Connection connection, JdbcSplit split, JdbcTableHandle table, List<JdbcColumnHandle> columns)
            throws SQLException;

    boolean supportsLimit();

    boolean isLimitGuaranteed(ConnectorSession session);

    void addColumn(ConnectorSession session, JdbcTableHandle handle, ColumnMetadata column);

    void dropColumn(JdbcIdentity identity, JdbcTableHandle handle, JdbcColumnHandle column);

    void renameColumn(JdbcIdentity identity, JdbcTableHandle handle, JdbcColumnHandle jdbcColumn, String newColumnName);

    void renameTable(JdbcIdentity identity, JdbcTableHandle handle, SchemaTableName newTableName);

    void createTable(ConnectorSession session, ConnectorTableMetadata tableMetadata);

    JdbcOutputTableHandle beginCreateTable(ConnectorSession session, ConnectorTableMetadata tableMetadata);

    void commitCreateTable(JdbcIdentity identity, JdbcOutputTableHandle handle);

    JdbcOutputTableHandle beginInsertTable(ConnectorSession session, JdbcTableHandle tableHandle, List<JdbcColumnHandle> columns);

    void finishInsertTable(JdbcIdentity identity, JdbcOutputTableHandle handle);

    void dropTable(JdbcIdentity identity, JdbcTableHandle jdbcTableHandle);

    void rollbackCreateTable(JdbcIdentity identity, JdbcOutputTableHandle handle);

    String buildInsertSql(JdbcOutputTableHandle handle);

    Connection getConnection(JdbcIdentity identity, JdbcOutputTableHandle handle)
            throws SQLException;

    PreparedStatement getPreparedStatement(Connection connection, String sql)
            throws SQLException;

    TableStatistics getTableStatistics(ConnectorSession session, JdbcTableHandle handle, TupleDomain<ColumnHandle> tupleDomain);

    void createSchema(JdbcIdentity identity, String schemaName);

    void dropSchema(JdbcIdentity identity, String schemaName);

    default Optional<SystemTable> getSystemTable(ConnectorSession session, SchemaTableName tableName)
    {
        return Optional.empty();
    }

    String quoted(String name);

    String quoted(RemoteTableName remoteTableName);
}
