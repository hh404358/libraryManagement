package com.ljr;

import javax.swing.table.AbstractTableModel;
import java.sql.*;
import java.util.Vector;

public class ResultSetTableModel extends AbstractTableModel {
    private final Vector<String> columnNames;
    private final Vector<Vector<Object>> data;

    public ResultSetTableModel(ResultSet rs) {
        columnNames = new Vector<>();
        data = new Vector<>();

        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            // 获取列名
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(rsmd.getColumnName(i));
            }
            columnNames.add("IsBorrowed");

            // 获取数据
            while (rs.next()) {
                Vector<Object> row = new Vector<>(columnCount + 1);
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                row.add(isBookBorrowed(rs.getString("ISBN"))); // 添加借出状态列
                data.add(row);
            }

            rs.close();
            rs.getStatement().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isBookBorrowed(String isbn) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM Record WHERE ISBN = ? AND ReturnDate IS NULL";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, isbn);
            ResultSet rs = stmt.executeQuery();
            boolean isBorrowed = rs.next();
            rs.close();
            stmt.close();
            return isBorrowed;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex).get(columnIndex);
    }
}
