package org.vatplanner.importer.postgis.status.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A cache reading a set of ID and name pairs from database treated as an
 * enumeration mapped to a Java enum.
 *
 * <p>
 * Mappings are expected to be static and thus are read in advance upon
 * instantiation. A cache miss is not tolerated resulting in
 * {@link IllegalArgumentException}s, hence this is a "strict" cache.
 * </p>
 *
 * @param <T> Java enum to map names from/to
 */
public class StrictEnumCache<T extends Enum> {

    private final Map<T, Integer> idByEnum = new HashMap<>();
    private final Map<Integer, T> enumById = new HashMap<>();

    private final Function<String, T> mappingNameToEnum;

    /**
     * Initializes the cache by immediately running the given query.
     *
     * @param db database connection
     * @param sql query to read id (column 1) and enumeration name (column 2)
     * @param mappingNameToEnum maps an enumeration name read from database to a
     * Java enum
     * @throws SQLException if query fails
     * @throws RuntimeException if data is inconsistent (expecting unique
     * mapping to both sides)
     */
    public StrictEnumCache(Connection db, String sql, Function<String, T> mappingNameToEnum) throws SQLException {
        this.mappingNameToEnum = mappingNameToEnum;
        readFromDatabase(db, sql);
    }

    private void readFromDatabase(Connection db, String sql) throws SQLException {
        try (
                Statement stmt = db.createStatement();
                ResultSet rs = stmt.executeQuery(sql);) {

            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);

                T enumValue = mappingNameToEnum.apply(name);
                if (enumValue == null) {
                    throw new RuntimeException("database name \"" + name + "\" could not be resolved to a Java enum");
                }

                if (idByEnum.containsKey(enumValue)) {
                    throw new RuntimeException("database name \"" + name + "\" resolved to ambiguous Java enum " + enumValue + ", already recorded with ID " + id);
                }

                if (enumById.containsKey(id)) {
                    throw new RuntimeException("database name \"" + name + "\" has ambiguous ID, already recorded " + enumById.get(id));
                }

                idByEnum.put(enumValue, id);
                enumById.put(id, enumValue);
            }
        }
    }

    /**
     * Returns the database ID given enum is known with.
     *
     * @param enumValue Java enum to retrieve ID for
     * @return ID of database equivalent to Java enum
     * @throws IllegalArgumentException if enum is unknown to database
     */
    public int getId(T enumValue) {
        Integer id = idByEnum.get(enumValue);

        if (id == null) {
            throw new IllegalArgumentException("Java enum " + enumValue + " does not exist in database");
        }

        return id;
    }

    /**
     * Returns the Java enum matching the given database ID.
     *
     * @param id database enumeration ID to resolve
     * @return Java enum matching given database ID
     * @throws IllegalArgumentException if database ID is unknown
     */
    public T getEnum(int id) {
        T enumValue = enumById.get(id);

        if (enumValue == null) {
            throw new IllegalArgumentException("ID " + id + " is unknown");
        }

        return enumValue;
    }

}
