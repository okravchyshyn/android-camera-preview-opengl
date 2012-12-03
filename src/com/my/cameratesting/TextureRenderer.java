package com.my.cameratesting;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//import com.learnopengles.android.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;

public class TextureRenderer implements GLSurfaceView.Renderer {
	
	/** This will be used to pass in model position information. */
	private int mPositionHandle;
	private int mTexCoordinateHandle;
	
	private int mTextureUniformHandle0;
	private int mTextureUniformHandle1;

	private int mTexture0Id;
	private int mTexture1Id;
	
	private int mProgramHandle;
	
	
	private final int mBytesPerFloat = 4;
	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;	
	private final int mTexCoordinateDataSize = 2;
	
	/** Store our model data in a float buffer. */
	private final FloatBuffer mScreenPosition;
	private final FloatBuffer mTextureCoordinate;
	private Context mActivityContext;
	
	private Bitmap mImage;
	private ByteBuffer mBuffer;
	private int mWidth;
	private int mHeight;
	
	TextureRenderer( final Context activityContext ) {

		//int bufferCapacity = mWidth * mHeight * 3 / 2;
		//mBuffer = ByteBuffer.allocate(mWidth * mHeight * 3 / 2);
		
		mActivityContext = activityContext;
/*
		final float[] screenPosition =
			{
					-0.5f, 0.5f, 0.0f,				
					-0.5f,-0.5f, 0.0f,
					 0.5f, 0.5f, 0.0f, 
					-0.5f,-0.5f, 0.0f, 				
					 0.5f,-0.5f, 0.0f,
					 0.5f, 0.5f, 0.0f
			};
*/			
		
		final float[] screenPosition =
			{
					-1.0f, 1.0f, 0.0f,				
					-1.0f,-1.0f, 0.0f,
					 1.0f, 1.0f, 0.0f, 
					-1.0f,-1.0f, 0.0f, 				
					 1.0f,-1.0f, 0.0f,
					 1.0f, 1.0f, 0.0f
			};
		
		
			final float[] textureCoordinateData =
			{												
					// Front face
					0.0f, 0.0f, 				
					0.0f, 1.0f,
					1.0f, 0.0f,
					0.0f, 1.0f,
					1.0f, 1.0f,
					1.0f, 0.0f
			};

			// Initialize the buffers.
			mScreenPosition = ByteBuffer.allocateDirect(screenPosition.length * mBytesPerFloat)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();	
			mTextureCoordinate = ByteBuffer.allocateDirect(textureCoordinateData.length * mBytesPerFloat)
			        .order(ByteOrder.nativeOrder()).asFloatBuffer();

			mScreenPosition.put(screenPosition).position(0);
			mTextureCoordinate.put(textureCoordinateData).position(0);
	}
	

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);

	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background clear color to gray.
		//GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
	

		final String vertexShader =
		    "attribute vec4 a_Position;            \n"		// Per-vertex position information we will pass in.
		  + "attribute vec2 a_TexCoordinate;       \n"		// Per-vertex position information we will pass in.
		  + "                                      \n" 
		  + "varying vec2 v_TexCoordinate;         \n" 
		  + "                                      \n" 
		  + "void main()                           \n"		// The entry point for our vertex shader.
		  + "{                                     \n"
		  + " 	v_TexCoordinate = a_TexCoordinate; \n" 
		  + "   gl_Position = a_Position;          \n" 	// gl_Position is a special variable used to store the final position.
		  + "                                  	   \n"     // Multiply the vertex by the matrix to get the final point in 			                                            			 
		  + "}                                     \n";    // normalized screen coordinates.
		
		final String fragmentShader =
			"precision mediump float;								  \n"
		  + "                                                         \n"
		  + "uniform sampler2D u_Texture0;                            \n" // The input texture.
		  + "uniform sampler2D u_Texture1;                            \n" // The input texture.
		  + "varying vec2 v_TexCoordinate;                            \n"
		  + "                                                         \n"
		  + "const vec3 offset = vec3(0.0625, 0.5, 0.5);              \n"
		  + "const mat3 coeffs = mat3(              				  \n"
		  + "	1.164,  1.164,  1.164,              				  \n"
		  + "	1.596, -0.813,  0.0,              					  \n"
		  + "	0.0  , -0.391,  2.018 );              				  \n"
		  + "              											  \n"
		  + "vec3 texture2Dsmart(vec2 uv)              				  \n"
		  + "  {													  \n"	
		  + "		return coeffs*(vec3(texture2D(u_Texture0, uv).r, texture2D(u_Texture1, uv).ra) - offset);  \n"
		  + "  }              										  \n"
		  + "              											  \n"
		  + "void main()                                              \n"		// The entry point for our fragment shader.
		  + "{                                                         \n"
		  + "  gl_FragColor = vec4(texture2Dsmart(v_TexCoordinate), 1.0);\n"		// Pass the color directly through the pipeline.		  
//		  + "  gl_FragColor = texture2D(u_Texture0, v_TexCoordinate);\n"		// Pass the color directly through the pipeline.		  
		  + "}                                                         \n";												
		
		// Load in the vertex shader.
		int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		if (vertexShaderHandle != 0) 
		{
			// Pass in the shader source.
			GLES20.glShaderSource(vertexShaderHandle, vertexShader);
			// Compile the shader.
			GLES20.glCompileShader(vertexShaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) 
			{				
				GLES20.glDeleteShader(vertexShaderHandle);
				vertexShaderHandle = 0;
			}
		}
		if (vertexShaderHandle == 0)
		{
			throw new RuntimeException("Error creating vertex shader.");
		}
		
		// Load in the fragment shader shader.
		int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		if (fragmentShaderHandle != 0) 
		{
			// Pass in the shader source.
			GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);
			// Compile the shader.
			GLES20.glCompileShader(fragmentShaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) 
			{				
				GLES20.glDeleteShader(fragmentShaderHandle);
				fragmentShaderHandle = 0;
			}
		}
		if (fragmentShaderHandle == 0)
		{
			throw new RuntimeException("Error creating fragment shader.");
		}
		
		// Create a program object and store the handle to it.
		mProgramHandle = GLES20.glCreateProgram();
		if (mProgramHandle != 0) 
		{
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(mProgramHandle, vertexShaderHandle);			
			// Bind the fragment shader to the program.
			GLES20.glAttachShader(mProgramHandle, fragmentShaderHandle);
			// Bind attributes
			GLES20.glBindAttribLocation(mProgramHandle, 0, "a_Position");
			// Link the two shaders together into a program.
			GLES20.glLinkProgram(mProgramHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
			// If the link failed, delete the program.
			if (linkStatus[0] == 0) 
			{				
				GLES20.glDeleteProgram(mProgramHandle);
				mProgramHandle = 0;
			}
		}
		if (mProgramHandle == 0)
		{
			throw new RuntimeException("Error creating program.");
		}
        
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mTexCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
    	mTextureUniformHandle0 = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture0");
    	mTextureUniformHandle1 = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture1");
    	
		final int[] textureHandle = new int[1];
		GLES20.glGenTextures(1, textureHandle, 0);
		if (textureHandle[0] != 0)
		{
			mTexture0Id = textureHandle[0];
		} else {
			throw new RuntimeException("Error loading texture.");
		}
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture0Id);
    	
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		//GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		//GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		GLES20.glGenTextures(1, textureHandle, 0);
		if (textureHandle[0] != 0)
		{
			mTexture1Id = textureHandle[0];
		} else {
			throw new RuntimeException("Error loading texture.");
		}
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture1Id);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

//		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		

	}
	

	public synchronized void drawFrame( int width, int height, byte [] buf /*final int [] imageBytes*/)
	{

		if(mBuffer == null) {
			mBuffer = ByteBuffer.allocateDirect(buf.length);
		}

		mBuffer.clear();
		mBuffer.put(buf);
		//mBuffer;
		mBuffer.position(0);

		mWidth = width;
		mHeight = height;
			
	}
	
	public void loadTexture( int width, int height, Bitmap bmp /*final int [] imageBytes*/)
	{

			final Bitmap bitmap = bmp; //Bitmap.createBitmap(imageBytes, width, height, Bitmap.Config.ARGB_8888);
					
			setImage( bitmap );
			
	}

	
	@Override
	public void onDrawFrame(GL10 gl) {
		
		
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);			        
		GLES20.glUseProgram(mProgramHandle);

        // Set program handles. These will later be used to pass in values to the program.
        //mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        //mTexCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
    	//mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
		
    	//loadAndDrawTexture();
        
		mScreenPosition.position( 0 );
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        		0, mScreenPosition);        
        GLES20.glEnableVertexAttribArray(mPositionHandle);        
        
        // Pass in the color information
        mTextureCoordinate.position( 0 );
        GLES20.glVertexAttribPointer(mTexCoordinateHandle, mTexCoordinateDataSize, GLES20.GL_FLOAT, false,
        		0, mTextureCoordinate);        
        GLES20.glEnableVertexAttribArray(mTexCoordinateHandle);
		
    	loadAndDrawTextureFromBuffer();
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);                               
	}

	
	private void loadAndDrawTexture() {

		Bitmap image = getImage();
		if( image != null ) {
			
        	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture0Id);
        	GLES20.glUniform1i(mTextureUniformHandle0, 0);      	
        	GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);
            //GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
            //        textureWidth, textureHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameData);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			image.recycle();
			//mImage = null;

		}
		
	}

	
	private void loadAndDrawTextureFromBuffer() {

		ByteBuffer frameData = getBuffer();
		if( frameData  != null ) {
			
        	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            frameData.position(0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture0Id);
            GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                    mWidth, mHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameData);

        	GLES20.glUniform1i(mTextureUniformHandle0, 0);      	
        	
        	//GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);


        	GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

        	frameData.position(mWidth * mHeight);
        	 int pos = frameData.position();
        	 int remain = frameData.remaining();
        	 
        	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,  mTexture1Id);
            GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                    mWidth/2, mHeight/2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, frameData);
            frameData.position(0);

        	GLES20.glUniform1i(mTextureUniformHandle0, 1);      	
        	//GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);
        	

            // Recycle the bitmap, since its data has been loaded into OpenGL.
			//image.recycle();
			//mImage = null;

		}
		
	}

	
	private synchronized Bitmap getImage() {
		Bitmap img = mImage;
		mImage = null;
		return img;
	} 

	
	private synchronized ByteBuffer getBuffer() {
		return mBuffer;
	} 
	
	private synchronized void setImage(Bitmap img) {
		mImage = img;
		
	} 

	
}
