package com.example.mapnav;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.mapnav.R;
import com.example.mapnav.R.id;
import com.example.mapnav.R.layout;
import com.example.mapnav.R.menu;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity{
	
	static LineGraphView graph;
	static int stepCount, stepCountA, stepCountN, stepCountNE, stepCountE, stepCountSE;
	static float[] mRotation, mGeomagnetic, mGravity, mOrientation;
	static float displacementN, displacementE;
	static float oValues, oValueOffset, rawOValue;
	static final int offset = 1;
	static NavigationalMap nm;
	static MapView mv;
	static PointF pointOrigin, pointDest, pointUser;
	static final float stepSize = 0.66f;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
            stepCount = 0; 
        }
        else {
        	stepCount = savedInstanceState.getInt("Step_Count");
        }

        // Initialize graph
		graph = new LineGraphView(getApplicationContext(), 100, Arrays.asList("x","y","z"));
		graph.setVisibility(View.VISIBLE);
		
		// Get display size for map
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		
		// Initialize navigational map and map view
		mv = new MapView (getApplicationContext(), size.x, 650, 28, 28);
		registerForContextMenu(mv);
		nm = MapLoader.loadMap(getExternalFilesDir(null),"E2-3344-Lab-room.svg");
		mv.setMap(nm);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	mv.onCreateContextMenu(menu, v, menuInfo);
    }
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	return super.onContextItemSelected (item) || mv.onContextItemSelected(item);
    }
    
    @Override
	public void onSaveInstanceState(Bundle outState) {
    	outState.putInt("Step_Count", stepCount);
		super.onSaveInstanceState(outState);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    
    public static class PlaceholderFragment extends Fragment {
    	
    	public PlaceholderFragment() {
    	}
    	
    	/**
    	 * @param inflater
    	 * @param container
    	 * @param savedInstanceState
    	 * @return
    	 */
    	@Override
    	public View onCreateView(LayoutInflater inflater, ViewGroup container,
    	Bundle savedInstanceState) {
    		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
    		
    		// Title
    		TextView tv = (TextView) rootView.findViewById(R.id.label1);
    		tv.setText("Navigator");
    		tv.setTextSize(24f);
    		
    		// Lab info
    		TextView title = new TextView(rootView.getContext());
    		title.setText("Lab 4 | Section 201 | Group 04");
    		
    		// Accelerometer information
    		TextView linearAccelerometer = new TextView(rootView.getContext());
    		linearAccelerometer.setTypeface(null, Typeface.BOLD);
       		
    		// Button to reset displacement
    		Button resetButton = new Button(rootView.getContext());
    		resetButton.setText("Reset Count");
    		
    		resetButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    stepCount = stepCountN = stepCountNE = stepCountE = stepCountSE = 0;
                    displacementN = displacementE = 0.0f;
                }
            });
    		
    		// Button to calibrate North
    		final Button calibrateButton = new Button(rootView.getContext());
    		calibrateButton.setText("Calibrate North");
    		
    		calibrateButton.setOnClickListener(new View.OnClickListener() {
    			public void onClick(View view) {
    				oValueOffset = rawOValue;
    			}
    		});
    		
    		// Create layout and add objects
       		LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.layout1);
    		layout.setOrientation(LinearLayout.VERTICAL);
    		
    		layout.addView(title);
    		layout.addView(linearAccelerometer);
    		layout.addView(mv);
    		layout.addView(resetButton);
    		layout.addView(calibrateButton);
    		//layout.addView(graph);
    		
    		// Register sensors
    		SensorManager sensorManager = (SensorManager) rootView.getContext().getSystemService(SENSOR_SERVICE);
    		
    		Sensor linearAccelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    		Sensor rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    		Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    		Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    		
    		SensorEventListener linearAccelerometerSensorListener = new AccelerometerEventListener(linearAccelerometer);
    		SensorEventListener rotationSensorListener = new RotationEventListener();
    		SensorEventListener orientationSensorListener = new OrientationListener();
    		SensorEventListener magneticSensorListener = new MagneticFieldListener();
    		
    		sensorManager.registerListener(rotationSensorListener, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST);
    		sensorManager.registerListener(linearAccelerometerSensorListener, linearAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
    		sensorManager.registerListener(orientationSensorListener, orientationSensor, SensorManager.SENSOR_DELAY_UI);
    		sensorManager.registerListener(magneticSensorListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
    		
    		// Register position listener
    		Position myPositionListener = new Position();
    		mv.addListener(myPositionListener);
    		
    		return rootView;
    	}
    	
    	class RotationEventListener implements SensorEventListener {
    		
			@Override
			public void onSensorChanged(SensorEvent se) {
				if (se.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
					mRotation = se.values;
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {

			}
    	}
    	
    	class OrientationListener implements SensorEventListener {

			@Override
			public void onSensorChanged(SensorEvent event) {
				if(event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
					// Determine heading
					mOrientation = event.values;
					
					rawOValue = event.values[0];
					oValues = (event.values[0] - oValueOffset) % 360;
					
					if(oValues < 0) {
						oValues += 360;
					}
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				
			}
    		
    	}
    	
    	class MagneticFieldListener implements SensorEventListener {
    		
			@Override
			public void onAccuracyChanged(Sensor arg0, int arg1) {
				
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
					// Store geomagnetic values
					mGeomagnetic = event.values;
				}
			}
    	}
    	
    	class AccelerometerEventListener implements SensorEventListener {
        	TextView output; // Output textview
        	float[] store;
        	float previousOutputz;
        	boolean highPoint, lowPoint, endPoint;
        	double z;
        	final float alpha = 0.35f;        	
        	
        	public AccelerometerEventListener(TextView outputView)	{
        		output = outputView;
            	store = new float [3];
        	}
        	
        	public void onAccuracyChanged(Sensor s, int i){
        		
        	}
        	
        	public void onSensorChanged(SensorEvent se){
        		if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
        	        mGravity = se.values;
        			store [2] = store [1];
        			store [1] = store [0];
        			store [0] = se.values[2] - offset;
  
        			// Smooth out acceleration
        			z = alpha * store[0] + (1 - alpha) * previousOutputz;
        			previousOutputz = (float) z;
        			
        			float[] myArray = { (float) z };
        			
        			graph.addPoint(myArray);
        			    
        			// State machine to detect footsteps
        			if ( z > 1 && Math.abs(se.values[1]) > 1.5 && Math.abs(mRotation[0]) < 2.5 && Math.abs(mRotation[1]) < 2.5) 
        			{
        					highPoint = true;        					
        			}
        			else if ( z < -.5 && highPoint && Math.abs(mRotation[0]) < 2.5 && Math.abs(mRotation[1]) < 2.5) 
        			{
        					lowPoint = true;
        			}
        			else if ( z < 1 && z > -.5 && highPoint && lowPoint && Math.abs(mRotation[0]) < 2.5 && Math.abs(mRotation[1]) < 2.5)
        			{
        					endPoint = true;
        			}

        			if( Math.abs(mRotation[0]) > 5 || Math.abs(mRotation[1]) > 5) 
        			{
        				highPoint = lowPoint = endPoint=  false;
        			}
        			
        			if (highPoint && lowPoint && endPoint )
        			{
        				highPoint = lowPoint = endPoint = false;
        				
        				/*8 quadrants instead of 4
    					if (oValues >=  337.5 || oValues < 22.5 )
    						stepCountN++;
    					else if(oValues >= 22.5 && oValues < 67.5)
    						stepCountNE++;
    					else if(oValues >= 67.5 && oValues < 112.5)
    						stepCountE++;
    					else if(oValues >= 112.5 && oValues < 157.5)
    						stepCountSE++;
    					else if(oValues >= 157.5 && oValues < 202.5)
    						stepCountN--;
    					else if(oValues >= 202.5 && oValues < 247.5)
    						stepCountNE--;
    					else if(oValues >= 247.5 && oValues < 292.5)
    						stepCountE--;
    					else if(oValues >= 292.5 && oValues < 337.5)
    						stepCountSE--;*/
        				
        				// Detect if stepping through wall
        				Integer stepCountNTemp = new Integer(stepCountN);
        				Integer stepCountETemp = new Integer(stepCountE);
        				
        				if(oValues >= 315 || oValues < 45)
        					stepCountNTemp++;
        				else if(oValues >= 45 && oValues < 135)
        					stepCountETemp++;
        				else if(oValues >= 135 && oValues < 225)
        					stepCountNTemp--;
        				else if(oValues >= 225 && oValues < 315)
        					stepCountETemp--;
        				
        				if(nm.calculateIntersections(pointUser, new PointF(pointOrigin.x + ( stepCountETemp / stepSize ), pointOrigin.y - ( stepCountNTemp / stepSize ))).isEmpty()){
        					stepCountN = new Integer(stepCountNTemp);
        					stepCountE = new Integer(stepCountETemp);
            				stepCount++;
        				}
        			}
        			
        			// Calculate displacement
        			displacementN = (float) stepCountN;
        			displacementE = (float) stepCountE;
        			
        			//Calculate displacement when using 8 quadrants
        			//displacementN = (float) (stepCountN + Math.sqrt(0.5)*stepCountNE - Math.sqrt(0.5)*stepCountSE);
					//displacementE = (float) (stepCountE + Math.sqrt(0.5)*stepCountNE + Math.sqrt(0.5)*stepCountSE);
        			
        			// Calculate user position
					pointUser = new PointF(displacementE / stepSize + pointOrigin.x, -displacementN / stepSize + pointOrigin.y);
					mv.setUserPoint(pointUser);
					
					//Output information
					String value = "";
					double distance = VectorUtils.distance(pointUser, pointDest);
					String orientation = "";
					
   					if (oValues >=  337.5 || oValues < 22.5 )
						orientation = "N";
					else if(oValues >= 22.5 && oValues < 67.5)
						orientation = "NE";
					else if(oValues >= 67.5 && oValues < 112.5)
						orientation = "E";
					else if(oValues >= 112.5 && oValues < 157.5)
						orientation = "SE";
					else if(oValues >= 157.5 && oValues < 202.5)
						orientation = "S";
					else if(oValues >= 202.5 && oValues < 247.5)
						orientation = "SW";
					else if(oValues >= 247.5 && oValues < 292.5)
						orientation = "W";
					else if(oValues >= 292.5 && oValues < 337.5)
						orientation = "NW";
					
					value = String.format("Step Count: %d\n" +
							"Displacement North: %.3f\n" +
							"Displacement East: %.3f\n" +
							"Orientation: %s\n" +
							"Heading: %.3f\n" +
							"Distance: %.3f\n",
							stepCount, displacementN, displacementE, orientation, oValues, distance);
					
					// Calculate path to destinaion
					PointF pointHead = new PointF(pointUser.x, pointUser.y);
					PointF pointNext = pointDest;
					List<PointF> myList = new ArrayList<PointF>();
					myList.add(pointUser);
					
					//Direct line of sight
					if(nm.calculateIntersections(pointUser, pointDest).isEmpty()){
						pointNext = new PointF(pointDest.x, pointDest.y);
						myList.add(pointNext);
					//Check if there is a perpendicular path
					}else if(nm.calculateIntersections(pointUser, new PointF(pointUser.x, pointDest.y)).isEmpty()
							&& nm.calculateIntersections(pointDest, new PointF(pointUser.x, pointDest.y)).isEmpty()){
						pointNext = new PointF(pointUser.x, pointDest.y);
						myList.add(pointNext);
						myList.add(pointDest);
					}else if(nm.calculateIntersections(pointUser, new PointF(pointDest.x, pointUser.y)).isEmpty()
							&& nm.calculateIntersections(pointDest, new PointF(pointDest.x, pointUser.y)).isEmpty()){
						pointNext = new PointF(pointDest.x, pointUser.y);
						myList.add(pointNext);
						myList.add(pointDest);
					//Find nearest wall
					}else{
						List<InterceptPoint> myIntercepts = new ArrayList<InterceptPoint>();
						myIntercepts = nm.calculateIntersections(pointUser, pointDest);
						
						LineSegment wall = myIntercepts.get(0).getLine();
						
						float[] difference;// = VectorUtils.difference(wall.end, myIntercepts.get(0).getPoint());
						float[] uV = wall.findUnitVector();
						
						// Determine which point to use as next point
						if(pointUser.x > pointDest.x){
							difference = VectorUtils.difference(wall.start, myIntercepts.get(0).getPoint());
							
							if(wall.start.y > pointUser.y){
								pointNext = new PointF(pointUser.x, myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.1f);
							}else{
								pointNext = new PointF(myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.2f, myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.2f);
							}
						}else{
							difference = VectorUtils.difference(wall.end, myIntercepts.get(0).getPoint());
							
							if(wall.end.y > pointUser.y){
								pointNext = new PointF(pointUser.x, myIntercepts.get(0).getPoint().y + uV[1]*difference[1]*1.1f);
							}else{
								pointNext = new PointF(myIntercepts.get(0).getPoint().y + uV[1]*difference[1]*1.2f, myIntercepts.get(0).getPoint().y - uV[1]*difference[1]*1.2f);
							}
						}
						
						// Add the next point
						myList.add(pointNext);
						PointF a = new PointF(pointNext.x, pointNext.y);
						
						PointF b;

						// Get perpendicular path from next path to destination
						if(nm.calculateIntersections(a, new PointF(a.x, pointDest.y)).isEmpty()
								&& nm.calculateIntersections(pointDest, new PointF(a.x, pointDest.y)).isEmpty()){
							b = new PointF(a.x, pointDest.y);
							myList.add(b);
						}else if(nm.calculateIntersections(a, new PointF(pointDest.x, a.y)).isEmpty()
								&& nm.calculateIntersections(pointDest, new PointF(pointDest.x, a.y)).isEmpty()){
							b = new PointF(pointDest.x, a.y);
							myList.add(b);
						}
							
						myList.add(pointDest);
					}
					
					// Get angle to next point
   					if (oValues >=  337.5 || oValues < 22.5 ){
						pointHead.y -= 1;
   					}else if(oValues >= 22.5 && oValues < 67.5){
						pointHead.y -= 1;
						pointHead.x += 1;
   					}else if(oValues >= 67.5 && oValues < 112.5){
						pointHead.x += 1;
   					}else if(oValues >= 112.5 && oValues < 157.5){
						pointHead.y += 1;
   						pointHead.x += 1;
   					}else if(oValues >= 157.5 && oValues < 202.5){
						pointHead.y += 1;
   					}else if(oValues >= 202.5 && oValues < 247.5){
						pointHead.y += 1;
						pointHead.x -= 1;
   					}else if(oValues >= 247.5 && oValues < 292.5){
						pointHead.x -= 1;
        			}else if(oValues >= 292.5 && oValues < 337.5){
						pointHead.y -= 1;
						pointHead.x -= 1;
        			}
        	
					float angle = (float) ( VectorUtils.angleBetween(pointUser, pointHead, pointNext) * 180 / Math.PI);
				
					mv.setUserPath(myList);
					
					// Check distance from destination
					if(distance < 1f){
						value += "Destination Reached!\n";
        			}else if(Math.abs(angle) > 25){
        				if(angle > 0)
        					value += String.format("Turn right %.2f degrees\n", angle);
        				else
        					value += String.format("Turn left %.2f degrees\n", Math.abs(angle));
					}else{
						value += "Walk forward\n";
					}

					output.setText(value);
        		}
        	}
        }
    	
    	class Position implements PositionListener
    	{	
    		public Position(){
    			pointOrigin = pointDest = new PointF(0,0);
    		}
    		
			@Override
			public void originChanged(MapView source, PointF loc) {
				//Set origin point
				source.setUserPoint(loc);
				pointOrigin = loc;
				stepCount = stepCountN = stepCountNE = stepCountE = stepCountSE = 0;
                displacementN = displacementE = 0.0f;
			}

			@Override
			public void destinationChanged(MapView source, PointF dest) {
				//Set destination point
				source.setDestinationPoint(dest);
				pointDest = dest;
			}
    	}
	}
}


