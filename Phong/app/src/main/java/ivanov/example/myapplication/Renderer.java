package ivanov.example.myapplication;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Renderer implements GLSurfaceView.Renderer
{
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mLightModelMatrix = new float[16];

    private FloatBuffer mVertexBuffer;
    private FloatBuffer textureBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer colorBuffer;
    private int n;

    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mLightPosHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mNormalHandle;

    private final int mPositionDataSize = 3;
    private final int mColorDataSize = 4;
    private final int mNormalDataSize = 3;

    private final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] mLightPosInWorldSpace = new float[4];
    private final float[] mLightPosInEyeSpace = new float[4];

    private int mPerVertexProgramHandle;
    private int mPointProgramHandle;

    public Renderer()
    {
        float R = 1;
        n = 0;
        int dtheta = 15, dphi = 15;

        float DTOR = (float)(Math.PI / 180.0f);

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(5000 * 3 * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBuf.asFloatBuffer();

        byteBuf = ByteBuffer.allocateDirect(5000 * 3 * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        normalBuffer = byteBuf.asFloatBuffer();

        byteBuf = ByteBuffer.allocateDirect(5000 * 2 * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuf.asFloatBuffer();
        for (int theta = -90; theta <= 90 - dtheta; theta += dtheta)
        {
            for (int phi = 0; phi <= 360 - dphi; phi += dphi)
            {

                mVertexBuffer.put((float)(Math.cos(theta*DTOR) * Math.cos(phi*DTOR))*R);
                mVertexBuffer.put((float)(Math.cos(theta*DTOR) * Math.sin(phi*DTOR))*R);
                mVertexBuffer.put((float)(Math.sin(theta*DTOR))*R);

                mVertexBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.cos(phi*DTOR))*R);
                mVertexBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.sin(phi*DTOR))*R);
                mVertexBuffer.put((float)(Math.sin((theta+dtheta)*DTOR))*R);

                mVertexBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.cos((phi+dphi)*DTOR))*R);
                mVertexBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.sin((phi+dphi)*DTOR))*R);
                mVertexBuffer.put((float)(Math.sin((theta+dtheta)*DTOR))*R);


                mVertexBuffer.put((float)(Math.cos(theta*DTOR) * Math.cos((phi+dphi)*DTOR))*R);
                mVertexBuffer.put((float)(Math.cos(theta*DTOR) * Math.sin((phi+dphi)*DTOR))*R);
                mVertexBuffer.put((float)(Math.sin(theta*DTOR))*R);
                n += 4;

                normalBuffer.put((float)(Math.cos(theta*DTOR) * Math.cos(phi*DTOR))*R);
                normalBuffer.put((float)(Math.cos(theta*DTOR) * Math.sin(phi*DTOR))*R);
                normalBuffer.put((float)(Math.sin(theta*DTOR))*R);

                normalBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.cos(phi*DTOR))*R);
                normalBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.sin(phi*DTOR))*R);
                normalBuffer.put((float)(Math.sin((theta+dtheta)*DTOR))*R);

                normalBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.cos((phi+dphi)*DTOR))*R);
                normalBuffer.put((float)(Math.cos((theta+dtheta)*DTOR) * Math.sin((phi+dphi)*DTOR))*R);
                normalBuffer.put((float)(Math.sin((theta+dtheta)*DTOR))*R);

                normalBuffer.put((float)(Math.cos(theta*DTOR) * Math.cos((phi+dphi)*DTOR))*R);
                normalBuffer.put((float)(Math.cos(theta*DTOR) * Math.sin((phi+dphi)*DTOR))*R);
                normalBuffer.put((float)(Math.sin(theta*DTOR))*R);

                textureBuffer.put((float)(phi/360.0f));
                textureBuffer.put((float)((90+theta)/180.0f));

                textureBuffer.put((float)(phi/360.0f));
                textureBuffer.put((float)((90+theta+dtheta)/180.0f));

                textureBuffer.put((float)((phi+dphi)/360.0f));
                textureBuffer.put((float)((90+theta+dtheta)/180.0f));

                textureBuffer.put((float)((phi+dphi)/360.0f));
                textureBuffer.put((float)((90+theta)/180.0f));
            }
        }
        mVertexBuffer.position(0);
        textureBuffer.position(0);
        normalBuffer.position(0);
    }

    protected String getVertexShader()
    {
        final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n"
                        + "uniform mat4 u_MVMatrix;       \n"
                        + "uniform vec3 u_LightPos;       \n"

                        + "attribute vec4 a_Position;     \n"
                        + "attribute vec4 a_Color;        \n"
                        + "attribute vec3 a_Normal;       \n"

                        + "varying vec4 v_Color;          \n"

                        + "void main()                    \n"
                        + "{                              \n"
                        + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
                        + "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));     \n"
                        + "   float distance = length(u_LightPos - modelViewVertex);             \n"
                        + "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);        \n"
                        + "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);       \n"
                        + "   vec3 lookvector = normalize(u_LightPos - modelViewVertex);\n"
                        + "   float specular = pow(max(dot(modelViewNormal, lookvector), 0.0), 40.0);\n"
                        + "   float ambient = 0.2;\n"
                        + "   diffuse = diffuse * (3.0 / (1.0 + (0.25 * distance * distance)));  \n"
                        //+ "   v_Color = vec4(diffuse, diffuse, diffuse, 1);                                       \n"
                        + "   vec4 one = vec4(1.0, 1.0, 1.0, 1.0);"
                        + "   v_Color = (ambient + specular + diffuse) * one;\n"
                        + "   gl_Position = u_MVPMatrix * a_Position;                            \n"
                        + "}                                                                     \n";

        return vertexShader;
    }

    protected String getFragmentShader()
    {
        final String fragmentShader =
                "precision mediump float;       \n"
                        + "varying vec4 v_Color;          \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = v_Color;     \n"
                        + "}                              \n";

        return fragmentShader;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();
        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Color", "a_Normal"});

        final String pointVertexShader =
                "uniform mat4 u_MVPMatrix;      \n"
                        + "attribute vec4 a_Position;     \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_Position = u_MVPMatrix   \n"
                        + "               * a_Position;   \n"
                        + "   gl_PointSize = 10.0;         \n"
                        + "}                              \n";

        final String pointFragmentShader =
                "precision mediump float;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = vec4(1.0,    \n"
                        + "   1.0, 1.0, 1.0);             \n"
                        + "}                              \n";

        final int pointVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[]{"a_Position"});
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
        GLES20.glUseProgram(mPerVertexProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal");

        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);
        drawSphere();
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();
    }

    private void drawSphere()
    {
        mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        normalBuffer.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, normalBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        for (int i = 0; i < n; i += 4)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, i,4);
    }

    private void drawLight()
    {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);
        GLES20.glDisableVertexAttribArray(pointPositionHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    private int compileShader(final int shaderType, final String shaderSource)
    {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0)
        {
            GLES20.glShaderSource(shaderHandle, shaderSource);

            GLES20.glCompileShader(shaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }
        if (shaderHandle == 0)
            throw new RuntimeException("Error creating shader.");
        return shaderHandle;
    }

    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes)
    {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            if (attributes != null)
            {
                for (int i = 0; i < attributes.length; i++)
                {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }
            GLES20.glLinkProgram(programHandle);
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0)
            {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }
        if (programHandle == 0)
            throw new RuntimeException("Error creating program.");
        return programHandle;
    }
}