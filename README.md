# Advanced Persistent RDBMS Engine

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Database](https://img.shields.io/badge/Database-4479A1?style=for-the-badge&logo=database&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active-success?style=for-the-badge)

A custom-engineered Relational Database Management System (RDBMS) built from the ground up to explore the internal mechanics of data persistence, physical storage abstraction, and query optimization. This project implements a full-stack database logic, moving from high-level DBMS management down to low-level byte-stream serialization.

## 🎯 Overview

This RDBMS implementation demonstrates advanced database concepts including page-based storage, multiple indexing strategies, and efficient query processing. The system is designed to handle persistent data storage with robust serialization mechanisms and provides insights into how modern database systems manage and retrieve data at scale.

## ✨ Key Features

### Core Database Operations
- **Table Management**: Create and manage relational tables with custom schemas
- **CRUD Operations**: Full support for Create, Read, Update, and Delete operations
- **Data Persistence**: File-based serialization for durable storage across sessions
- **Page-Based Storage**: Efficient memory management through configurable page sizes

### Advanced Indexing Strategies
- **Bitmap Indexing**: Space-efficient indexing for low-cardinality columns
- **Dense Indexing**: High-performance indexing for frequently queried columns
- **Dynamic Index Updates**: Automatic index maintenance on data insertion
- **Multi-Column Query Optimization**: Intelligent index selection for complex queries

### Data Integrity & Recovery
- **Record Validation**: Built-in mechanisms to detect missing or corrupted records
- **Automatic Recovery**: Self-healing capabilities to restore data integrity
- **Transaction Tracing**: Comprehensive audit trail of all database operations
- **File System Management**: Organized storage structure with metadata tracking

## 🏗️ Architecture

```
src/DBMS/
├── DBApp.java              # Main database application interface
├── Table.java              # Table management and operations
├── Page.java               # Page-based storage implementation
├── FileManager.java        # Persistent storage and serialization
├── BitmapIndex.java        # Bitmap indexing implementation
└── DenseIndexBlock.java    # Dense index block structure
```

### System Components

**DBApp (Database Application Layer)**
- Primary interface for all database operations
- Configurable page sizes for data and index storage
- Comprehensive testing framework

**Table Management**
- Dynamic schema definition
- Multi-column support
- Trace logging for debugging and auditing

**Storage Engine**
- Page-based data organization
- Efficient serialization/deserialization
- Hierarchical file system structure

**Indexing Engine**
- Multiple indexing strategies
- Automatic index maintenance
- Query optimization through index selection

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Any Java IDE (Eclipse, IntelliJ IDEA, NetBeans) or command-line tools

### Installation

1. Clone the repository:
```bash
git clone https://github.com/markrtak/Advanced-Persistent-RDBMS-Engine.git
cd Advanced-Persistent-RDBMS-Engine
```

2. Compile the Java source files:
```bash
javac -d bin src/DBMS/*.java
```

3. Run the application:
```bash
java -cp bin DBMS.DBApp
```

## 💡 Usage Examples

### Creating a Table

```java
String[] columns = {"id", "name", "major", "semester", "gpa"};
DBApp.createTable("student", columns);
```

### Inserting Records

```java
String[] record = {"1", "John Doe", "CS", "5", "3.8"};
DBApp.insert("student", record);
```

### Querying Data

```java
// Select all records
ArrayList<String[]> allStudents = DBApp.select("student");

// Select by position
ArrayList<String[]> specificRecord = DBApp.select("student", 0, 1);

// Select by column condition
ArrayList<String[]> csStudents = DBApp.select(
    "student", 
    new String[]{"major"}, 
    new String[]{"CS"}
);
```

### Creating Indexes

```java
// Create bitmap index
DBApp.createBitMapIndex("student", "major");

// Create dense index
DBApp.createDenseIndex("student", "id");

// Query using index
ArrayList<String[]> results = DBApp.selectIndex(
    "student",
    new String[]{"major", "gpa"},
    new String[]{"CS", "3.8"}
);
```

### Data Recovery

```java
// Validate records
ArrayList<String[]> missing = DBApp.validateRecords("student");

// Recover missing records
if (missing.size() > 0) {
    DBApp.recoverRecords("student", missing);
}
```

## 🔧 Configuration

The system can be configured through static variables in `DBApp.java`:

```java
static int dataPageSize = 2;    // Number of records per data page
static int indexPageSize = 5;   // Number of entries per index page
```

## 📊 Performance Characteristics

### Storage Efficiency
- **Page-based allocation**: Reduces memory overhead
- **Bitmap indexing**: Optimal for low-cardinality data
- **Dense indexing**: Fast lookups for sorted data

### Query Performance
- **Index-assisted queries**: O(log n) lookup time with dense indexes
- **Bitmap operations**: Efficient multi-column filtering
- **Trace logging**: Minimal performance impact

## 🧪 Testing

The project includes comprehensive tests covering:

- ✅ Basic CRUD operations
- ✅ Position-based selection
- ✅ Column-condition filtering
- ✅ Bitmap index creation and usage
- ✅ Dense index creation and representation
- ✅ Multi-column indexed queries
- ✅ Record validation and recovery
- ✅ File system operations

Run the main method in `DBApp.java` to execute the full test suite.

## 📈 Technical Highlights

### Low-Level Implementation
- Custom serialization mechanisms
- Direct file I/O operations
- Manual memory management through pages
- Binary data representation for bitmaps

### Data Structures
- ArrayList-based record storage
- Sorted index structures
- Bitstring representations
- Hierarchical file organization

### Algorithm Design
- Efficient sorting for index creation
- Bitwise operations for bitmap queries
- Page-based binary search
- Dynamic index maintenance

## 🛣️ Future Enhancements

Potential areas for expansion:

- **B+ Tree Indexing**: Add more sophisticated index structures
- **Query Optimizer**: Cost-based query planning
- **Concurrency Control**: Multi-user transaction support
- **SQL Parser**: SQL query language support
- **Buffer Pool Manager**: In-memory page caching
- **Join Operations**: Multi-table query support
- **Logging & Recovery**: Write-ahead logging for crash recovery

## 📝 Implementation Details

### File Structure
The database creates a persistent file system structure:

```
Tables/
├── student/
│   ├── table.ser           # Table metadata
│   ├── page_0.ser          # Data pages
│   ├── page_1.ser
│   └── indexes/
│       ├── major_bitmap/   # Bitmap indexes
│       └── id_dense/       # Dense indexes
```

### Trace System
Every operation is logged for debugging and auditing:
- Table creation events
- Record insertions
- Index creation timing
- Query execution details

## 👤 Author

**Mark Tak**

- GitHub: [@markrtak](https://github.com/markrtak)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by modern RDBMS implementations
- Built to understand database internals
- Demonstrates fundamental CS concepts in action

---

**Note**: This is an educational implementation designed to demonstrate database concepts. For production use, consider established database systems like PostgreSQL, MySQL, or MongoDB.
