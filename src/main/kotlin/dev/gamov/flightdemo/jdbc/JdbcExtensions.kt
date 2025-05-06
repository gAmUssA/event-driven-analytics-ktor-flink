package dev.gamov.flightdemo.jdbc

import java.sql.*
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A lightweight, type-safe Kotlin JDBC extension library that provides a concise and idiomatic API
 * for working with JDBC in Kotlin applications.
 */

// ===== Connection Extensions =====

/**
 * Executes the given [block] function with this connection and returns the result.
 * The connection is automatically closed after the block is executed.
 */
inline fun <T> Connection.use(block: (Connection) -> T): T {
    return try {
        block(this)
    } finally {
        try {
            close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

/**
 * Executes the given SQL [statement] and returns the result.
 */
inline fun <T> Connection.execute(statement: String, block: (Statement) -> T): T {
    return createStatement().use { stmt ->
        block(stmt)
    }
}

/**
 * Executes the given SQL [statement] with [parameters] and returns the result.
 */
inline fun <T> Connection.execute(statement: String, vararg parameters: Any?, block: (PreparedStatement) -> T): T {
    return prepareStatement(statement).use { stmt ->
        parameters.forEachIndexed { index, param ->
            stmt.setParameter(index + 1, param)
        }
        block(stmt)
    }
}

/**
 * Executes the given SQL [statement] and returns the generated keys.
 */
inline fun <T> Connection.executeWithKeys(statement: String, vararg parameters: Any?, block: (ResultSet) -> T): T {
    return prepareStatement(statement, Statement.RETURN_GENERATED_KEYS).use { stmt ->
        parameters.forEachIndexed { index, param ->
            stmt.setParameter(index + 1, param)
        }
        stmt.executeUpdate()
        stmt.generatedKeys.use(block)
    }
}

/**
 * Executes the given SQL [statement] with [parameters] and returns the number of affected rows.
 */
fun Connection.update(statement: String, vararg parameters: Any?): Int {
    return execute(statement, *parameters) { stmt ->
        stmt.executeUpdate()
    }
}

/**
 * Executes the given SQL [statement] with [parameters] and returns the result set.
 */
inline fun <T> Connection.query(statement: String, vararg parameters: Any?, block: (ResultSet) -> T): T {
    return execute(statement, *parameters) { stmt ->
        stmt.executeQuery().use(block)
    }
}

/**
 * Executes the given SQL [statement] with [parameters] and maps the result set to a list of objects.
 */
inline fun <T> Connection.queryList(statement: String, vararg parameters: Any?, mapper: (ResultSet) -> T): List<T> {
    return query(statement, *parameters) { rs ->
        buildList {
            while (rs.next()) {
                add(mapper(rs))
            }
        }
    }
}

/**
 * Executes the given SQL [statement] with [parameters] and maps the result set to a single object or null.
 */
inline fun <T> Connection.querySingle(statement: String, vararg parameters: Any?, mapper: (ResultSet) -> T): T? {
    return query(statement, *parameters) { rs ->
        if (rs.next()) mapper(rs) else null
    }
}

/**
 * Executes the given [block] in a transaction and returns the result.
 * The transaction is automatically committed if the block executes successfully,
 * or rolled back if an exception is thrown.
 */
inline fun <T> Connection.transaction(block: (Connection) -> T): T {
    val autoCommit = autoCommit
    try {
        autoCommit = false
        val result = block(this)
        commit()
        return result
    } catch (e: Exception) {
        try {
            rollback()
        } catch (rollbackEx: Exception) {
            e.addSuppressed(rollbackEx)
        }
        throw e
    } finally {
        try {
            autoCommit = autoCommit
        } catch (e: Exception) {
            // Ignore
        }
    }
}

// ===== PreparedStatement Extensions =====

/**
 * Sets a parameter at the given [index] to the given [value].
 */
fun PreparedStatement.setParameter(index: Int, value: Any?) {
    when (value) {
        null -> setNull(index, Types.NULL)
        is String -> setString(index, value)
        is Int -> setInt(index, value)
        is Long -> setLong(index, value)
        is Double -> setDouble(index, value)
        is Float -> setFloat(index, value)
        is Boolean -> setBoolean(index, value)
        is LocalDateTime -> setTimestamp(index, Timestamp.valueOf(value))
        is java.util.Date -> setTimestamp(index, Timestamp(value.time))
        is ByteArray -> setBytes(index, value)
        else -> setObject(index, value)
    }
}

// ===== ResultSet Extensions =====

/**
 * Gets a value from the result set by column name.
 */
inline fun <reified T> ResultSet.get(columnName: String): T? {
    return getTyped(columnName, T::class)
}

/**
 * Gets a value from the result set by column index.
 */
inline fun <reified T> ResultSet.get(columnIndex: Int): T? {
    return getTyped(columnIndex, T::class)
}

/**
 * Gets a typed value from the result set by column name.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> ResultSet.getTyped(columnName: String, type: KClass<T>): T? {
    val value = when (type) {
        String::class -> getString(columnName)
        Int::class -> getInt(columnName).takeUnless { wasNull() }
        Long::class -> getLong(columnName).takeUnless { wasNull() }
        Double::class -> getDouble(columnName).takeUnless { wasNull() }
        Float::class -> getFloat(columnName).takeUnless { wasNull() }
        Boolean::class -> getBoolean(columnName).takeUnless { wasNull() }
        LocalDateTime::class -> getTimestamp(columnName)?.toLocalDateTime()
        java.util.Date::class -> getTimestamp(columnName)?.let { java.util.Date(it.time) }
        ByteArray::class -> getBytes(columnName)
        else -> getObject(columnName, type.java)
    }
    return value as T?
}

/**
 * Gets a typed value from the result set by column index.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> ResultSet.getTyped(columnIndex: Int, type: KClass<T>): T? {
    val value = when (type) {
        String::class -> getString(columnIndex)
        Int::class -> getInt(columnIndex).takeUnless { wasNull() }
        Long::class -> getLong(columnIndex).takeUnless { wasNull() }
        Double::class -> getDouble(columnIndex).takeUnless { wasNull() }
        Float::class -> getFloat(columnIndex).takeUnless { wasNull() }
        Boolean::class -> getBoolean(columnIndex).takeUnless { wasNull() }
        LocalDateTime::class -> getTimestamp(columnIndex)?.toLocalDateTime()
        java.util.Date::class -> getTimestamp(columnIndex)?.let { java.util.Date(it.time) }
        ByteArray::class -> getBytes(columnIndex)
        else -> getObject(columnIndex, type.java)
    }
    return value as T?
}

// ===== Table and Column Definitions =====

/**
 * Represents a database table.
 */
abstract class Table(val name: String) {
    val columns = mutableListOf<Column<*>>()
    var primaryKey: PrimaryKey? = null
    
    fun <T> column(name: String, type: ColumnType<T>): Column<T> {
        val column = Column(name, type, this)
        columns.add(column)
        return column
    }
    
    fun varchar(name: String, length: Int): Column<String> = column(name, VarcharType(length))
    fun integer(name: String): Column<Int> = column(name, IntegerType)
    fun long(name: String): Column<Long> = column(name, LongType)
    fun double(name: String): Column<Double> = column(name, DoubleType)
    fun boolean(name: String): Column<Boolean> = column(name, BooleanType)
    fun datetime(name: String): Column<LocalDateTime> = column(name, DateTimeType)
    fun blob(name: String): Column<ByteArray> = column(name, BlobType)
    
    fun primaryKey(vararg columns: Column<*>): PrimaryKey {
        val pk = PrimaryKey(*columns)
        primaryKey = pk
        return pk
    }
    
    /**
     * Creates the table in the database.
     */
    fun create(connection: Connection) {
        val columnsDefinition = columns.joinToString(", ") { it.getDefinition() }
        val pkDefinition = primaryKey?.let { 
            ", PRIMARY KEY (${it.columns.joinToString(", ") { col -> col.name }})" 
        } ?: ""
        
        val createStatement = "CREATE TABLE IF NOT EXISTS $name ($columnsDefinition$pkDefinition)"
        connection.update(createStatement)
    }
    
    /**
     * Drops the table from the database.
     */
    fun drop(connection: Connection) {
        connection.update("DROP TABLE IF EXISTS $name")
    }
}

/**
 * Represents a primary key constraint.
 */
class PrimaryKey(vararg val columns: Column<*>)

/**
 * Represents a database column.
 */
class Column<T>(
    val name: String,
    val type: ColumnType<T>,
    val table: Table
) {
    fun getDefinition(): String {
        return "$name ${type.sqlType}"
    }
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): ColumnReference<T> {
        return ColumnReference(this)
    }
}

/**
 * Represents a reference to a column in a query.
 */
class ColumnReference<T>(val column: Column<T>) {
    val name: String get() = column.name
    val table: Table get() = column.table
    
    infix fun eq(value: T?): Condition = Condition("${column.name} = ?", value)
    infix fun neq(value: T?): Condition = Condition("${column.name} <> ?", value)
    infix fun greater(value: T): Condition = Condition("${column.name} > ?", value)
    infix fun less(value: T): Condition = Condition("${column.name} < ?", value)
    infix fun greaterEq(value: T): Condition = Condition("${column.name} >= ?", value)
    infix fun lessEq(value: T): Condition = Condition("${column.name} <= ?", value)
    infix fun like(value: String): Condition = Condition("${column.name} LIKE ?", value)
    fun isNull(): Condition = Condition("${column.name} IS NULL")
    fun isNotNull(): Condition = Condition("${column.name} IS NOT NULL")
    infix fun inList(values: List<T>): Condition {
        val placeholders = values.joinToString(", ") { "?" }
        return Condition("${column.name} IN ($placeholders)", *values.toTypedArray())
    }
}

/**
 * Represents a SQL condition.
 */
class Condition(val sql: String, vararg val parameters: Any?) {
    infix fun and(other: Condition): Condition {
        return Condition("($sql) AND (${other.sql})", *(parameters + other.parameters))
    }
    
    infix fun or(other: Condition): Condition {
        return Condition("($sql) OR (${other.sql})", *(parameters + other.parameters))
    }
}

/**
 * Represents a column type.
 */
abstract class ColumnType<T>(val sqlType: String)

class VarcharType(length: Int) : ColumnType<String>("VARCHAR($length)")
object IntegerType : ColumnType<Int>("INTEGER")
object LongType : ColumnType<Long>("BIGINT")
object DoubleType : ColumnType<Double>("DOUBLE PRECISION")
object BooleanType : ColumnType<Boolean>("BOOLEAN")
object DateTimeType : ColumnType<LocalDateTime>("TIMESTAMP")
object BlobType : ColumnType<ByteArray>("BLOB")

// ===== Query DSL =====

/**
 * Represents a query builder.
 */
class QueryBuilder(val table: Table) {
    private var conditions: Condition? = null
    private var orderByColumns = mutableListOf<Pair<Column<*>, SortOrder>>()
    private var limitValue: Int? = null
    private var offsetValue: Int? = null
    
    fun where(condition: Condition): QueryBuilder {
        conditions = condition
        return this
    }
    
    fun orderBy(column: Column<*>, order: SortOrder = SortOrder.ASC): QueryBuilder {
        orderByColumns.add(column to order)
        return this
    }
    
    fun orderBy(columnOrder: Pair<Column<*>, SortOrder>): QueryBuilder {
        orderByColumns.add(columnOrder)
        return this
    }
    
    fun limit(limit: Int): QueryBuilder {
        limitValue = limit
        return this
    }
    
    fun offset(offset: Int): QueryBuilder {
        offsetValue = offset
        return this
    }
    
    fun buildSelectQuery(columns: List<Column<*>> = table.columns): String {
        val columnsStr = columns.joinToString(", ") { it.name }
        var query = "SELECT $columnsStr FROM ${table.name}"
        
        conditions?.let { query += " WHERE ${it.sql}" }
        
        if (orderByColumns.isNotEmpty()) {
            val orderByStr = orderByColumns.joinToString(", ") { 
                "${it.first.name} ${it.second.name}" 
            }
            query += " ORDER BY $orderByStr"
        }
        
        limitValue?.let { query += " LIMIT $it" }
        offsetValue?.let { query += " OFFSET $it" }
        
        return query
    }
    
    fun getParameters(): Array<Any?> {
        return conditions?.parameters ?: emptyArray()
    }
    
    inline fun <T> executeQuery(connection: Connection, mapper: (ResultSet) -> T): List<T> {
        val query = buildSelectQuery()
        val params = getParameters()
        return connection.queryList(query, *params, mapper = mapper)
    }
    
    inline fun <T> executeSingleQuery(connection: Connection, mapper: (ResultSet) -> T): T? {
        val query = buildSelectQuery()
        val params = getParameters()
        return connection.querySingle(query, *params, mapper = mapper)
    }
    
    fun count(connection: Connection): Long {
        val query = "SELECT COUNT(*) FROM ${table.name}" + 
                    (conditions?.let { " WHERE ${it.sql}" } ?: "")
        val params = getParameters()
        return connection.querySingle(query, *params) { rs -> rs.getLong(1) } ?: 0
    }
}

enum class SortOrder(val name: String) {
    ASC("ASC"),
    DESC("DESC")
}

// ===== Table Operations =====

/**
 * Selects all columns from the table.
 */
fun Table.select(where: (QueryBuilder) -> QueryBuilder = { it }): QueryBuilder {
    return where(QueryBuilder(this))
}

/**
 * Inserts a new row into the table.
 */
fun Table.insert(connection: Connection, values: Map<Column<*>, Any?>): Long? {
    val columns = values.keys.joinToString(", ") { it.name }
    val placeholders = values.keys.joinToString(", ") { "?" }
    val params = values.values.toTypedArray()
    
    val query = "INSERT INTO $name ($columns) VALUES ($placeholders)"
    
    return connection.executeWithKeys(query, *params) { rs ->
        if (rs.next()) rs.getLong(1) else null
    }
}

/**
 * Updates rows in the table.
 */
fun Table.update(connection: Connection, values: Map<Column<*>, Any?>, condition: Condition): Int {
    val setClause = values.keys.joinToString(", ") { "${it.name} = ?" }
    val params = values.values.toList() + condition.parameters
    
    val query = "UPDATE $name SET $setClause WHERE ${condition.sql}"
    
    return connection.update(query, *params.toTypedArray())
}

/**
 * Deletes rows from the table.
 */
fun Table.delete(connection: Connection, condition: Condition): Int {
    val query = "DELETE FROM $name WHERE ${condition.sql}"
    return connection.update(query, *condition.parameters)
}

// ===== Database Operations =====

/**
 * Creates a database connection.
 */
fun createConnection(url: String, username: String, password: String): Connection {
    return DriverManager.getConnection(url, username, password)
}

/**
 * Creates a connection pool.
 * Note: This is a simple implementation. In a real application, use a proper connection pool library.
 */
class ConnectionPool(
    private val url: String,
    private val username: String,
    private val password: String,
    private val maxConnections: Int = 10
) {
    private val connections = mutableListOf<Connection>()
    private val lock = Any()
    
    fun getConnection(): Connection {
        synchronized(lock) {
            if (connections.isNotEmpty()) {
                return connections.removeAt(connections.size - 1)
            }
            
            return createConnection(url, username, password)
        }
    }
    
    fun releaseConnection(connection: Connection) {
        synchronized(lock) {
            if (connections.size < maxConnections) {
                connections.add(connection)
            } else {
                connection.close()
            }
        }
    }
    
    inline fun <T> withConnection(block: (Connection) -> T): T {
        val connection = getConnection()
        return try {
            block(connection)
        } finally {
            releaseConnection(connection)
        }
    }
    
    inline fun <T> transaction(block: (Connection) -> T): T {
        return withConnection { conn ->
            conn.transaction(block)
        }
    }
    
    fun close() {
        synchronized(lock) {
            connections.forEach { it.close() }
            connections.clear()
        }
    }
}