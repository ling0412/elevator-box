package com.ling.box.calculator.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ling.box.calculator.model.calculateActualLoad
import com.ling.box.calculator.model.calculateActualPercentage
import com.ling.box.calculator.model.calculateNumberOfBlocks
import com.ling.box.calculator.model.calculateTheoreticalLoad
import kotlinx.coroutines.delay

@Composable
fun StandardPercentageReference(
    ratedLoadValue: Double,
    blockWeightValue: Double,
    defaultLoadPercentages: List<Int>,
    onBlockCountClick: (Int) -> Unit
) {
    val combinedPercentages = remember(defaultLoadPercentages) {
        (defaultLoadPercentages + listOf(100, 110, 125)).distinct().sorted()
    }

    val showData = ratedLoadValue != 0.0 && blockWeightValue != 0.0

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    ) {
        Text(
            text = "标准百分比参考",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!showData) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .width(200.dp)
                        .height(80.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "请先输入额定载重和砝码重量",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                var itemsVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    itemsVisible = true
                }

                combinedPercentages.forEachIndexed { index, percentage ->
                    val theoreticalLoad = calculateTheoreticalLoad(ratedLoadValue, percentage)
                    val blocks = calculateNumberOfBlocks(theoreticalLoad, blockWeightValue)
                    val actualLoad = calculateActualLoad(blocks, blockWeightValue)
                    val actualPercentage = calculateActualPercentage(actualLoad, ratedLoadValue)

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    var isTapped by remember { mutableStateOf(false) }

                    val targetScale = remember(isPressed, isTapped) {
                        if (isPressed || isTapped) 0.95f else 1.0f
                    }
                    val scale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "scaleAnim"
                    )

                    LaunchedEffect(isTapped) {
                        if (isTapped) {
                            delay(400)
                            isTapped = false
                        }
                    }

                    val staggerDelay = 100 + (index * 50)
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (itemsVisible) 1.0f else 0f,
                        animationSpec = tween(durationMillis = 300, delayMillis = staggerDelay, easing = LinearOutSlowInEasing),
                        label = "alphaAnim"
                    )
                    val animatedOffsetY by animateDpAsState(
                        targetValue = if (itemsVisible) 0.dp else 24.dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = staggerDelay, easing = FastOutSlowInEasing),
                        label = "offsetYAnim"
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .width(90.dp)
                            .graphicsLayer(
                                alpha = animatedAlpha,
                                translationY = animatedOffsetY.value
                            )
                            .scale(scale)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    onBlockCountClick(blocks)
                                    isTapped = true
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${blocks}块",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "(${ "%.1f".format(actualPercentage)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

