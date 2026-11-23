package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HadithDataset
import com.example.data.HadithItem
import com.example.ui.MainViewModel
import com.example.ui.theme.HighlightBlue
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TealGradientEnd
import com.example.ui.theme.TealGradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    initialCategory: String? = null
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val dailyHadith by viewModel.dailyHadith.collectAsState()
    val selectedCategory by viewModel.selectedHadithCategory.collectAsState()
    val searchQuery by viewModel.hadithSearchQuery.collectAsState()
    val filteredHadiths by viewModel.filteredHadiths.collectAsState()
    val favoriteIds by viewModel.favoriteHadithIds.collectAsState()
    val gregorianDate by viewModel.gregorianDateString.collectAsState()
    val hijriDate by viewModel.hijriDateString.collectAsState()

    var showLatinGlobal by remember { mutableStateOf(true) }
    var selectedDetailHadith by remember { mutableStateOf<HadithItem?>(null) }

    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank()) {
            viewModel.selectHadithCategory(initialCategory)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hadits Harian & Pilihan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${HadithDataset.allHadiths.size} Hadits Shahih & Hasan Terverifikasi",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("hadith_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    FilterChip(
                        selected = showLatinGlobal,
                        onClick = { showLatinGlobal = !showLatinGlobal },
                        label = { Text("Latin", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showLatinGlobal) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO CARD: HADITS HARI INI
            item {
                DailyHadithHeroCard(
                    hadith = dailyHadith,
                    dateSubtitle = if (hijriDate.isNotEmpty()) "$gregorianDate • $hijriDate" else gregorianDate,
                    showLatin = showLatinGlobal,
                    isFavorite = favoriteIds.contains(dailyHadith.id),
                    onToggleFavorite = { viewModel.toggleFavoriteHadith(dailyHadith.id) },
                    onNextHadith = { viewModel.cycleDailyHadith() },
                    onCopy = { copyHadithToClipboard(context, dailyHadith) },
                    onShare = { shareHadith(context, dailyHadith) }
                )
            }

            // 2. SEARCH BAR
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateHadithSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hadith_search_input"),
                    placeholder = {
                        Text(
                            "Cari hadits, topik, atau perawi (misal: shalat, bukhari)...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = PrimaryBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateHadithSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // 3. CATEGORY BROWSER HEADER & CHIPS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jelajahi Berdasarkan Kategori",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${filteredHadiths.size} Hadits",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("hadith_category_row")
                    ) {
                        items(HadithDataset.categories) { category ->
                            val isSelected = selectedCategory == category
                            val count = remember(category, favoriteIds) {
                                HadithDataset.getHadithsByCategory(category, favoriteIds).size
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectHadithCategory(category) },
                                label = {
                                    Text(
                                        text = "$category ($count)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(category),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(50.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 4. FILTERED HADITH LIST
            if (filteredHadiths.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedCategory == HadithDataset.CATEGORY_FAVORIT) Icons.Default.BookmarkBorder else Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = PrimaryBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (selectedCategory == HadithDataset.CATEGORY_FAVORIT) {
                                    "Belum ada Hadits Favorit"
                                } else {
                                    "Hadits tidak ditemukan"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (selectedCategory == HadithDataset.CATEGORY_FAVORIT) {
                                    "Ketuk ikon simpan/favorit pada kartu hadits untuk menyimpannya di sini."
                                } else {
                                    "Coba gunakan kata kunci lain atau pilih kategori Semua."
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredHadiths, key = { it.id }) { hadith ->
                    HadithListItemCard(
                        hadith = hadith,
                        showLatin = showLatinGlobal,
                        isFavorite = favoriteIds.contains(hadith.id),
                        onToggleFavorite = { viewModel.toggleFavoriteHadith(hadith.id) },
                        onClickDetail = { selectedDetailHadith = hadith },
                        onCopy = { copyHadithToClipboard(context, hadith) },
                        onShare = { shareHadith(context, hadith) }
                    )
                }
            }
        }
    }

    // Detail Dialog when user taps a Hadith item
    selectedDetailHadith?.let { hadith ->
        AlertDialog(
            onDismissRequest = { selectedDetailHadith = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = HighlightBlue,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = hadith.category,
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = hadith.grade,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = hadith.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Arabic Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HighlightBlue.copy(alpha = 0.5f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = hadith.arabic,
                            fontSize = 21.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Latin
                    Text(
                        text = hadith.latin,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = PrimaryBlue,
                        lineHeight = 19.sp
                    )

                    // Indonesian Translation
                    Text(
                        text = "\"${hadith.translation}\"",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "— ${hadith.narrator}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    // Hikmah Box
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Hikmah & Kandungan Hadits",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryBlue
                                )
                            }
                            Text(
                                text = hadith.hikmah,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { shareHadith(context, hadith) }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan")
                    }
                    Button(onClick = { selectedDetailHadith = null }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}

@Composable
private fun DailyHadithHeroCard(
    hadith: HadithItem,
    dateSubtitle: String,
    showLatin: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onNextHadith: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_hadith_hero_card"),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(TealGradientStart, TealGradientEnd)
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "HADITS HARI INI",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFFB9F6CA),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = hadith.grade,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNextHadith,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Ganti Hadits Pilihan",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (dateSubtitle.isNotEmpty()) {
                    Text(
                        text = dateSubtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                // Title
                Text(
                    text = hadith.title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                )

                // Arabic Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = hadith.arabic,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 35.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Optional Latin
                AnimatedVisibility(visible = showLatin) {
                    Text(
                        text = hadith.latin,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }

                // Translation
                Text(
                    text = "\"${hadith.translation}\"",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp
                )

                // Hikmah Pill Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.16f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "💡 Hikmah Hari Ini:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = hadith.hikmah,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Bottom Bar: Narrator + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hadith.narrator,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Kategori: ${hadith.category}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Simpan Favorit",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin Hadits",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Bagikan Hadits",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithListItemCard(
    hadith: HadithItem,
    showLatin: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClickDetail: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        onClick = onClickDetail,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hadith_card_${hadith.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Category & Verified Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = HighlightBlue,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = hadith.category,
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = hadith.grade,
                                color = Color(0xFF2E7D32),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favorit",
                        tint = if (isFavorite) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Hadith Title
            Text(
                text = hadith.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Arabic Text
            Text(
                text = hadith.arabic,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Latin Text
            AnimatedVisibility(visible = showLatin) {
                Text(
                    text = hadith.latin,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = PrimaryBlue,
                    lineHeight = 18.sp
                )
            }

            // Translation
            Text(
                text = "\"${hadith.translation}\"",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Footer: Narrator & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hadith.narrator,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onClickDetail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Hikmah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Salin",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Bagikan",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: String) = when (category) {
    HadithDataset.CATEGORY_IBADAH -> Icons.Default.AccessTime
    HadithDataset.CATEGORY_AKHLAK -> Icons.Default.Favorite
    HadithDataset.CATEGORY_ILMU -> Icons.Default.School
    HadithDataset.CATEGORY_SOSIAL -> Icons.Default.Groups
    HadithDataset.CATEGORY_DZIKIR -> Icons.Default.AutoAwesome
    HadithDataset.CATEGORY_SABAR -> Icons.Default.Spa
    HadithDataset.CATEGORY_FAVORIT -> Icons.Default.Bookmark
    else -> Icons.Default.Apps
}

fun copyHadithToClipboard(context: Context, hadith: HadithItem) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = buildString {
        appendLine("📖 ${hadith.title}")
        appendLine()
        appendLine(hadith.arabic)
        appendLine()
        appendLine("\"${hadith.translation}\"")
        appendLine("(${hadith.narrator} - ${hadith.grade})")
        appendLine()
        appendLine("💡 Hikmah: ${hadith.hikmah}")
    }
    clipboard.setPrimaryClip(ClipData.newPlainText("Hadits Harian", text))
    Toast.makeText(context, "Hadits berhasil disalin", Toast.LENGTH_SHORT).show()
}

fun shareHadith(context: Context, hadith: HadithItem) {
    val text = buildString {
        appendLine("📖 *${hadith.title}*")
        appendLine()
        appendLine(hadith.arabic)
        appendLine()
        appendLine("_${hadith.latin}_")
        appendLine()
        appendLine("\"${hadith.translation}\"")
        appendLine("— *${hadith.narrator}* (${hadith.grade})")
        appendLine()
        appendLine("💡 *Hikmah:* ${hadith.hikmah}")
        appendLine()
        appendLine("Dibagikan dari aplikasi Payo Sholat!")
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Bagikan Hadits")
    context.startActivity(shareIntent)
}
