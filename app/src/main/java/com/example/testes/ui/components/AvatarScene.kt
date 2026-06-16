package com.example.testes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.Radius
import com.example.testes.ui.theme.Spacing
import io.github.sceneview.SceneView
import io.github.sceneview.environment.rememberKTXEnvironment
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

private const val AvatarGlbAsset = "models/avatar.glb"

@Composable
fun AvatarScene(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hasGlb = remember {
        runCatching {
            context.assets.open(AvatarGlbAsset).close()
            true
        }.getOrDefault(false)
    }
    val shape = RoundedCornerShape(Radius.lg)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f), shape)
            .border(1.dp, CardBorder, shape)
    ) {
        if (hasGlb) {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val environmentLoader = rememberEnvironmentLoader(engine)
            val mainLightNode = rememberMainLightNode(engine) {
                intensity = 42_000f
                position = Position(x = -1.2f, y = 2.0f, z = 2.6f)
            }
            val environment = rememberKTXEnvironment(
                environmentLoader,
                "environments/neutral/neutral_ibl.ktx",
                "environments/neutral/neutral_skybox.ktx"
            )

            if (environment != null) {
                SceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF07101D), shape),
                    engine = engine,
                    modelLoader = modelLoader,
                    environment = environment,
                    mainLightNode = mainLightNode,
                    cameraManipulator = rememberCameraManipulator()
                ) {
                    rememberModelInstance(modelLoader, AvatarGlbAsset)?.let { modelInstance ->
                        ModelNode(
                            modelInstance = modelInstance,
                            scaleToUnits = 1.65f,
                            centerOrigin = Position(y = 0.82f),
                            autoAnimate = true
                        )
                    }
                }
            } else {
                AvatarMissingAssetMessage(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            AvatarMissingAssetMessage(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun AvatarMissingAssetMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Avatar 3D pronto para conectar",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Converta app/src/main/assets/models/avatar.obj para avatar.glb e salve em assets/models/avatar.glb.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
