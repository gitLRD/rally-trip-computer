package io.github.gitlrd.rallytripcomputer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The dashboard's visual vocabulary, in one place.
 *
 * The reference is a rally trip meter — a Brantz or a Terratrip — rather than a phone app:
 * a black face behind a bezel, engraved legends in small letterspaced capitals, and
 * numerals large enough to read in a glance away from a roadbook. Everything here exists to
 * serve one question the navigator is asking several times a minute: what does it say?
 */

/** Corner radius, kept small: instruments have machined edges, not pill shapes. */
private val PANEL_CORNER = 6.dp
private val PANEL_PADDING = 14.dp

/**
 * Legends are set in letterspaced capitals, the way they are engraved on an instrument
 * face. It also makes them unmistakably *not* the reading, which matters when the only
 * thing you want off the screen is the number.
 */
@Composable
fun legendStyle(): TextStyle = MaterialTheme.typography.labelSmall.copy(
    fontSize = 11.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.6.sp
)

/**
 * Tabular figures. Proportional digits are narrower on a 1 than a 0, so a trip meter set in
 * them shuffles sideways every time it ticks over — which is precisely the moment you are
 * trying to read it. `tnum` pins every digit to the same width, so the number changes
 * without moving.
 */
@Composable
fun readoutStyle(size: TextUnit): TextStyle = MaterialTheme.typography.displayMedium.copy(
    fontSize = size,
    lineHeight = size * 1.02f,
    fontWeight = FontWeight.Medium,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum"
)

/**
 * A legend sized to fit the width it is given.
 *
 * Legends are the most likely thing on the panel to overrun: they are the only text that
 * changes length with the language, and a clipped one turns "MAX 59.95 MPH" into "MAX
 * 59.95", which reads as a different measurement rather than as a truncation. Shrinking is
 * always better than lying.
 *
 * The 0.62 is the average advance of letterspaced capitals as a fraction of point size.
 */
@Composable
private fun fittedLegend(text: String, maxWidthDp: Float, base: TextStyle): TextStyle {
    if (text.isEmpty()) return base
    val spacing = base.letterSpacing.value
    val fits = (maxWidthDp / text.length - spacing) / 0.62f
    val size = fits.coerceIn(8f, base.fontSize.value)
    return base.copy(fontSize = size.sp, lineHeight = (size * 1.2f).sp)
}

/**
 * One instrument: a bezelled face carrying an eyebrow, a reading and a footer.
 *
 * @param onLongClick when given, the panel also responds to a hold. Only the stopwatch
 *   wants this, and passing null keeps every other panel on a plain click, so a stray hold
 *   on a trip meter cannot do anything at all.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(PANEL_CORNER)
    val haptics = LocalHapticFeedback.current
    val interaction = when {
        onClick == null -> Modifier
        onLongClick == null -> Modifier.combinedClickable(onClick = onClick)
        else -> Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                // Confirmation you can feel. Clearing a stopwatch at night, on a bumpy
                // road, should not need you to look at the screen to know it worked.
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    }
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(interaction)
            .padding(PANEL_PADDING),
        content = content
    )
}

/**
 * The line above the reading: what this instrument is. Two facts at most, separated by a
 * middot, because a third turns a legend into a sentence.
 */
@Composable
fun PanelEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    indicator: Color? = null
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val label = text.uppercase()
        // The indicator and its gap are not available to the text.
        val forText = maxWidth.value - if (indicator != null) 15f else 0f
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = fittedLegend(label, forText, legendStyle()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (indicator != null) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(7.dp).background(indicator, CircleShape))
            }
        }
    }
}

/**
 * The reading itself: a large tabular number with its unit set small beside it.
 *
 * The unit is a separate, quieter element rather than part of the string. Rendered at the
 * same size it competes with the number for attention, and the unit is the one thing on the
 * panel that never changes.
 */
@Composable
fun PanelReadout(
    value: String,
    unit: String?,
    maxSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Readings vary in length far more than a fixed size can serve: distance runs from
        // "0.00" to "123.45", and a stopwatch from "0:00.0" to "1:04:31.2". Sized for the
        // longest, the common case is needlessly small; sized for the common case, the long
        // one is clipped. So the size is derived from the width actually available.
        //
        // 0.60 is Roboto's digit advance as a fraction of point size, with a little margin.
        // Periods and colons are narrower, which the estimate ignores — erring towards a
        // slightly smaller number, which is the safe direction.
        val unitWidth = if (unit == null) 0f else maxSize.value * 0.34f
        val available = maxWidth.value - unitWidth
        val fitted = (available / (value.length * 0.60f)).coerceIn(20f, maxSize.value)

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = readoutStyle(fitted.sp),
                color = color,
                maxLines = 1,
                softWrap = false
            )
            if (unit != null) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = unit,
                    style = legendStyle().copy(
                        fontSize = (fitted * 0.30f).sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    // Lifted clear of the baseline so it sits with the digits rather than
                    // hanging off the bottom of them.
                    modifier = Modifier.padding(bottom = (fitted * 0.13f).dp)
                )
            }
        }
    }
}

/** A hairline and a secondary reading, closing the panel off. */
@Composable
fun PanelFooter(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.height(7.dp))
        BoxWithConstraints {
            val label = text.uppercase()
            Text(
                text = label,
                style = fittedLegend(label, maxWidth.value, legendStyle()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * A linear speed scale under the current-speed reading.
 *
 * The one piece of decoration on the dashboard that earns its place: on a regularity you
 * are holding a set average, and a bar you can catch in peripheral vision tells you whether
 * you are near it without looking away from the road to read digits.
 */
@Composable
fun SpeedScale(
    speed: Double,
    fullScale: Double,
    modifier: Modifier = Modifier
) {
    val fraction = (speed / fullScale).coerceIn(0.0, 1.0).toFloat()
    val track = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(track, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(accent, RoundedCornerShape(3.dp))
            )
        }
        Spacer(Modifier.height(4.dp))
        // Ticks at each quarter, as rules rather than characters: a row of middots reads as
        // text the eye tries to parse, where a tick is just a mark on a scale.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(5) { i ->
                Box(
                    Modifier
                        .width(1.dp)
                        .height(if (i == 0 || i == 4) 6.dp else 4.dp)
                        .background(track)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        // Labelled at the ends only: numbers in between would compete with the reading
        // above, which is the thing actually being read.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0",
                style = legendStyle().copy(fontSize = 9.sp, letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = fullScale.toInt().toString(),
                style = legendStyle().copy(fontSize = 9.sp, letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}
