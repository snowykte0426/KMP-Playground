package com.amond.kmpbook

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Gray1 = Color(0xFF222222)
private val Gray2 = Color(0xFF666666)
private val Gray3 = Color(0xFF999999)
private val Gray4 = Color(0xFFE0E0E0)
private val GrayBg = Color(0xFFF7F7F7)

@Composable
fun MealScreen(
    viewModel: MealViewModel,
    onChangeSchool: () -> Unit
) {
    val mealState by viewModel.mealState.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val schoolName = viewModel.prefs.schoolName
    var showApiKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        MealHeader(
            schoolName = schoolName,
            currentDate = currentDate,
            onPrevious = { viewModel.previousDay() },
            onNext = { viewModel.nextDay() },
            onToday = { viewModel.goToToday() },
            onChangeSchool = onChangeSchool,
            onApiKeyClick = { showApiKeyDialog = true }
        )

        HorizontalDivider(color = Gray4, thickness = 1.dp)

        AnimatedContent(
            targetState = mealState,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
            when (state) {
                is MealUiState.Loading -> LoadingView()
                is MealUiState.Success -> MealContent(state.meals)
                is MealUiState.Empty -> EmptyView(state.message)
                is MealUiState.Error -> ErrorView(state.message) { viewModel.loadMeals() }
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = viewModel.prefs.apiKey,
            onConfirm = { key ->
                viewModel.prefs.apiKey = key
                showApiKeyDialog = false
                viewModel.loadMeals()
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
private fun MealHeader(
    schoolName: String,
    currentDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onChangeSchool: () -> Unit,
    onApiKeyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = schoolName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Gray2,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onChangeSchool() }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "API 키",
                    fontSize = 12.sp,
                    color = Gray3,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onApiKeyClick() }
                )
                Text(
                    text = "변경",
                    fontSize = 12.sp,
                    color = Gray3,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onChangeSchool() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "‹",
                fontSize = 24.sp,
                color = Gray2,
                modifier = Modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onPrevious() }
                    .padding(8.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${currentDate.month.number}월 ${currentDate.day}일",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "${currentDate.year} ${currentDate.toDayOfWeekKorean()}요일",
                    fontSize = 13.sp,
                    color = Gray3,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onToday() }
                )
            }

            Text(
                text = "›",
                fontSize = 24.sp,
                color = Gray2,
                modifier = Modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onNext() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue(currentKey)) }
    val keyboard = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                text = "NEIS API 키",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "open.neis.go.kr 에서 발급받은 키를 입력하세요",
                    fontSize = 13.sp,
                    color = Gray3,
                    lineHeight = 18.sp
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("API 키 입력", color = Gray3, fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Gray1),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        onConfirm(input.text.trim())
                    }),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Black,
                        unfocusedBorderColor = Gray4,
                        focusedContainerColor = White,
                        unfocusedContainerColor = GrayBg,
                        cursorColor = Black,
                    )
                )
            }
        },
        confirmButton = {
            Text(
                text = "저장",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Black,
                modifier = Modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        keyboard?.hide()
                        onConfirm(input.text.trim())
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        dismissButton = {
            Text(
                text = "취소",
                fontSize = 15.sp,
                color = Gray3,
                modifier = Modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    )
}

@Composable
private fun MealContent(meals: List<MealInfo>) {
    if (meals.size == 1) {
        SingleMealView(meals[0])
    } else {
        var selectedTab by remember(meals) { mutableIntStateOf(
            meals.indexOfFirst { it.mealTypeCode == "2" }.coerceAtLeast(0)
        ) }

        Column {
            Spacer(modifier = Modifier.height(12.dp))
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = White,
                contentColor = Black,
                edgePadding = 24.dp,
                indicator = {},
                divider = {}
            ) {
                meals.forEachIndexed { index, meal ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedTab == index) Black else GrayBg,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = meal.mealTypeName,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal,
                                color = if (selectedTab == index) White else Gray2
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Gray4, thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))

            SingleMealView(meals[selectedTab])
        }
    }
}

@Composable
private fun SingleMealView(meal: MealInfo) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(24.dp)) }

        val dishes = meal.dishList()
        items(dishes) { dish ->
            Text(
                text = dish,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Gray1,
                modifier = Modifier.padding(vertical = 9.dp),
                lineHeight = 24.sp
            )
            HorizontalDivider(color = Gray4, thickness = 0.5.dp)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            CaloriesRow(meal.calories)
            Spacer(modifier = Modifier.height(8.dp))
            NutritionRow(meal.nutritionList())
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CaloriesRow(calories: String) {
    if (calories.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("열량", fontSize = 13.sp, color = Gray2)
        Text(
            text = calories,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Gray1
        )
    }
}

@Composable
private fun NutritionRow(nutritionList: List<Pair<String, String>>) {
    if (nutritionList.isEmpty()) return
    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        nutritionList.forEach { (name, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, fontSize = 12.sp, color = Gray3)
                Text(value, fontSize = 12.sp, color = Gray2)
            }
        }
    }
}

@Composable
private fun LoadingView() {
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

@Composable
private fun EmptyView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 15.sp,
            color = Gray3,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Gray3,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Text(
                text = "다시 시도",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Black,
                modifier = Modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onRetry() }
                    .background(GrayBg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}