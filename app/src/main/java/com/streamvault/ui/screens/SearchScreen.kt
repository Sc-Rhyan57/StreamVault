package com.streamvault.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamvault.data.models.MediaItem
import com.streamvault.data.repository.Result
import com.streamvault.data.repository.StreamRepository
import com.streamvault.ui.components.LoadingShimmer
import com.streamvault.ui.components.MediaCard
import com.streamvault.ui.theme.StreamColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: StreamRepository
) : ViewModel() {

    private val _query   = MutableStateFlow("")
    private val _results = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _error   = MutableStateFlow<String?>(null)

    val query: StateFlow<String>           = _query.asStateFlow()
    val results: StateFlow<List<MediaItem>> = _results.asStateFlow()
    val loading: StateFlow<Boolean>        = _loading.asStateFlow()
    val error: StateFlow<String?>          = _error.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) { _results.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(300)
            _loading.value = true
            _error.value   = null
            when (val r = repository.searchContent(q)) {
                is Result.Success -> _results.value = r.data
                is Result.Error   -> { _error.value = r.message; _results.value = emptyList() }
                else -> {}
            }
            _loading.value = false
        }
    }

    fun clear() { _query.value = ""; _results.value = emptyList() }
}

@Composable
fun SearchScreen(onItemClick: (MediaItem) -> Unit, onBack: () -> Unit, vm: SearchViewModel = hiltViewModel()) {
    val query   by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val loading by vm.loading.collectAsState()
    val error   by vm.error.collectAsState()
    val focus   = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value         = query,
                onValueChange = vm::onQueryChange,
                placeholder   = { Text("Busque séries, filmes, jogos...", color = StreamColors.TextMuted) },
                leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = StreamColors.TextMuted) },
                trailingIcon  = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { vm.clear() }) {
                            Icon(Icons.Outlined.Close, null, tint = StreamColors.TextMuted)
                        }
                    }
                },
                modifier      = Modifier.weight(1f).focusRequester(focus),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = StreamColors.Primary,
                    unfocusedBorderColor = StreamColors.Divider,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = StreamColors.Primary
                ),
                shape         = RoundedCornerShape(4.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.onQueryChange(query) })
            )
            TextButton(onClick = onBack) {
                Text("Cancelar", color = StreamColors.TextSecondary)
            }
        }

        when {
            loading -> SearchShimmer()
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = StreamColors.TextSecondary)
            }
            results.isEmpty() && query.isNotBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SearchOff, null, tint = StreamColors.TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhum resultado para \"$query\"", color = StreamColors.TextSecondary, fontSize = 14.sp)
                }
            }
            results.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { item ->
                    MediaCard(
                        item     = item,
                        onClick  = { onItemClick(item) },
                        modifier = Modifier.aspectRatio(2f / 3f)
                    )
                }
            }
            else -> SearchEmpty()
        }
    }
}

@Composable
private fun SearchEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Search, null, tint = StreamColors.TextMuted, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Séries e filmes recomendados", color = StreamColors.TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SearchShimmer() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp)
    ) {
        items(12) {
            LoadingShimmer(Modifier.aspectRatio(2f / 3f))
        }
    }
}
