package kt.nostr.nosky_compose

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import kt.nostr.nosky_compose.intro.NewProfileScreen
import kt.nostr.nosky_compose.intro.WelcomeScreen
import kt.nostr.nosky_compose.profile.model.ProfileViewModel
import kt.nostr.nosky_compose.reusable_ui_components.theme.BlackSurface
import kt.nostr.nosky_compose.reusable_ui_components.theme.NoskycomposeTheme
import kt.nostr.nosky_compose.reusable_ui_components.theme.Purple500
import kt.nostr.nosky_compose.settings.backend.AppThemeState
import kt.nostr.nosky_compose.settings.backend.ThemeStateSaver

class IntroActivity : ComponentActivity() {

    private val preferenceManager: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
    }

    private lateinit var profileViewModel: ProfileViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val systemTheme = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val darkMode = systemTheme.nightMode == UiModeManager.MODE_NIGHT_YES



        setContent {
            profileViewModel = viewModel(factory = ProfileViewModel.create())


            //val darkMode = isSystemInDarkTheme()
            val appThemeState = rememberSaveable(saver = ThemeStateSaver) {
                AppThemeState(darkMode)
            }


            NoskycomposeTheme(appThemeState.themeState.value) {
                Surface {
                    IntroScreen(appThemeState = appThemeState,
                        profileViewModel = profileViewModel,
                        onLoginClick = {
                            preferenceManager.edit {
                                putBoolean("profile_present", true)
                                putBoolean("dark_theme", appThemeState.isDark())
                            }
                            startActivity(
                                Intent(this@IntroActivity, MainActivity::class.java)
                                //.putExtra(PROFILE_DATA, Profile(pubKey, privKey))
                            )
                            finish()
                        }, onThemeChange = {
                            appThemeState.switchTheme(it)
                            systemTheme.apply {
                                if (it) this.nightMode = UiModeManager.MODE_NIGHT_YES
                                 else nightMode = UiModeManager.MODE_NIGHT_NO
                            }
                        })
                }
            }
        }

    }

}

//@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun IntroScreen(appThemeState: AppThemeState,
                profileViewModel: ProfileViewModel,
                onLoginClick:() -> Unit,
                onThemeChange:(Boolean) -> Unit) {

    var userIsNew by rememberSaveable {
        mutableStateOf(false)
    }


    //For account creation, or for account login/import
    val newUserProfile by profileViewModel.newUserProfile.collectAsState()

    val themeIcon = remember {
        { if (appThemeState.isDark()) Icons.Default.LightMode else Icons.Default.DarkMode }
    }
    val backgroundColor: @Composable () -> Color = remember {
        { if (appThemeState.isDark()) BlackSurface else Purple500 }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = backgroundColor())
    ) {
        AnimatedVisibility(visible = userIsNew,
            enter = fadeIn(), //+ slideInHorizontally(),
            exit = fadeOut() //+ slideOutHorizontally()
        ) {
            NewProfileScreen(themeState = appThemeState,
                userName = { newUserProfile::userName.get() },
                onUserNameUpdate = profileViewModel::updateUserName,
                userBio = { newUserProfile.bio },
                onUserBioUpdate = profileViewModel::updateBio,
                profileImageLink = { newUserProfile.profileImage },
                onImageLinkUpdate = profileViewModel::updateProfileImageLink,
                pubkey = newUserProfile.pubKey,
                generatePubkey = { profileViewModel.generateProfile() },
                onLoginClicked = {
                    profileViewModel.deleteResetProfile()
                    userIsNew = !userIsNew
                },
                onProfileCreated = {})
        }

        AnimatedVisibility(visible = !userIsNew, enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            WelcomeScreen(appThemeState = appThemeState,
                privKey = { newUserProfile.privKey },
                updatePrivKey = profileViewModel::updatePrivKey,
                pubKey = { newUserProfile.pubKey },
                updatePubKey = profileViewModel::updatePubKey,
                onLoginClick = onLoginClick,
                onCreateProfileClick = { userIsNew = !userIsNew })
        }

        Icon(imageVector = themeIcon(),
            contentDescription = "Switch App theme",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(35.dp)
                .padding(end = 5.dp, top = 10.dp)
                .clickable { onThemeChange(!appThemeState.isDark()) }, tint = Color.White)
    }





}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
fun IntroScreenPreview() {
    val darkTheme = rememberSaveable(saver = ThemeStateSaver) {
        AppThemeState(false)
    }
    val testProfile by remember {
        mutableStateOf(ProfileViewModel(SavedStateHandle()))
    }

    NoskycomposeTheme(darkTheme = darkTheme.isDark()) {
        IntroScreen (appThemeState = darkTheme,
            profileViewModel = testProfile,
            onLoginClick = { },
            onThemeChange = {
                darkTheme.switchTheme(it)
            })
    }
}