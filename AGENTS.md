# AGENTS.md - Saku Development Guide

## Project Overview

Saku is a Kotlin Multiplatform project targeting Android, Desktop (JVM), and iOS. It uses:
- **Compose Multiplatform** for UI
- **Room** for local database
- **Kotlin Coroutines** for async operations
- **KSP** for annotation processing
- **Kotlin Test** for unit testing

## Build Commands

### Gradle Tasks

```bash
# Run desktop app with hot reload
./run_desktop_hot.sh

# Build Android APK (prod release)
./build_apk.sh <version> <output_dir>

# Run all tests (JVM)
./gradlew :shared:jvmTest

# Run all tests (Android - requires device/emulator)
./gradlew :shared:testDebugUnitTest

# Run a single test class
./gradlew :shared:jvmTest --tests "dev.nichidori.saku.data.repo.DefaultBudgetRepositoryTest"

# Run a single test method
./gradlew :shared:jvmTest --tests "dev.nichidori.saku.data.dao.BudgetDaoTest.insertAndGetByIdWithCategory"

# Build desktop distribution
./gradlew :composeApp:desktopDistribute

# Clean and rebuild
./gradlew clean build

# Check dependencies
./gradlew dependencies

# Android build variants
./gradlew :composeApp:assembleDevDebug    # Dev debug
./gradlew :composeApp:assembleProdDebug   # Prod debug
./gradlew :composeApp:assembleProdRelease # Prod release (requires signing)
```

## Code Style Guidelines

### Project Structure

```
dev.nichidori.saku/
├── core/
│   ├── composable/    # Shared UI components (MyButton, MyTextField, etc.)
│   ├── model/         # Core models (Status<T, E>)
│   ├── platform/      # Platform-specific utilities (showToast)
│   ├── theme/         # Theme configuration
│   └── util/          # Extensions and utilities
├── domain/
│   ├── model/         # Domain models (Budget, Category, Account, Trx)
│   └── repo/          # Repository interfaces
└── data/
    ├── dao/           # Room DAOs
    ├── entity/        # Room entities
    └── repo/          # Repository implementations

composeApp/
├── src/
│   ├── androidMain/   # Android-specific code
│   ├── desktopMain/   # Desktop-specific code
│   └── commonMain/    # Shared Compose UI
│       └── feature/   # Feature modules (home, trx, budget, etc.)
└── build.gradle.kts
```

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Packages | lowercase | `dev.nichidori.saku.data.repo` |
| Classes | PascalCase | `BudgetRepository`, `HomeViewModel` |
| Functions | camelCase | `getAllBudgetTemplates`, `onBalanceToggle` |
| Variables | camelCase | `budgetTemplates`, `selectedMonth` |
| Constants | PascalCase enum | `TrxType.Income`, `AccountType.Cash` |
| Private fields | _prefix optional | `_uiState` or `uiState` |
| Extension functions | camelCase | `toDomain()`, `toRupiah()` |
| Test classes | Test suffix | `DefaultBudgetRepositoryTest` |
| Test methods | should/action | `insertAndGetById_shouldReturnMatchingBudget` |

### Import Organization

Organize imports in this order (no blank lines between groups):

1. Kotlin standard library (`kotlin.*`, `kotlinx.*`)
2. Third-party libraries (`androidx.*`, `com.*`, `org.*`)
3. Project imports (`dev.nichidori.saku.*`)

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.repo.BudgetRepository
import dev.nichidori.saku.data.entity.toDomain
```

### Types and Generics

- Use explicit types for public properties
- Use `val` by default, `var` only when mutation is required
- Leverage Kotlin's type system; avoid raw `Any`

```kotlin
// Good
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

// State classes should be data classes
data class HomeUiState(
    val loadStatus: Status<YearMonth, Exception> = Initial,
    val accounts: List<Account> = emptyList(),
)

// Sealed interfaces for discriminated unions
sealed interface Status<out T, out E> {
    data object Initial : Status<Nothing, Nothing>
    data class Success<T>(val data: T) : Status<T, Nothing>
    data class Failure<E>(val error: E) : Status<Nothing, E>
}
```

### Error Handling

1. **Use sealed `Status` for UI state**:
```kotlin
sealed interface Status<out T, out E> { ... }

class HomeViewModel(...) : ViewModel() {
    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loadStatus = Loading) }
                val data = repository.getData()
                _uiState.update { it.copy(loadStatus = Success(data)) }
            } catch (e: Exception) {
                this@HomeViewModel.log(e)
                _uiState.update { it.copy(loadStatus = Failure(e)) }
            }
        }
    }
}
```

2. **Throw exceptions for repository operations**:
```kotlin
// Use NoSuchElementException for not found
fun getBudgetById(id: String): Budget? {
    return db.useReaderConnection {
        db.budgetDao().getByIdWithCategory(id)?.toDomain()
    }
}

// Throw for operations that should fail
fun updateBudget(id: String, baseAmount: Long) {
    db.useWriterConnection {
        it.immediateTransaction {
            val existing = db.budgetDao().getByIdWithCategory(id)?.toDomain()
                ?: throw NoSuchElementException("Budget not found")
            // ...
        }
    }
}
```

### Compose Guidelines

- Annotate composables with `@Composable`
- Use `@OptIn` for experimental APIs
- ViewModels receive dependencies via constructor injection
- Use `StateFlow` for UI state, exposed as `StateFlow` (not `MutableStateFlow`)

```kotlin
@Composable
fun HomePage(
    viewModel: HomeViewModel,
    onCategoryClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

### Database Layer

**Entities** (Room):
```kotlin
@Entity(tableName = "budget", ...)
data class BudgetEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    // ...
)
```

**DAOs** return suspend functions for database operations.

**Mappers** are extension functions in `MapperExt.kt`:
```kotlin
fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    baseAmount = baseAmount,
    // ...
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    baseAmount = baseAmount,
    // ...
)
```

### Testing

- Use `@BeforeTest` and `@AfterTest` for setup/teardown
- Use `runTest` from `kotlinx.coroutines.test` for coroutine tests
- Use in-memory Room database for DAO tests
- Group related tests with comment headers

```kotlin
class BudgetDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var budgetDao: BudgetDao

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        budgetDao = db.budgetDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    // Budget tests
    @Test
    fun insertAndGetByIdWithCategory_shouldReturnMatchingBudget() = runTest {
        // test code
    }
}
```

### Multiplatform Considerations

- Use `commonMain` for shared code
- Platform-specific code in `androidMain`, `desktopMain`, `iosMain`
- Expect/actual pattern for platform-specific implementations:
  - `FlowExt.kt` (common)
  - `FlowExt.android.kt`, `FlowExt.desktop.kt` (platform)

### Experimental APIs

Use `@OptIn` when required:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyComponent() { ... }
```

Opt-in to experimental time APIs in `build.gradle.kts`:
```kotlin
sourceSets.all {
    languageSettings.optIn("kotlin.time.ExperimentalTime")
}
```

### Code Avoidance

- Avoid `!!` operator; use null-safety and `?.let` patterns
- Avoid `lateinit` for immutable data; prefer nullable types
- Avoid raw strings for SQL; use Room's query annotation
- Avoid side effects in composables; use `LaunchedEffect` or `SideEffect`
