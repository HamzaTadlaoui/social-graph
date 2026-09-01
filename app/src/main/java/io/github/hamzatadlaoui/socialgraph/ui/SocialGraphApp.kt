package io.github.hamzatadlaoui.socialgraph.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.hamzatadlaoui.socialgraph.AppContainer
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.ui.documents.DocumentScreen
import io.github.hamzatadlaoui.socialgraph.ui.documents.DocumentViewModel
import io.github.hamzatadlaoui.socialgraph.ui.documents.DocumentsScreen
import io.github.hamzatadlaoui.socialgraph.ui.documents.DocumentsViewModel
import io.github.hamzatadlaoui.socialgraph.ui.family.FamilyScreen
import io.github.hamzatadlaoui.socialgraph.ui.family.FamilyViewModel
import io.github.hamzatadlaoui.socialgraph.ui.graph.GraphScreen
import io.github.hamzatadlaoui.socialgraph.ui.graph.GraphViewModel
import io.github.hamzatadlaoui.socialgraph.ui.people.PeopleListScreen
import io.github.hamzatadlaoui.socialgraph.ui.people.PeopleViewModel
import io.github.hamzatadlaoui.socialgraph.ui.people.PersonEditScreen
import io.github.hamzatadlaoui.socialgraph.ui.people.PersonEditViewModel
import io.github.hamzatadlaoui.socialgraph.ui.person.AddRelationshipScreen
import io.github.hamzatadlaoui.socialgraph.ui.person.AddRelationshipViewModel
import io.github.hamzatadlaoui.socialgraph.ui.person.PersonProfileScreen
import io.github.hamzatadlaoui.socialgraph.ui.person.PersonProfileViewModel
import io.github.hamzatadlaoui.socialgraph.ui.settings.SettingsScreen
import io.github.hamzatadlaoui.socialgraph.ui.settings.SettingsViewModel

private object Routes {
    const val PEOPLE = "people"
    const val GRAPH = "graph"
    const val FAMILY = "family"
    const val FILES = "files"
    const val DOCUMENT = "document"
    const val DOCUMENT_ID = "documentId"
    const val EDIT = "edit"
    const val PROFILE = "person"
    const val ADD_TIE = "tie"
    const val SETTINGS = "settings"
    const val PERSON_ID = "personId"

    /** Passing no id means a new person. */
    fun edit(personId: String? = null) = "$EDIT?$PERSON_ID=${personId.orEmpty()}"

    fun profile(personId: String) = "$PROFILE/$personId"

    fun addTie(personId: String) = "$ADD_TIE/$personId"

    fun document(documentId: String) = "$DOCUMENT/$documentId"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.PEOPLE, R.string.tab_people, Icons.Default.Group),
    Tab(Routes.GRAPH, R.string.tab_graph, Icons.Default.Hub),
    Tab(Routes.FAMILY, R.string.tab_family, Icons.Default.AccountTree),
    Tab(Routes.FILES, R.string.tab_files, Icons.Default.Folder),
)

@Composable
fun SocialGraphApp(container: AppContainer) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Scaffold(
        bottomBar = {
            // The bar is for the four ways of looking at the same database;
            // a form opened on top of one of them is not a fifth place to be.
            if (route in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = entry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.switchTo(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(stringResource(tab.label)) },
                            // Material's selected pill is a hard-coded circle, the one
                            // soft edge left in the app. Drop it and let the cyan say
                            // which tab you are on.
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PEOPLE,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.PEOPLE) {
                val viewModel: PeopleViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { PeopleViewModel(container.repository) }
                    },
                )
                PeopleListScreen(
                    viewModel = viewModel,
                    photos = container.photos,
                    onOpenPerson = { id -> navController.navigate(Routes.profile(id)) },
                    onAddPerson = { navController.navigate(Routes.edit()) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SettingsViewModel(
                                container.repository,
                                container.photos,
                                container.documents,
                            )
                        }
                    },
                )
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.GRAPH) {
                val viewModel: GraphViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { GraphViewModel(container.repository) }
                    },
                )
                GraphScreen(
                    viewModel = viewModel,
                    photos = container.photos,
                    onOpenPerson = { id -> navController.navigate(Routes.profile(id)) },
                )
            }

            composable(Routes.FAMILY) {
                val viewModel: FamilyViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { FamilyViewModel(container.repository) }
                    },
                )
                FamilyScreen(
                    viewModel = viewModel,
                    onOpenPerson = { id -> navController.navigate(Routes.profile(id)) },
                )
            }

            composable(Routes.FILES) {
                val viewModel: DocumentsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { DocumentsViewModel(container.repository, container.documents) }
                    },
                )
                DocumentsScreen(
                    viewModel = viewModel,
                    files = container.documents,
                    onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
                )
            }

            composable(
                route = "${Routes.DOCUMENT}/{${Routes.DOCUMENT_ID}}",
                arguments = listOf(navArgument(Routes.DOCUMENT_ID) { type = NavType.StringType }),
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getString(Routes.DOCUMENT_ID).orEmpty()
                val viewModel: DocumentViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            DocumentViewModel(
                                container.repository,
                                container.documents,
                                container.photos,
                                documentId,
                            )
                        }
                    },
                )
                DocumentScreen(
                    viewModel = viewModel,
                    files = container.documents,
                    photos = container.photos,
                    onOpenPerson = { id -> navController.navigate(Routes.profile(id)) },
                    onDeleted = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "${Routes.EDIT}?${Routes.PERSON_ID}={${Routes.PERSON_ID}}",
                arguments = listOf(
                    navArgument(Routes.PERSON_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val personId = backStackEntry.arguments
                    ?.getString(Routes.PERSON_ID)
                    ?.takeIf { it.isNotEmpty() }

                val viewModel: PersonEditViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            PersonEditViewModel(container.repository, container.photos, personId)
                        }
                    },
                )
                PersonEditScreen(
                    viewModel = viewModel,
                    photos = container.photos,
                    onSaved = { savedId ->
                        if (personId == null) {
                            // A person just created opens straight into their page.
                            navController.popBackStack()
                            navController.navigate(Routes.profile(savedId))
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onDeleted = {
                        // Their page went with them, so do not land back on it.
                        navController.popBackStack(Routes.PEOPLE, inclusive = false)
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "${Routes.PROFILE}/{${Routes.PERSON_ID}}",
                arguments = listOf(navArgument(Routes.PERSON_ID) { type = NavType.StringType }),
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getString(Routes.PERSON_ID).orEmpty()
                val viewModel: PersonProfileViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { PersonProfileViewModel(container.repository, personId) }
                    },
                )
                PersonProfileScreen(
                    viewModel = viewModel,
                    photos = container.photos,
                    files = container.documents,
                    onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
                    onEdit = { navController.navigate(Routes.edit(personId)) },
                    onAddRelationship = { navController.navigate(Routes.addTie(personId)) },
                    // Walking to a neighbour keeps the trail, so Back retraces it.
                    onOpenPerson = { other -> navController.navigate(Routes.profile(other)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "${Routes.ADD_TIE}/{${Routes.PERSON_ID}}",
                arguments = listOf(navArgument(Routes.PERSON_ID) { type = NavType.StringType }),
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getString(Routes.PERSON_ID).orEmpty()
                val viewModel: AddRelationshipViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AddRelationshipViewModel(container.repository, personId) }
                    },
                )
                val subject by container.repository.person(personId)
                    .collectAsStateWithLifecycle(initialValue = null)

                AddRelationshipScreen(
                    viewModel = viewModel,
                    subjectName = subject?.fullName.orEmpty(),
                    photos = container.photos,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Switching tabs should never pile the same screen up on the back stack. */
private fun NavHostController.switchTo(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
