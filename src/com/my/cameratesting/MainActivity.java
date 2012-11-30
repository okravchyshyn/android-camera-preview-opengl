package com.my.cameratesting;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import com.my.cameratesting.TextureRenderer; 

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

	int defaultCameraId;
	Camera mCamera;
	SurfaceView mView;
	SurfaceHolder mHolder;
	private byte[] mData;
	private int[] mDataRGB8888;
	
	
	int mX, mY;
	private int pixelFormat;
	private GLSurfaceView mGLSurfaceView;
	private ViewGroup mFrame;
	private TextureRenderer mRenderer;
	//private AsynсNV21Decoder nv21Decoder;
	private int counter = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.activity_main);
        
		mGLSurfaceView = new GLSurfaceView(this);
		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

		if (supportsEs2) 
		{
			mRenderer = new TextureRenderer(this);
			mGLSurfaceView.setEGLContextClientVersion(2);
			mGLSurfaceView.setRenderer(mRenderer);
			mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		} 
		else 
		{
			return;
		}
		//setContentView(mGLSurfaceView);
        
        //  setContentView(R.layout.activity_main);
 /*       
        // Find the total number of cameras available
        int numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
        	Camera.getCameraInfo(i, cameraInfo);
        	if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
        		defaultCameraId = i;
        	}
        }
*/
		mFrame = (ViewGroup)this.findViewById(R.id.main_layout);
		mFrame.addView(mGLSurfaceView);

        mView = new SurfaceView(this);
        mHolder = mView.getHolder();
        mHolder.addCallback( this );
         
        mFrame.addView(mView);
         
        //mView.setVisibility(View.INVISIBLE);
         
  	 
         
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
   
    
    @Override
    protected void onResume() {
        super.onResume();

        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        
    	mCamera.startPreview();

    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
        mCamera.setPreviewCallback(null);
    	mCamera.stopPreview();
    	mCamera.release();

    	//if( nv21Decoder != null)
    	//	nv21Decoder.releaseThread();

    }

    
	 @Override
		
	 public void surfaceCreated(SurfaceHolder holder) {
			try {
				if (mCamera != null)
					mCamera.setPreviewDisplay(holder);
			}
			catch (Exception exception) {}
	
	 }
	 @Override
	 public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		 if( mCamera != null ) {
		        Camera.Parameters parameters = mCamera.getParameters();
		        
				pixelFormat = parameters.getPreviewFormat();

		        
		        mX = mFrame.getWidth();
		        mY = mFrame.getHeight();
		        mX = (mX/4) * 4;
		        mY = (mY/4) * 4;
		        parameters.setPreviewSize( mX, mY);
		        mCamera.setParameters(parameters);
		       
		        parameters = mCamera.getParameters();
		        Camera.Size size = parameters.getPreviewSize();
        
		        mX = size.width;
		        mY = size.height;
		        
		        //nv21Decoder = new AsynсNV21Decoder(mX, mY);
		        //nv21Decoder.start();
		        
		        mData = new byte[mX * mY * 3 / 2];
		        mCamera.addCallbackBuffer(mData);
		        mCamera.setPreviewCallback(this);
		        
		        mDataRGB8888 = new int[mX * mY];
			}
	 }
		
		
	 @Override
	 public void surfaceDestroyed(SurfaceHolder holder) {
	 }

	 @Override
	 public void onPreviewFrame(byte[] data, Camera camera) {
	        // System.arraycopy(data, 0, mData, 0, data.length);

		 Camera.Parameters parameters = mCamera.getParameters();
		 Camera.Size s = parameters.getPreviewSize();

		 //if( nv21Decoder != null ) nv21Decoder.processBuffer( data );
		 mRenderer.drawFrame(s.width, s.height, data);
		 mGLSurfaceView.requestRender();
		 
/*		 
		 System.arraycopy(data, 0, mData , 0, s.width * s.height * 3 / 2);
		
		 
		 Bitmap bmp = getBitmapFromNV21(mData, s.width, s.height );
		 mRenderer.loadTexture(s.width, s.height, bmp);
		 mGLSurfaceView.requestRender();
*/		 
		 mCamera.addCallbackBuffer(mData);

	 }

     public Bitmap getBitmapFromNV21(byte[] data, int width, int height) {
         
         int grey = 0;
         //int pixelsNumber = width * height;
         //int[] colors = new int[pixelsNumber];
        
         //for (int pixel = 0; pixel < pixelsNumber; pixel++) {
         //        grey = data[pixel] & 0xff;
         //        colors[pixel] = 0xff000000 | (grey * 0x00010101);
         //}
         
         
         //decodeYUV(mDataRGB8888, data, width, height);
         decodeYUV420SP(mDataRGB8888, data, width, height);
        
        Bitmap bitmap ;
        //if( counter % 2 == 0 ) {
        //   bitmap = loadDemoBitmap();
        //}  else {
        	/*
            for (int i = 0; i < width * height ; i++)
            {
            	mDataRGB8888[i] = 0xFFFF0000;
            }
            */
           bitmap = Bitmap.createBitmap(mDataRGB8888, width, height, Bitmap.Config.ARGB_8888);
        //}
        //counter++;
        
         return bitmap;
     }

     public static void YUV_NV21_TO_RGB(int[] argb, byte[] yuv, int width, int height) {
    	    final int frameSize = width * height;

    	    final int ii = 0;
    	    final int ij = 0;
    	    final int di = +1;
    	    final int dj = +1;

    	    int a = 0;
    	    for (int i = 0, ci = ii; i < height; ++i, ci += di) {
    	        for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
    	            int y = (0xff & ((int) yuv[ci * width + cj]));
    	            int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
    	            int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
    	            y = y < 16 ? 16 : y;

    	            int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
    	            int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
    	            int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

    	            r = r < 0 ? 0 : (r > 255 ? 255 : r);
    	            g = g < 0 ? 0 : (g > 255 ? 255 : g);
    	            b = b < 0 ? 0 : (b > 255 ? 255 : b);

    	            argb[a++] = 0xff000000 | (r << 16) | (g << 8) | b;
    	        }
    	    }
    	}
     
     public static void decodeYUV(int[] out, byte[] fg, int width, int height) throws NullPointerException, IllegalArgumentException {
    		         
    	 final int sz = width * height;
    	 if(out == null) throw new NullPointerException("buffer 'out' is null");
    	 if(out.length < sz) throw new IllegalArgumentException("buffer 'out size " + out.length + " < minimum " + sz);
    	 if(fg == null) throw new NullPointerException("buffer 'fg' is null");
    	 if(fg.length < sz) throw new IllegalArgumentException("buffer 'fg' size " + fg.length + " < minimum " + sz * 3/ 2);
    	 int i, j;
    	 int Y, Cr = 0, Cb = 0;
    	 for(j = 0; j < height; j++) {
    		 int pixPtr = j * width;
    		 final int jDiv2 = j >> 1;
    	     for(i = 0; i < width; i++) {
    	    	 Y = fg[pixPtr]; if(Y < 0) Y += 255;
    	    	 if((i & 0x1) != 1) {
    	    		 final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
    	    		 Cb = fg[cOff];
    	    		 if(Cb < 0) Cb += 127; else Cb -= 128;
    	    		 Cr = fg[cOff + 1];
    	    		 if(Cr < 0) Cr += 127; else Cr -= 128;
    	    	 }
    	    	 int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
    	    	 if(R < 0) R = 0; else if(R > 255) R = 255;
    	    	 int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
    	    	 if(G < 0) G = 0; else if(G > 255) G = 255;
    	    	 int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
    	    	 if(B < 0) B = 0; else if(B > 255) B = 255;
    	    	 out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
    	     }
    	 }
     } 
     
     //Method from Ketai project! Not mine! See below...  
     void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {  
               
             final int frameSize = width * height;  
   
             for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;  
               for (int i = 0; i < width; i++, yp++) {  
                 int y = (0xff & ((int) yuv420sp[yp])) - 16;  
                 if (y < 0)  
                   y = 0;  
                 if ((i & 1) == 0) {  
                   v = (0xff & yuv420sp[uvp++]) - 128;  
                   u = (0xff & yuv420sp[uvp++]) - 128;  
                 }  
   
                 int y1192 = 1192 * y;  
                 int r = (y1192 + 1634 * v);  
                 int g = (y1192 - 833 * v - 400 * u);  
                 int b = (y1192 + 2066 * u);  
   
                 if (r < 0)                  r = 0;               else if (r > 262143)  
                    r = 262143;  
                 if (g < 0)                  g = 0;               else if (g > 262143)  
                    g = 262143;  
                 if (b < 0)                  b = 0;               else if (b > 262143)  
                    b = 262143;  
   
                 rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);  
               }  
             }  
           }  
     
     class AsynсNV21Decoder extends Thread {

      	private int width;
      	private int height;
     	private byte [] buffer; 
        int[] colors;
    	//private byte [] buffer2; 
    	//private byte [] buffer3; 

    	private boolean isFreeBuffer;
    	private boolean quitFromThrea = false;
    	final Lock lock = new ReentrantLock();
    	final Condition readyToProcess  = lock.newCondition(); 
    	 

    	public AsynсNV21Decoder(int width, int height) {
    		this.width = width;
    		this.height = height;
        	buffer = new byte[width * height * 3 /2];
        	isFreeBuffer = true; 
            colors = new int [width * height];
    	}
    	

    	public void processBuffer( byte[] buf) {
    		lock.lock();
    		if( isFreeBuffer )
    		{
        		System.arraycopy(buf, 0, buffer, 0, width * height * 3 / 2);
        		//buffer = buf;
        		//isFreeBuffer = false;
        		readyToProcess.signal();
    		}
    		lock.unlock();
    	}
    	
    	@Override
    	public synchronized void start () {
			quitFromThrea = false;
			isFreeBuffer = true;
    		super.start();
    	}
    	
    	public void releaseThread() {
    		lock.lock();
    		if( isFreeBuffer )
    		{
    			quitFromThrea = true;
        		readyToProcess.signal();
    		}
    		lock.unlock();
    		
    	}
    	
    	 //ThreeBuffer
   	    @Override
		public void run() {
   	    
   	    	while(!quitFromThrea)
   	    	{
   	    		lock.lock();
   	    		try {
					readyToProcess.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
   	    		isFreeBuffer = false;
   	    		lock.unlock();
   	    		
   	    		if( quitFromThrea ) break;
   	    		
   	    		Bitmap bmp = getBitmapFromNV21(buffer, width, height );
   	    		mRenderer.loadTexture(width, height, bmp);
   	    
   	    		mGLSurfaceView.requestRender();
   	    		/*
   	    		runOnUiThread ( new Runnable() {
						@Override
						public void run() {
   	    	   	    		mGLSurfaceView.requestRender();
							
						}
   	    		}
   	    		);
   	    		*/
   	    		

   	    		lock.lock();
   	    		isFreeBuffer = true;
   	    		lock.unlock();

   	    		
   	    	}
   	    	
   	    }
    	 
     }


	public Bitmap loadDemoBitmap()
	{
		final int[] textureHandle = new int[1];
		
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;	// No pre-scaling

			// Read in the resource
		final Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), 
			R.drawable.bumpy_bricks_public_domain, options);
						
		
		return bitmap;
	}


}
