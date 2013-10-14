package org.blackware.logicmaker;


import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public class Logicmaker extends Activity {

    public Datapath datapath;
    public Handler handler;
    private Logicmaker logicmaker;
    public TextView statustext;
    public Spinner placespinner;
    private String filenameString="/sdcard/circuit.xml";
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler=new Handler();
        logicmaker=this;
        
        datapath=new Datapath(this);
        datapath.setOnTouchListener(datapath);
 
        setupScreen();
    }
    
    private void setupScreen()
    {
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout buttonlayout=new LinearLayout(this);
        buttonlayout.setOrientation(LinearLayout.HORIZONTAL);
        setupButtons(buttonlayout);
        HorizontalScrollView sv=new HorizontalScrollView(this);
        buttonlayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
        sv.addView(buttonlayout);
        sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
        layout.addView(sv);
        statustext=new TextView(this);
        statustext.setHeight(25);
        statustext.setTextSize(18);
        statustext.setText("");
        statustext.setBackgroundColor(Color.BLACK);
        statustext.setTextColor(Color.WHITE);
        layout.addView(statustext);
        layout.addView(datapath);
        setContentView(layout);
    }
    
    public void onBackPressed()
    {
    	datapath.undo();
    }
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{
    	case R.id.Exit:
			System.exit(0);
    	case R.id.Help:
    		doHelp();
    	}
    	return true;
    }
    private void setupButtons(LinearLayout buttonlayout)
    {
       	buttonlayout.addView(makeButton("Help",new View.OnClickListener() {
    			public void onClick(View v) {
    				doHelp();
    			}
    		}));
    	buttonlayout.addView(makeButton("Exit",new View.OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(logicmaker);
				builder.setTitle("Are you sure?");
				builder.setCancelable(true);

				builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						System.exit(0);
						}
					});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						}
					});
				AlertDialog box = builder.create();
				box.show();	
			}
		}));
    	buttonlayout.addView(makeButton("Save",new View.OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(logicmaker);
				builder.setTitle("Save to sdcard");
				builder.setCancelable(true);
				final EditText filename=new EditText(logicmaker);
				filename.setText(filenameString);
				builder.setView(filename);

				builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						datapath.dosave(filename.getText().toString());
						filenameString=filename.getText().toString();
						}
					});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						}
					});
				AlertDialog box = builder.create();
				box.show();	
			}
		}));
    	buttonlayout.addView(makeButton("Load",new View.OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(logicmaker);
				builder.setTitle("Load from sdcard");
				builder.setCancelable(true);
				final EditText filename=new EditText(logicmaker);
				filename.setText(filenameString);
				builder.setView(filename);

				builder.setPositiveButton("Load", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						datapath.doload(filename.getText().toString());
						filenameString=filename.getText().toString();
						}
					});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						}
					});
				AlertDialog box = builder.create();
				box.show();	
			}
		}));
    	buttonlayout.addView(makeButton("Unselect",new View.OnClickListener() {
			public void onClick(View v) {
				datapath.unselectAll();
				datapath.clearAction();
			}
		}));
/*    	buttonlayout.addView(makeButton("Undo",new View.OnClickListener() {
			public void onClick(View v) {
				datapath.undo();
			}
		}));*/
     	buttonlayout.addView(makeButton("Delete",new View.OnClickListener() {
			public void onClick(View v) {
				datapath.delete();
			}
		}));
     	buttonlayout.addView(makeButton("Select",new View.OnClickListener() {
			public void onClick(View v) {
				datapath.setStatusLabel("Drag and release to select blocks");
				datapath.action="massselect";
			}
		}));
     	buttonlayout.addView(makeButton("Duplicate",new View.OnClickListener() {
			public void onClick(View v) {
				if (datapath.isSimulating)
				{
					datapath.setStatusLabel("Turn off simulation before placing");
					return;
				}
				datapath.action="duplicate";
				datapath.setStatusLabel("Click on the top left of where you want to paste");
			}
		}));
       	buttonlayout.addView(makeButton("Verify",new View.OnClickListener() {
    			public void onClick(View v) {
    				datapath.verify();
    			}
    		}));
       	buttonlayout.addView(makeButton("Simulate",new View.OnClickListener() {
    			public void onClick(View v) {
					if (datapath.isSimulating)
					{
						datapath.isSimulating=false;
						datapath.unselectAll();
						datapath.postInvalidate();
						datapath.setStatusLabel("Simulation stopped");
					}
					else
					{
						datapath.unselectAll();
						datapath.simulate();
						if (datapath.isSimulating)
							datapath.setStatusLabel("Simulation started");
					}
   			}
    		}));
       	TextView text=new TextView(this);
       	text.setText("      Place a new: ");
       	text.setTextSize(20);
       	buttonlayout.addView(text);
       	buttonlayout.addView(makeButton("Bus",new View.OnClickListener() {
    			public void onClick(View v) {
    				if (datapath.isSimulating)
    				{
    					datapath.setStatusLabel("Turn off simulation before placing");
    					return;
    				}
    				
    				datapath.action="place";
    				datapath.component="bus";
    				datapath.tempbus1=null;
    				datapath.setStatusLabel("Press the mouse on a block and drag to draw a bus");
   			}
    		}));
       	
       	placespinner=new Spinner(this);
       	ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
       	        R.array.blocktypes, android.R.layout.simple_spinner_item);
       	// Specify the layout to use when the list of choices appears
       	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
       	// Apply the adapter to the spinner
       	placespinner.setAdapter(adapter);
       	placespinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (datapath.isSimulating)
				{
					datapath.setStatusLabel("Turn off simulation before placing");
					return;
				}
				if (arg2==0)
					return;
				datapath.action="place";
				datapath.component=(String)arg0.getItemAtPosition(arg2);
				datapath.setStatusLabel("Touch to place a "+datapath.component);
				if (arg2>=14)
					datapath.component="combinational-"+datapath.component;	
			}
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
       	buttonlayout.addView(placespinner);
       }
    
    private Button makeButton(String text, View.OnClickListener listener)
    {
    	Button b=new Button(this);
    	b.setText(text);
    	b.setBackgroundColor(Color.argb(255, 0, 0, 100));
    	b.setTextColor(Color.WHITE);
    	b.setTextSize(17);
    	b.setOnClickListener(listener);
    	return b;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_logicmaker, menu);
        return true;
    }

    public void onConfigurationChanged(Configuration newConfig) 
    {
    	  super.onConfigurationChanged(newConfig);
    }
    
    public void doHelp()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(logicmaker);
		builder.setTitle("Help");
		builder.setCancelable(true);

		HelpView helpview=new HelpView(this);
		builder.setView(helpview.scroll);
//		builder.setMessage("Written by Michael Black, 12/2012");
		
		builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				}
			});
		AlertDialog box = builder.create();
		box.show();	    	
    }
}
