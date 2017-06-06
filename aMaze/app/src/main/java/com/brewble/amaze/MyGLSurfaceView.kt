package com.brewble.amaze

import android.content.Context
import android.opengl.GLSurfaceView

/**
 * Created by ashkanabedian on 2017-06-04.
 */

open class GLSurfaceView(context : Context)

class MyGLSurfaceView constructor(context : Context): GLSurfaceView(context){

    val mRenderer : MyGLRenderer = MyGLRenderer()

    fun start() : Unit{
        setRenderer(mRenderer)
    }

    init {
        setEGLContextClientVersion(3)

    }


}



