package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserSession
import com.example.ui.components.GlobalAiCommandBar
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.NotesExpensesTheme
import com.example.ui.viewmodel.NotesExpensesViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NotesExpensesViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            NotesExpensesTheme(darkTheme = isDarkTheme) {
                NotesExpensesApp(viewModel = viewModel)
            }
        }
    }
}

enum class NavigationDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    AI_CHAT("ai_chat", "Notes AI Chat", Icons.Default.AutoAwesome),
    AUTOMATION("automation", "AI Rules Engine", Icons.Default.SettingsSuggest),
    NOTES("notes", "Notes", Icons.Default.EditNote),
    BUDGET("budget", "Budget", Icons.Default.AccountBalanceWallet),
    ANALYTICS("analytics", "Analytics", Icons.Default.PieChart),
    CALENDAR("calendar", "Calendar", Icons.Default.CalendarMonth),
    CATEGORIES("categories", "Categories", Icons.Default.Category),
    RECEIPTS("receipts", "Export & Receipts", Icons.AutoMirrored.Filled.ReceiptLong),
    PROFILE("profile", "Profile", Icons.Default.Person),
    SETTINGS("settings", "Settings", Icons.Default.Settings),
    ABOUT("about", "About & Help", Icons.Default.Info),
    AUTH("auth", "Authentication", Icons.Default.Lock)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesExpensesApp(
    viewModel: NotesExpensesViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf(NavigationDestination.DASHBOARD) }
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var currentCurrency by remember { mutableStateOf("₹") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val trashNotes by viewModel.trashNotes.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fontSizeOption by viewModel.fontSizeOption.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Observe snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Notes Expenses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Natural AI Financial Tracker",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val primaryDestinations = listOf(
                    NavigationDestination.DASHBOARD,
                    NavigationDestination.AI_CHAT,
                    NavigationDestination.AUTOMATION,
                    NavigationDestination.NOTES,
                    NavigationDestination.BUDGET,
                    NavigationDestination.ANALYTICS,
                    NavigationDestination.CALENDAR
                )

                val secondaryDestinations = listOf(
                    NavigationDestination.CATEGORIES,
                    NavigationDestination.RECEIPTS,
                    NavigationDestination.PROFILE,
                    NavigationDestination.SETTINGS,
                    NavigationDestination.ABOUT
                )

                primaryDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.title) },
                        selected = currentDestination == dest,
                        icon = { Icon(dest.icon, contentDescription = null) },
                        onClick = {
                            currentDestination = dest
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                secondaryDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.title) },
                        selected = currentDestination == dest,
                        icon = { Icon(dest.icon, contentDescription = null) },
                        onClick = {
                            currentDestination = dest
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("main_app_scaffold"),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentDestination.title,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer Navigation")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { currentDestination = NavigationDestination.AI_CHAT },
                            modifier = Modifier.testTag("ai_chat_top_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Open Notes AI Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val bottomTabItems = listOf(
                        NavigationDestination.DASHBOARD,
                        NavigationDestination.AI_CHAT,
                        NavigationDestination.NOTES,
                        NavigationDestination.BUDGET,
                        NavigationDestination.ANALYTICS
                    )

                    bottomTabItems.forEach { dest ->
                        NavigationBarItem(
                            selected = currentDestination == dest,
                            onClick = { currentDestination = dest },
                            icon = { Icon(dest.icon, contentDescription = dest.title) },
                            label = { Text(dest.title) },
                            modifier = Modifier.testTag("nav_tab_${dest.route}")
                        )
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showQuickAddSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Quick Note") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("main_quick_add_fab")
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Global AI Command Bar visible on Dashboard and Notes screens
                if (currentDestination == NavigationDestination.DASHBOARD || currentDestination == NavigationDestination.NOTES) {
                    GlobalAiCommandBar(
                        onExecuteCommand = { cmd -> viewModel.executeAiCommand(cmd) },
                        onStartVoiceInput = { currentDestination = NavigationDestination.AI_CHAT }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentDestination) {
                        NavigationDestination.DASHBOARD -> DashboardScreen(
                            notes = activeNotes,
                            budgets = budgets,
                            currencySymbol = currentCurrency,
                            onQuickAddClick = { showQuickAddSheet = true },
                            onNavigateToNotes = { currentDestination = NavigationDestination.NOTES },
                            onNavigateToBudgets = { currentDestination = NavigationDestination.BUDGET },
                            onNavigateToAnalytics = { currentDestination = NavigationDestination.ANALYTICS },
                            onNoteClick = { },
                            onPinToggle = { viewModel.togglePin(it) },
                            onArchiveToggle = { viewModel.toggleArchive(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onLockToggle = { viewModel.toggleLock(it) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onDuplicateNote = { viewModel.duplicateNote(it) },
                            onShareNote = { viewModel.shareNoteText(context, it) }
                        )

                        NavigationDestination.AI_CHAT -> AiChatAssistantScreen(
                            viewModel = viewModel
                        )

                        NavigationDestination.AUTOMATION -> AutomationRulesScreen(
                            viewModel = viewModel
                        )

                        NavigationDestination.NOTES -> NotesScreen(
                            notes = allNotes,
                            trashNotes = trashNotes,
                            searchQuery = searchQuery,
                            currencySymbol = currentCurrency,
                            categoriesList = categories.map { it.name },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onSaveNote = { viewModel.saveNoteItem(it) },
                            onPinToggle = { viewModel.togglePin(it) },
                            onArchiveToggle = { viewModel.toggleArchive(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onLockToggle = { viewModel.toggleLock(it) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                            onDeletePermanently = { viewModel.deleteNotePermanently(it) },
                            onEmptyTrash = { viewModel.emptyTrash() },
                            onDuplicateNote = { viewModel.duplicateNote(it) },
                            onShareNote = { viewModel.shareNoteText(context, it) },
                            onQuickAddClick = { showQuickAddSheet = true }
                        )

                        NavigationDestination.BUDGET -> BudgetScreen(
                            budgets = budgets,
                            notes = activeNotes,
                            categoriesList = categories.map { it.name },
                            currencySymbol = currentCurrency,
                            onAddBudget = { name, cat, amount -> viewModel.addBudget(name, cat, amount) },
                            onDeleteBudget = { viewModel.deleteBudget(it) }
                        )

                        NavigationDestination.ANALYTICS -> AnalyticsScreen(
                            notes = activeNotes,
                            budgets = budgets,
                            currencySymbol = currentCurrency,
                            onApplyRecommendation = { recAction ->
                                when (recAction) {
                                    "Create Grocery Limit" -> viewModel.addBudget("Grocery Budget", "Groceries", 4000.0)
                                    "Backup Now" -> viewModel.triggerSupabaseSync()
                                    else -> currentDestination = NavigationDestination.AUTOMATION
                                }
                            }
                        )

                        NavigationDestination.CALENDAR -> CalendarScreen(
                            notes = activeNotes,
                            viewModel = viewModel,
                            currencySymbol = currentCurrency,
                            onNoteClick = { },
                            onPinToggle = { viewModel.togglePin(it) },
                            onArchiveToggle = { viewModel.toggleArchive(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onLockToggle = { viewModel.toggleLock(it) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onDuplicateNote = { viewModel.duplicateNote(it) },
                            onShareNote = { viewModel.shareNoteText(context, it) }
                        )

                        NavigationDestination.CATEGORIES -> CategoriesScreen(
                            categories = categories,
                            onAddCustomCategory = { name, hex -> viewModel.addCustomCategory(name, hex) },
                            onDeleteCategory = { }
                        )

                        NavigationDestination.RECEIPTS -> ReceiptsExportScreen(
                            notes = activeNotes,
                            currencySymbol = currentCurrency
                        )

                        NavigationDestination.PROFILE -> ProfileScreen(
                            userSession = userSession,
                            authManager = viewModel.authManager,
                            onNavigateToAuth = { currentDestination = NavigationDestination.AUTH }
                        )

                        NavigationDestination.SETTINGS -> SettingsScreen(
                            currentThemeMode = themeMode,
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            currentFontSize = fontSizeOption,
                            onFontSizeChange = { viewModel.setFontSizeOption(it) },
                            currentCurrency = currentCurrency,
                            onCurrencyChange = { currentCurrency = it }
                        )

                        NavigationDestination.ABOUT -> AboutHelpScreen()

                        NavigationDestination.AUTH -> AuthScreen(
                            authManager = viewModel.authManager,
                            onAuthSuccess = { currentDestination = NavigationDestination.DASHBOARD }
                        )
                    }
                }
            }
        }
    }

    // Quick Natural Note Entry Bottom Sheet
    if (showQuickAddSheet) {
        QuickAddBottomSheet(
            currencySymbol = currentCurrency,
            categoriesList = categories.map { it.name },
            onDismissRequest = { showQuickAddSheet = false },
            onSaveNote = { noteItem ->
                viewModel.addNoteFromRawText(noteItem.rawText)
            }
        )
    }
}
