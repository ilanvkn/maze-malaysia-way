package my.maze.malaysia

import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.DateFormat
import java.util.Date

@Composable fun MazeTheme(appearance:Appearance,content:@Composable (Boolean)->Unit){val night=appearance==Appearance.NIGHT||(appearance==Appearance.SYSTEM&&isSystemInDarkTheme());val colors=if(night)darkColorScheme(primary=Color(0xFFBAC4FF),onPrimary=Color(0xFF14205F),primaryContainer=Color(0xFF293465),onPrimaryContainer=Color(0xFFE1E6FF),secondary=Color(0xFFAFE4EF),secondaryContainer=Color(0xFF183D49),onSecondaryContainer=Color(0xFFD1F4FA),background=Color(0xFF101623),onBackground=Color(0xFFF0F3FC),surfaceVariant=Color(0xFF303A50),surfaceContainerLowest=Color(0xFF0B101A),surfaceContainerLow=Color(0xFF131B2B),surfaceContainer=Color(0xFF182236),surfaceContainerHigh=Color(0xFF202C43),surfaceContainerHighest=Color(0xFF29364F),surface=Color(0xFF182132),onSurface=Color(0xFFF0F3FC),onSurfaceVariant=Color(0xFFBAC5D9),outline=Color(0xFF8C9AB5))else lightColorScheme(primary=Color(0xFF354FD6),onPrimary=Color.White,primaryContainer=Color(0xFFE4E9FF),onPrimaryContainer=Color(0xFF192C82),secondary=Color(0xFF354FD6),secondaryContainer=Color(0xFFDAEEF3),onSecondaryContainer=Color(0xFF192C82),background=Color(0xFFF7F8FC),onBackground=Color(0xFF182239),surfaceVariant=Color(0xFFE5E9F2),surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFF2F4FA),surfaceContainer=Color(0xFFEDF0F8),surfaceContainerHigh=Color(0xFFE7EBF5),surfaceContainerHighest=Color(0xFFDDE3F0),surface=Color(0xFFF7F8FC),onSurface=Color(0xFF182239),onSurfaceVariant=Color(0xFF515E76),outline=Color(0xFF74809A));MaterialTheme(colorScheme=colors){content(night)}}
fun timeLabel(value:Long?):String=if(value==null)"date unavailable"else DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(value))
