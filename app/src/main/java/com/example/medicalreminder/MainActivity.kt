package com.example.medicalreminder

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medicalreminder.data.PendingRegistration
import com.example.medicalreminder.ui.auth.LoginScreen
import com.example.medicalreminder.ui.auth.RegisterScreen
import com.example.medicalreminder.ui.auth.UserRole
import com.example.medicalreminder.ui.home.HomeScreen
import com.example.medicalreminder.ui.link.CaregiverPatientsScreen
import com.example.medicalreminder.ui.link.CaregiverScanScreen
import com.example.medicalreminder.ui.link.LinkErrorToast
import com.example.medicalreminder.ui.link.LinkResponseScreen
import com.example.medicalreminder.ui.link.MirrorPanelScreen
import com.example.medicalreminder.ui.link.PatientLinksScreen
import com.example.medicalreminder.ui.link.PatientQrScreen
import com.example.medicalreminder.ui.profile.ProfileSetupScreen
import com.example.medicalreminder.ui.theme.MRColors
import com.example.medicalreminder.ui.theme.MedicalReminderTheme
import com.example.medicalreminder.ui.auth.ForgotPasswordScreen
import kotlinx.coroutines.delay


object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROFILE = "profile"
    const val HOME = "home"
    const val FORGOT_PASSWORD = "forgot_password"

    // Paciente
    const val CAREGIVERS = "caregivers"
    const val LINK_QR = "link_qr"
    const val LINK_RESPOND = "link_respond/{id}"
    fun linkRespond(id: Int) = "link_respond/$id"

    // Cuidador
    const val PATIENTS = "patients"
    const val LINK_SCAN = "link_scan"
    const val PANEL = "panel/{id}"
    fun panel(id: Int) = "panel/$id"
}

class MainActivity : ComponentActivity() {
    private val signUpVm: SignUpViewModel by viewModels()
    private val authVm: AuthViewModel by viewModels()
    private val linkVm: LinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalReminderTheme {
                Box(Modifier.fillMaxSize().background(MRColors.Background)) {
                    AppNavigation(signUpVm, authVm, linkVm)
                }
            }
        }
    }
}

private const val DURATION = 350

@Composable
fun AppNavigation(signUpVm: SignUpViewModel, authVm: AuthViewModel, linkVm: LinkViewModel) {
    val nav = rememberNavController()
    val left = AnimatedContentTransitionScope.SlideDirection.Left
    val right = AnimatedContentTransitionScope.SlideDirection.Right
    val idArg = listOf(navArgument("id") { type = NavType.IntType })

    // Avisos de error de la vinculación (una sola vez para toda la navegación)
    LinkErrorToast(linkVm)

    // Se decide UNA sola vez al abrir la app: con sesión guardada entra directo a la pantalla principal.
    val startDestination = remember { if (authVm.session.isLoggedIn) Routes.HOME else Routes.LOGIN }

    NavHost(
        navController = nav,
        startDestination = startDestination,
        enterTransition = { slideIntoContainer(left, tween(DURATION)) + fadeIn(tween(DURATION)) },
        exitTransition = { slideOutOfContainer(left, tween(DURATION), targetOffset = { it / 3 }) + fadeOut(tween(DURATION)) },
        popEnterTransition = { slideIntoContainer(right, tween(DURATION), initialOffset = { it / 3 }) + fadeIn(tween(DURATION)) },
        popExitTransition = { slideOutOfContainer(right, tween(DURATION)) + fadeOut(tween(DURATION)) }
    ) {
        composable(
            Routes.LOGIN,
            // Al cerrar sesión desde Home: fundido suave en lugar de deslizar
            enterTransition = {
                if (initialState.destination.route == Routes.HOME)
                    fadeIn(tween(500)) + scaleIn(initialScale = 1.06f, animationSpec = tween(500))
                else
                    slideIntoContainer(left, tween(DURATION)) + fadeIn(tween(DURATION))
            }
        ) {
            val context = LocalContext.current
            LaunchedEffect(authVm.error) {
                authVm.error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    authVm.error = null
                }
            }
            LoginScreen(
                loading = authVm.loading,
                onLogin = { email, password ->
                    authVm.login(email, password) {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    }
                },
                onForgotPassword = { nav.navigate(Routes.FORGOT_PASSWORD) },
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
            enterTransition = { fadeIn(tween(500)) + scaleIn(initialScale = 0.92f, animationSpec = tween(500)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            val context = LocalContext.current
            val live = authVm.session.user
            // Al cerrar sesión, el usuario pasa a null antes de que termine la animación de salida:
            // se conserva el último para no dejar la pantalla en blanco.
            var lastKnown by remember { mutableStateOf(live) }
            LaunchedEffect(live) { if (live != null) lastKnown = live }
            val user = live ?: lastKnown
            val isPatient = user?.role == UserRole.PATIENT

            // Valida el token guardado con el servidor (y refresca el nombre si cambió)
            LaunchedEffect(Unit) {
                authVm.refreshSession {
                    linkVm.reset()
                    Toast.makeText(context, "Tu sesión expiró. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            }
            // El paciente revisa cada 10 s si le llegó una solicitud de vinculación
            LaunchedEffect(isPatient) {
                if (isPatient) {
                    while (true) {
                        linkVm.refreshLinks(silent = true)
                        delay(10_000)
                    }
                }
            }

            if (user != null) {
                HomeScreen(
                    user = user,
                    pendingRequests = if (isPatient) linkVm.links.count { it.pendiente } else 0,
                    onOpenCaregivers = { nav.navigate(Routes.CAREGIVERS) },
                    onOpenPatients = { nav.navigate(Routes.PATIENTS) },
                    onLinkPatient = { nav.navigate(Routes.LINK_SCAN) },
                    onLogout = {
                        nav.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                        authVm.logout()
                        linkVm.reset()
                    }
                )
            }
        }

        // ---------------- Vinculación: paciente ----------------
        composable(Routes.CAREGIVERS) {
            PatientLinksScreen(
                vm = linkVm,
                onBack = { nav.popBackStack() },
                onAddCaregiver = { nav.navigate(Routes.LINK_QR) },
                onOpenLink = { id -> nav.navigate(Routes.linkRespond(id)) }
            )
        }
        composable(Routes.LINK_QR) {
            PatientQrScreen(
                vm = linkVm,
                onBack = { nav.popBackStack() },
                // Llegó una solicitud: se reemplaza la pantalla del QR por la de respuesta
                onOpenLink = { id ->
                    nav.navigate(Routes.linkRespond(id)) { popUpTo(Routes.CAREGIVERS) }
                }
            )
        }
        composable(Routes.LINK_RESPOND, arguments = idArg) { entry ->
            val id = entry.arguments?.getInt("id") ?: return@composable
            LinkResponseScreen(
                vm = linkVm, id = id,
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() }
            )
        }

        // ---------------- Vinculación: cuidador ----------------
        composable(Routes.PATIENTS) {
            CaregiverPatientsScreen(
                vm = linkVm,
                onBack = { nav.popBackStack() },
                onAddPatient = { nav.navigate(Routes.LINK_SCAN) },
                onOpenPanel = { id -> nav.navigate(Routes.panel(id)) }
            )
        }
        composable(Routes.LINK_SCAN) {
            val context = LocalContext.current
            CaregiverScanScreen(
                vm = linkVm,
                onBack = { nav.popBackStack() },
                onRequestSent = {
                    Toast.makeText(context, "Solicitud enviada. El paciente debe aceptarla.", Toast.LENGTH_LONG).show()
                    nav.navigate(Routes.PATIENTS) { popUpTo(Routes.HOME) }
                }
            )
        }
        composable(Routes.PANEL, arguments = idArg) { entry ->
            val id = entry.arguments?.getInt("id") ?: return@composable
            MirrorPanelScreen(
                vm = linkVm, id = id,
                onBack = { nav.popBackStack() },
                onUnlinked = { nav.popBackStack() }
            )
        }


        composable(Routes.FORGOT_PASSWORD) {
            val context = LocalContext.current

            // Escucha errores
            LaunchedEffect(signUpVm.error) {
                signUpVm.error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    signUpVm.error = null
                }
            }

            // Escucha mensaje de éxito
            LaunchedEffect(signUpVm.successMessage) {
                signUpVm.successMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    signUpVm.successMessage = null
                }
            }

            ForgotPasswordScreen(
                loading = signUpVm.loading,
                onSendResetLink = { email ->
                    signUpVm.recoverPassword(email) {
                        nav.popBackStack() // Regresa a la pantalla de Login tras enviar el correo
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
    }
}