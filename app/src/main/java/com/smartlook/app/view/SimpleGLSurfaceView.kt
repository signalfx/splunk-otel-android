/*
 * Copyright 2024 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartlook.app.view

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// FIXME Animations

// https://android.googlesource.com/platform/development/+/master/samples/OpenGL/HelloOpenGLES20/src/com/example/android/opengl
class SimpleGLSurfaceView(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    init {
        setEGLContextClientVersion(2)
        setRenderer(MyGLRenderer())
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private class MyGLRenderer : Renderer {
        private lateinit var mTriangle: Triangle
        private lateinit var mSquare: Square

        private val mMVPMatrix = FloatArray(16)
        private val mProjectionMatrix = FloatArray(16)
        private val mViewMatrix = FloatArray(16)
        private val mRotationMatrix = FloatArray(16)

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
            // Set the background frame color
            GLES20.glClearColor(0.8f, 0.8f, 0.8f, 1.0f)
            mTriangle = Triangle()
            mSquare = Square()
        }

        override fun onDrawFrame(unused: GL10) {
            val scratch = FloatArray(16)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
            mSquare.draw(mMVPMatrix)

            val angle = 0.09f * SystemClock.uptimeMillis() % 4000L
            Matrix.setRotateM(mRotationMatrix, 0, angle, 0f, 0f, 1.0f)
            Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0)
            mTriangle.draw(scratch)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            // Adjust the viewport based on geometry changes,
            // such as screen rotation
            GLES20.glViewport(0, 0, width, height)
            val ratio = width.toFloat() / height
            // this projection matrix is applied to object coordinates
            // in the onDrawFrame() method
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        }

        companion object {
            private const val TAG = "MyGLRenderer"

            fun loadShader(type: Int, shaderCode: String?): Int {
                // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
                // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
                val shader = GLES20.glCreateShader(type)
                // add the source code to the shader and compile it
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
                return shader
            }

            fun checkGlError(glOperation: String) {
                var error: Int
                while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
                    Log.e(TAG, "$glOperation: glError $error")
                    throw java.lang.RuntimeException("$glOperation: glError $error")
                }
            }
        }
    }

    private class Square {

        // number of coordinates per vertex in this array
        val COORDS_PER_VERTEX = 3
        var squareCoords = floatArrayOf(
            -0.5f, 0.5f, 0.0f, // top left
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f, // bottom right
            0.5f, 0.5f, 0.0f
        ) // top right

        private val vertexShaderCode = // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" + // The matrix must be included as a modifier of gl_Position.
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

        private val fragmentShaderCode = (
            "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"
            )

        private val vertexBuffer: FloatBuffer
        private val drawListBuffer: ShortBuffer
        private val mProgram: Int
        private var mPositionHandle = 0
        private var mColorHandle = 0
        private var mMVPMatrixHandle = 0
        private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices
        private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
        var color = floatArrayOf(0f, 0f, 1f, 1f)

        fun draw(mvpMatrix: FloatArray?) {
            // Add program to OpenGL environment
            GLES20.glUseProgram(mProgram)
            // get handle to vertex shader's vPosition member
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer
            )
            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
            // Set color for drawing the triangle
            GLES20.glUniform4fv(mColorHandle, 1, color, 0)
            // get handle to shape's transformation matrix
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            MyGLRenderer.checkGlError("glGetUniformLocation")
            // Apply the projection and view transformation
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
            MyGLRenderer.checkGlError("glUniformMatrix4fv")
            // Draw the square
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.size,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer
            )
            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle)
        }

        init {
            // initialize vertex byte buffer for shape coordinates
            val bb = ByteBuffer.allocateDirect( // (# of coordinate values * 4 bytes per float)
                squareCoords.size * 4
            )
            bb.order(ByteOrder.nativeOrder())
            vertexBuffer = bb.asFloatBuffer()
            vertexBuffer.put(squareCoords)
            vertexBuffer.position(0)
            // initialize byte buffer for the draw list
            val dlb = ByteBuffer.allocateDirect( // (# of coordinate values * 2 bytes per short)
                drawOrder.size * 2
            )
            dlb.order(ByteOrder.nativeOrder())
            drawListBuffer = dlb.asShortBuffer()
            drawListBuffer.put(drawOrder)
            drawListBuffer.position(0)
            // prepare shaders and OpenGL program
            val vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode
            )
            val fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode
            )
            mProgram = GLES20.glCreateProgram() // create empty OpenGL Program
            GLES20.glAttachShader(mProgram, vertexShader) // add the vertex shader to program
            GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
            GLES20.glLinkProgram(mProgram) // create OpenGL program executables
        }
    }

    private class Triangle {
        private val vertexShaderCode = // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" + // the matrix must be included as a modifier of gl_Position
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

        private val fragmentShaderCode = (
            "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"
            )

        private val vertexBuffer: FloatBuffer
        private val mProgram: Int
        private var mPositionHandle = 0
        private var mColorHandle = 0
        private var mMVPMatrixHandle = 0
        private val vertexCount = triangleCoords.size / COORDS_PER_VERTEX
        private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
        var color = floatArrayOf(1f, 0f, 0f, 0f)

        fun draw(mvpMatrix: FloatArray?) {
            // Add program to OpenGL environment
            GLES20.glUseProgram(mProgram)
            // get handle to vertex shader's vPosition member
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer
            )
            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
            // Set color for drawing the triangle
            GLES20.glUniform4fv(mColorHandle, 1, color, 0)
            // get handle to shape's transformation matrix
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            MyGLRenderer.checkGlError("glGetUniformLocation")
            // Apply the projection and view transformation
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
            MyGLRenderer.checkGlError("glUniformMatrix4fv")
            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle)
        }

        companion object {
            // number of coordinates per vertex in this array
            val COORDS_PER_VERTEX = 3
            var triangleCoords = floatArrayOf( // in counterclockwise order:
                0.0f, 0.7f, 0.0f, // top
                -0.7f, -0.3f, 0.0f, // bottom left
                0.7f, -0.3f, 0.0f // bottom right
            )
        }

        init {
            // initialize vertex byte buffer for shape coordinates
            val bb = ByteBuffer.allocateDirect( // (number of coordinate values * 4 bytes per float)
                triangleCoords.size * 4
            )
            // use the device hardware's native byte order
            bb.order(ByteOrder.nativeOrder())
            // create a floating point buffer from the ByteBuffer
            vertexBuffer = bb.asFloatBuffer()
            // add the coordinates to the FloatBuffer
            vertexBuffer.put(triangleCoords)
            // set the buffer to read the first coordinate
            vertexBuffer.position(0)
            // prepare shaders and OpenGL program
            val vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER, vertexShaderCode
            )
            val fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode
            )
            mProgram = GLES20.glCreateProgram() // create empty OpenGL Program
            GLES20.glAttachShader(mProgram, vertexShader) // add the vertex shader to program
            GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
            GLES20.glLinkProgram(mProgram) // create OpenGL program executables
        }
    }
}
