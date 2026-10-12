package com.zephyr.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private val Background = Color(0xFF0B0B0F)
private val Panel = Color(0xFF17171D)
private val Red = Color(0xFFE32636)
private val White = Color(0xFFF5F5F5)
private val Muted = Color(0xFFAAAAAA)

data class GameVersion(
val id: String,
val type: String
)

class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)

    setContent {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Red,
                background = Background,
                surface = Panel
            )
        ) {
            ZephyrScreen()
        }
    }
}

}

@Composable
fun ZephyrScreen() {
val scope = rememberCoroutineScope()

var versions by remember { mutableStateOf(emptyList<GameVersion>()) }
var selectedVersion by remember { mutableStateOf("") }
var versionType by remember { mutableStateOf("Vanilla") }
var accountType by remember { mutableStateOf("Offline") }
var username by remember { mutableStateOf("") }
var loading by remember { mutableStateOf(false) }
var message by remember {
    mutableStateOf("Minecraft sürümleri yüklenmeye hazır.")
}

fun loadVersions() {
    scope.launch {
        loading = true
        message = "Sürümler yükleniyor..."

        try {
            val result = withContext(Dispatchers.IO) {
                if (versionType == "Fabric") {
                    val json = URL(
                        "https://meta.fabricmc.net/v2/versions/game"
                    ).readText()

                    val array = org.json.JSONArray(json)
                    val list = mutableListOf<GameVersion>()

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val version = item.optString("version", "")

                        if (
                            version.matches(
                                Regex("""\d+\.\d+(\.\d+)?""")
                            ) &&
                            version.substringBefore(".").toIntOrNull() == 1 &&
                            (version.split(".").getOrNull(1)
                                ?.toIntOrNull() ?: 0) >= 21
                        ) {
                            list.add(GameVersion(version, "Fabric"))
                        }
                    }

                    list.distinctBy { it.id }
                } else {
                    val json = JSONObject(
                        URL(
                            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
                        ).readText()
                    )

                    val array = json.getJSONArray("versions")
                    val list = mutableListOf<GameVersion>()

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val id = item.optString("id", "")
                        val type = item.optString("type", "")

                        if (
                            type == "release" &&
                            id.matches(
                                Regex("""\d+\.\d+(\.\d+)?""")
                            ) &&
                            id.substringBefore(".").toIntOrNull() == 1 &&
                            (id.split(".").getOrNull(1)
                                ?.toIntOrNull() ?: 0) >= 21
                        ) {
                            list.add(GameVersion(id, "Vanilla"))
                        }
                    }

                    list
                }
            }

            versions = result
            if (result.isNotEmpty()) {
                selectedVersion = result.first().id
                message = "${result.size} sürüm bulundu."
            } else {
                message = "Sürüm bulunamadı."
            }
        } catch (e: Exception) {
            message = "Bağlantı hatası. İnternetini kontrol et."
        } finally {
            loading = false
        }
    }
}

Column(
    modifier = Modifier
        .fillMaxSize()
        .background(Background)
        .padding(18.dp)
) {
    Text(
        "ZEPHYR",
        color = Red,
        fontSize = 32.sp,
        fontWeight = FontWeight.Black
    )

    Text(
        "LAUNCHER MOBILE",
        color = White,
        fontSize = 13.sp,
        letterSpacing = 4.sp
    )

    Spacer(Modifier.height(24.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "MINECRAFT JAVA",
                color = White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Android için Zephyr oyun merkezi",
                color = Muted,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(18.dp))

            Text("SÜRÜM TÜRÜ", color = Red, fontSize = 12.sp)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Vanilla", "Fabric").forEach { type ->
                    FilterChip(
                        selected = versionType == type,
                        onClick = {
                            versionType = type
                            versions = emptyList()
                            selectedVersion = ""
                            loadVersions()
                        },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SÜRÜMLER", color = Red, fontSize = 12.sp)

                TextButton(
                    onClick = { loadVersions() },
                    enabled = !loading
                ) {
                    Text("Yenile")
                }
            }

            if (versions.isEmpty()) {
                Text(
                    "Sürümleri görmek için yükle butonuna bas.",
                    color = Muted,
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 180.dp)
                ) {
                    items(versions, key = { it.id }) { version ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVersion = version.id
                                }
                                .background(
                                    if (selectedVersion == version.id)
                                        Red.copy(alpha = 0.18f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                version.id,
                                color = White,
                                modifier = Modifier.weight(1f)
                            )

                            if (selectedVersion == version.id) {
                                Text("SEÇİLDİ", color = Red, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { loadVersions() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red
                )
            ) {
                Text(if (loading) "YÜKLENİYOR..." else "SÜRÜMLERİ YÜKLE")
            }

            Text(message, color = Muted, fontSize = 12.sp)
        }
    }

    Spacer(Modifier.height(18.dp))

    Text(
        "HESAP SİSTEMİ",
        color = Red,
        fontWeight = FontWeight.Bold
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Offline", "Microsoft").forEach { type ->
            FilterChip(
                selected = accountType == type,
                onClick = { accountType = type },
                label = {
                    Text(
                        if (type == "Offline") "Offline / Cracked"
                        else "Microsoft"
                    )
                }
            )
        }
    }

    if (accountType == "Offline") {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Oyuncu adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Text(
            "Microsoft hesabı girişi sonraki aşamada eklenecek.",
            color = Muted,
            fontSize = 12.sp
        )
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = {
            message = if (selectedVersion.isBlank()) {
                "Önce bir sürüm seç."
            } else if (
                accountType == "Offline" &&
                !username.matches(Regex("[A-Za-z0-9_]{3,16}"))
            ) {
                "Oyuncu adı 3-16 karakter olmalı."
            } else {
                "Arayüz hazır. Oyun başlatma sistemi henüz eklenmedi."
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Red
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            "OYUNU BAŞLAT",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(Modifier.weight(1f))

    Text(
        "ZEPHYR GAMING  •  MOBILE",
        color = Muted,
        fontSize = 10.sp,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

}
