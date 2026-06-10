package com.example.testes.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.testes.data.api.ChatApiClient
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.ChatMessageBubble
import com.example.testes.ui.components.VoiceButton
import com.example.testes.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun ChatScreen(viewModel: ChatViewModel, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val chatApiClient = remember { ChatApiClient() }
    var textState by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }
    var lastSpokenMessageId by remember { mutableStateOf("1") }
    var ttsReady by remember { mutableStateOf(false) }
    var voiceResponsesEnabled by rememberSaveable { mutableStateOf(false) }
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val textToSpeech = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(textToSpeech) {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit
            @Deprecated("Deprecated in Android SDK")
            override fun onError(utteranceId: String?) = Unit
        })
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            textToSpeech.language = Locale.forLanguageTag("pt-BR")
            textToSpeech.voices
                ?.bestPortugueseVoice()
                ?.let { textToSpeech.voice = it }
            textToSpeech.setSpeechRate(0.88f)
            textToSpeech.setPitch(1.0f)
        }
    }

    fun stopSpeech() {
        activePlayer?.runCatchingStop()
        activePlayer = null
        textToSpeech.stop()
    }

    fun playRemoteSpeech(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        return runCatching {
            val audioFile = File.createTempFile("titio-renato-", ".mp3", context.cacheDir)
            audioFile.writeBytes(bytes)
            val player = MediaPlayer()
            player.setDataSource(audioFile.absolutePath)
            player.setOnCompletionListener {
                it.release()
                audioFile.delete()
                if (activePlayer === it) {
                    activePlayer = null
                }
                voiceStatus = null
            }
            player.setOnErrorListener { mediaPlayer, _, _ ->
                mediaPlayer.release()
                audioFile.delete()
                if (activePlayer === mediaPlayer) {
                    activePlayer = null
                }
                false
            }
            player.prepare()
            activePlayer = player
            player.start()
            true
        }.getOrDefault(false)
    }

    fun speakWithDeviceVoice(text: String, flush: Boolean = true) {
        val chunks = text.toFluidSpeech().chunkForSpeech()
        if (!ttsReady || chunks.isEmpty()) return

        chunks.forEachIndexed { index, chunk ->
            textToSpeech.speak(
                chunk,
                if (flush && index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) },
                "tutor-${System.currentTimeMillis()}-$index"
            )
            if (index < chunks.lastIndex) {
                textToSpeech.playSilentUtterance(850, TextToSpeech.QUEUE_ADD, "pause-${System.currentTimeMillis()}-$index")
            }
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!voiceResponsesEnabled) return
        scope.launch {
            stopSpeech()
            voiceStatus = "Titio Renato esta preparando a fala."
            val remoteSpeech = chatApiClient.synthesizeSpeech(text)
            val playedRemote = remoteSpeech.getOrNull()?.let { playRemoteSpeech(it) } == true
            if (playedRemote) {
                voiceStatus = "Titio Renato esta falando."
            } else {
                voiceStatus = "Usando a voz do aparelho."
                speakWithDeviceVoice(text, flush)
            }
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
                voiceStatus = "Pode falar sua duvida."
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
                    SpeechRecognizer.ERROR_NO_MATCH -> "Nao entendi. Tente falar novamente."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nao ouvi nada. Toque no microfone e tente de novo."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permita o microfone para conversar por voz."
                    else -> "Nao consegui captar o audio agora."
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()

                if (spokenText.isNullOrBlank()) {
                    voiceStatus = "Nao entendi. Tente novamente."
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
            voiceStatus = "Nao consegui ouvir neste aparelho."
            return
        }
        stopSpeech()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua duvida de Analise Dimensional")
        }
        runCatching {
            speechRecognizer.startListening(intent)
        }.onFailure {
            isListening = false
            voiceStatus = "Nao consegui abrir o microfone agora."
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Titio Renato", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    reverseLayout = false
                ) {
                    item {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                        AppHeroPanel(
                            title = "Converse com o titio Renato",
                            subtitle = "Pergunte sobre formulas, unidades e dimensoes."
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                    }
                    items(messages) { message ->
                        ChatMessageBubble(
                            message = message.text,
                            isFromUser = message.isFromUser
                        )
                    }
                }

                (voiceStatus ?: statusMessage)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Escutar o titio Renato", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = voiceResponsesEnabled,
                        onCheckedChange = { enabled ->
                            voiceResponsesEnabled = enabled
                            voiceStatus = if (enabled) {
                                "Voz do tutor ativada."
                            } else {
                                "Voz do tutor desativada."
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoiceButton(onClick = { requestVoiceInput() }, isListening = isListening)

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        enabled = !isSending,
                        placeholder = { Text(if (isListening) "Ouvindo..." else "Pergunte sobre uma formula...") },
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            messages.lastOrNull { !it.isFromUser }?.let { speak(it.text) }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Ouvir ultima resposta")
                    }

                    IconButton(
                        enabled = !isSending && textState.isNotBlank(),
                        onClick = {
                            viewModel.sendMessage(textState)
                            textState = ""
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if (isSending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun String.toFluidSpeech(): String {
    val answerOnly = substringAfter("Resposta simulada:", this)
    return answerOnly
        .replace(Regex("(?is)Pergunta recebida:.*?Resposta simulada:"), "")
        .replace(Regex("(?im)^\\s*IA F[ií]sica\\s*-\\s*MODO SIMULADO\\s*$"), "")
        .replace(Regex("(?im)^\\s*(N[ií]vel do aluno|Assunto|Pergunta recebida|Pergunta):.*$"), "")
        .replace(Regex("(?im)^\\s*\\d+\\.\\s*"), "")
        .replace(Regex("(?m)^\\s*[-•]\\s*"), "")
        .replace("Dados do problema:", "Vamos pelos dados do problema.")
        .replace("Fórmula usada:", "A formula usada e:")
        .replace("Formula usada:", "A formula usada e:")
        .replace("Trabalho:", "Calculando o trabalho:")
        .replace("Potência:", "Calculando a potencia:")
        .replace("Potencia:", "Calculando a potencia:")
        .replace("Observação:", "Observacao:")
        .replace("Observacao:", "Observacao:")
        .replace("P = W / t", "Potencia e igual ao trabalho dividido pelo tempo.")
        .replace("W = F · d", "Trabalho e igual a forca vezes a distancia.")
        .replace(Regex("\\b([0-9]+(?:[,.][0-9]+)?)\\s*N\\b"), "$1 newtons")
        .replace(Regex("\\b([0-9]+(?:[,.][0-9]+)?)\\s*J\\b"), "$1 joules")
        .replace(Regex("\\b([0-9]+(?:[,.][0-9]+)?)\\s*W\\b"), "$1 watts")
        .replace(Regex("\\bkm/h\\b"), "quilometros por hora")
        .replace(Regex("\\bm/s\\b"), "metros por segundo")
        .replace("v^2", "v ao quadrado")
        .replace("[M]", " dimensao massa ")
        .replace("[L]", " dimensao comprimento ")
        .replace("[T]", " dimensao tempo ")
        .replace("^-1", " elevado a menos um ")
        .replace("^-2", " elevado a menos dois ")
        .replace("=", " igual a ")
        .replace("/", " dividido por ")
        .replace("·", " vezes ")
        .replace("^2", " ao quadrado ")
        .replace(":", ". ")
        .replace(";", ". ")
        .replace("*", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun Set<Voice>.bestPortugueseVoice(): Voice? {
    return filter { voice ->
        voice.locale.language == "pt"
    }.sortedWith(
        compareByDescending<Voice> { if (it.locale.country == "BR") 1 else 0 }
            .thenByDescending { it.quality }
            .thenBy { if (it.isNetworkConnectionRequired) 1 else 0 }
            .thenBy { it.latency }
    ).firstOrNull()
}

private fun String.chunkForSpeech(maxLength: Int = 185): List<String> {
    val sentences = split(Regex("(?<=[.!?])\\s+|(?<=,)\\s+(?=e |mas |entao |agora )"))
    val chunks = mutableListOf<String>()
    var current = StringBuilder()

    sentences.forEach { sentence ->
        if (current.isNotEmpty() && current.length + sentence.length + 1 > maxLength) {
            chunks += current.toString().trim()
            current = StringBuilder()
        }
        if (sentence.length > maxLength) {
            if (current.isNotEmpty()) {
                chunks += current.toString().trim()
                current = StringBuilder()
            }
            sentence.chunked(maxLength).forEach { chunks += it.trim() }
        } else {
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
    }

    if (current.isNotEmpty()) chunks += current.toString().trim()
    return chunks.filter { it.isNotBlank() }
}

private fun String.chunkForHumanSpeech(maxLength: Int = 520): List<String> {
    return chunkForSpeech(maxLength).filter { it.isNotBlank() }
}

private fun MediaPlayer.runCatchingStop() {
    runCatching {
        stop()
        release()
    }
}
