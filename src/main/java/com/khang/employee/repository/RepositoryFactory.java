package com.khang.employee.repository;

import com.khang.employee.annotations.KColumn;
import com.khang.employee.annotations.KId;
import com.khang.employee.annotations.KTable;
import com.khang.employee.annotations.KTransient;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class RepositoryFactory<K, ID> implements KhangCrudRepository<K, ID> {
    private final Class<K> entityClass;
    private final String tableName;
    private final String idColumnName;
    private final Connection connection;

    public RepositoryFactory(Class<K> entityClass, String dbUrl, String dbUser, String dbPass) throws SQLException {
        this.entityClass = (Class<K>) entityClass;
        this.connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);
        this.tableName = getTableName();
        this.idColumnName = getIdColumnName();
    }

    private String getTableName() {
        KTable tableAnnotation = entityClass.getAnnotation(KTable.class);
        if (tableAnnotation == null) {
            throw new IllegalStateException("Entity must be annotated with @KTable");
        }
        String name = tableAnnotation.name();
        return name.isEmpty() ? entityClass.getSimpleName().toLowerCase() : name;
    }

    private String getIdColumnName() {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(KId.class)) {
                KColumn column = field.getAnnotation(KColumn.class);
                field.setAccessible(true);
                return column != null && !column.name().isEmpty() ? column.name() : field.getName();
            }
        }
        throw new IllegalStateException("Entity must have a field annotated with @KId");
    }

    private List<Field> getMappedFields() {
        List<Field> fields = new ArrayList<Field>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(KTransient.class) && field.isAnnotationPresent(KColumn.class)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    @Override
    public K save(K entity) {
        List<Field> fields = getMappedFields();
        StringBuilder columns = new StringBuilder();
        StringBuilder valuesPlaceholder = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Field field : fields) {
            try {
                Object value = field.get(entity);
                if (value != null) {
                    KColumn column = field.getAnnotation(KColumn.class);
                    String columnName = column != null && !column.name().isEmpty() ? column.name() : field.getName();
                    columns.append(columnName).append(",");
                    valuesPlaceholder.append("?,");
                    values.add(value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field " + field.getName(), e);
            }
        }

        if (columns.length() == 0) {
            throw new IllegalStateException("No non-null fields to save for entity");
        }

        columns.setLength(columns.length() - 1);
        valuesPlaceholder.setLength(valuesPlaceholder.length() - 1);

        String sql = String.format("MERGE INTO %s (%s) KEY(%s) VALUES (%s)",
                tableName, columns, idColumnName, valuesPlaceholder);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }
            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    @Override
    public Optional<K> findById(ID id) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumnName);
        try (PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                K entity = entityClass.getDeclaredConstructor().newInstance();
                for (Field field : getMappedFields()) {
                    KColumn column = field.getAnnotation(KColumn.class);
                    String columnName = column != null && !column.name().isEmpty() ? column.name() : field.getName();
                    field.set(entity, rs.getObject(columnName));
                }
                return Optional.of(entity);
            }
            return Optional.empty();
        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("Failed to find entity by ID", e);
        }
    }

    @Override
    public List<K> findAll() {
        List<K> entities = new ArrayList<>();
        String sql = String.format("SELECT * FROM %s", tableName);
        try (PreparedStatement stmt = connection.prepareStatement(sql)){
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                K entity = entityClass.getDeclaredConstructor().newInstance();
                for (Field field : getMappedFields()) {
                    KColumn column = field.getAnnotation(KColumn.class);
                    String columnName = column != null && !column.name().isEmpty() ? column.name() : field.getName();
                    field.set(entity, rs.getObject(columnName));
                }
                entities.add(entity);
            }
            return entities;
        }
        catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("Failed to find entities", e);
        }
    }

    @Override
    public void deleteById(ID id) {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", tableName, idColumnName);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete entity by ID", e);
        }
    }

    @Override
    public long count() {
        String sql = String.format("SELECT COUNT(*) FROM %s ", tableName);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count entities", e);
        }
    }
}

