package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `por defecto no hay imagen de fondo`() {
        assertThat(AppSettings().usesBackgroundImage).isFalse()
    }

    @Test
    fun `una uri vacia no cuenta como imagen`() {
        // Puede quedar una cadena vacia si algo se guarda a medias; no debe activar
        // el modo imagen, porque dejaria el fondo transparente y sin nada detras.
        assertThat(AppSettings(backgroundImageUri = "").usesBackgroundImage).isFalse()
        assertThat(AppSettings(backgroundImageUri = "   ").usesBackgroundImage).isFalse()
    }

    @Test
    fun `con una uri real si hay imagen de fondo`() {
        assertThat(AppSettings(backgroundImageUri = "content://foto/1").usesBackgroundImage)
            .isTrue()
    }

    @Test
    fun `las paletas ofrecidas no tienen colores repetidos`() {
        assertThat(AppSettings.ACCENT_CHOICES).containsNoDuplicates()
        assertThat(AppSettings.BACKGROUND_CHOICES).containsNoDuplicates()
    }

    @Test
    fun `los valores por defecto estan entre las opciones ofrecidas`() {
        assertThat(AppSettings.ACCENT_CHOICES).contains(AppSettings.DEFAULT_ACCENT)
        assertThat(AppSettings.BACKGROUND_CHOICES).contains(AppSettings.DEFAULT_BACKGROUND)
        assertThat(AppSettings.ACCENT_CHOICES).contains(AppSettings.DEFAULT_FOLDER)
    }
}
