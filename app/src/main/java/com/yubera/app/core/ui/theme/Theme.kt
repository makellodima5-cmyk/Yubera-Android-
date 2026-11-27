package com.yubera.app.core.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.yubera.app.data.settings.ThemeMode
private val DarkPalette=YuberaPalette(Color(0xFF111216),Color(0xFF1A1B20),Color(0xFF202126),Color(0xFFF47724),Color(0xFFFF9A3D),Color(0xFFF2F2F3),Color(0xFFA7A7AD))
private val LightPalette=YuberaPalette(Color(0xFFF7F5F6),Color.White,Color(0xFFECE9EC),Color(0xFFD85D16),Color(0xFFF47724),Color(0xFF1B1920),Color(0xFF6C6872))
@Composable fun YuberaTheme(themeMode:ThemeMode=ThemeMode.SYSTEM,content:@Composable()->Unit){
 val sys=isSystemInDarkTheme()
 val p=when(themeMode){ThemeMode.SYSTEM->if(sys)DarkPalette else LightPalette;ThemeMode.LIGHT->LightPalette;ThemeMode.DARK->DarkPalette}
 val dark=themeMode==ThemeMode.DARK||(themeMode==ThemeMode.SYSTEM&&sys)
 val scheme=if(dark)darkColorScheme(primary=p.primary,secondary=p.accent,background=p.background,surface=p.surface,onBackground=p.onBackground,onSurface=p.onBackground)else lightColorScheme(primary=p.primary,secondary=p.accent,background=p.background,surface=p.surface,onBackground=p.onBackground,onSurface=p.onBackground)
 CompositionLocalProvider(LocalYuberaPalette provides p){MaterialTheme(colorScheme=scheme,content=content)}
}
