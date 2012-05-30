package cv.game.jpct;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

public class MyJpctActivity extends Activity {
    /** Called when the activity is first created. */
	
	 static MyJpctActivity master = null;

	 GLSurfaceView mGLView;
	 MyRenderer renderer = null;
	 FrameBuffer fb = null;
	 World world = null;
	 RGBColor back = new RGBColor(50, 50, 100);
	 boolean rendered = true;


	 float xpos = 0;

	 Object3D cube = null;
	 private Object3D plane;
	 int fps = 0;

	 Light sun = null;

	 private int width;
	 
	 boolean touched = false;

	private float prevxpos;
	
	private int an = 2;//do a walk animation
    private float ind = 0;
    int cnt = 0;
    boolean moved = true;

	public Object3D plane2;
	
	
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Logger.log("onCreate");
    	WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
    	Display display = wm.getDefaultDisplay();
    	width = display.getWidth(); 

		if (master != null) {
			copy(master);			//Getting back to a saved state.
		}

		super.onCreate(savedInstanceState);
		mGLView = new GLSurfaceView(getApplication());

		mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
			public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
				// Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
				// back to Pixelflinger on some device (read: Samsung I7500)
				int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
				EGLConfig[] configs = new EGLConfig[1];
				int[] result = new int[1];
				egl.eglChooseConfig(display, attributes, configs, 1, result);
				return configs[0];
			}
		});

		renderer = new MyRenderer();
		mGLView.setRenderer(renderer);
		setContentView(mGLView);
    }
    
    @Override
	protected void onPause() {
		super.onPause();
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	 void copy(Object src) {
		try {
			Logger.log("Copying data from master Activity!");
			Field[] fs = src.getClass().getDeclaredFields();
			for (Field f : fs) {
				f.setAccessible(true);
				f.set(this, f.get(src));		//Setting the fields of the new class to that of the old one.
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean onTouchEvent(MotionEvent me) {

		if (moved)
		{
			//prevxpos = xpos;
			xpos = ((me.getX()-(width/2))  / (width/2)*50);
			Log.i("TOuch",Float.toString(me.getX()));
			Log.i("TOuch",Float.toString(xpos));
			
			if (xpos>=17)
				xpos = 35;
			else if (xpos<=-17)
				xpos = -35;
			else 
				xpos = 0; 
			//Log.i("TOuch",Float.toString(xpos));
			//cube.translate(5,0,0);
			moved = false;
			for (int i=0; i<=Math.abs(xpos-prevxpos)/4;i++)
			{
				cube.translate(Math.signum(xpos-prevxpos)*4,0,0);
				//Log.i("Moving",Float.toString(i+prevxpos));
				rendered= false;
				while(!rendered);
			}
			prevxpos = xpos;
			moved = true;
			
			touched = true;
			return true;
		}

		try {
			Thread.sleep(15);
		} catch (Exception e) {
			// No need for this...
		}

		return super.onTouchEvent(me);
	}		

	 
	 
	protected boolean isFullscreenOpaque() {
		return true;
	}
	
	
	
	
	class MyRenderer implements GLSurfaceView.Renderer {
		
		private long time = System.currentTimeMillis();
		float rotangle=0;
		String textures[]={"glass0","carbonfi","carbonfi","black","grille2","lights","nodamage","tirea0","undercar","undercarriage","wheel","wheel0"};
		private boolean loop = false;
		

		public MyRenderer() {
			Config.maxPolysVisible = 500;
			Config.farPlane = 1500;
			Config.glTransparencyMul = 0.1f;
			Config.glTransparencyOffset = 0.1f;
			Config.useVBO=true;
			
			Texture.defaultToMipmapping(true);
			Texture.defaultTo4bpp(true);
		}

		public void onSurfaceChanged(GL10 gl, int w, int h) {
			if (fb != null) {
				fb.dispose();
			}
			fb = new FrameBuffer(gl, w, h);

			if (master == null) {

				world = new World();
				world.setAmbientLight(20, 20, 20);

				sun = new Light(world);
				sun.setIntensity(250, 250, 250);
				
				cube = loadModel(getResources().openRawResource(R.raw.lp670),10);
				//plane = Loader.loadSerializedObject(getResources().openRawResource(R.raw.serplane));
				plane = loadModel(getResources().openRawResource(R.raw.truck2), 50);
				plane2 = loadModel(getResources().openRawResource(R.raw.truck2), 50);
				//plane.rotateX(-1*(float) Math.PI / 4f);
				
				plane.setName("plane");
				plane2.setName("plane2");
				
				world.addObject(plane);
				//world.addObject(plane2);
				world.addObject(cube);
				
				plane2.strip();
				plane.strip();
				cube.strip();

				Camera cam = world.getCamera();
				cam.lookAt(cube.getCenter());
				cam.setPosition(0, 0, -100);
				cam.lookAt(cube.getTransformedCenter());
				
				cube.translate(0,50, -50);
				
				
				cube.align(cam);
				cube.rotateY((float)Math.PI);
				plane.align(cube);
				plane2.align(cube);
				plane2.translate(-5*4500*-3.604914E-4f,-5*4500*-7.883396E-4f,-5*4500*-0.99999964f);

				
				world.buildAllObjects();
				
				SimpleVector sv = new SimpleVector();
				sv.set(cube.getTransformedCenter());
				sv.y -= 100;
				sv.z -= 100;
				sun.setPosition(sv);
				MemoryHelper.compact();

				if (master == null) {
					Logger.log("Saving master Activity!");
					master = MyJpctActivity.this;
				}
			}
		}


		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		}

		public void onDrawFrame(GL10 gl) {
			
			
		//	plane.translate(5*-3.604914E-4f,5*-7.883396E-4f,5*-0.99999964f);
			if(cnt <= 4500){
				plane.translate(5*-3.604914E-4f,5*-7.883396E-4f,5*-0.99999964f);
				Log.i("plane1","Tranlate"+Float.toString(plane.getTranslation().y));
				
				if(cnt==4500&&loop)
					plane2.translate(-5*4500*-3.604914E-4f,-5*4500*-7.883396E-4f,-5*4500*-0.99999964f);
				else if(cnt==4500)
					plane2.translate(-5*4500*-3.604914E-4f,-5*4500*-7.883396E-4f,-5*4500*-0.99999964f);
				
			}
			else if (cnt>=4500 && cnt<=9000)
			{
				plane2.translate(5*-3.604914E-4f,5*-7.883396E-4f,5*-0.99999964f);
				Log.i("plane2","Tranlate"+Float.toString(plane2.getTranslation().y));
				if (cnt==9000)
				{
					plane.translate(-5*2*4500*-3.604914E-4f,-5*2*4500*-7.883396E-4f,-2*5*4500*-0.99999964f);;
					cnt = 0;
					loop = true;
				}
				
				
			}
				
			fb.clear(back);
			world.renderScene(fb);
			world.draw(fb);
			fb.display();
			Log.i("Draw Frame",Float.toString(cnt));
			cnt++;
			rendered = true;
			
			

			if (System.currentTimeMillis() - time >= 1000) {
				Logger.log(fps + "fps");
				fps = 0;
				time = System.currentTimeMillis();
			}
			fps++;
		}
		
		private Object3D loadModel(InputStream filename, float scale) {
	        Object3D[] model = Loader.load3DS(filename, scale);
	        Object3D o3d = new Object3D(0);
	        Object3D temp = null;
	        for (int i = 0; i < model.length; i++) {
	            temp = model[i];
	            temp.setCenter(SimpleVector.ORIGIN);
	            temp.rotateX((float)( -.5*Math.PI));
	            temp.rotateMesh();
	            temp.setRotationMatrix(new Matrix());
	            o3d = Object3D.mergeObjects(o3d, temp);
	            o3d.build();
	        }
	        return o3d;
	    }
		
		
	}
	
	
}








