package com.amond.kmpbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Gray1 = Color(0xFF222222)
private val Gray3 = Color(0xFF999999)
private val Gray4 = Color(0xFFE0E0E0)
private val GrayBg = Color(0xFFF7F7F7)

@Composable
fun SchoolSetupScreen(
    viewModel: MealViewModel,
    isChanging: Boolean = false,
    onDone: () -> Unit
) {
    val searchState by viewModel.searchState.collectAsState()
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var apiKey by remember { mutableStateOf(TextFieldValue(viewModel.prefs.apiKey)) }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        if (isChanging) {
            Text(
                text = "← 취소",
                fontSize = 14.sp,
                color = Gray3,
                modifier = Modifier
                    .clickable { onDone() }
                    .padding(bottom = 24.dp)
            )
        }

        Text(
            text = "학교 설정",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "학교 이름으로 검색하세요",
            fontSize = 14.sp,
            color = Gray3,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // API 키 입력
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                viewModel.prefs.apiKey = it.text.trim()
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("NEIS API 키 (open.neis.go.kr)", color = Gray3, fontSize = 13.sp) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = Gray1),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = Gray4,
                focusedContainerColor = White,
                unfocusedContainerColor = GrayBg,
                cursorColor = Black,
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 검색창
        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = {
                keyboard?.hide()
                viewModel.searchSchools(query.text)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 결과
        when (val state = searchState) {
            is SchoolSearchState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "학교명을 입력하고\n검색 버튼을 누르세요",
                        fontSize = 14.sp,
                        color = Gray3,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            is SchoolSearchState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Black,
                        strokeWidth = 1.5.dp
                    )
                }
            }

            is SchoolSearchState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        fontSize = 14.sp,
                        color = Gray3,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is SchoolSearchState.Success -> {
                LazyColumn {
                    items(state.schools) { school ->
                        SchoolItem(school = school) {
                            viewModel.selectSchool(school)
                            onDone()
                        }
                        HorizontalDivider(color = Gray4, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "학교 이름",
                    fontSize = 16.sp,
                    color = Gray3
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            textStyle = TextStyle(fontSize = 16.sp, color = Gray1),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = Gray4,
                focusedContainerColor = White,
                unfocusedContainerColor = GrayBg,
                cursorColor = Black,
            )
        )

        Box(
            modifier = Modifier
                .background(Black, RoundedCornerShape(10.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSearch() }
                .padding(horizontal = 18.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "검색",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = White
            )
        }
    }
}

@Composable
private fun SchoolItem(school: SchoolInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = school.schoolName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Gray1
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (school.schoolKind.isNotBlank()) {
                    Text(text = school.schoolKind, fontSize = 12.sp, color = Gray3)
                }
                if (school.location.isNotBlank()) {
                    Text(text = school.location, fontSize = 12.sp, color = Gray3)
                }
            }
        }
        Text(text = "›", fontSize = 18.sp, color = Gray4)
    }
}