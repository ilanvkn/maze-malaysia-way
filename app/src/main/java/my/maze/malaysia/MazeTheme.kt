package my.maze.malaysia

import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.DateFormat
import java.util.Date

@Composable fun MazeTheme(appearance:Appearance,content:@Composable (Boolean)->Unit){val night=appearance==Appearance.NIGHT||(appearance==Appearance.SYSTEM&&isSystemInDarkTheme());val colors=if(night)darkColorScheme(primary=Color(0xFF83DFC3),onPrimary=Color(0xFF00382C),primaryContainer=Color(0xFF205246),onPrimaryContainer=Color(0xFFD0F5E4),secondary=Color(0xFFB0D9C9),secondaryContainer=Color(0xFF284B40),onSecondaryContainer=Color(0xFFD7F4E6),background=Color(0xFF101D19),onBackground=Color(0xFFE8F2EA),surfaceVariant=Color(0xFF2B3F34),surfaceContainerLowest=Color(0xFF0C1712),surfaceContainerLow=Color(0xFF14231C),surfaceContainer=Color(0xFF1A2D23),surfaceContainerHigh=Color(0xFF21372B),surfaceContainerHighest=Color(0xFF294334),surface=Color(0xFF162720),onSurface=Color(0xFFE8F2EA),onSurfaceVariant=Color(0xFFB9CCBE),outline=Color(0xFF8CADA0))else lightColorScheme(primary=Color(0xFF0B715F),onPrimary=Color.White,primaryContainer=Color(0xFFDDEFE8),onPrimaryContainer=Color(0xFF103C35),secondary=Color(0xFF0B715F),secondaryContainer=Color(0xFFE2EEE7),onSecondaryContainer=Color(0xFF103C35),background=Color(0xFFF6F9F4),onBackground=Color(0xFF17382D),surfaceVariant=Color(0xFFE4EEE7),surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFF2F7F0),surfaceContainer=Color(0xFFEDF4EC),surfaceContainerHigh=Color(0xFFE5EEE4),surfaceContainerHighest=Color(0xFFDDE9DE),surface=Color(0xFFF6F9F4),onSurface=Color(0xFF17382D),onSurfaceVariant=Color(0xFF496658),outline=Color(0xFF6D8178));MaterialTheme(colorScheme=colors){content(night)}}
fun timeLabel(value:Long?):String=if(value==null)"date unavailable"else DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(value))
