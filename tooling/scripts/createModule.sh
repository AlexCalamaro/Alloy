#!/bin/bash

# Module Scaffolding Script
# Usage: ./createModule.sh -name=Settings -description="User settings"

set -e

# Parse arguments
MODULE_NAME=""
MODULE_DESCRIPTION=""
INCLUDE_ROOM=false
INCLUDE_TESTS=true

while [[ $# -gt 0 ]]; do
    case $1 in
        -name=*)
            MODULE_NAME="${1#*=}"
            ;;
        -description=*)
            MODULE_DESCRIPTION="${1#*=}"
            ;;
        -room)
            INCLUDE_ROOM=true
            ;;
        -no-tests)
            INCLUDE_TESTS=false
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
    shift
done

# Validate inputs
if [ -z "$MODULE_NAME" ]; then
    echo "❌ Error: Module name is required. Use -name=YourModuleName"
    exit 1
fi

if ! [[ "$MODULE_NAME" =~ ^[A-Za-z][A-Za-z0-9]*$ ]]; then
    echo "❌ Error: Module name must start with a letter and contain only letters and numbers"
    exit 1
fi

# Generate IDs
PROJECT_ID=$(echo "$MODULE_NAME" | tr '[:upper:]' '[:lower:]' | sed 's/ /-/g')
PACKAGE_NAME="com.squidink.alloy.modules.$PROJECT_ID"
NAMESPACE="com.squidink.alloy.modules.$PROJECT_ID"

echo "Creating module: $MODULE_NAME ($PROJECT_ID)"

# Check if module exists
if [ -d "modules/$PROJECT_ID" ]; then
    echo "❌ Error: Module already exists: $PROJECT_ID"
    exit 1
fi

# Create directory structure
mkdir -p "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/di"
mkdir -p "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/data"
mkdir -p "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/ui"
mkdir -p "modules/$PROJECT_ID/src/test/java/${PACKAGE_NAME//.//}"

# Generate build.gradle.kts
ROOM_DEPS=""
if [ "$INCLUDE_ROOM" = true ]; then
    ROOM_DEPS="    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.wrapper)

"
fi

cat > "modules/$PROJECT_ID/build.gradle.kts" << EOF
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "$NAMESPACE"
    compileSdk = 37

    defaultConfig {
        minSdk = 37
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:design"))
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

$ROOM_DEPS    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
EOF

# Generate AndroidManifest.xml
cat > "modules/$PROJECT_ID/src/main/AndroidManifest.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

</manifest>
EOF

# Generate ViewModel
cat > "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/${MODULE_NAME}ViewModel.kt" << EOF
package $PACKAGE_NAME

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ${MODULE_NAME}UiState(
    val isLoading: Boolean = false,
) : UiState

sealed interface ${MODULE_NAME}UiAction : UiAction {
    data object Refresh : ${MODULE_NAME}UiAction
}

sealed interface ${MODULE_NAME}UiEffect : UiEffect

@HiltViewModel
class ${MODULE_NAME}ViewModel
    @Inject
    constructor(
    ) : BaseViewModel<${MODULE_NAME}UiState, ${MODULE_NAME}UiAction, ${MODULE_NAME}UiEffect>(
            ${MODULE_NAME}UiState(),
        ) {

    init {
        // Initialize view model
    }

    override fun onAction(action: ${MODULE_NAME}UiAction) {
        when (action) {
            is ${MODULE_NAME}UiAction.Refresh -> refreshData()
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            // Implement refresh logic here
        }
    }
}
EOF

# Generate Screen
cat > "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/ui/${MODULE_NAME}Screen.kt" << EOF
package ${PACKAGE_NAME}.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.modules.${PROJECT_ID}.${MODULE_NAME}UiAction
import com.squidink.alloy.modules.${PROJECT_ID}.${MODULE_NAME}UiState
import com.squidink.alloy.modules.${PROJECT_ID}.${MODULE_NAME}ViewModel

@Composable
fun ${MODULE_NAME}Screen(
    viewModel: ${MODULE_NAME}ViewModel,
    onNavigateTo: (String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    ${MODULE_NAME}ScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateTo = onNavigateTo,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
internal fun ${MODULE_NAME}ScreenContent(
    uiState: ${MODULE_NAME}UiState,
    onAction: (${MODULE_NAME}UiAction) -> Unit,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "$MODULE_NAME",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Add your screen content here
        }
    }
}
EOF

# Generate Repository
cat > "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/data/I${MODULE_NAME}Repository.kt" << EOF
package ${PACKAGE_NAME}.data

import kotlinx.coroutines.flow.Flow

interface I${MODULE_NAME}Repository {
    fun get${MODULE_NAME}s(): Flow<List<Any>>
    fun get${MODULE_NAME}ById(id: String): Flow<Any?>
    suspend fun insert${MODULE_NAME}(${MODULE_NAME,,}: Any)
    suspend fun update${MODULE_NAME}(${MODULE_NAME,,}: Any)
    suspend fun delete${MODULE_NAME}(id: String)
    suspend fun deleteAll${MODULE_NAME}s()
}
EOF

# Generate RepositoryImpl
cat > "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/data/${MODULE_NAME}RepositoryImpl.kt" << EOF
package ${PACKAGE_NAME}.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ${MODULE_NAME}RepositoryImpl(
) : I${MODULE_NAME}Repository {
    
    override fun get${MODULE_NAME}s(): Flow<List<Any>> {
        TODO("Implement this method")
    }
    
    override fun get${MODULE_NAME}ById(id: String): Flow<Any?> {
        TODO("Implement this method")
    }
    
    override suspend fun insert${MODULE_NAME}(${MODULE_NAME,,}: Any) {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
    
    override suspend fun update${MODULE_NAME}(${MODULE_NAME,,}: Any) {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
    
    override suspend fun delete${MODULE_NAME}(id: String) {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
    
    override suspend fun deleteAll${MODULE_NAME}s() {
        withContext(Dispatchers.IO) {
            TODO("Implement this method")
        }
    }
}
EOF

# Generate DI Module
cat > "modules/$PROJECT_ID/src/main/java/${PACKAGE_NAME//.//}/di/${MODULE_NAME}Module.kt" << EOF
package ${PACKAGE_NAME}.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ${MODULE_NAME}Module {
    
    // Add @Provides methods here as needed
}
EOF

# Generate test if enabled
if [ "$INCLUDE_TESTS" = true ]; then
    cat > "modules/$PROJECT_ID/src/test/java/${PACKAGE_NAME//.//}/${MODULE_NAME}ViewModelTest.kt" << EOF
package $PACKAGE_NAME

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ${MODULE_NAME}ViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    fun \`initial state is correct\`() =
        runTest {
            val viewModel = ${MODULE_NAME}ViewModel()
            assertEquals(${MODULE_NAME}UiState(), viewModel.uiState.value)
        }
}
EOF
fi

# Generate README
cat > "modules/$PROJECT_ID/README.md" << EOF
# $MODULE_NAME Module

$MODULE_DESCRIPTION

## Structure

\`\`\`
$PROJECT_ID/
├── build.gradle.kts
├── README.md
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/squidink/alloy/modules/$PROJECT_ID/
    │       ├── di/
    │       │   └── ${MODULE_NAME}Module.kt
    │       ├── data/
    │       │   ├── I${MODULE_NAME}Repository.kt
    │       │   └── ${MODULE_NAME}RepositoryImpl.kt
    │       ├── ${MODULE_NAME}ViewModel.kt
    │       └── ui/
    │           └── ${MODULE_NAME}Screen.kt
    └── test/
        └── ${MODULE_NAME}ViewModelTest.kt
\`\`\`

## Usage

### Navigation

\`\`\`kotlin
navController.navigate("$PROJECT_ID")
\`\`\`

## Development

Run tests:

\`\`\`bash
./gradlew :modules:$PROJECT_ID:test
\`\`\`
EOF

echo "✅ Module created successfully: $PROJECT_ID"
echo "   Location: modules/$PROJECT_ID"
echo "   Package: $PACKAGE_NAME"
echo "   Room: $([ "$INCLUDE_ROOM" = true ] && echo "Yes" || echo "No")"
echo "   Tests: $([ "$INCLUDE_TESTS" = true ] && echo "Yes" || echo "No")"
echo ""
echo "Next steps:"
echo "1. Add to settings.gradle.kts: include(\":modules:$PROJECT_ID\")"
echo "2. Run: ./gradlew :modules:$PROJECT_ID:build"
echo "3. Implement repository and ViewModel logic"
