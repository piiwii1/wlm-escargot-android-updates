from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Missing expected block: {label}")
    return text.replace(old, new, 1)

# Persist profile photo URI in DataStore
p = Path('android/app/src/main/java/ch/piiwii/streamradar/data/AppStore.kt')
s = p.read_text()
s = replace_once(s,
    '    val region: String = "CH"\n)',
    '    val region: String = "CH",\n    val profileImageUri: String? = null\n)',
    'UserPreferences profileImageUri')
s = replace_once(s,
    '        val region = stringPreferencesKey("region")\n',
    '        val region = stringPreferencesKey("region")\n        val profileImageUri = stringPreferencesKey("profile_image_uri")\n',
    'profile_image_uri key')
s = replace_once(s,
    '            region = prefs[Keys.region] ?: "CH"\n',
    '            region = prefs[Keys.region] ?: "CH",\n            profileImageUri = prefs[Keys.profileImageUri]?.takeIf { it.isNotBlank() }\n',
    'profileImageUri mapping')
s = replace_once(s,
    '    suspend fun setAfterReminderMonths(months: Int) = context.dataStore.edit { it[Keys.afterMonths] = months.coerceIn(1, 12) }\n',
    '    suspend fun setAfterReminderMonths(months: Int) = context.dataStore.edit { it[Keys.afterMonths] = months.coerceIn(1, 12) }\n'
    '    suspend fun setProfileImageUri(uri: String?) = context.dataStore.edit { prefs ->\n'
    '        if (uri.isNullOrBlank()) prefs.remove(Keys.profileImageUri) else prefs[Keys.profileImageUri] = uri\n'
    '    }\n',
    'setProfileImageUri')
p.write_text(s)

# Wire profile editor into Compose app
p = Path('android/app/src/main/java/ch/piiwii/streamradar/StreamRadarApp.kt')
s = p.read_text()
s = replace_once(s,
    'import androidx.compose.foundation.Image\n',
    'import androidx.compose.foundation.Image\n'
    'import androidx.activity.compose.rememberLauncherForActivityResult\n'
    'import androidx.activity.result.contract.ActivityResultContracts\n',
    'activity result imports')
s = replace_once(s,
    '    var updateMessage by remember { mutableStateOf<String?>(null) }\n',
    '    var updateMessage by remember { mutableStateOf<String?>(null) }\n'
    '    var showProfileEditor by remember { mutableStateOf(false) }\n'
    '    val profileImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->\n'
    '        if (uri != null) {\n'
    '            runCatching {\n'
    '                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)\n'
    '            }\n'
    '            scope.launch { store.setProfileImageUri(uri.toString()) }\n'
    '        }\n'
    '    }\n',
    'profile picker state')
s = replace_once(s,
    '    MaterialTheme(colorScheme = StreamRadarColors) {\n',
    '    MaterialTheme(colorScheme = StreamRadarColors) {\n'
    '        if (showProfileEditor) {\n'
    '            ProfileEditorDialog(\n'
    '                profileImageUri = prefs.profileImageUri,\n'
    '                onPickPhoto = { profileImagePicker.launch(arrayOf("image/*")) },\n'
    '                onRemovePhoto = { scope.launch { store.setProfileImageUri(null) } },\n'
    '                onDismiss = { showProfileEditor = false }\n'
    '            )\n'
    '        }\n',
    'profile editor dialog')
s = replace_once(s,
    '                        onOpenNews = { showNews = true }\n',
    '                        onOpenNews = { showNews = true },\n'
    '                        profileImageUri = prefs.profileImageUri,\n'
    '                        onProfileClick = { showProfileEditor = true }\n',
    'home profile args')
s = replace_once(s,
    '                        onFollow = { id -> scope.launch { store.toggleFollow(id) } },\n'
    '                        onOpen = { selectedRelease = it }\n'
    '                    )\n'
    '                    Tab.FOLLOWED',
    '                        onFollow = { id -> scope.launch { store.toggleFollow(id) } },\n'
    '                        onOpen = { selectedRelease = it },\n'
    '                        profileImageUri = prefs.profileImageUri,\n'
    '                        onProfileClick = { showProfileEditor = true }\n'
    '                    )\n'
    '                    Tab.FOLLOWED',
    'search profile args')
s = replace_once(s,
    '    onOpenTab: (Tab) -> Unit, onOpenNews: () -> Unit\n)',
    '    onOpenTab: (Tab) -> Unit, onOpenNews: () -> Unit, profileImageUri: String?, onProfileClick: () -> Unit\n)',
    'HomeScreen signature')
s = replace_once(s,
    '        item { PremiumHomeHeader(syncing = syncing, onRefresh = onRefresh) }',
    '        item { PremiumHomeHeader(syncing = syncing, onRefresh = onRefresh, profileImageUri = profileImageUri, onProfileClick = onProfileClick) }',
    'PremiumHomeHeader call')
s = replace_once(s,
    'private fun PremiumHomeHeader(syncing: Boolean, onRefresh: () -> Unit) {',
    'private fun PremiumHomeHeader(syncing: Boolean, onRefresh: () -> Unit, profileImageUri: String?, onProfileClick: () -> Unit) {',
    'PremiumHomeHeader signature')
s = replace_once(s,
    '        Surface(shape = RoundedCornerShape(50), color = Color(0xFF122233), border = BorderStroke(1.dp, Color(0xFF2B4050)), modifier = Modifier.size(40.dp)) {\n'
    '            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = Color(0xFFC5D0DA), modifier = Modifier.size(31.dp)) }\n'
    '        }',
    '        ProfileAvatar(\n'
    '            profileImageUri = profileImageUri,\n'
    '            onClick = onProfileClick,\n'
    '            modifier = Modifier.size(42.dp)\n'
    '        )',
    'home avatar')
s = replace_once(s,
    '    onFollow: (String) -> Unit, onOpen: (Release) -> Unit\n) {',
    '    onFollow: (String) -> Unit, onOpen: (Release) -> Unit, profileImageUri: String?, onProfileClick: () -> Unit\n) {',
    'SearchScreen signature')
s = replace_once(s,
    '                Surface(shape = RoundedCornerShape(50), color = Color(0xFF122233), modifier = Modifier.size(38.dp)) {\n'
    '                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = Color(0xFFC5D0DA), modifier = Modifier.size(30.dp)) }\n'
    '                }',
    '                ProfileAvatar(\n'
    '                    profileImageUri = profileImageUri,\n'
    '                    onClick = onProfileClick,\n'
    '                    modifier = Modifier.size(40.dp)\n'
    '                )',
    'search avatar')

marker = '@Composable\nprivate fun PremiumHomeHeader'
profile_components = '''@Composable
private fun ProfileAvatar(profileImageUri: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFF122233),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        if (!profileImageUri.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(profileImageUri).crossfade(true).build(),
                contentDescription = "Modifier la photo de profil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(50)),
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) } },
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = Color(0xFFC5D0DA), modifier = Modifier.fillMaxSize().padding(4.dp)) } }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Modifier la photo de profil", tint = Color(0xFFC5D0DA), modifier = Modifier.fillMaxSize().padding(4.dp))
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    profileImageUri: String?,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Photo de profil", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                ProfileAvatar(profileImageUri = profileImageUri, onClick = onPickPhoto, modifier = Modifier.size(112.dp))
                Spacer(Modifier.height(14.dp))
                Text(
                    if (profileImageUri.isNullOrBlank()) "Ajoute une photo. Elle restera enregistrée dans StreamRadar après les mises à jour." else "Appuie sur la photo ou sur le bouton ci-dessous pour la remplacer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (profileImageUri.isNullOrBlank()) "Choisir une photo" else "Changer la photo")
                }
                if (!profileImageUri.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRemovePhoto, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DeleteOutline, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Supprimer la photo")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun PremiumHomeHeader'''
s = replace_once(s, marker, profile_components, 'profile components')
p.write_text(s)

# Version bump
p = Path('android/app/build.gradle.kts')
s = p.read_text()
s = replace_once(s, 'versionCode = 25', 'versionCode = 26', 'versionCode')
s = replace_once(s, 'versionName = "0.25.0"', 'versionName = "0.26.0"', 'versionName')
p.write_text(s)

print('StreamRadar 0.26 profile editor applied successfully')
