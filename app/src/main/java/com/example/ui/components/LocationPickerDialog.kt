package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationPreset
import com.example.data.PrayerCountry
import com.example.data.PrayerLocationCatalog

@Composable
fun LocationPickerDialog(
    currentLocationName: String,
    currentCountry: PrayerCountry,
    currentJakimZone: String?,
    onSelectLocation: (LocationPreset) -> Unit,
    onUseGps: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCountryTab by remember(currentCountry) { mutableStateOf(currentCountry) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(selectedCountryTab, searchQuery) {
        val base = when (selectedCountryTab) {
            PrayerCountry.INDONESIA -> PrayerLocationCatalog.indonesiaPresets
            PrayerCountry.MALAYSIA -> PrayerLocationCatalog.malaysiaPresets
        }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            base
        } else {
            base.filter { preset ->
                preset.name.lowercase().contains(q) ||
                    preset.region.lowercase().contains(q) ||
                    (preset.jakimZone?.lowercase()?.contains(q) == true) ||
                    (preset.daerahDetail?.lowercase()?.contains(q) == true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Pilih Lokasi Jadwal Shalat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Indonesia (Kemenag RI) & Malaysia (JAKIM)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Auto GPS Detect Button
                FilledTonalButton(
                    onClick = {
                        onUseGps()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_detect_gps_location"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deteksi Lokasi GPS Otomatis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Country Switcher Tabs (Indonesia vs Malaysia)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCountryTab == PrayerCountry.INDONESIA,
                        onClick = { selectedCountryTab = PrayerCountry.INDONESIA },
                        label = {
                            Text(
                                text = "🇮🇩 Indonesia",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_country_indonesia")
                    )
                    FilterChip(
                        selected = selectedCountryTab == PrayerCountry.MALAYSIA,
                        onClick = { selectedCountryTab = PrayerCountry.MALAYSIA },
                        label = {
                            Text(
                                text = "🇲🇾 Malaysia",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_country_malaysia")
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_search_input"),
                    placeholder = {
                        Text(
                            text = if (selectedCountryTab == PrayerCountry.MALAYSIA) {
                                "Cari negeri, daerah, atau zon (mis: WLY01, KL, Johor)..."
                            } else {
                                "Cari kota atau provinsi..."
                            },
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Official Source Note
                Text(
                    text = if (selectedCountryTab == PrayerCountry.MALAYSIA) {
                        "Sumber resmi: JAKIM e-Solat Malaysia (Waktu MYT / GMT+8)"
                    } else {
                        "Sumber resmi: Kemenag RI via Equran.id (WIB/WITA/WIT)"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Location List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        item {
                            Text(
                                text = "Lokasi tidak ditemukan.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(filteredList) { city ->
                            val isSelected = if (city.country == PrayerCountry.MALAYSIA && !currentJakimZone.isNullOrBlank()) {
                                city.jakimZone.equals(currentJakimZone, ignoreCase = true)
                            } else {
                                currentLocationName.equals(city.name, ignoreCase = true)
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectLocation(city)
                                        onDismiss()
                                    },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = city.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!city.jakimZone.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = city.jakimZone,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (!city.daerahDetail.isNullOrBlank()) {
                                                "${city.region} • ${city.daerahDetail}"
                                            } else {
                                                city.region
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Dipilih",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}
