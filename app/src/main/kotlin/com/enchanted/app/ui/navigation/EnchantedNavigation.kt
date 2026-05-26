package com.enchanted.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.enchanted.app.ui.chat.ChatScreen
import com.enchanted.app.ui.completions.CompletionsScreen
import com.enchanted.app.ui.settings.SettingsScreen
import com.enchanted.app.ui.voice.VoiceScreen
import com.enchanted.app.ui.studio.StudioScreen
import com.enchanted.app.ui.sync.SyncQueueScreen
import com.enchanted.app.ui.mcp.McpDropdownScreen
import com.enchanted.app.ui.mcp.McpOAuthScreen
import com.enchanted.app.ui.mcp.McpPageScreen

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val COMPLETIONS = "completions"
    const val VOICE = "voice"
    const val STUDIO = "studio"
    const val SYNC_QUEUE = "syncQueue"
    const val MCP_DROPDOWN = "mcpDropdown"
    const val MCP_OAUTH = "mcpOAuth"
    const val MCP_PAGE = "mcpPage"
}

@Composable
fun EnchantedNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CHAT
    ) {
        composable(Routes.CHAT) {
            ChatScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToCompletions = {
                    navController.navigate(Routes.COMPLETIONS)
                },
                onNavigateToVoice = {
                    navController.navigate(Routes.VOICE)
                },
                onNavigateToStudio = {
                    navController.navigate(Routes.STUDIO)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.COMPLETIONS) {
            CompletionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.VOICE) {
            VoiceScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.STUDIO) {
            StudioScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SYNC_QUEUE) {
            SyncQueueScreen()
        }

        composable(Routes.MCP_DROPDOWN) {
            McpDropdownScreen()
        }

        composable(Routes.MCP_OAUTH) {
            McpOAuthScreen()
        }

        composable(Routes.MCP_PAGE) {
            McpPageScreen()
        }
    }
}
