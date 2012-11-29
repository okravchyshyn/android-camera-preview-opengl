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
	
	private int mTextureUniformHandle;
	private int mTextureDataHandle;
	
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
	
	TextureRenderer( final Context activityContext ) {
		
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
		  + "uniform sampler2D u_Texture;                             \n" // The input texture.
		  + "varying vec2 v_TexCoordinate;                            \n" 
		  + "void main()                                              \n"		// The entry point for our fragment shader.
		  + "{                                                        \n"
		  + "    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);\n"		// Pass the color directly through the pipeline.		  
		  + "}                                                        \n";												
		
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
//			GLES20.glBindAttribLocation(mProgramHandle, 1, "u_Texture");
			//GLES20.glBindAttribLocation(mProgramHandle, 1, "a_TexCoordinate");
			//GLES20.glBindAttribLocation(mProgramHandle, 2, "u_Texture");
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
    	mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
    	
		final int[] textureHandle = new int[1];
		GLES20.glGenTextures(1, textureHandle, 0);
		if (textureHandle[0] != 0)
		{
			mTextureDataHandle = textureHandle[0];
		} else {
			throw new RuntimeException("Error loading texture.");
		}
		
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

		// Set filtering
        //GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		// Set filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        //GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, null, 0);

	}
	

	public void loadTexture( int width, int height, Bitmap bmp /*final int [] imageBytes*/)
	{
			// Read in the resource
			//final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
			//byte[] image = (byte[]) imageBytes;
			final Bitmap bitmap = bmp; //Bitmap.createBitmap(imageBytes, width, height, Bitmap.Config.ARGB_8888);
					
			setImage( bitmap );
			
	}

	
	@Override
	public void onDrawFrame(GL10 gl) {
		
		
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);			        
		GLES20.glUseProgram(mProgramHandle);

        // Set program handles. These will later be used to pass in values to the program.
        //mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");        
        //mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        //mTexCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
    	//mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
		
    	loadAndDrawTexture();
        
		mScreenPosition.position( 0 );
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        		0, mScreenPosition);        
        GLES20.glEnableVertexAttribArray(mPositionHandle);        
        
        // Pass in the color information
        mTextureCoordinate.position( 0 );
        GLES20.glVertexAttribPointer(mTexCoordinateHandle, mTexCoordinateDataSize, GLES20.GL_FLOAT, false,
        		0, mTextureCoordinate);        
        GLES20.glEnableVertexAttribArray(mTexCoordinateHandle);
		
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);                               
	}

	
	private void loadAndDrawTexture() {

		Bitmap image = getImage();
		if( image != null ) {
			
        	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        	GLES20.glUniform1i(mTextureUniformHandle, 0);      	

        	GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);
            //GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
            //        textureWidth, textureHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameData);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			image.recycle();
			//mImage = null;

		}
		
	}
	
	private synchronized Bitmap getImage() {
		Bitmap img = mImage;
		mImage = null;
		return img;
	} 

	private synchronized void setImage(Bitmap img) {
		mImage = img;
		
	} 

	
	public static int loadTexture1(final Context context, final int resourceId)
	{
		final int[] textureHandle = new int[1];
		
		GLES20.glGenTextures(1, textureHandle, 0);
		
		if (textureHandle[0] != 0)
		{
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;	// No pre-scaling

			// Read in the resource
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
						
			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
			
			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			
			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
			
			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();						
		}
		
		if (textureHandle[0] == 0)
		{
			throw new RuntimeException("Error loading texture.");
		}
		
		return textureHandle[0];
	}
	
}