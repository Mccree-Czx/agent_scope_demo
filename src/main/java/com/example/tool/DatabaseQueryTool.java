package com.example.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.sql.*;

public class DatabaseQueryTool {

    private final String jdbcUrl, username, password;

    public DatabaseQueryTool(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl; this.username = username; this.password = password;
    }

    @Tool(
        name = "query_database",
        description = "执行 SQL SELECT 查询并返回结果集。用于查询用户信息、订单数据、产品信息等。仅支持 SELECT。",
        readOnly = true
    )
    public String queryDatabase(
            @ToolParam(name = "sql", description = "SQL 查询语句，如 SELECT * FROM orders WHERE user_id = 1001")
            String sql
    ) {
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            return "错误：只允许 SELECT 查询";
        }

        StringBuilder result = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) result.append(" | ");
                result.append(meta.getColumnName(i));
            }
            result.append("\n").append("-".repeat(40)).append("\n");

            int rowCount = 0;
            while (rs.next() && rowCount < 20) {
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) result.append(" | ");
                    result.append(rs.getString(i));
                }
                result.append("\n"); rowCount++;
            }
            result.append("共 ").append(rowCount).append(" 行");
        } catch (SQLException e) {
            return "查询失败: " + e.getMessage();
        }
        return result.toString();
    }


    @Tool(
            name ="excute_update",
            description = "执行数据更新操作（UPDATE/DELETE/INSERT）。需要人工审批。"
    )
    public String executeUpdate(@ToolParam(name = "sql",description = "sql更新语句") String sql){
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            int affected = stmt.executeUpdate(sql);
            return "执行成功，影响 " + affected + " 行";
        } catch (SQLException e) {
            return "执行失败: " + e.getMessage();
        }

    }
}

