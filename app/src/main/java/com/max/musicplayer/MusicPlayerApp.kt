package com.max.musicplayer

import android.app.Application

/**
 * Punto de entrada del proceso.
 *
 * Por ahora no hay inyeccion de dependencias: las pocas piezas que necesitan Context
 * se construyen donde se usan. Si el proyecto crece, aca es donde iria un contenedor.
 */
class MusicPlayerApp : Application()
