package com.example.chatapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.chatapp.AppID
import com.example.chatapp.AppSign
import com.example.chatapp.MainActivity
import com.example.chatapp.feature.chat.CallButton
import com.example.chatapp.ui.theme.DarkGray
import com.example.chatapp.ui.theme.LightGray
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    val context = LocalContext.current as MainActivity

    LaunchedEffect(Unit) {
        Firebase.auth.currentUser?.let {
            context.initZegoService(
                appID = AppID,
                appSign = AppSign,
                userID = it.email!!,
                userName = it.displayName ?: ""
            )
        }
    }

    val viewModel = hiltViewModel<HomeViewModel>()
    val channels = viewModel.channels.collectAsState()

    val addChannel = remember {
        mutableStateOf(false)
    }

    val sheetState = rememberModalBottomSheetState()

    var searchText by remember {
        mutableStateOf("")
    }

    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Blue.copy(0.7f))
                    .clickable {
                        addChannel.value = true
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Channel",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(40.dp),
                )
            }
        },

        containerColor = LightGray
    ){
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            LazyColumn {
                item {
                    Text(
                        text = "Messages",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black,
                        style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black)
                    )
                }

                item {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text(text = "Search...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(40.dp)),
                        textStyle = TextStyle(color = Color.LightGray),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            unfocusedIndicatorColor = Color.Blue.copy(0.4f),
                            focusedIndicatorColor = Color.Blue.copy(0.4f),

                        ),
                        trailingIcon = {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = "search")
                        }
                    )
                }

                items(channels.value) { channel ->
                    Column {
                        ChannelItem(
                            channel.name,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            onClick = { navController.navigate("chat/${channel.id}&${channel.name}") },
                            false,
                            onCall = {}
                        )
                    }
                }
            }
        }
    }
    
    if (addChannel.value) {
        
        ModalBottomSheet(
            onDismissRequest = { addChannel.value = false },
            sheetState = sheetState
        ) {
            AddChannelDialog {
                viewModel.addChannel(it)
                addChannel.value = false
            }
        }
    }


}



@Composable
fun ChannelItem(
    channelName: String,
    modifier: Modifier ,
    onClick: () -> Unit,
    shouldShowCallButtons: Boolean = false,
    onCall: (ZegoSendCallInvitationButton) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkGray)
            .clickable {
            onClick()
        },
    ){
    Row(
        modifier = modifier
            .align(Alignment.CenterStart),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Yellow.copy(alpha = 0.3f))

        ) {
            Text(
                text = channelName[0].uppercase(),
                color = Color.White,
                style = TextStyle(fontSize = 30.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }


        Text(text = channelName, modifier = Modifier.padding(8.dp), color = Color.White)
    }

    if (shouldShowCallButtons) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            CallButton(isVideoCall = true, onCall)
            CallButton(isVideoCall = false, onCall)
        }
    }
    }
}



@Composable
fun AddChannelDialog(
    onAddChannel: (String) -> Unit
) {
    val channelName = rememberSaveable {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Add Group")

        Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = channelName.value,
            onValueChange = { channelName.value = it },
            label = { Text(text = "Group Name") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                cursorColor = Color.Blue.copy(0.7f),
                unfocusedIndicatorColor = Color.Blue.copy(0.7f),
                focusedIndicatorColor = Color.Blue.copy(0.7f)
            )
        )

        Button(
            onClick = { onAddChannel(channelName.value) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Add"
            )
        }
    }
}