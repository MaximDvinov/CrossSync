/**
 * Реализация пользовательской индикации [IndicationNodeFactory] с эффектом масштабирования и тактильной отдачи
 * для Jetpack Compose. Используется для анимации при нажатии на элементы интерфейса.
 */
package com.cross.sync.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Узел модификатора, реализующий анимацию масштабирования и поддержку тактильной отдачи при взаимодействии.
 *
 * @property interactionSource источник взаимодействий (нажатие, отпускание, отмена)
 */
private class ScaleIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    val stiffness = 3_000f
    val animatedScalePercent = Animatable(1f)
    val defaultSpec = spring<Float>(stiffness = stiffness)


    /**
     * Выполняет анимацию масштабирования до уменьшенного состояния (0.9x)
     */
    private suspend fun animateToPressed() {
        animatedScalePercent.animateTo(0.9f, defaultSpec)
    }

    /**
     * Выполняет возврат элемента к исходному масштабу через промежуточную анимацию.
     */
    private suspend fun animateToResting() {
        animatedScalePercent.animateTo(0.9f, defaultSpec)
        animatedScalePercent.animateTo(1f, defaultSpec)
    }

    /**
     * Подписывается на взаимодействия пользователя (нажатие, отпускание, отмена) и запускает соответствующие анимации.
     */
    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> animateToPressed()
                    is PressInteraction.Release -> animateToResting()
                    is PressInteraction.Cancel -> animateToResting()
                }
            }
        }
    }

    /**
     * Отрисовывает содержимое с текущим масштабом, заданным в [animatedScalePercent].
     */
    override fun ContentDrawScope.draw() {
        scale(
            scale = animatedScalePercent.value,
            pivot = center
        ) {
            this@draw.drawContent()
        }


    }
}

/**
 * Индикация для Jetpack Compose, реализующая анимацию масштабирования
 */
class ScaleIndication() : IndicationNodeFactory {
    /**
     * Создает и возвращает [ScaleIndicationNode] с переданным источником взаимодействий.
     */
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return ScaleIndicationNode(
            interactionSource = interactionSource,
        )
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode() = 100
}

/**
 * Запоминает и возвращает экземпляр [ScaleIndication], который создает эффект масштабирования
 * и тактильной отдачи при нажатии на элемент.
 */
@Composable
fun rememberScaleIndication(): ScaleIndication {
    return remember(Unit) {
        ScaleIndication()
    }
}