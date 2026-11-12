package com.example.vehiclesensormodule.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.model.VehicleType
import com.example.vehiclesensormodule.ui.theme.Accent
import com.example.vehiclesensormodule.ui.theme.AccentDeep
import com.example.vehiclesensormodule.ui.theme.Background
import com.example.vehiclesensormodule.ui.theme.Danger
import com.example.vehiclesensormodule.ui.theme.OnAccent
import com.example.vehiclesensormodule.ui.theme.OutlineSubtle
import com.example.vehiclesensormodule.ui.theme.SurfaceCard
import com.example.vehiclesensormodule.ui.theme.SurfaceRaised
import com.example.vehiclesensormodule.ui.theme.TextMuted
import com.example.vehiclesensormodule.ui.theme.TextPrimary
import com.example.vehiclesensormodule.ui.theme.TextSecondary

val CardShape = RoundedCornerShape(16.dp)
val ControlShape = RoundedCornerShape(12.dp)

private val ControlHeight = 52.dp

/** Full-screen background that stays clear of the status bar and keyboard. */
@Composable
fun ScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 1100f
                )
            )
            .safeDrawingPadding()
    ) {
        CompositionLocalProvider(LocalContentColor provides TextPrimary) {
            content()
        }
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 44.dp, iconSize: Dp = 24.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(Accent, AccentDeep))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_car),
            contentDescription = null,
            tint = OnAccent,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** The product's mark and name, at the top of the login and signup screens. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandMark()

        Text(
            text = "Connected Vehicle",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = modifier
    )
}

/**
 * Text field with a leading icon. A password field gets a show/hide button.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    isPassword: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        shape = ControlShape,
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        leadingIcon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) {
                                R.drawable.ic_visibility_off
                            } else {
                                R.drawable.ic_visibility
                            }
                        ),
                        contentDescription =
                        if (passwordVisible) "Hide password" else "Show password",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            null
        },
        visualTransformation =
        if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
            disabledContainerColor = SurfaceCard,
            errorContainerColor = SurfaceCard,
            focusedBorderColor = Accent,
            unfocusedBorderColor = OutlineSubtle,
            focusedLeadingIconColor = Accent,
            unfocusedLeadingIconColor = TextMuted,
            focusedTrailingIconColor = TextSecondary,
            unfocusedTrailingIconColor = TextMuted,
            focusedLabelColor = Accent,
            unfocusedLabelColor = TextSecondary
        )
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = ControlShape,
        modifier = modifier
            .fillMaxWidth()
            .height(ControlHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = OnAccent,
            disabledContainerColor = if (loading) Accent else SurfaceRaised,
            disabledContentColor = if (loading) OnAccent else TextMuted
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = OnAccent,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(text)
        }
    }
}

enum class MessageKind(val color: Color) {
    Error(Danger),
    Success(com.example.vehiclesensormodule.ui.theme.Success)
}

/** Inline message, used in place of a toast that would disappear. */
@Composable
fun MessageBanner(text: String, kind: MessageKind, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(kind.color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                if (kind == MessageKind.Error) R.drawable.ic_error else R.drawable.ic_check
            ),
            contentDescription = null,
            tint = kind.color,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * One tile for each vehicle type, used at signup and when changing it later.
 * In rows of three, so longer names such as Ambulance fit on a phone.
 */
@Composable
fun VehiclePicker(
    selected: VehicleType?,
    onSelected: (VehicleType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VehicleType.entries.chunked(PICKER_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { type ->
                    VehicleTile(
                        type = type,
                        isSelected = type == selected,
                        enabled = enabled,
                        onSelected = onSelected,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Keeps a shorter last row's tiles the same width
                repeat(PICKER_COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val PICKER_COLUMNS = 3

@Composable
private fun VehicleTile(
    type: VehicleType,
    isSelected: Boolean,
    enabled: Boolean,
    onSelected: (VehicleType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(ControlShape)
            .selectable(
                selected = isSelected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { onSelected(type) }
            ),
        shape = ControlShape,
        color = if (isSelected) Accent.copy(alpha = 0.14f) else SurfaceCard,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Accent else OutlineSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(type.icon),
                contentDescription = null,
                tint = if (isSelected) Accent else TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = type.label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) TextPrimary else TextSecondary,
                maxLines = 1
            )
        }
    }
}

/** The six-digit code from an email. */
@Composable
fun CodeField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    AppTextField(
        value = value,
        onValueChange = { typed ->
            onValueChange(typed.filter(Char::isDigit).take(CODE_LENGTH))
        },
        label = "6-digit code",
        icon = R.drawable.ic_email,
        keyboardType = KeyboardType.NumberPassword,
        imeAction = imeAction,
        onDone = onDone,
        enabled = enabled
    )
}

const val CODE_LENGTH = 6

/** Space between stacked form controls. */
@Composable
fun FormGap() {
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}
