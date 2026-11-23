package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Ayat
import com.example.data.Surah
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun IslamicStarNumber(
    number: Int,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryBlue
) {
    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = this.size.minDimension
            val radius = size / 2f
            val centerX = size / 2f
            val centerY = size / 2f
            
            val path = Path().apply {
                for (i in 0 until 8) {
                    val angle1 = i * Math.PI / 4
                    val angle2 = angle1 + Math.PI / 8
                    
                    val x1 = (centerX + radius * Math.cos(angle1)).toFloat()
                    val y1 = (centerY + radius * Math.sin(angle1)).toFloat()
                    
                    val x2 = (centerX + (radius * 0.75f) * Math.cos(angle2)).toFloat()
                    val y2 = (centerY + (radius * 0.75f) * Math.sin(angle2)).toFloat()
                    
                    if (i == 0) {
                        moveTo(x1, y1)
                    } else {
                        lineTo(x1, y1)
                    }
                    lineTo(x2, y2)
                }
                close()
            }
            drawPath(
                path = path,
                color = tint.copy(alpha = 0.12f),
                style = Fill
            )
            drawPath(
                path = path,
                color = tint.copy(alpha = 0.6f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        Text(
            text = number.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    initialSurahId: Int? = null
) {
    var selectedSurahId by remember { mutableStateOf<Int?>(initialSurahId) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("Surat") }

    val surahs by viewModel.quranSurahs.collectAsState()
    val isQuranLoading by viewModel.isQuranLoading.collectAsState()
    val currentSurahDetail by viewModel.currentSurahDetail.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadQuranSurahList()
    }

    LaunchedEffect(selectedSurahId) {
        selectedSurahId?.let { id ->
            viewModel.loadSurahDetail(id)
        }
    }

    // Intercept system back when viewing a Surah detail to return to the Surah list
    BackHandler(enabled = selectedSurahId != null) {
        viewModel.stopQuranAudio()
        selectedSurahId = null
    }

    // Ensure Quran audio stops cleanly when leaving QuranScreen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopQuranAudio()
        }
    }

    val context = LocalContext.current
    
    AnimatedContent(
        targetState = selectedSurahId,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "QuranNavigation"
    ) { surahId ->
        if (surahId == null) {
            // Surah List Screen
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { 
                                    viewModel.stopQuranAudio()
                                    onBack() 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Book Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(HighlightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title
                        Column {
                            Text(
                                text = "Al-Quran",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "114 Surat",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("quran_search_input"),
                        placeholder = { Text("Cari surat...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )

                    // Tab Indicators (Surat, Makkiyah, Madaniyah)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf("Surat", "Makkiyah", "Madaniyah")
                        tabs.forEach { tab ->
                            val isActive = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isActive) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { activeTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                  Text(
                                    text = tab,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredSurahs = remember(searchQuery, activeTab, surahs) {
                        surahs.filter { surah ->
                            val matchesSearch = surah.name.contains(searchQuery, ignoreCase = true) ||
                                    surah.translation.contains(searchQuery, ignoreCase = true)
                            val matchesTab = when (activeTab) {
                                "Makkiyah" -> surah.type.lowercase().contains("mekah") || surah.type.lowercase().contains("makkiyah")
                                "Madaniyah" -> surah.type.lowercase().contains("madinah") || surah.type.lowercase().contains("madaniyah")
                                else -> true
                            }
                            matchesSearch && matchesTab
                        }
                    }

                    if (filteredSurahs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isQuranLoading) {
                                CircularProgressIndicator(color = PrimaryBlue)
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = PrimaryBlue.copy(alpha = 0.4f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Surah tidak ditemukan",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredSurahs) { surah ->
                                SurahRowItem(surah = surah) {
                                    selectedSurahId = surah.id
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Surah Detail Reader Screen
            val surah = currentSurahDetail
            val bookmark by viewModel.bookmarkState.collectAsState()
            
            val playingSurahId by viewModel.playingSurahId.collectAsState()
            val playingAyatNumber by viewModel.playingAyatNumber.collectAsState()
            val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
            val isAudioLoading by viewModel.isAudioLoading.collectAsState()

            val listState = rememberLazyListState()

            // Smooth scroll to playing Ayat automatically!
            LaunchedEffect(playingAyatNumber) {
                playingAyatNumber?.let { num ->
                    if (playingSurahId == surahId && surah != null) {
                        val indexInList = surah.ayats.indexOfFirst { it.number == num }
                        if (indexInList != -1) {
                            val basmalahOffset = if (surah.id != 1 && surah.id != 9) 2 else 1
                            val targetIndex = indexInList + basmalahOffset
                            
                            // Calculate scroll offset to position the highlighted item near the center of the screen
                            val visibleHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                            val offset = if (visibleHeight > 0) {
                                - (visibleHeight / 2 - 180)
                            } else {
                                -400 // fallback in pixels
                            }
                            
                            listState.animateScrollToItem(
                                index = targetIndex,
                                scrollOffset = offset
                            )
                        }
                    }
                }
            }

            if (surah == null) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Memuat Surah...") },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    selectedSurahId = null 
                                    viewModel.stopQuranAudio()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // 1. Scrollable List of Surah Content
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // Big Gradient Header containing Surah Title info
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(TealGradientStart, TealGradientEnd)
                                        ),
                                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                                    )
                                    .statusBarsPadding()
                                    .padding(top = 56.dp + 16.dp, bottom = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = surah.arabicName,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Serif
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = surah.name,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = surah.translation,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Badges Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isMakkiyah = surah.type.lowercase().contains("mekah") || surah.type.lowercase().contains("makkiyah")
                                        val typeText = if (isMakkiyah) "Makkiyah" else "Madaniyah"
                                        
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(typeText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("${surah.totalAyat} Ayat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }

                        // Basmalah (exclude for Al-Fatihah/At-Taubah)
                        if (surah.id != 1 && surah.id != 9) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        fontFamily = FontFamily.Serif,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Ayats List
                        items(surah.ayats) { ayat ->
                            val isPlayingThis = playingSurahId == surah.id && playingAyatNumber == ayat.number && isAudioPlaying
                            val isLoadingThis = playingSurahId == surah.id && playingAyatNumber == ayat.number && isAudioLoading
                            
                            AyatCardItem(
                                ayat = ayat,
                                isPlaying = isPlayingThis,
                                isLoading = isLoadingThis,
                                isBookmarked = bookmark?.surahId == surah.id && bookmark?.ayatNumber == ayat.number,
                                onBookmarkClick = {
                                    if (bookmark?.surahId == surah.id && bookmark?.ayatNumber == ayat.number) {
                                        viewModel.deleteQuranBookmark()
                                        Toast.makeText(context, "Penanda ayat dihapus!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.saveQuranBookmark(surah.id, surah.name, ayat.number)
                                        Toast.makeText(context, "Ditandai: ${surah.name} Ayat ${ayat.number}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPlayClick = {
                                    viewModel.togglePlayPauseQuranAudio(surah.id, ayat.number)
                                }
                            )
                        }

                        // Surah Selanjutnya Button
                        if (surah.id < 114) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        viewModel.stopQuranAudio()
                                        selectedSurahId = surah.id + 1
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Surah Selanjutnya", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // 2. Floating Sticky Top Bar: Back Button & Qori selector
                    val isScrolled by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 30
                        }
                    }

                    val topBarBgColor = if (isScrolled) {
                        TealGradientEnd
                    } else {
                        Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(topBarBgColor)
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                selectedSurahId = null
                                viewModel.stopQuranAudio()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color.White
                                )
                            }

                            var showQoriDropdown by remember { mutableStateOf(false) }
                            val selectedQori by viewModel.selectedQori.collectAsState()

                            Box {
                                TextButton(
                                    onClick = { showQoriDropdown = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(selectedQori.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showQoriDropdown,
                                    onDismissRequest = { showQoriDropdown = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    viewModel.qoriList.forEach { qori ->
                                        val isSelected = qori.id == selectedQori.id
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = qori.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PrimaryBlue else Color.Black
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectQori(qori)
                                                showQoriDropdown = false
                                            },
                                            leadingIcon = {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SurahRowItem(
    surah: Surah,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("surah_item_${surah.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Star Number Frame
            IslamicStarNumber(number = surah.id, tint = PrimaryBlue)

            Spacer(modifier = Modifier.width(16.dp))

            // Surah Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isMakkiyah = surah.type.lowercase().contains("mekah") || surah.type.lowercase().contains("makkiyah")
                    val badgeBg = if (isMakkiyah) Color(0xFFE8EAF6) else Color(0xFFE3F2FD)
                    val badgeText = if (isMakkiyah) Color(0xFF1A237E) else Color(0xFF1565C0)
                    val typeText = if (isMakkiyah) "Makkiyah" else "Madaniyah"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }

                    Text(
                        text = "${surah.totalAyat} ayat",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Arabic Spelling and Translation
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.arabicName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = surah.translation,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun AyatCardItem(
    ayat: Ayat,
    isPlaying: Boolean,
    isLoading: Boolean,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val highlightColor = PrimaryBlue
    val cardBorder = if (isPlaying) {
        androidx.compose.foundation.BorderStroke(1.5.dp, highlightColor)
    } else null

    val cardBg = if (isPlaying) {
        HighlightBlue.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Star Number on Left, Book and Play/Pause Actions on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IslamicStarNumber(number = ayat.number, tint = if (isPlaying) highlightColor else Color.Gray)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bookmark / Book Option
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Ayat",
                            tint = if (isBookmarked) highlightColor else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play / Pause Option
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = highlightColor,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(6.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        IconButton(
                            onClick = onPlayClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isPlaying) highlightColor else Color.Transparent,
                                contentColor = if (isPlaying) Color.White else Color.Gray
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                modifier = Modifier.size(24.dp),
                                tint = if (isPlaying) Color.White else highlightColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Arabic Text
            Text(
                text = ayat.arabic,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 44.sp,
                fontFamily = FontFamily.Serif
            )

            if (ayat.latin.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                // Transliteration (Latin)
                Text(
                    text = ayat.latin,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = SecondaryBlue,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Indonesian translation
            Text(
                text = ayat.translation,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
        }
    }
}
