package com.example.testes.ui.screens

import android.Manifest
import android.content.ClipData
import android.util.Log
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.testes.data.api.ChatApiClient
import com.example.testes.data.api.SessionManager
import com.example.testes.data.local.LocalBackend
import com.example.testes.data.voice.RemoteVoicePlayer
import com.example.testes.data.voice.VoiceSettingsRepository
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.AssistantAvatar
import com.example.testes.ui.components.AvatarScene
import com.example.testes.ui.components.ChatMessageBubble
import com.example.testes.ui.components.GlassCard
import com.example.testes.ui.components.VoiceButton
import com.example.testes.ui.components.aiTextForSpeech
import com.example.testes.viewmodel.ChatViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG_TTS = "TTS"
private const val TAG_ATTACH = "ChatAttachment"

/** Pedido para abrir um launcher de anexo logo quando o Chat é aberto (vindo de atalho externo). */
enum class AttachmentRequest { CameraNow }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onOpenLesson: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    initialAttachmentRequest: AttachmentRequest? = null
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val lastFailedPrompt by viewModel.lastFailedPrompt.collectAsStateWithLifecycle()
    val suggestedQuestions by viewModel.suggestedQuestions.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val chatApiClient = remember { ChatApiClient() }
    val voiceSettingsRepository = remember { VoiceSettingsRepository(context) }
    var textState by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }
    var lastSpokenMessageId by remember { mutableStateOf("1") }
    var voiceResponsesEnabled by rememberSaveable { mutableStateOf(false) }
    var lastSpokenText by remember { mutableStateOf<String?>(null) }
    var speakJob by remember { mutableStateOf<Job?>(null) }
    var localTtsReady by remember { mutableStateOf(false) }
    var showEnableRemoteVoiceDialog by remember { mutableStateOf(false) }

    val localTts = remember {
        TextToSpeech(context.applicationContext) { status ->
            localTtsReady = status == TextToSpeech.SUCCESS
        }
    }

    val voicePlayer = remember {
        RemoteVoicePlayer(context).also { p ->
            p.setListener(object : RemoteVoicePlayer.Listener {
                override fun onPlaybackStarted() { voiceStatus = "Renato está falando." }
                override fun onPlaybackEnded() { voiceStatus = null }
                override fun onPlaybackError(message: String) {
                    voiceStatus = "Voz personalizada indisponível: $message"
                }
            })
        }
    }

    DisposableEffect(voicePlayer) {
        onDispose {
            speakJob?.cancel()
            voicePlayer.release()
            localTts.stop()
            localTts.shutdown()
        }
    }

    LaunchedEffect(localTtsReady) {
        if (localTtsReady) {
            localTts.language = Locale.forLanguageTag("pt-BR")
        }
    }

    fun stopSpeech() {
        speakJob?.cancel()
        voicePlayer.stop()
        localTts.stop()
    }

    fun speakLocal(text: String, reason: String? = null) {
        if (!localTtsReady) {
            voiceStatus = reason ?: "TTS local ainda nao esta pronto."
            return
        }
        voiceStatus = reason?.let { "$it Usando voz local." } ?: "Usando voz local."
        localTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "renato-local-${System.currentTimeMillis()}")
        SessionManager.accessToken?.let { token -> LocalBackend.recordVoiceUsed(token, false) }
    }

    fun speak(text: String, forceRemote: Boolean = false) {
        if (!voiceResponsesEnabled) return
        val speechText = aiTextForSpeech(text)
        if (speechText.isBlank()) return
        speakJob?.cancel()
        speakJob = scope.launch {
            voicePlayer.stop()
            localTts.stop()
            voiceStatus = "Sintetizando voz do Renato no servidor..."
            lastSpokenText = speechText
            val settings = voiceSettingsRepository.getSettings()
            if (!settings.remoteVoiceEnabled && !forceRemote) {
                Log.i(TAG_TTS, "Remote voice disabled by user settings; using local fallback")
                if (settings.fallbackToLocalTts) {
                    speakLocal(speechText, "Voz remota desativada.")
                } else {
                    voiceStatus = "Voz remota desativada nas configuracoes."
                }
                return@launch
            }
            Log.i(TAG_TTS, "Requesting backend speech (${speechText.length} chars)")
            val remoteSpeech = chatApiClient.synthesizeSpeech(speechText)
            remoteSpeech.onSuccess { bytes ->
                Log.i(TAG_TTS, "Received WAV from backend: ${bytes.size} bytes")
                voicePlayer.play(bytes)
                SessionManager.accessToken?.let { token -> LocalBackend.recordVoiceUsed(token, true) }
            }.onFailure { e ->
                Log.e(TAG_TTS, "Backend speech failed: ${e.message}", e)
                if (voiceSettingsRepository.getSettings().fallbackToLocalTts) {
                    speakLocal(speechText, "Voz personalizada indisponivel.")
                    return@onFailure
                }
                voiceStatus = "Voz personalizada indisponível. ${e.message ?: ""}".trim()
            }
        }
    }

    fun retrySpeak() {
        val text = lastSpokenText ?: return
        if (!voiceSettingsRepository.getSettings().remoteVoiceEnabled) {
            showEnableRemoteVoiceDialog = true
        } else {
            speak(text, forceRemote = true)
        }
    }

    LaunchedEffect(voiceResponsesEnabled) {
        if (!voiceResponsesEnabled) {
            stopSpeech()
        }
    }

    val speechRecognizer = remember {
        runCatching {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        }.getOrElse {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                voiceStatus = "Estou ouvindo..."
            }

            override fun onBeginningOfSpeech() {
                voiceStatus = "Pode falar sua dúvida."
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                isListening = false
                voiceStatus = "Processando sua fala..."
            }

            override fun onError(error: Int) {
                isListening = false
                voiceStatus = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi. Tente falar novamente."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada. Toque no microfone e tente de novo."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permita o microfone para conversar por voz."
                    else -> "Não consegui captar o áudio agora."
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()

                if (spokenText.isNullOrBlank()) {
                    voiceStatus = "Não entendi. Tente novamente."
                } else {
                    textState = spokenText
                    voiceStatus = "Pergunta reconhecida: $spokenText"
                    viewModel.sendMessage(spokenText)
                    textState = ""
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { partial ->
                        if (partial.isNotBlank()) {
                            textState = partial
                        }
                    }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        onDispose {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            voiceStatus = "Não consegui ouvir neste aparelho."
            return
        }
        stopSpeech()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua dúvida de Análise Dimensional")
        }
        runCatching {
            speechRecognizer.startListening(intent)
        }.onFailure {
            isListening = false
            voiceStatus = "Não consegui abrir o microfone agora."
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            voiceStatus = "Permita o microfone para conversar por voz."
        }
    }

    fun requestVoiceInput() {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            if (isListening) {
                speechRecognizer?.stopListening()
                isListening = false
            } else {
                startListening()
            }
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
            val lastMessage = messages.last()
            if (!lastMessage.isFromUser && lastMessage.id != lastSpokenMessageId) {
                lastSpokenMessageId = lastMessage.id
                if (voiceResponsesEnabled) {
                    speak(lastMessage.text)
                }
            }
        }
    }

    // -------- Attachment pipeline --------
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    fun kickOff(uri: android.net.Uri, kind: com.example.testes.viewmodel.AttachmentKind) {
        android.util.Log.i(TAG_ATTACH, "kickOff kind=$kind uri=$uri")
        val startedAt = System.currentTimeMillis()
        scope.launch {
            try {
                val file = com.example.testes.data.image.FormulaImageProcessor.compress(context, uri)
                viewModel.sendAttachment(file, uri.toString(), kind, startedAt)
            } catch (e: Throwable) {
                android.util.Log.e(TAG_ATTACH, "Failed to process attachment: ${e.message}", e)
                voiceStatus = "Não consegui ler esse anexo: ${e.message ?: "erro desconhecido"}"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (ok && uri != null) kickOff(uri, com.example.testes.viewmodel.AttachmentKind.Camera)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) kickOff(uri, com.example.testes.viewmodel.AttachmentKind.Gallery)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jpgUri = renderPdfFirstPageToJpgUri(context, uri)
                    if (jpgUri != null) kickOff(jpgUri, com.example.testes.viewmodel.AttachmentKind.Pdf)
                    else voiceStatus = "Não consegui abrir esse PDF."
                } catch (e: Throwable) {
                    android.util.Log.e(TAG_ATTACH, "PDF failed: ${e.message}", e)
                    voiceStatus = "PDF inválido: ${e.message ?: ""}".trim()
                }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            voiceStatus = "Permita a câmera para tirar foto."
        }
    }
    fun openCamera() {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            val uri = createCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(initialAttachmentRequest) {
        if (initialAttachmentRequest == AttachmentRequest.CameraNow) openCamera()
    }

    // -------- UI --------
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.example.testes.R.drawable.chat_space_bg),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f)))

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                androidx.compose.material3.CenterAlignedTopAppBar(
                    title = { Text("Renato", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (messages.size <= 1) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                Text(
                                    "Como posso ajudar?",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Pergunte, fale, tire foto, envie imagem ou PDF.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                AvatarScene(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                suggestedQuestions.take(4).chunked(2).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                        row.forEach { suggestion ->
                                            AssistChip(
                                                onClick = { viewModel.sendMessage(suggestion, source = "sugestao_chat") },
                                                label = { Text(suggestion, maxLines = 2, style = MaterialTheme.typography.labelMedium) },
                                                enabled = !isSending,
                                                modifier = Modifier.weight(1f),
                                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
                                                    labelColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        com.example.testes.ui.components.RenatoMessageBubble(
                            message = message.copy(text = message.text.asRenatoUiText()),
                            onCopy = { text ->
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("Resposta do Renato", text))
                                    )
                                }
                            },
                            onShare = { txt ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, txt)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar"))
                            },
                            onSave = { txt ->
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("Resposta do Renato", txt))
                                    )
                                }
                                voiceStatus = "Resposta copiada para a área de transferência."
                            },
                            onOpenLesson = onOpenLesson
                        )
                    }
                }

                (voiceStatus ?: statusMessage)?.let {
                    val voiceUnavailable = voiceStatus.isVoiceUnavailableStatus()
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it.asRenatoUiText(),
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                voiceUnavailable -> MaterialTheme.colorScheme.error
                                isListening -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                        when {
                            voiceUnavailable && lastSpokenText != null -> {
                                TextButton(onClick = { retrySpeak() }) { Text("Tentar voz") }
                            }
                            lastFailedPrompt != null -> {
                                TextButton(onClick = { viewModel.retryLastFailed() }, enabled = !isSending) {
                                    Text("Tentar de novo")
                                }
                            }
                        }
                    }
                }

                // Composer
                ChatComposer(
                    textState = textState,
                    onTextChange = { textState = it },
                    isSending = isSending,
                    isListening = isListening,
                    voiceEnabled = voiceResponsesEnabled,
                    onToggleVoice = {
                        voiceResponsesEnabled = !voiceResponsesEnabled
                        voiceStatus = if (voiceResponsesEnabled) "Voz ativada" else "Voz desativada"
                    },
                    onMic = { requestVoiceInput() },
                    onSend = {
                        if (textState.isNotBlank() && !isSending) {
                            viewModel.sendMessage(textState)
                            textState = ""
                        }
                    },
                    onOpenAttachmentSheet = { sheetOpen = true }
                )
            }
        }
    }

    if (sheetOpen) {
        AttachmentBottomSheet(
            onDismiss = { sheetOpen = false },
            onPickCamera = { sheetOpen = false; openCamera() },
            onPickGallery = { sheetOpen = false; galleryLauncher.launch("image/*") },
            onPickPdf = { sheetOpen = false; pdfLauncher.launch("application/pdf") }
        )
    }

    if (showEnableRemoteVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showEnableRemoteVoiceDialog = false },
            title = { Text("Ativar voz remota?") },
            text = { Text("A voz personalizada do Renato esta desligada. Voce pode ativar agora ou revisar em Configuracoes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableRemoteVoiceDialog = false
                        voiceSettingsRepository.setRemoteVoiceEnabled(true)
                        retrySpeak()
                    }
                ) { Text("Ativar e tentar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showEnableRemoteVoiceDialog = false }) {
                        Text("Agora nao")
                    }
                    TextButton(
                        onClick = {
                            showEnableRemoteVoiceDialog = false
                            onOpenSettings()
                        }
                    ) { Text("Configuracoes") }
                }
            }
        )
    }
}

private fun String?.isVoiceUnavailableStatus(): Boolean {
    val normalized = this
        ?.lowercase()
        ?.replace("í", "i")
        ?.replace("Ã­", "i")
        ?: return false
    return normalized.contains("voz") && normalized.contains("indisponivel")
}

private fun String.asRenatoUiText(): String {
    return replace("titio Renato", "Renato")
        .replace("Titio Renato", "Renato")
        .replace("formula", "fórmula")
        .replace("duvida", "dúvida")
        .replace("Analise Dimensional", "Análise Dimensional")
}

@androidx.compose.runtime.Composable
private fun ChatComposer(
    textState: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    isListening: Boolean,
    voiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onMic: () -> Unit,
    onSend: () -> Unit,
    onOpenAttachmentSheet: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenAttachmentSheet, enabled = !isSending) {
                Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Anexo", tint = MaterialTheme.colorScheme.primary)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = textState,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                enabled = !isSending,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (textState.isEmpty()) {
                        Text(
                            if (isListening) "Ouvindo..." else "Pergunte ou envie um exercício",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
            IconButton(onClick = onMic) {
                Icon(
                    if (isListening) androidx.compose.material.icons.Icons.Default.MicOff else androidx.compose.material.icons.Icons.Default.Mic,
                    contentDescription = "Falar",
                    tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onToggleVoice) {
                Icon(
                    if (voiceEnabled) androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp
                    else androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (voiceEnabled) "Desativar voz" else "Ativar voz",
                    tint = if (voiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(44.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = if (textState.isNotBlank() && !isSending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onPickPdf: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 16.dp)) {
            Text("Enviar para Renato", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            AttachmentOption("📷", "Tirar foto", "Câmera do aparelho", onPickCamera)
            AttachmentOption("🖼", "Escolher imagem", "Galeria do aparelho", onPickGallery)
            AttachmentOption("📄", "Enviar PDF", "Primeira página do documento", onPickPdf)
        }
    }
}

@androidx.compose.runtime.Composable
private fun AttachmentOption(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, modifier = Modifier.padding(end = 14.dp), style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun createCameraUri(context: android.content.Context): android.net.Uri {
    val dir = java.io.File(context.cacheDir, "chat_photos").apply { mkdirs() }
    val file = java.io.File.createTempFile("photo-", ".jpg", dir)
    return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * Renderiza a primeira página de um PDF (URI) em JPG temp e retorna o Uri do JPG.
 * Usa android.graphics.pdf.PdfRenderer nativo — sem dep extra.
 */
private suspend fun renderPdfFirstPageToJpgUri(
    context: android.content.Context,
    pdfUri: android.net.Uri
): android.net.Uri? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val resolver = context.contentResolver
    val tmp = java.io.File.createTempFile("pdf-", ".pdf", context.cacheDir)
    resolver.openInputStream(pdfUri)?.use { input ->
        tmp.outputStream().use { out -> input.copyTo(out) }
    } ?: return@withContext null
    val pfd = android.os.ParcelFileDescriptor.open(tmp, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = android.graphics.pdf.PdfRenderer(pfd)
    try {
        if (renderer.pageCount < 1) return@withContext null
        val page = renderer.openPage(0)
        val scale = 2  // 2x ~ 144dpi para OCR decente
        val bitmap = android.graphics.Bitmap.createBitmap(page.width * scale, page.height * scale, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        val outDir = java.io.File(context.cacheDir, "chat_photos").apply { mkdirs() }
        val outFile = java.io.File.createTempFile("pdf-page-", ".jpg", outDir)
        outFile.outputStream().use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, stream)
        }
        bitmap.recycle()
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    } finally {
        renderer.close()
        pfd.close()
        tmp.delete()
    }
}
