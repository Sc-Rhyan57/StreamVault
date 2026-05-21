package com.streamvault.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamvault.data.models.ConnectionType
import com.streamvault.ui.theme.StreamColors

@Composable
fun SetupScreen(onComplete: () -> Unit, viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            Text("STREAMVAULT", color = StreamColors.Primary, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Spacer(Modifier.height(8.dp))
            Text("Configure seu servidor", color = StreamColors.TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(48.dp))

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "step"
            ) { step ->
                when (step) {
                    SetupStep.SERVER -> ServerStep(state, viewModel)
                    SetupStep.AUTH   -> AuthStep(state, viewModel, onComplete)
                    SetupStep.PROFILES -> {
                        LaunchedEffect(Unit) { onComplete() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerStep(state: SetupState, vm: SetupViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Servidor", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Insira a URL base da sua API de streaming.", color = StreamColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        StreamTextField(value = state.baseUrl, onValueChange = vm::updateBaseUrl, label = "URL Base (ex: https://api.meusite.com)", icon = Icons.Outlined.Language)
        StreamTextField(value = state.apiKey, onValueChange = vm::updateApiKey, label = "API Key (opcional)", icon = Icons.Outlined.Key)
        StreamTextField(value = state.drmUrl, onValueChange = vm::updateDrmUrl, label = "URL Licença DRM Widevine (opcional)", icon = Icons.Outlined.Lock)

        Text("Tipo de Conexão", color = StreamColors.TextSecondary, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ConnectionType.values().forEach { type ->
                FilterChip(
                    selected = state.connectionType == type,
                    onClick  = { vm.updateConnType(type) },
                    label    = { Text(type.name) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StreamColors.Primary,
                        selectedLabelColor     = Color.White
                    )
                )
            }
        }

        if (state.connectionType == ConnectionType.WEBSOCKET) {
            StreamTextField(value = state.wsUrl, onValueChange = vm::updateWsUrl, label = "WebSocket URL (wss://...)", icon = Icons.Outlined.Wifi)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick  = { vm.saveServer() },
            enabled  = state.baseUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = StreamColors.Primary),
            shape    = RoundedCornerShape(4.dp)
        ) {
            Text("Próximo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AuthStep(state: SetupState, vm: SetupViewModel, onComplete: () -> Unit) {
    var showPassword by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Autenticação", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Faça login na sua conta ou pule se a API não exigir autenticação.", color = StreamColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        StreamTextField(value = state.authEndpoint, onValueChange = vm::updateAuthEndpoint, label = "Endpoint de Login (ex: /auth/login)", icon = Icons.Outlined.Api)
        StreamTextField(value = state.username, onValueChange = vm::updateUsername, label = "Usuário / Email", icon = Icons.Outlined.Person)
        OutlinedTextField(
            value         = state.password,
            onValueChange = vm::updatePassword,
            label         = { Text("Senha", color = StreamColors.TextMuted) },
            leadingIcon   = { Icon(Icons.Outlined.Lock, null, tint = StreamColors.TextMuted) },
            trailingIcon  = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = StreamColors.TextMuted)
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier    = Modifier.fillMaxWidth(),
            colors      = streamTextFieldColors(),
            shape       = RoundedCornerShape(4.dp)
        )

        if (state.error != null) {
            Text(state.error, color = StreamColors.Primary, fontSize = 13.sp)
        }

        Button(
            onClick  = { vm.authenticate(onComplete) },
            enabled  = !state.isLoading && state.username.isNotBlank() && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = StreamColors.Primary),
            shape    = RoundedCornerShape(4.dp)
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Entrar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }

        OutlinedButton(
            onClick  = { vm.skipAuth(onComplete) },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(4.dp),
            border   = BorderStroke(1.dp, StreamColors.TextMuted)
        ) {
            Text("Pular autenticação", color = StreamColors.TextSecondary)
        }
    }
}

@Composable
fun StreamTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = StreamColors.TextMuted, fontSize = 13.sp) },
        leadingIcon   = { Icon(icon, null, tint = StreamColors.TextMuted) },
        modifier      = Modifier.fillMaxWidth(),
        colors        = streamTextFieldColors(),
        shape         = RoundedCornerShape(4.dp),
        singleLine    = true
    )
}

@Composable
fun streamTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = StreamColors.Primary,
    unfocusedBorderColor = StreamColors.Divider,
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = StreamColors.Primary,
    focusedLabelColor    = StreamColors.Primary,
    unfocusedLabelColor  = StreamColors.TextMuted
)
