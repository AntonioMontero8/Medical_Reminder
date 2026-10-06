package com.example.medicalreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.medicalreminder.data.PendingRegistration
import com.example.medicalreminder.ui.auth.LoginScreen
import com.example.medicalreminder.ui.auth.RegisterScreen
import com.example.medicalreminder.ui.profile.ProfileSetupScreen
import com.example.medicalreminder.ui.theme.MRColors
import com.example.medicalreminder.ui.theme.MedicalReminderTheme

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROFILE = "profile"
    const val HOME = "home"
}

class MainActivity : ComponentActivity() {
    private val signUpVm: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalReminderTheme {
                Box(Modifier.fillMaxSize().background(MRColors.Background)) { AppNavigation(signUpVm) }
            }
        }
    }
}

private const val DURATION = 350

@Composable
fun AppNavigation(signUpVm: SignUpViewModel) {
    val nav = rememberNavController()
    val left = AnimatedContentTransitionScope.SlideDirection.Left
    val right = AnimatedContentTransitionScope.SlideDirection.Right

    NavHost(
        navController = nav,
        startDestination = Routes.LOGIN,
        enterTransition = { slideIntoContainer(left, tween(DURATION)) + fadeIn(tween(DURATION)) },
        exitTransition = { slideOutOfContainer(left, tween(DURATION), targetOffset = { it / 3 }) + fadeOut(tween(DURATION)) },
        popEnterTransition = { slideIntoContainer(right, tween(DURATION), initialOffset = { it / 3 }) + fadeIn(tween(DURATION)) },
        popExitTransition = { slideOutOfContainer(right, tween(DURATION)) + fadeOut(tween(DURATION)) }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLogin = { _, _ ->
                    // TODO: autenticar contra tu API
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onForgotPassword = { /* TODO */ },
                onGoToRegister = { nav.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegister = { firstName, lastName, email, password, role ->
                    // Paso 1 de 2: aún no se crea la cuenta, se guarda y se pasa al perfil
                    signUpVm.pending = PendingRegistration(firstName, lastName, email, password, role)
                    nav.navigate(Routes.PROFILE)
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            val context = LocalContext.current
            // Muestra los errores del servidor (correo repetido, sin conexión, etc.)
            LaunchedEffect(signUpVm.error) {
                signUpVm.error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    signUpVm.error = null
                }
            }
            val pending = signUpVm.pending
            if (pending == null) {
                LaunchedEffect(Unit) { nav.popBackStack(Routes.REGISTER, inclusive = false) }
                return@composable
            }
            val goHome = { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }; Unit }
            ProfileSetupScreen(
                role = pending.role,
                loading = signUpVm.loading,
                onBack = { if (!signUpVm.loading) nav.popBackStack() },
                onSavePatient = { profile -> signUpVm.submitPatient(profile, goHome) },
                onSaveCaregiver = { profile -> signUpVm.submitCaregiver(profile, goHome) }
            )
        }

        composable(
            Routes.HOME,
            enterTransition = { fadeIn(tween(500)) + scaleIn(initialScale = 0.92f, animationSpec = tween(500)) }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pantalla principal (próximamente)") }
        }
    }
}