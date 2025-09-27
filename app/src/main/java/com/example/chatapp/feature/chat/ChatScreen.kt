package com.example.chatapp.feature.chat

import android.Manifest
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.util.DebugLogger
import com.example.chatapp.R
import com.example.chatapp.feature.home.ChannelItem
import com.example.chatapp.model.Message
import com.example.chatapp.ui.theme.DarkGray
import com.example.chatapp.ui.theme.ForestGreen
import com.example.chatapp.ui.theme.LightGray
import com.example.chatapp.ui.theme.LightGreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    channelId: String,
    channelName: String,
) {
    val viewModel: ChatViewModel = hiltViewModel()
    val chooserDialog = remember {
        mutableStateOf(false)
    }
    val cameraImageUri = remember {
        mutableStateOf<Uri?>(null)
    }

    val cameraImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let {
                viewModel.sendImageMessage(it, channelId)
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendImageMessage(it, channelId)
        }
    }

    fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = ContextCompat.getExternalFilesDirs(
            navController.context, Environment.DIRECTORY_PICTURES
        ).first()
        return FileProvider.getUriForFile(navController.context,
            "${navController.context.packageName}.provider",
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                cameraImageUri.value = Uri.fromFile(this)
            })
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraImageLauncher.launch(createImageUri())
        }
    }

    Scaffold(
        containerColor = LightGray,
        topBar = {
           TopAppBar(
               colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGray),
               title = {
                   ChannelItem(
                       channelName = channelName,
                       Modifier,
                       onClick = {},
                       true,
                       onCall = { callButton->
                           viewModel.getAllUserEmails(channelId) {
                               val list: MutableList<ZegoUIKitUser> = mutableListOf()
                               it.forEach { email ->
                                   Firebase.auth.currentUser?.email?.let { em ->
                                       if(email != em){
                                           list.add(
                                               ZegoUIKitUser(
                                                   email, email
                                               )
                                           )
                                       }
                                   }
                               }
                               callButton.setInvitees(list)
                           }
                       })
               },
           )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {

            val messages = viewModel.messages.collectAsState()

            LaunchedEffect(key1 = true) {
                viewModel.listenForMessages(channelId)
            }

            ChatMessages(
                viewModel = viewModel,
                messages = messages.value,
                onSendMessage = { message ->
                    viewModel.sendMessage(channelId, message)
                },
                onImageClicked = { chooserDialog.value = true },
                channelName = channelName,
                channelID = channelId
            )
        }


        if (chooserDialog.value) {
            ContentSelectionDialog(
                onCameraSelected = {
                    chooserDialog.value = false
                    if (navController.context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        cameraImageLauncher.launch(createImageUri())
                    } else {
                        // Request the camera permission
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGallerySelected = {
                    chooserDialog.value = false
                    imageLauncher.launch("image/*")
                }
            )
        }



    }


}


@Composable
fun ContentSelectionDialog(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {  },
        dismissButton = {
            TextButton(onClick = { onGallerySelected() }) {
                Text(
                    text = "Gallery",
                    //color = Color.White
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCameraSelected() }) {
                Text(
                    text = "Camera",
                    //color = Color.White
                )
            }
        },
        
        title = { Text(text = "Select your Source")},
        
        text = { Text(text = "Would you like to pick an image from the gallery or use the camera?")}
    )
}


@Composable
fun ChatMessages(
    viewModel: ChatViewModel,
    messages: List<Message>,
    channelName: String,
    channelID: String,
    onSendMessage: (String) -> Unit,
    onImageClicked: () -> Unit
) {

    val msg = remember {
        mutableStateOf("")
    }
    val hideKeyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    // Scroll to the last item when a new message arrives
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.scrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,  // Attach the LazyListState to the LazyColumn
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
//            item {
//                ChannelItem(
//                    channelName = channelName,
//                    Modifier,
//                    onClick = {},
//                    true,
//                    onCall = { callButton->
//                    viewModel.getAllUserEmails(channelID) {
//                        val list: MutableList<ZegoUIKitUser> = mutableListOf()
//                        it.forEach { email ->
//                            Firebase.auth.currentUser?.email?.let { em ->
//                                if(email != em){
//                                    list.add(
//                                        ZegoUIKitUser(
//                                            email, email
//                                        )
//                                    )
//                                }
//                            }
//                        }
//                        callButton.setInvitees(list)
//                    }
//                })
//            }
            items(messages) { message ->
                ChatBubble(message = message)
            }
        }

        Box{
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(LightGray)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            IconButton(
                onClick = {
                    onImageClicked()
                    msg.value = ""
                }
            ) {
                Box(
                    modifier = Modifier
                        .background(ForestGreen)
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.attach),
                        contentDescription = "Send",
                    )
                }
            }

            TextField(
                value = msg.value,
                onValueChange = { msg.value = it },
                placeholder = { Text(text = "Type a message", color = Color.Black) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { hideKeyboardController?.hide() }
                ),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedPlaceholderColor = Color.White,
                    unfocusedPlaceholderColor = Color.White,
                    cursorColor = ForestGreen,
                    focusedIndicatorColor = ForestGreen,
                    unfocusedIndicatorColor = ForestGreen,
                ),
                modifier = Modifier.weight(1f)

            )

            IconButton(
                onClick = {
                    onSendMessage(msg.value)
                    msg.value = ""
                }
            ) {
                Box(
                    modifier = Modifier
                        .background(ForestGreen)
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.send),
                        contentDescription = "Send",
                    )
                }
            }
        }
    }
    }

}


@Composable
fun ChatBubble(message: Message) {

    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) {
        LightGreen
    } else {
        Color.White
    }

    fun formatTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(date)
    }

    LaunchedEffect(key1 = message.imageUrl) {
        println("Loading image from URL: ${message.imageUrl}")
    }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)

    ) {
        val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart

        Row(
            modifier = Modifier
                .padding(8.dp)
                .align(alignment),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCurrentUser) {
                Image(
                    painter = painterResource(id = R.drawable.friend),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .background(
                        color = bubbleColor,
                        shape = RoundedCornerShape(
                            topStart = 30.dp,
                            topEnd = 30.dp,
                            bottomEnd = if (isCurrentUser) 0.dp else 30.dp,
                            bottomStart = if (isCurrentUser) 30.dp else 0.dp,
                        )
                    )
                    .padding(16.dp)
            ) {
                if (message.imageUrl != null ) {

                    val context = LocalContext.current
                    val imageLoader = ImageLoader.Builder(context)
                        .logger(DebugLogger())
                        .build()
                    Column{
                    AsyncImage(
                        model = message.imageUrl,
                        placeholder = painterResource(id = R.drawable.placeholder),
                        error = painterResource(id = R.drawable.errorimage),
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp),
                        contentScale = ContentScale.Crop,
                        imageLoader = imageLoader
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatTime(time = message.createdAt),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Bottom)
                        )

                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            // Double check mark icon for sent messages
                            Text(
                                text = "✓✓",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    }
                } else {
                    Column {
                        Text(
                            text = message.message?.trim() ?: "",
                            color = Color.Black,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(time = message.createdAt),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.Bottom)
                            )

                            if (isCurrentUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                // Double check mark icon for sent messages
                                Text(
                                    text = "✓✓",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                    }
                }
            }

        }
    }



}


@Composable
fun CallButton(
    isVideoCall: Boolean,
    onClick: (ZegoSendCallInvitationButton) -> Unit,
    buttonColor: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(45.dp)
            .background(buttonColor, shape = CircleShape) // Shape and background color
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(factory = { context ->
            val button = ZegoSendCallInvitationButton(context).apply {
                setIsVideoCall(isVideoCall)
                resourceID = "zego_data"
            }
            button
        }, modifier = Modifier.size(38.dp)) { zegoCallButton ->
            zegoCallButton.setOnClickListener { _ -> onClick(zegoCallButton) }
        }
    }
}
