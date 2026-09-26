package com.mparsict.vosolmaliat.common.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;

import java.util.List;
import java.util.stream.Collectors;

/** SQLite requires foreign keys inside CREATE TABLE; ALTER TABLE ADD CONSTRAINT is unsupported. */
public class RelationalSqliteDialect extends SQLiteDialect {
    private final Exporter<Table> tables = new StandardTableExporter(this) {
        @Override
        protected void applyTableCheck(Table table, StringBuilder sql) {
            super.applyTableCheck(table, sql);
            for (var foreignKey : table.getForeignKeys().values()) {
                if (!foreignKey.isCreationEnabled() || !foreignKey.isPhysicalConstraint()) continue;
                var referencedTable = foreignKey.getReferencedTable();
                var referencedColumns = foreignKey.isReferenceToPrimaryKey()
                        ? referencedTable.getPrimaryKey().getColumns() : foreignKey.getReferencedColumns();
                sql.append(", foreign key (").append(columns(foreignKey.getColumns()))
                        .append(") references ").append(referencedTable.getQuotedName(dialect))
                        .append(" (").append(columns(referencedColumns)).append(')');
                if (foreignKey.isCascadeDeleteEnabled()) sql.append(" on delete cascade");
            }
        }

        private String columns(List<Column> columns) {
            return columns.stream().map(column -> column.getQuotedName(dialect)).collect(Collectors.joining(", "));
        }
    };

    @Override
    public Exporter<Table> getTableExporter() {
        return tables;
    }
}
