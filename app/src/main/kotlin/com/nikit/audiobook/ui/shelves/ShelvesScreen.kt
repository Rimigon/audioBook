package com.nikit.audiobook.ui.shelves

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelvesScreen(vm: ShelvesViewModel = hiltViewModel()) {
    val shelves by vm.shelves.collectAsState()
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Полки", style = MaterialTheme.typography.headlineMedium) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (newName.isNotBlank()) {
                    vm.addShelf(newName)
                    newName = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить полку")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Название полки") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shelves, key = { it.id }) { shelf ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(shelf.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
